package com.atlassian.confluence.extra.jira;

import bucket.cache.CacheManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.renderer.radeox.macros.include.AbstractHttpRetrievalMacro;
import com.atlassian.confluence.security.GateKeeper;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.setup.ConfluenceBootstrapConstants;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.core.util.FileUtils;
import com.atlassian.user.User;
import com.opensymphony.util.TextUtils;
import com.opensymphony.webwork.ServletActionContext;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.macro.parameter.MacroParameter;
import org.springframework.util.StopWatch;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends AbstractHttpRetrievalMacro
{
    private final Logger log = Logger.getLogger(JiraIssuesMacro.class);

    private static final String MACRO_REFRESH = "macro.refresh";
    private static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";

    private final String[] myParamDescription = new String[]{"1: url", "?2: columns"};
    private final Set defaultColumns = new LinkedHashSet();

    private JiraIconMappingManager jiraIconMappingManager;
    private BootstrapManager bootstrapManager;
    private GateKeeper gateKeeper;
    private CacheManager cacheManager;

    /**
     * Trivial string cache factory to support wrapping of caches in a compression layer
     */
    private static interface SimpleStringCacheFactory
    {
        SimpleStringCache getCache();
    }

    private final SimpleStringCacheFactory stringCacheFactory = new SimpleStringCacheFactory()
    {
        public SimpleStringCache getCache()
        {
            return new CompressingStringCache(cacheManager.getCache(JiraIssuesMacro.class.getName()));
        }
    };

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    public void setInitialContext(InitialRenderContext initialRenderContext)
    {
        super.setInitialContext(initialRenderContext);

        defaultColumns.clear();
        defaultColumns.add("type");
        defaultColumns.add("key");
        defaultColumns.add("summary");
        defaultColumns.add("assignee");
        defaultColumns.add("reporter");
        defaultColumns.add("priority");
        defaultColumns.add("status");
        defaultColumns.add("resolution");
        defaultColumns.add("created");
        defaultColumns.add("updated");
        defaultColumns.add("due");
    }

    public String getName()
    {
        return "jiraissues";
    }

    public String[] getParamDescription()
    {
        return (String[]) ArrayUtils.clone(myParamDescription);
    }

    public String getHtml(MacroParameter macroParameter) throws IllegalArgumentException, IOException
    {
        String url = cleanUrlParentheses(TextUtils.noNull(macroParameter.get("url", 0)).trim());
        String columns = TextUtils.noNull(macroParameter.get("columns", 1)).trim();
        String cacheParameter = macroParameter.get("cache", 2);
        String template = TextUtils.noNull(macroParameter.get("template", 3)).trim();
        boolean showCount = Boolean.valueOf(StringUtils.trim(macroParameter.get("count"))).booleanValue();
        String anonymousStr = TextUtils.noNull(macroParameter.get("anonymous", 4)).trim();
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        boolean useCache = StringUtils.isBlank(cacheParameter) || Boolean.valueOf(cacheParameter).booleanValue();
        boolean useTrustedConnection = !Boolean.valueOf(anonymousStr).booleanValue() && !isUserNamePasswordProvided(url);

        StopWatch macroStopWatch = getNewStopWatch();
        macroStopWatch.start("Render HTML");

        if (log.isDebugEnabled())
        {
            log.debug("creating html content for url [ " + url + " ]");
            log.debug("showCount [ " + showCount + " ]");
            log.debug("template [ " + template + " ]");
            log.debug("columns [ " + columns + " ]");
        }

        CacheKey key = new CacheKey(url, columns, showCount, template);
        String html = getHtml(key, macroParameter.get("baseurl"), useCache && !useTrustedConnection, useTrustedConnection);

        macroStopWatch.stop();

        if (log.isDebugEnabled())
        {
            log.debug("created html content for url [ " + url + " ]");
            log.debug("length [ " + html.length() + " ]");
            log.debug("time [ " + macroStopWatch.prettyPrint() + " ]");
        }

        return html;
    }

    private SimpleStringCache getResultCache()
    {
        return stringCacheFactory.getCache();
    }

    private StopWatch getNewStopWatch()
    {
        return new StopWatch(getName());
    }

    private String getHtml(CacheKey key, String baseurl, boolean useCache, boolean useTrustedConnection) throws IOException
    {
        SimpleStringCache cache = getResultCache();

        boolean flush = !useCache || isCacheParameterSet();
        if (flush)
        {
            if (log.isDebugEnabled())
                log.debug("flushing cache");

            cache.remove(key);
        }

        String result = cache.get(key);
        if (result != null)
            return result;

        StopWatch macroStopWatch = getNewStopWatch();
        macroStopWatch.start("retrieving new xml content");

        if (log.isDebugEnabled())
            log.debug("retrieving new xml content");

        Channel channel = fetchChannel(key.getUrl(), useTrustedConnection);

        String clickableUrl = makeClickableUrl(key.getUrl());
        if (TextUtils.stringSet(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());

        macroStopWatch.stop();

        macroStopWatch.start("transforming to html");
         if (log.isDebugEnabled())
            log.debug("transforming to html");

        result = key.isShowCount() ?
                countChannel(clickableUrl, channel) :
                renderChannel(key, clickableUrl, channel);

        macroStopWatch.stop();

        if (log.isDebugEnabled())
            log.debug("Macro timings: " + macroStopWatch.prettyPrint());

        if (!useTrustedConnection)
            cache.put(key, result);

        return result;
    }

    private String renderChannel(CacheKey key, String clickableUrl, Channel channel)
    {
        Element element = channel.getElement();
        Map contextMap = MacroUtils.defaultVelocityContext();

        contextMap.put("url", key.getUrl());
        contextMap.put("clickableUrl", clickableUrl);
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        contextMap.put("columns", prepareDisplayColumns(key.getColumns()));
        contextMap.put("icons", prepareIconMap(element));
        contextMap.put("refreshUrl", getRefreshUrl());
        contextMap.put("trustedConnection", Boolean.valueOf(channel.isTrustedConnection()));
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());

        String template = key.getTemplate();
        if (!"".equals(template))
            template = "-" + template;

        return VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues" + template + ".vm", contextMap);
    }

    private boolean isUserNamePasswordProvided(String url)
    {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.indexOf("os_username") != -1 && lowerUrl.indexOf("os_password") != -1;
    }

    public String rebaseUrl(String clickableUrl, String baseUrl)
    {
        return clickableUrl.replaceFirst(
            "^" + // only at start of string
                ".*?" + // minimum number of characters (the schema) followed by...
                "://" + // literally: colon-slash-slash
                "[^/]+", // one or more non-slash characters (the hostname)
            baseUrl);
    }

    private String countChannel(String url, Channel channel)
    {
        ConfluenceActionSupport cas = GeneralUtil.newWiredConfluenceActionSupport();
        return "<a href=\"" + url + "\">" + channel.getElement().getChildren("item").size() + " " + cas.getText("jiraissues.issues.word") + "</a>";
    }

    private String makeClickableUrl(String url)
    {
        String link = url;

        if (link.indexOf("view=rss") > 0)
            link = link.replaceAll("view=rss", "");

        if (link.indexOf("decorator=none") > 0)
            link = link.replaceAll("decorator=none", "");

        int usernameIdx = link.indexOf("&os_username=");
        if (usernameIdx > 0)
        {
            int nextAmp = link.indexOf('&', usernameIdx + 1);

            if (nextAmp > 0)
                link = link.substring(0, usernameIdx) + link.substring(nextAmp);
            else
                link = link.substring(0, usernameIdx);
        }

        int passwordIdx = link.indexOf("&os_password=");
        if (passwordIdx > 0)
        {
            int nextAmp = link.indexOf('&', passwordIdx + 1);

            if (nextAmp > 0)
                link = link.substring(0, passwordIdx) + link.substring(nextAmp);
            else
                link = link.substring(0, passwordIdx);
        }
        return link;
    }

    private Set prepareDisplayColumns(String columns)
    {
        if (!TextUtils.stringSet(columns))
            return defaultColumns;

        Set columnSet = new LinkedHashSet(Arrays.asList(columns.split(",|;")));
        columnSet.retainAll(defaultColumns);
        return columnSet.isEmpty() ? defaultColumns : columnSet;
    }

    private Map prepareIconMap(Element channel)
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

    /*
     * fetchChannel need to return its result plus a trusted connection status. This is a value class to allow this.
     */
    protected final static class Channel
    {
        private final Element element;
        private final TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus;

        private Channel(Element element, TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus)
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

    protected Channel fetchChannel (String url) throws IOException
    {
        return fetchChannel(url, false);
    }

    protected Channel fetchChannel (String url, boolean trust) throws IOException
    {
        if (log.isDebugEnabled())
            log.debug("Fetching XML feed " + url + " ...");

        GetMethod method = null;

        try
	    {
            TrustedTokenAuthenticator.TrustedConnectionStatus trustedConnectionStatus = null;
            method = (GetMethod) retrieveRemoteUrl(url, trust);
            if (log.isDebugEnabled())
                logResponseHeaders(method);

            if (trust)
                trustedConnectionStatus = getTrustedConnectionStatusFromMethod(method);

            Element element = getChannelElement(method.getResponseBodyAsString(), url);
            return new Channel(element, trustedConnectionStatus);
        }
        finally
        {
            releaseMethodQuietly(method);
        }
    }

    private void releaseMethodQuietly(HttpMethod method)
    {
        if (method == null)
            return;

        try
        {
            method.releaseConnection();
        }
        catch (Exception e)
        {
            log.error("Error calling HttpMethod.releaseConnection", e);
        }
    }

    private Element getChannelElement(String content, String url) throws IOException
    {
        try
        {
            SAXBuilder saxBuilder = new SAXBuilder(SAX_PARSER_CLASS);
            Document document = saxBuilder.build(new StringReader(content));
            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        catch (JDOMException e)
        {
            throw launderJdomException(url, content, e);
        }
    }

    private void logResponseHeaders(HttpMethod method)
    {
        StringBuffer headerBuffer = new StringBuffer("Response headers:\n");
        Header[] headers = method.getResponseHeaders();
        for (int x = 0; x < headers.length; x++)
        {
            headerBuffer.append(headers[x].getName());
            headerBuffer.append(": ");
            headerBuffer.append(headers[x].getValue());
            headerBuffer.append("\n");
        }

        log.debug(headerBuffer.toString());
    }

    private IOException launderJdomException(String url, String webContent, JDOMException e)
            throws IOException
    {
        String filename = "rssoutput" + url.hashCode() + ".txt";

        // store retrieved output in a text file that can be downloaded
        File rssParseError = new File(bootstrapManager.getFilePathProperty(ConfluenceBootstrapConstants.TEMP_DIR_PROP), filename);
        FileUtils.saveTextFile(webContent, rssParseError);

        String path = "/download/temp/" + filename;
        User user = getRemoteUser();

        if (user != null)
            gateKeeper.addKey(path, user);
        else
            log.error("Could not reference remoteUser when trying to store results of RSS failure in temp dir.");

        try
        {
            MDC.put("jiraissues.result", bootstrapManager.getWebAppContextPath() + "/download/temp/" + filename);
            log.error("Error while trying to assemble the RSS result: " + e.getMessage());
        }
        finally
        {
            MDC.remove("jiraissues.result");
        }

        return new IOException(e.getMessage() + " <a href='" + bootstrapManager.getWebAppContextPath() + "/download/temp/" + filename + "'>" + filename + "</a>");
    }


    /**
     * creates a URL including a refresh parameter to flush the cache of a macro (eg. JiraIssues Macro)
     *
     * @return String current url including the refresh parameter
     */
    protected String getRefreshUrl()
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        if (request == null)
            return null;

        StringBuffer refreshUrl = new StringBuffer(request.getRequestURI());
        String query = request.getQueryString();
        if (TextUtils.stringSet(query))
        {
            refreshUrl.append("?").append(query);

            if (request.getParameter(MACRO_REFRESH) == null)
                refreshUrl.append("&").append(MACRO_REFRESH).append("=true");
        }
        else
        {
            refreshUrl.append("?").append(MACRO_REFRESH).append("=true");
        }

        return refreshUrl.toString();
    }

    /**
     * Checks the current url for a macro refresh parameter
     *
     * @return boolean whether or not the macro cache should be flushed
     */
    protected boolean isCacheParameterSet()
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        return request != null && (request.getParameter(MACRO_REFRESH) != null);
    }

    public void setBootstrapManager(BootstrapManager bootstrapManager)
    {
        this.bootstrapManager = bootstrapManager;
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setGateKeeper(GateKeeper gateKeeper)
    {
        this.gateKeeper = gateKeeper;
    }
}



