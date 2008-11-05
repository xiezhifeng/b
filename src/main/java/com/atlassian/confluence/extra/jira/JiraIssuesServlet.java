package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.i18n.UserI18NBeanFactory;

public class JiraIssuesServlet extends HttpServlet
{
    private final Logger log = Logger.getLogger(JiraIssuesServlet.class);
    private CacheFactory cacheFactory;
    private UserI18NBeanFactory i18NBeanFactory;
    
    private JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();
    

    public JiraIssuesUtils getJiraIssuesUtils()
    {
        return jiraIssuesUtils;
    }

    public void setJiraIssuesUtils(JiraIssuesUtils jiraIssuesUtils)
    {
        this.jiraIssuesUtils = jiraIssuesUtils;
    }

    private JiraIssuesUtils jiraIssuesUtils;

    public void setCacheFactory(CacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }

    public void setUserI18NBeanFactory(UserI18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    protected String trustedStatusToMessage(TrustedConnectionStatus trustedConnectionStatus)
    {
        if(trustedConnectionStatus!=null)
        {
            if(!trustedConnectionStatus.isTrustSupported())
                return getText("jiraissues.server.trust.unsupported");
            else if (trustedConnectionStatus.isTrustedConnectionError())
            {
                if(!trustedConnectionStatus.isAppRecognized())
                {
                    String linkText = getText("jiraissues.server.trust.not.established");
                    String anonymousWarning = getText("jiraissues.anonymous.results.warning");
                    return "<a href=\"http://www.atlassian.com/software/jira/docs/latest/trusted_applications.html\">"+linkText+"</a> "+anonymousWarning;
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
                        while(trustedErrorsListIterator.hasNext())
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        PrintWriter out = null;
        String errorMessage = null;
        try
        {
            out = response.getWriter();

            boolean useTrustedConnection = Boolean.valueOf(request.getParameter("useTrustedConnection")).booleanValue();
            boolean useCache = Boolean.valueOf(request.getParameter("useCache")).booleanValue();

            String[] columns = request.getParameterValues("columns");
            List columnsList = Arrays.asList(columns);
            boolean showCount = Boolean.valueOf(request.getParameter("showCount")).booleanValue();

            Map params = request.getParameterMap();
            String partialUrl = createPartialUrlFromParams(params); // TODO: CONFJIRA-11: would be nice to check if url really points to a jira to prevent potentially being an open relay, but how exactly to do the check?
            CacheKey key = new CacheKey(partialUrl, columnsList, showCount, useTrustedConnection);


            /* append to url what # issue to start retrieval at. this is not done when other url stuff is because there is
            one partial url for a set of pages, so the partial url can be used in the CacheKey for the whole set. with what
            issue to start on appended, the url is specific to a page
             */
            String[] resultsPerPageArray = (String[])params.get("rp");
            int requestedPage = 0;
            String url;
            String requestedPageString = request.getParameter("page");
            if(StringUtils.isNotEmpty(requestedPageString) && resultsPerPageArray!=null)
            {
                int resultsPerPage = Integer.parseInt(resultsPerPageArray[0]);
                requestedPage = Integer.parseInt(requestedPageString);
                url = partialUrl+"&pager/start="+(resultsPerPage*(requestedPage-1));
            }
            else
                url = partialUrl;


            // write issue data out in json format
            response.setContentType("application/json");
            out.println(getResultJson(key, useTrustedConnection, useCache, requestedPage, showCount, url));
        }
        catch (IOException e)
        {
            errorMessage = formatErrorMessage(e);
            log.warn("An IO Exception has been encountered: " + e.getMessage());
            if (log.isDebugEnabled())
                log.debug("An IO Exception has been encountered", e);
        }
        catch (IllegalArgumentException e)
        {
            errorMessage = e.getMessage() == null ? "Unable to parse parameters" : e.getMessage();
            if (log.isDebugEnabled())
                log.debug("Unable to parse parameters" + e.getMessage(), e);
        }
        catch (Exception e)
        {
            errorMessage = formatErrorMessage(e);
            log.warn("Unexpected Exception, could not retrieve JIRA issues: " + e.getMessage());
            if (log.isDebugEnabled())
                log.debug("Unexpected Exception, Could not retrieve JIRA issues", e);
        }

        if (!StringUtils.isEmpty(errorMessage))
        {
            response.setContentType("text/html");
            response.setStatus(500);
            if (out!=null)
            {
                out.flush();
                out.println(errorMessage);
            }
        }
    }
    
    private String formatErrorMessage( Exception e ) 
    {
        StringBuffer result = new StringBuffer();
        
        if( e.getMessage() != null )
        {
            result.append( e.getMessage() ).append("<br/>");
        }
        
        result.append( e.getClass().toString() );
        
        return result.toString();
    }

    protected String getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage, boolean showCount, String url) throws Exception
    {
        SimpleStringCache subCacheForKey = getSubCacheForKey(key, !useCache);

        Integer requestedPageKey = new Integer(requestedPage);
        String result = subCacheForKey.get(requestedPageKey);

        if (result != null)
            return result;

        // TODO: time this with macroStopWatch?
        // and log more debug statements?

        // get data from jira and transform into json
        JiraIssuesUtils.Channel channel = jiraIssuesUtils.retrieveXML(url, useTrustedConnection);
        String jiraResponseToJson = jiraResponseToOutputFormat(channel, key.getColumns(), requestedPage, showCount, url);

        subCacheForKey.put(requestedPageKey,jiraResponseToJson);
        return jiraResponseToJson;
    }

    private SimpleStringCache getSubCacheForKey(CacheKey key, boolean flush)
    {
        Cache cacheCache = cacheFactory.getCache(JiraIssuesServlet.class.getName());

        if (flush)
        {
            if (log.isDebugEnabled())
                log.debug("flushing cache for key: "+key);

            cacheCache.remove(key);
        }

        SimpleStringCache subCacheForKey = (SimpleStringCache)cacheCache.get(key);
        if(subCacheForKey==null)
        {
            if(key.isShowCount())
                subCacheForKey = new StringCache(Collections.synchronizedMap(new HashMap()));
            else
                subCacheForKey = new CompressingStringCache(Collections.synchronizedMap(new HashMap()));
            cacheCache.put(key, subCacheForKey);
        }
        return subCacheForKey;
    }

    protected String createPartialUrlFromParams(Map params)
    {
        String[] urls = (String[]) params.get("url");
        if (urls == null) throw new IllegalArgumentException("url parameter is required");
        String urlString = urls[0];
        if (StringUtils.isEmpty(urlString)) throw new IllegalArgumentException("url parameter is required");
        StringBuffer url = new StringBuffer(urls[0]);

        // if there are no existing url parameters, need to add the ? to the url
        if(url.indexOf("?")==-1)
            url.append("?");

        String[] resultsPerPageArray = (String[])params.get("rp"); // TODO: this param is dealt with in doGet(), would be nice to refactor somehow to use that...
        if(resultsPerPageArray!=null)
        {
            // append max results to return (for this request/page)
            url.append("&tempMax=");
            url.append(resultsPerPageArray[0]);
        }

        // determine what field issue to start retrieval at (possibly translating the field name) and append to url
        String[] sortFieldArray = (String[])params.get("sortname");
        if(sortFieldArray!=null)
        {
            String sortField = sortFieldArray[0];
            if(sortField.equals("key"))
                sortField = "issuekey";
            else if(sortField.equals("type"))
                sortField = "issuetype";
            else
            {
                Map columnMapForJiraInstance = jiraIssuesUtils.getColumnMap(jiraIssuesUtils.getColumnMapKeyFromUrl(url.toString()));
                if(columnMapForJiraInstance!=null && columnMapForJiraInstance.containsKey(sortField))
                    sortField = (String)columnMapForJiraInstance.get(sortField);
            }
            url.append("&sorter/field=");
            url.append(sortField);
        }

        // append sort order (ascending or descending) to url
        String[] sortOrderArray = (String[])params.get("sortorder");
        if(sortOrderArray!=null)
        {
            String sortOrder = sortOrderArray[0];
            url.append("&sorter/order=");
            url.append(sortOrder.toUpperCase()); // seems to work without upperizing but thought best to do it
        }

        // if added a ? and then &, remove the & so the server doesn't give errors about incorrect url syntax
        int questionAmpersandIndex = url.indexOf("?&");
        if(questionAmpersandIndex!=-1)
            url.delete(questionAmpersandIndex+1,questionAmpersandIndex+2);

        return url.toString();
    }

    // convert response to json format or just a string of an integer if showCount=true
    protected String jiraResponseToOutputFormat(JiraIssuesUtils.Channel jiraResponseChannel, List columnsList, int requestedPage, boolean showCount, String url) throws Exception
    {
        Element jiraResponseElement = jiraResponseChannel.getElement();

        //Set allCols = getAllCols(jiraResponseElement);
        // keep a map of column ID to column name so the macro can send
        // proper sort requests
        Map columnMap = new HashMap();
        List entries = jiraResponseChannel.getElement().getChildren("item");

        // if totalItems is not present in the XML, we are dealing with an older version of jira
        // in that case, consider the number of items retrieved to be the same as the overall total items
        Element totalItemsElement = jiraResponseElement.getChild("issue");
        String count = totalItemsElement!=null ? totalItemsElement.getAttributeValue("total") : ""+entries.size();

        if (showCount)
            return count;

        StringBuffer jiraResponseInJson = new StringBuffer();
        Map iconMap = jiraIssuesUtils.prepareIconMap(jiraResponseElement);
        Iterator entriesIterator = entries.iterator();

        String trustedMessage = trustedStatusToMessage(jiraResponseChannel.getTrustedConnectionStatus());
        if(trustedMessage!=null)
            trustedMessage = "'"+StringEscapeUtils.escapeJavaScript(trustedMessage)+"'";
        jiraResponseInJson.append("{\npage: ").append(requestedPage).append(",\n")
            .append("total: ").append(count).append(",\n")
            .append("trustedMessage: ").append(trustedMessage).append(",\n")
            .append("rows: [\n");

        while (entriesIterator.hasNext())
        {
            jiraResponseInJson.append(getElementJson((Element)entriesIterator.next(), columnsList, iconMap, columnMap));

            // no comma after last row
            if(entriesIterator.hasNext())
                jiraResponseInJson.append(',');

            jiraResponseInJson.append('\n');
        }

        jiraResponseInJson.append("]}");

        // persist the map of column names to bandana for later use
        jiraIssuesUtils.putColumnMap(jiraIssuesUtils.getColumnMapKeyFromUrl(url), columnMap);

        return jiraResponseInJson.toString();
    }

    protected StringBuffer getElementJson(Element element, List columnsList, Map iconMap, Map columnMap) throws Exception
    {
        StringBuffer elementJson = new StringBuffer();

        String key = element.getChild("key").getValue();

        Iterator columnsListIterator = columnsList.iterator();

        elementJson.append("{id:'").append(key).append("',cell:[");
        String link = element.getChild("link").getValue();
        while(columnsListIterator.hasNext())
        {
            String columnName = (String)columnsListIterator.next();
            if( xmlXformer.isColumnMultivalued(columnName))
            {
                elementJson.append("'");
                Element xFormedElement = xmlXformer.collapseMultiple(element, columnName);
                elementJson.append(StringEscapeUtils.escapeJavaScript(xFormedElement.getValue()));
                elementJson.append("'");
            }
            else
            {
                String value;
                Element child = element.getChild(columnName);
                if(child!=null)
                    value = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(child.getValue()));
                else
                    value = "";

                if(columnName.equalsIgnoreCase("type"))
                    elementJson.append("'<a href=\"").append(link).append("\" >").append(createImageTag(jiraIssuesUtils.findIconUrl(child, iconMap), value)).append("</a>'");
                else if(columnName.equalsIgnoreCase("key") || columnName.equals("summary"))
                    elementJson.append("'<a href=\"").append(link).append("\" >").append(value).append("</a>'");
                else if(columnName.equalsIgnoreCase("priority") )
                {
                     elementJson.append("'").append(createImageTag(jiraIssuesUtils.findIconUrl(child, iconMap), value)).append("'");
                }
                else if(columnName.equalsIgnoreCase("status"))
                {
                    String imgTag = createImageTag(jiraIssuesUtils.findIconUrl(child, iconMap), value);
                    elementJson.append("'");
                    if( StringUtils.isNotBlank(imgTag))
                    {
                        elementJson.append(imgTag).append(" ");
                    }
                    elementJson.append(value).append("'");
                }
                else if(columnName.equalsIgnoreCase("created") || columnName.equalsIgnoreCase("updated") || columnName.equalsIgnoreCase("due"))
                {
                    if(StringUtils.isNotEmpty(value))
                    {
                        DateFormat dateFormatter = new SimpleDateFormat("dd/MMM/yy"); // TODO: eventually may want to get a formatter using with user's locale here
                        elementJson.append("'").append(dateFormatter.format(GeneralUtil.convertMailFormatDate(value))).append("'");
                    }
                    else
                        elementJson.append("''");
                }
                else
                {
                    Element fieldValue = xmlXformer.valueForField(element, columnName, columnMap);
                    elementJson.append("'").append(StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(fieldValue.getValue()))).append("'");
                }
            }

            // no comma after last item in row, but closing stuff instead
            if(columnsListIterator.hasNext())
                elementJson.append(',');
            else
                elementJson.append("]}\n");
        }

        return elementJson;
    }

    private String createImageTag( String iconUrl, String value )
    {
        StringBuffer result = new StringBuffer();
        
        if( StringUtils.isNotBlank(iconUrl))
        {
            result.append("<img src=\"").append(iconUrl).append("\" alt=\"").append( value).append("\"/>");
        }
        
        return result.toString();
    }

    private Set getAllCols(Element channelElement) throws JDOMException
    {
        List fields =
            XPath.selectNodes(channelElement,
                              "//item/*[not(*) and normalize-space(text())] | " +
                                  "//item/customfields/customfield/customfieldname/text()");
        Set allCols = new LinkedHashSet();
        for (Iterator iter = fields.iterator(); iter.hasNext(); )
        {
            Object nextMatch = iter.next();
            if (nextMatch instanceof Element)
            {
                allCols.add(((Element) nextMatch).getName());
            }
            else if (nextMatch instanceof Text)
            {
                allCols.add(StringUtils.trim(((Text) nextMatch).getText()));
            }
        }
        return allCols;
    }

    public String getText(String key)
    {
        return i18NBeanFactory.getI18NBean().getText(key);
    }
}
