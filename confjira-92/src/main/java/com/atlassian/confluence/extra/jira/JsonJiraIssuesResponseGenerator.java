package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.i18n.UserI18NBeanFactory;
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
import java.util.Map;

public class JsonJiraIssuesResponseGenerator implements DelegatableJiraIssuesResponseGenerator
{
    private static final Logger log = Logger.getLogger(JsonJiraIssuesResponseGenerator.class);

    private static final String DATE_VALUE_FORMAT = "dd/MMM/yy";

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private final UserI18NBeanFactory i18NBeanFactory;

    private final JiraIssuesManager jiraIssuesManager;

    private final JiraIssuesColumnManager jiraIssuesColumnManager;

    public JsonJiraIssuesResponseGenerator(UserI18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, JiraIssuesColumnManager jiraIssuesColumnManager)
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
        // TODO: eventually may want to get a formatter using with user's locale here
        return new SimpleDateFormat(DATE_VALUE_FORMAT);
    }

    protected String getElementJson(Element itemElement, Collection<String> columnNames, Map<String, String> iconMap, Map<String, String> columnMap) throws Exception
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
                jsonIssueElementBuilder.append("'");
                jsonIssueElementBuilder.append(StringEscapeUtils.escapeJavaScript(xmlXformer.collapseMultiple(itemElement, columnName).getValue()));
                jsonIssueElementBuilder.append("'");
            }
            else
            {
                String value;
                Element child = itemElement.getChild(columnName);
                if (child != null)
                    value = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(child.getValue()));
                else
                    value = "";

                if (columnName.equalsIgnoreCase("type"))
                    jsonIssueElementBuilder.append("'<a href=\"").append(link).append("\" >").append(createImageTag(xmlXformer.findIconUrl(child, iconMap), value)).append("</a>'");
                else if (columnName.equalsIgnoreCase("key") || columnName.equals("summary"))
                    jsonIssueElementBuilder.append("'<a href=\"").append(link).append("\" >").append(value).append("</a>'");
                else if (columnName.equalsIgnoreCase("priority"))
                {
                    jsonIssueElementBuilder.append("'").append(createImageTag(xmlXformer.findIconUrl(child, iconMap), value)).append("'");
                }
                else if (columnName.equalsIgnoreCase("status"))
                {
                    String imgTag = createImageTag(xmlXformer.findIconUrl(child, iconMap), value);
                    jsonIssueElementBuilder.append("'");
                    if (StringUtils.isNotBlank(imgTag))
                    {
                        jsonIssueElementBuilder.append(imgTag).append(" ");
                    }
                    jsonIssueElementBuilder.append(value).append("'");
                }
                else if (columnName.equalsIgnoreCase("created") || columnName.equalsIgnoreCase("updated") || columnName.equalsIgnoreCase("due"))
                {
                    if (StringUtils.isNotEmpty(value))
                    {
                        DateFormat dateFormatter = getDateValueFormat();
                        jsonIssueElementBuilder.append("'").append(dateFormatter.format(GeneralUtil.convertMailFormatDate(value))).append("'");
                    }
                    else
                    {
                        jsonIssueElementBuilder.append("''");
                    }
                }
                else if (columnName.equals("description"))
                {
                    Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
                    jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(fieldValue.getValue())).append("'");
                }
                else
                {
                    Element fieldValue = xmlXformer.valueForField(itemElement, columnName, columnMap);
                    String fieldValueText = fieldValue.getValue();

                    /* Try to interpret value as date (CONFJIRA-136) */
                    try
                    {
                        Date customFieldValueDate;

                        /* While I'd expect the method to throw ParseException if the value is not a date, sometimes it just returns null?! */
                        if (StringUtils.isNotBlank(fieldValueText) && null != (customFieldValueDate = GeneralUtil.convertMailFormatDate(fieldValueText)))
                        {
                            jsonIssueElementBuilder.append("'").append(getDateValueFormat().format(customFieldValueDate)).append("'");
                        }
                        else
                        {
                            jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(fieldValueText))).append("'");
                        }
                    }
                    catch (ParseException pe)
                    {
                        log.debug("Unable to parse " + fieldValue.getText() + " into a date", pe);
                        jsonIssueElementBuilder.append("'").append(StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(fieldValueText))).append("'");
                    }
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

    private String getOutputAsString(String url, JiraIssuesManager.Channel jiraResponseChannel, Collection<String> columnNames, int requestedPage, boolean showCount)
            throws Exception
    {
        Element jiraResponseElement = jiraResponseChannel.getChannelElement();

        Map<String, String> columnMap = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        List<Element> itemElements = jiraResponseChannel.getChannelElement().getChildren("item");

        // if totalItems is not present in the XML, we are dealing with an older version of jira
        // in that case, consider the number of items retrieved to be the same as the overall total items
        Element totalItemsElement = jiraResponseElement.getChild("issue");
        String count = totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : String.valueOf(itemElements.size());

        if (showCount)
            return count;

        StringBuilder jiraResponseJsonBuilder = new StringBuilder();
        Map<String, String> iconMap = jiraIssuesManager.getIconMap(jiraResponseElement);

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
            jiraResponseJsonBuilder.append(getElementJson(itemIterator.next(), columnNames, iconMap, columnMap));

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
}
