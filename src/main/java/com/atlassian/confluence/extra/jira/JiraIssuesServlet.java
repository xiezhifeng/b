package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.DefaultI18NBeanFactory;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.Cache;
import com.atlassian.cache.memory.MemoryCache;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.text.ParseException;

public class JiraIssuesServlet extends HttpServlet
{
    private final Logger log = Logger.getLogger(JiraIssuesServlet.class);
    protected static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";
    private CacheFactory cacheFactory;
    private TrustedTokenAuthenticator trustedTokenAuthenticator;
    private UserAccessor userAccessor;
    private LocaleManager localeManager;
    private I18NBeanFactory i18NBeanFactory;
    private Locale userLocale = null;
    private I18NBean i18NBean;
    private HttpServletRequest request;

    public void setCacheFactory(CacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }

    public void setTrustedTokenFactory(TrustedTokenFactory trustedTokenFactory)
    {
        this.trustedTokenAuthenticator = new TrustedTokenAuthenticator(trustedTokenFactory);
    }

    private static JiraIconMappingManager jiraIconMappingManager;

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    private HttpMethod getMethod(String url, boolean useTrustedConnection, HttpClient client)
    {
        return (useTrustedConnection ? trustedTokenAuthenticator.makeMethod(client, url) : new GetMethod(url));
    }

    /**
     * Query the status of a trusted connection
     *
     * @param method An executed HttpClient method
     * @return the response status of a trusted connection request or null if the method doesn't use a trusted
     * connection
     */
    protected TrustedTokenAuthenticator.TrustedConnectionStatus getTrustedConnectionStatusFromMethod(HttpMethod method)
    {
        return trustedTokenAuthenticator.getTrustedConnectionStatus(method);
    }

    protected Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = getMethod(url, useTrustedConnection, httpClient);

        httpClient.executeMethod(method);
        InputStream xmlStream = method.getResponseBodyAsStream();
        Element channelElement = getChannelElement(xmlStream);

        TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus = null;
        if (useTrustedConnection)
            trustedConnectionStatus = getTrustedConnectionStatusFromMethod(method);

        return new Channel(channelElement, trustedConnectionStatus);

