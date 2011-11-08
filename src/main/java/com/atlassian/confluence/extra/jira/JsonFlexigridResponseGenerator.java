package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JsonFlexigridResponseGenerator implements FlexigridResponseGenerator
{   
    private static final Logger log = Logger.getLogger(JsonFlexigridResponseGenerator.class);

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private final I18NBeanFactory i18NBeanFactory;

    private final JiraIssuesManager jiraIssuesManager;

    private final JiraIssuesColumnManager jiraIssuesColumnManager;
    
    private static final String mailDateFormat = "EEE, d MMM yyyy HH:mm:ss Z";
    
    private Locale userLocale;

    public JsonFlexigridResponseGenerator(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, JiraIssuesColumnManager jiraIssuesColumnManager)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.jiraIssuesManager = jiraIssuesManager;
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }

    public boolean handles(JiraIssuesManager.Channel channel)
    {
        return true; /* Handle everything for now*/
    }

    private String getText(String i18nKey)
    {
        return i18NBeanFactory.getI18NBean().getText(i18nKey);
    }

    private String trustedStatusToMessage(TrustedConnectionStatus trustedConnectionStatus)
    {
        if (trustedConnectionStatus != null)
        {
            if (!trustedConnectionStatus.isTrustSupported())
                return getText("jiraissues.server.trust.unsupported");
            else if (trustedConnectionStatus.isTrustedConnectionError())
            {
                if (!trustedConnectionStatus.isAppRecognized())
                {
                    String linkText = getText("jiraissues.server.trust.not.established");
                    String anonymousWarning = getText("jiraissues.anonymous.results.warning");
                    return "<a href=\"http://www.atlassian.com/software/jira/docs/latest/trusted_applications.html\">" + linkText + "</a> " + anonymousWarning;
                }
                else if (!trustedConnectionStatus.isUserRecognized())
                    return getText("jiraissues.server.user.not.recognised");
                else
                {
                    List trustedErrorsList = trustedConnectionStatus.getTrustedConnectionErrors();
                    if (!trustedErrorsList.isEmpty())
                    {
                        StringBuffer errors = new StringBuffer();
                        errors.append(getText("jiraissues.server.errors.reported"));
                        Iterator trustedErrorsListIterator = trustedErrorsList.iterator();
                        errors.append("<ul>");
                        while (trustedErrorsListIterator.hasNext())
                        {
                            errors.append("<li>");
                            errors.append(trustedErrorsListIterator.next().toString());
                            errors.append("</li>");
                        }
                        errors.append("</ul>");
                        return errors.toString();
                    }
                }

            }
        }

        return null;
    }

    private String createImageTag(String iconUrl, String altText)
    {
        StringBuilder imageTagBuilder = new StringBuilder();

        if (StringUtils.isNotBlank(iconUrl))
            imageTagBuilder.append("<img src=\"").append(iconUrl).append("\" alt=\"").append(altText).append("\"/>");

        return imageTagBuilder.toString();
    }

    protected DateFormat getDateValueFormat()
    {
        return new SimpleDateFormat(DATE_VALUE_FORMAT, getUserLocale());
    }

    protected String getElementJson(Element itemElement, Collection<String> columnNames, Map<String, String> columnMap) throws Exception
    {
        Element keyElement = itemElement.getChild("key");
        String key = null != keyElement ? keyElement.getValue() : "";
        String link = itemElement.getChild("link").getValue();
        StringBuilder jsonIssueElementBuilder = new StringBuilder();

        jsonIssueElementBuilder.append("{id:'").append(key).append("',cell:[");

        for (Iterator<String> columnNamesIterator = columnNames.iterator(); columnNamesIterator.hasNext();)
        {
            String columnName = columnNamesIterator.next();

            if (jiraIssuesColumnManager.isBuiltInColumnMultivalue(columnName))
            {
                appendMultivalueBuiltinColumn(itemElement, columnName, jsonIssueElementBuilder);
            }
            else
            {
                Element child = itemElement.getChild(columnName);
                
                String value = null != child ? child.getValue() : "";
                if (!columnName.equalsIgnoreCase("created") && !columnName.equalsIgnoreCase("updated") && !columnName.equalsIgnoreCase("due"))
                {
                	value = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(value));
                }

                if (columnName.equalsIgnoreCase("type"))
                    jsonIssueElementBuilder.append("'<a href=\"").append(link).append("\" >").append(createImageTag(xmlXformer.findIconUrl(child), value)).append("</a>'");
                else if (columnName.equalsIgnoreCase("key") || columnName.equals("summary"))
                    jsonIssueElementBuilder.append("'<a href=\"").append(link).append("\" >").append(value).append("</a>'");
                else if (columnName.equalsIgnoreCase("priority"))
                {
                    jsonIssueElementBuilder.append("'").append(createImageTag(xmlXformer.findIconUrl(child), value)).append("'");
                }
                else if (columnName.equalsIgnoreCase("status"))
                {
                    appendIssueStatus(child, value, jsonIssueElementBuilder);
                }
                else if (columnName.equalsIgnoreCase("created") || columnName.equalsIgnoreCase("updated") || columnName.equalsIgnoreCase("due"))
                {
                    appendIssueDate(value, jsonIssueElementBuilder);
                }
                else if (columnName.equals("description"))
                {
                    Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
                    jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(fieldValue.getValue())).append("'");
                }
                else
                {
                    appendCustomField(itemElement, columnMap, columnName, jsonIssueElementBuilder);
                }
            }

            // no comma after last item in row, but closing stuff instead
            if (columnNamesIterator.hasNext())
                jsonIssueElementBuilder.append(',');
            else
                jsonIssueElementBuilder.append("]}\n");
        }

        return jsonIssueElementBuilder.toString();
    }

    private void appendCustomField(Element itemElement, Map<String, String> columnMap, String columnName, StringBuilder jsonIssueElementBuilder)
    {
        Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
        String fieldValueText = fieldValue.getValue();

        /* Try to interpret value as date (CONFJIRA-136) */
        try
        {
            /* While I'd expect the method to throw ParseException if the value is not a date, sometimes it just returns null?! */
            if (StringUtils.isNotBlank(fieldValueText))
            {
                Date customFieldValueDate = GeneralUtil.convertMailFormatDate(fieldValueText);
                if (null != customFieldValueDate)
                    jsonIssueElementBuilder.append("'").append(getDateValueFormat().format(customFieldValueDate)).append("'");
                else
                    appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder);
            }
            else
            {
                appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder);
            }
        }
        catch (ParseException pe)
        {
            log.debug("Unable to parse " + fieldValue.getText() + " into a date", pe);
            appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder);
        }
    }

    private void appendCustomFieldUnformatted(String fieldValueText, StringBuilder jsonIssueElementBuilder)
    {
        jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(fieldValueText))).append("'");
    }

    private void appendIssueDate(String value, StringBuilder jsonIssueElementBuilder)
            throws ParseException
    {
        if (StringUtils.isNotEmpty(value))
        {
            DateFormat dateFormatter = getDateValueFormat();
            DateFormat mailFormatDate = new SimpleDateFormat(mailDateFormat, getUserLocale());
            
            jsonIssueElementBuilder.append("'").append(dateFormatter.format(mailFormatDate.parse(value))).append("'");
        }
        else
        {
            jsonIssueElementBuilder.append("''");
        }
    }

    private void appendIssueStatus(Element child, String value, StringBuilder jsonIssueElementBuilder)
    {
        String imgTag = createImageTag(xmlXformer.findIconUrl(child), value);
        jsonIssueElementBuilder.append("'");
        if (StringUtils.isNotBlank(imgTag))
        {
            jsonIssueElementBuilder.append(imgTag).append(" ");
        }
        jsonIssueElementBuilder.append(value).append("'");
    }

    private void appendMultivalueBuiltinColumn(Element itemElement, String columnName, StringBuilder jsonIssueElementBuilder)
    {
        jsonIssueElementBuilder.append("'");
        jsonIssueElementBuilder.append(StringEscapeUtils.escapeJavaScript(xmlXformer.collapseMultiple(itemElement, columnName).getValue()));
        jsonIssueElementBuilder.append("'");
    }

    private String getOutputAsString(String url, JiraIssuesManager.Channel jiraResponseChannel, Collection<String> columnNames, int requestedPage, boolean showCount)
            throws Exception
    {
        Element jiraResponseElement = jiraResponseChannel.getChannelElement();

        Map<String, String> columnMap = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        List<Element> itemElements = jiraResponseChannel.getChannelElement().getChildren("item");
        String language = jiraResponseChannel.getChannelElement().getChildText("language");
        int languageSubstring = language.indexOf("-");
        
        if (StringUtils.isNotEmpty(language))
        {
        	setUserLocale(new Locale(language.substring(0, languageSubstring), language.substring(languageSubstring+1, language.length())));
        }
        else
        {
        	setUserLocale(Locale.getDefault());
        }
        
        // if totalItems is not present in the XML, we are dealing with an older version of jira
        // in that case, consider the number of items retrieved to be the same as the overall total items
        Element totalItemsElement = jiraResponseElement.getChild("issue");
        String count = totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : String.valueOf(itemElements.size());

        if (showCount)
            return count;

        StringBuilder jiraResponseJsonBuilder = new StringBuilder();

        String trustedMessage = trustedStatusToMessage(jiraResponseChannel.getTrustedConnectionStatus());
        if (StringUtils.isNotBlank(trustedMessage))
        {
            trustedMessage = jiraResponseJsonBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(trustedMessage)).append("'").toString();
            jiraResponseJsonBuilder.setLength(0);
        }

        jiraResponseJsonBuilder.append("{\npage: ").append(requestedPage).append(",\n")
                .append("total: ").append(count).append(",\n")
                .append("trustedMessage: ").append(trustedMessage).append(",\n")
                .append("rows: [\n");

        for (Iterator<Element> itemIterator = itemElements.iterator(); itemIterator.hasNext();)
        {
            jiraResponseJsonBuilder.append(getElementJson(itemIterator.next(), columnNames, columnMap));

            // no comma after last row
            if (itemIterator.hasNext())
                jiraResponseJsonBuilder.append(',');

            jiraResponseJsonBuilder.append('\n');
        }

        jiraResponseJsonBuilder.append("]}");

        // persist the map of column names to bandana for later use
        jiraIssuesManager.setColumnMap(url, columnMap);
        return jiraResponseJsonBuilder.toString();
    }

    public String generate(JiraIssuesManager.Channel jiraResponseChannel, Collection<String> columnNames, int requestedPage, boolean showCount) throws IOException
    {
        try
        {
            return getOutputAsString(jiraResponseChannel.getSourceUrl(), jiraResponseChannel, columnNames, requestedPage, showCount);
        }
        catch (Exception e)
        {
            IOException ioe = new IOException("Unable to generate JSON output");
            ioe.initCause(e);
            throw ioe;
        }
    }

	public void setUserLocale(Locale userLocale)
	{
		this.userLocale = userLocale;
	}

	public Locale getUserLocale()
	{
		return userLocale;
	}
	
}
