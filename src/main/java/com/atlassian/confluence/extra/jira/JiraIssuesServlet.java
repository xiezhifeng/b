package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.cache.CacheFactory;
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

import javax.servlet.ServletException;
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

    public void setCacheFactory(CacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }

    private SimpleStringCache getResultCache()
    {
        return stringCacheFactory.getCache();
    }

    private static interface SimpleStringCacheFactory
    {
        SimpleStringCache getCache();
    }

    private final SimpleStringCacheFactory stringCacheFactory = new SimpleStringCacheFactory()
    {
        public SimpleStringCache getCache()
        {
            return new CompressingStringCache(cacheFactory.getCache(JiraIssuesServlet.class.getName()));
        }
    };

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

    private InputStream retrieveXML(String url, boolean useTrustedConnection)
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = null;
        InputStream xmlStream;

        try
        {
            method = getMethod(url, useTrustedConnection, httpClient);

            httpClient.executeMethod(method);
            xmlStream = method.getResponseBodyAsStream();
        }
        catch (Exception e)
        {
            xmlStream = null;
        }
        // TODO: check that this is really not needed b/c an autocloseinputstream is used
//        finally
//        {
//            method.releaseConnection();
//        }
        return xmlStream;
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Map params = request.getParameterMap(); // TODO: instead use request.getParameter or request.getParameterValues? (and in createUrlFromParams)
        int requestedPage = Integer.parseInt(((String[])params.get("page"))[0]);
        String[] columns = (String[])params.get("columns");
        Set columnsSet = new LinkedHashSet(Arrays.asList(columns));
        boolean useTrustedConnection = Boolean.parseBoolean(((String[])params.get("useTrustedConnection"))[0]);

        String url = createUrlFromParams(params);
        CacheKey key = new CacheKey(url, columnsSet, false, "", useTrustedConnection);
        boolean useCache = Boolean.parseBoolean(((String[])params.get("useCache"))[0]);

        // write it out in json format
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        out.println(getResultJson(key, useTrustedConnection, useCache, requestedPage));
    }

    private String getResultJson(CacheKey key, boolean useTrustedConnection, boolean useCache, int requestedPage) throws IOException
    {
        SimpleStringCache cache = getResultCache();

        boolean flush = !useCache;
        if (flush)
        {
            if (log.isDebugEnabled())
                log.debug("flushing cache for key: "+key); // TODO: prints correctly?

            cache.remove(key);
        }
        else // if we just removed it from the cache, it can't be in the cache anymore so this can be skipped
        {
            String result = cache.get(key);
            if (result != null)
                return result;
        }

        // TODO: time this with macroStopWatch?

        InputStream xmlStream = retrieveXML(key.getUrl(), useTrustedConnection);
        Element jiraResponseElement = getChannelElement(xmlStream);
        String jiraResponseToJson = jiraResponseToJson(jiraResponseElement, key.getColumns(), requestedPage).toString();
        cache.put(key, jiraResponseToJson);
        return jiraResponseToJson;
    }

    protected static String createUrlFromParams(Map params)
    {
        int resultsPerPage = Integer.parseInt(((String[])params.get("rp"))[0]);
        StringBuffer url = new StringBuffer(((String[])params.get("url"))[0]);
        String sortField = ((String[])params.get("sortname"))[0];
        if(sortField.equals("key"))
            sortField = "issuekey";
        else if(sortField.equals("type"))
            sortField = "issuetype";
        String sortOrder = ((String[])params.get("sortorder"))[0];
        int requestedPage = Integer.parseInt(((String[])params.get("page"))[0]); // TODO: this is already done in doGet, refactor somehow...

        // TODO: check if url really points to a jira, how exactly?

        url.append("&pager/start=");
        url.append(resultsPerPage*(requestedPage-1));
        url.append("&tempMax=");
        url.append(resultsPerPage);
        url.append("&sorter/field=");
        url.append(sortField);
        url.append("&sorter/order=");
        url.append(sortOrder.toUpperCase()); // seems to work without upperizing but thought best to do it

        return url.toString();
    }

    protected static StringBuffer jiraResponseToJson(Element jiraResponseElement, Set columnsSet, int requestedPage)
    {
        Map iconMap = prepareIconMap(jiraResponseElement);
        StringBuffer jiraResponseInJson = new StringBuffer();

        List entries = jiraResponseElement.getChildren("item");
        Iterator entriesIterator = entries.iterator();

        jiraResponseInJson.append("{\npage: "+requestedPage+",\n" +
            "total: 10,\n" + // TODO: really actual total here, once can access it. can page through less than total issues by request?
            //"somethingelse: 5,\n" + // *** TODO: remove this test
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
                    if(columnName.equals("summary"))
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
                    {
                        try
                        {
                            jiraResponseInJson.append("'"+GeneralUtil.convertMailFormatDate(value)+"'");
                        }
                        catch (ParseException e)
                        {
                            throw new RuntimeException(e); // TODO: different error handling here?
                        }
                    }
                    else
                        jiraResponseInJson.append("''");
                }
                else
                    jiraResponseInJson.append("'"+value+"'");

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

        return jiraResponseInJson;
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
            log.error("Error while trying to assemble the RSS result: " + e.getMessage()); // TODO: change this msg?
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
}