        // TODO: check that this is really not needed b/c an autocloseinputstream is used
//        finally
//        {
//            method.releaseConnection();
//        }
    }


     /*
     * fetchChannel needs to return its result plus a trusted connection status. This is a value class to allow this.
     */
    protected final static class Channel
    {
        private final Element element;
        private final TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus;

        protected Channel(Element element, TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus)
        {
            this.element = element;
            this.trustedConnectionStatus = trustedConnectionStatus;
        }

        public Element getElement()
        {
            return element;
        }

        public TrustedTokenAuthenticator.TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return trustedConnectionStatus;
        }

        public boolean isTrustedConnection()
        {
            return trustedConnectionStatus != null;
        }
    }


    protected String trustedStatusToMessage(TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus)
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
        this.request = request;
        boolean useTrustedConnection = Boolean.valueOf(request.getParameter("useTrustedConnection")).booleanValue();
        boolean useCache = Boolean.parseBoolean(request.getParameter("useCache"));

        String[] columns = request.getParameterValues("columns");
        Set columnsSet = new LinkedHashSet(Arrays.asList(columns));
        boolean showCount = Boolean.valueOf(request.getParameter("showCount")).booleanValue();

        Map params = request.getParameterMap();
        String partialUrl = createPartialUrlFromParams(params); // TODO: would be nice to check if url really points to a jira to prevent potentially being an open relay, but how exactly to do the check?
        CacheKey key = new CacheKey(partialUrl, columnsSet, showCount, "", useTrustedConnection);


        /* append to url what # issue to start retrieval at. this is not done when other url stuff is because there is
        one partial url for a set of pages, so the partial url can be used in the CacheKey for the whole set. with what
        issue to start on appended, the url is specific to a page
         */
        String[] resultsPerPageArray = (String[])params.get("rp"); // TODO: this param is dealt with in doGet(), would be nice to refactor somehow to use that...
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
        PrintWriter out = null;
        try
        {
            out = response.getWriter();
            out.println(getResultJson(key, useTrustedConnection, useCache, requestedPage, showCount, url));
            response.setContentType("application/json");
        }
        catch (Exception e)
        {
            if (out!=null)
            {
                out.flush();
                String message = e.getMessage();

                if(message!=null)
                    out.println(e.getClass().toString()+" - "+message);
                else
                    out.println(e.getClass().toString());
            }
            response.setContentType("text/plain");
            response.setStatus(500);
        }
    }

    protected Cache getCacheOfCaches()
    {
        return cacheFactory.getCache(JiraIssuesServlet.class.getName());
    }

    protected String getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage, boolean showCount, String url) throws IOException, ParseException
    {
        Cache cacheCache = getCacheOfCaches();

        boolean flush = !useCache;
        if (flush)
        {
            if (log.isDebugEnabled())
                log.debug("flushing cache for key: "+key);

            cacheCache.remove(key);
        }

        SimpleStringCache subCacheForKey = (SimpleStringCache)cacheCache.get(key);
        if(subCacheForKey==null)
        {
            subCacheForKey = new CompressingStringCache(new MemoryCache(key.getPartialUrl()));
            cacheCache.put(key, subCacheForKey);
        }
        Integer requestedPageKey = new Integer(requestedPage);
        String result = subCacheForKey.get(requestedPageKey);

        if (result != null)
            return result;

        // TODO: time this with macroStopWatch?
        // and log more debug statements?

        // get data from jira and transform into json
        Channel channel = retrieveXML(url, useTrustedConnection);
        String jiraResponseToJson = jiraResponseToJson(channel, key.getColumns(), requestedPage, showCount);

        subCacheForKey.put(requestedPageKey,jiraResponseToJson);
        return jiraResponseToJson;
    }

    protected static String createPartialUrlFromParams(Map params)
    {
        StringBuffer url = new StringBuffer(((String[])params.get("url"))[0]);
        
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

        return url.toString();
    }

    protected String jiraResponseToJson(Channel jiraResponseChannel, Set columnsSet, int requestedPage, boolean showCount) throws ParseException
    {
        Element jiraResponseElement = jiraResponseChannel.getElement();
        List entries = jiraResponseChannel.getElement().getChildren("item");

        // if totalItems is not present in the XML, we are dealing with an older version of jira (theorectically at this point)
        // in that case, consider the number of items retrieved to be the same as the overall total items
        Element totalItemsElement = jiraResponseElement.getChild("issue");
        String count = totalItemsElement!=null ? totalItemsElement.getAttributeValue("total") : ""+entries.size();

        if (showCount)
            return count;

        StringBuffer jiraResponseInJson = new StringBuffer();
        Map iconMap = prepareIconMap(jiraResponseElement);
        Iterator entriesIterator = entries.iterator();

        String trustedMessage = trustedStatusToMessage(jiraResponseChannel.getTrustedConnectionStatus());
        if(trustedMessage!=null)
            trustedMessage = "'"+StringEscapeUtils.escapeJavaScript(trustedMessage)+"'";
        jiraResponseInJson.append("{\npage: "+requestedPage+",\n" +
            "total: "+count+",\n" +
            "trustedMessage: "+trustedMessage+",\n" +
            "rows: [\n");

        while (entriesIterator.hasNext())
        {
            Element element = (Element)entriesIterator.next();

            String key = element.getChild("key").getValue();

            Iterator columnsSetIterator = columnsSet.iterator();

            jiraResponseInJson.append("{id:'"+key+"',cell:[");
            String link = element.getChild("link").getValue();
            while(columnsSetIterator.hasNext())
            {
                String columnName = (String)columnsSetIterator.next();
                String value;
                Element child = element.getChild(columnName);
                if(child!=null)
                {
                    // only need to escape summary field because that's the only place bad characters should be  // TODO: really? user-created status etc?
                    if(columnName.equals("summary") || columnName.equals("title")  || columnName.equals("comments") )
                        value = StringEscapeUtils.escapeJavaScript(child.getValue());
                    else
                        value = child.getValue();
                }
                else
                    value = "";

                if(columnName.equals("type"))
                    jiraResponseInJson.append("'<a href=\""+link+"\" ><img src=\""+iconMap.get(value)+"\" alt=\""+value+"\"/></a>'");
                else if(columnName.equals("key") || columnName.equals("summary"))
                    jiraResponseInJson.append("'<a href=\""+link+"\" >"+value+"</a>'");
                else if(columnName.equals("priority"))
                {
                    String icon = (String)iconMap.get(value);
                    if(icon!=null)
                        jiraResponseInJson.append("'<img src=\""+iconMap.get(value)+"\" alt=\""+value+"\"/>'");
                    else
                        jiraResponseInJson.append("'"+value+"'");
                }
                else if(columnName.equals("status"))
                {
                    String icon = (String)iconMap.get(value);
                    if(icon!=null)
                        jiraResponseInJson.append("'<img src=\""+iconMap.get(value)+"\" alt=\""+value+"\"/> "+value+"'");
                    else
                        jiraResponseInJson.append("'"+value+"'");
                }
                else if(columnName.equals("created") || columnName.equals("updated") || columnName.equals("due"))
                {
                    if(StringUtils.isNotEmpty(value))
                        jiraResponseInJson.append("'"+GeneralUtil.convertMailFormatDate(value)+"'");
                    else
                        jiraResponseInJson.append("''");
                }
                else if (columnName.equals("title") || columnName.equals("link") || columnName.equals("resolution") || columnName.equals("assignee") || columnName.equals("reporter") ||
                    columnName.equals("version") || columnName.equals("votes") || columnName.equals("comments") || columnName.equals("attachments") || columnName.equals("subtasks"))
                    jiraResponseInJson.append("'"+value+"'");
                else // then we are dealing with a custom field (or nonexistent field)
                {
                    // TODO: maybe do this on first time only somehow?
                    Element customFieldsElement = element.getChild("customfields");
                    List customFieldList = customFieldsElement.getChildren();

                    // go through all the children and find which has the right customfieldname
                    Iterator customFieldListIterator = customFieldList.iterator();
                    while(customFieldListIterator.hasNext())
                    {
                        Element customFieldElement = (Element)customFieldListIterator.next();
                        String customFieldName = customFieldElement.getChild("customfieldname").getValue();
                        if(customFieldName.equals(columnName))
                        {
                            Element customFieldValuesElement = customFieldElement.getChild("customfieldvalues");
                            List customFieldValuesList = customFieldValuesElement.getChildren();
                            Iterator customFieldValuesListIterator = customFieldValuesList.iterator();
                            while(customFieldValuesListIterator.hasNext())
                                value += ((Element)customFieldValuesListIterator.next()).getValue()+" ";
                        }
                    }
                    jiraResponseInJson.append("'"+StringEscapeUtils.escapeJavaScript(value)+"'");

                }

                // no comma after last item in row, but closing stuff instead
                if(columnsSetIterator.hasNext())
                    jiraResponseInJson.append(',');
                else
                    jiraResponseInJson.append("]}\n");
            }

            // no comma after last row
            if(entriesIterator.hasNext())
                jiraResponseInJson.append(',');

            jiraResponseInJson.append('\n');
        }

        jiraResponseInJson.append("]}");

        return jiraResponseInJson.toString();
    }

    private Element getChannelElement(InputStream responseStream) throws IOException
    {
        try
        {
            SAXBuilder saxBuilder = new SAXBuilder(SAX_PARSER_CLASS);
            Document document = saxBuilder.build(responseStream);
            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        catch (JDOMException e)
        {
            log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage()); 
            throw new IOException(e.getMessage());
        }
    }

    private static Map prepareIconMap(Element channel)
    {
        String link = channel.getChild("link").getValue();
        // In pre 3.7 JIRA, the link is just http://domain/context, in 3.7 and later it is the full query URL,
        // which looks like http://domain/context/secure/IssueNaviagtor...
        int index = link.indexOf("/secure/IssueNavigator");
        if (index != -1)
            link = link.substring(0, index);

        String imagesRoot = link + "/images/icons/";
        Map result = new HashMap();

        for (Iterator iterator = jiraIconMappingManager.getIconMappings().entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            String icon = (String) entry.getValue();
            if (icon.startsWith("http://") || icon.startsWith("https://"))
                result.put(entry.getKey(), icon);
            else
                result.put(GeneralUtil.escapeXml((String) entry.getKey()), imagesRoot + icon);
        }

        return result;
    }


    // i18n stuff *****************************************************************************************************

    public UserAccessor getUserAccessor()
    {
        if (userAccessor == null)
            userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");

        return userAccessor;
    }

    public void setUserAccessor(UserAccessor userAccessor)
    {
        this.userAccessor = userAccessor;
    }

    public void setLocaleManager(LocaleManager localeManager)
    {
        this.localeManager = localeManager;
    }

    public LocaleManager getLocaleManager()
    {
        if (localeManager == null)
        {
            localeManager = (LocaleManager) ContainerManager.getComponent("localeManager");
        }
        return localeManager;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public User getRemoteUser()
    {
        User remoteUser = null;
        if (request.getRemoteUser() != null)
            remoteUser = getUser(request.getRemoteUser());
        return remoteUser;
    }

    public User getUser(String username)
    {
        return getUserAccessor().getUser(username);
    }

    public Locale getLocale()
    {
        if (userLocale == null)
        {
            userLocale = getLocaleManager().getLocale(getRemoteUser());
        }
        return userLocale;
    }

    public I18NBean getI18n()
    {
        if (i18NBean == null)
            return getI18NBeanFactory().getI18NBean(getLocale());

        return i18NBean;
    }

    public String getText(String key)
    {
        return getI18n().getText(key);
    }

    public String getText(String key, String[] args)
    {
        return getI18n().getText(key, args);
    }

    /**
     * During setup the i18NBeanFactory isn't autowired
     */
    private I18NBeanFactory getI18NBeanFactory()
    {
        if (i18NBeanFactory == null)
        {
            i18NBeanFactory = new DefaultI18NBeanFactory();
        }
        return i18NBeanFactory;
    }


}
