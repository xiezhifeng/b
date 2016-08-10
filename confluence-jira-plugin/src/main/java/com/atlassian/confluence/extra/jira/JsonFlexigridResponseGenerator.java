package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
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

    private final JiraIssuesDateFormatter jiraIssuesDateFormatter;

    private static final String mailDateFormat = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final String DUE_DATE_CONSTANT = "due";
    
    private Locale userLocale;

    public JsonFlexigridResponseGenerator(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, JiraIssuesColumnManager jiraIssuesColumnManager, JiraIssuesDateFormatter jiraIssuesDateFormatter)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.jiraIssuesManager = jiraIssuesManager;
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesDateFormatter = jiraIssuesDateFormatter;
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

    protected String getElementJson(Element itemElement, Collection<String> columnNames, Map<String, String> columnMap, boolean fromApplink) throws Exception
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
                	value = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(value));
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
                else if (columnName.equalsIgnoreCase("created") || columnName.equalsIgnoreCase("updated"))
                {
                    appendIssueDate(value, jsonIssueElementBuilder);
                }
                else if(columnName.equalsIgnoreCase(DUE_DATE_CONSTANT))
                {
                    appendDueDate(value, jsonIssueElementBuilder, fromApplink);
                }
                else if (columnName.equals("description"))
                {
                    Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
                    // If the targetted JIRA is not applinked, we need to escape the description
                    String description = fieldValue.getValue();
                    if (!fromApplink)
                    {
                        description = GeneralUtil.htmlEncode(description);
                    }
                    // Then we escape it for the json response
                    jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeEcmaScript(description)).append("'");
                }
                else if (jiraIssuesColumnManager.isColumnBuiltIn(columnName))
                {
                    Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
                    jsonIssueElementBuilder.append("'").append(GeneralUtil.htmlEncode(fieldValue.getValue())).append("'");
                }
                else
                {
                    appendCustomField(itemElement, columnMap, columnName, jsonIssueElementBuilder, fromApplink);
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

    private void appendCustomField(Element itemElement, Map<String, String> columnMap, String columnName, StringBuilder jsonIssueElementBuilder, boolean fromApplink)
    {
        Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
        String fieldValueText = fieldValue.getValue();

        /* Try to interpret value as date (CONFJIRA-136) */

        if (StringUtils.isNotBlank(fieldValueText))
        {
            String date =  jiraIssuesDateFormatter.reformatDateInUserLocale(fieldValueText, getUserLocale(), DATE_VALUE_FORMAT);
            if (StringUtils.isNotEmpty(date))
            {
                jsonIssueElementBuilder.append("'").append(date).append("'");
            }
            else
            {
                try
                {
                    String convertedDate = jiraIssuesDateFormatter.reformatDateInDefaultLocale(fieldValueText, getUserLocale(), DATE_VALUE_FORMAT);
                    if(StringUtils.isNotBlank(convertedDate))
                        jsonIssueElementBuilder.append("'").append(convertedDate).append("'");
                    else
                        appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder, fromApplink);
                }
                catch(ParseException pe)
                {
                    log.debug("Unable to parse " + fieldValue.getText() + " into a date", pe);
                    appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder, fromApplink);
                }
            }
        }
        else
        {
            appendCustomFieldUnformatted(fieldValueText, jsonIssueElementBuilder, fromApplink);
        }
    }

    private void appendCustomFieldUnformatted(String fieldValueText, StringBuilder jsonIssueElementBuilder, boolean fromAppLink)
    {
        if (!fromAppLink)
        {
            fieldValueText = StringEscapeUtils.escapeHtml4(fieldValueText);
        }
        jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeEcmaScript(fieldValueText)).append("'");
    }

    private void appendDueDate(String value, StringBuilder jsonIssueElementBuilder, boolean fromApplink) throws ParseException
    {
        if (StringUtils.isNotEmpty(value))
        {
            /* Due date format from JIRA changed a few times. Try to parse the date with different locale (CONFJIRA-214)*/
            String dueDate = jiraIssuesDateFormatter.reformatDateInUserLocale(value, getUserLocale(), DATE_VALUE_FORMAT);
            if (StringUtils.isNotEmpty(dueDate))
            {
                jsonIssueElementBuilder.append("'").append(dueDate).append("'");
            }
            else
            {
                try
                {
                    String convertedDate = jiraIssuesDateFormatter.reformatDateInDefaultLocale(value, getUserLocale(), DATE_VALUE_FORMAT);
                    if(StringUtils.isNotBlank(convertedDate))
                        jsonIssueElementBuilder.append("'").append(convertedDate).append("'");
                    else
                        appendCustomFieldUnformatted(value, jsonIssueElementBuilder, fromApplink);

                }
                catch(ParseException pe)
                {
                    appendCustomFieldUnformatted(value, jsonIssueElementBuilder, fromApplink);
                }
            }
        }
        else
        {
            jsonIssueElementBuilder.append("''");
        }
    }

    /**
     * <a href="https://studio.plugins.atlassian.com/browse/CONFJIRA-214">CONFJIRA-214</a>
     */
    private void appendIssueDate(String value, StringBuilder jsonIssueElementBuilder) throws ParseException
    {
        if (StringUtils.isNotEmpty(value))
        {
            DateFormat mailFormatDate = new SimpleDateFormat(mailDateFormat, getUserLocale());
            jsonIssueElementBuilder.append("'").append(getDateValueFormat().format(mailFormatDate.parse(value))).append("'");
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
        String fieldValue = StringEscapeUtils.escapeEcmaScript(xmlXformer.collapseMultiple(itemElement, columnName).getValue());
        jsonIssueElementBuilder.append(GeneralUtil.htmlEncode(fieldValue));
        jsonIssueElementBuilder.append("'");
    }

    private String getOutputAsString(String url, JiraIssuesManager.Channel jiraResponseChannel, Collection<String> columnNames, int requestedPage, boolean showCount,
            boolean fromApplink) throws Exception
    {
        Element jiraResponseElement = jiraResponseChannel.getChannelElement();
        
        Map<String, String> columnMap = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        List<Element> itemElements = jiraResponseElement.getChildren("item");
        String language = jiraResponseElement.getChildText("language");

        if (StringUtils.isNotEmpty(language))
        {
            if (language.contains("-"))
        	    setUserLocale(new Locale(language.substring(0, 2), language.substring(language.indexOf('-') + 1)));
            else
                setUserLocale(new Locale(language));// Just the language code only
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
            trustedMessage = jiraResponseJsonBuilder.append("'").append(StringEscapeUtils.escapeEcmaScript(trustedMessage)).append("'").toString();
            jiraResponseJsonBuilder.setLength(0);
        }

        jiraResponseJsonBuilder.append("{\npage: ").append(requestedPage).append(",\n")
                .append("total: ").append(count).append(",\n")
                .append("trustedMessage: ").append(trustedMessage).append(",\n")
                .append("rows: [\n");

        for (Iterator<Element> itemIterator = itemElements.iterator(); itemIterator.hasNext();)
        {
            jiraResponseJsonBuilder.append(getElementJson(itemIterator.next(), columnNames, columnMap, fromApplink));

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

    public String generate(JiraIssuesManager.Channel jiraResponseChannel, Collection<String> columnNames, int requestedPage, boolean showCount, boolean fromApplink) throws IOException
    {
        try
        {
            return getOutputAsString(jiraResponseChannel.getSourceUrl(), jiraResponseChannel, columnNames, requestedPage, showCount, fromApplink);
        }
        catch (Exception e)
        {
            throw new IOException("Unable to generate JSON output", e);
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
