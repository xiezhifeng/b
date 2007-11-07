package com.atlassian.confluence.extra.jira;

import bucket.cache.CacheManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.renderer.radeox.macros.include.AbstractHttpRetrievalMacro;
import com.atlassian.confluence.security.GateKeeper;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.setup.ConfluenceBootstrapConstants;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.JiraIconMappingManager;
import com.atlassian.confluence.util.io.IOUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.core.util.FileUtils;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.atlassian.user.impl.cache.Cache;
import com.opensymphony.util.TextUtils;
import com.opensymphony.webwork.ServletActionContext;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.macro.parameter.MacroParameter;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends AbstractHttpRetrievalMacro
{
    private Logger log = Logger.getLogger(JiraIssuesMacro.class);

    /**
     * if true, this class will use a compressed form of the HTML in the cache.
     */
    private static final boolean USE_COMPRESSION = true;

    private static final String MACRO_REFRESH = "macro.refresh";
    private String[] myParamDescription = new String[]{"1: url", "?2: columns"};
    private List defaultColumns = new LinkedList();
    private CacheManager cacheManager;
    private BootstrapManager bootstrapManager;
    private GateKeeper gateKeeper;

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
        return myParamDescription;
    }

    public String getHtml(MacroParameter macroParameter) throws IllegalArgumentException, IOException
    {
        String url = cleanUrlParentheses(TextUtils.noNull(macroParameter.get("url", 0)).trim());
        String columns = TextUtils.noNull(macroParameter.get("columns", 1)).trim();
        String template = TextUtils.noNull(macroParameter.get("template", 3)).trim();
        boolean showCount = Boolean.valueOf(StringUtils.trim(macroParameter.get("count"))).booleanValue();

        long start = System.currentTimeMillis();
        if (log.isDebugEnabled())
        {
            log.debug("creating html content for url [ " + url + " ]");
            log.debug("showCount [ " + showCount + " ]");
            log.debug("template [ " + template + " ]");
            log.debug("columns [ " + columns + " ]");
        }

        String cacheParameter = macroParameter.get("cache", 2);
        boolean useCache = StringUtils.isBlank(cacheParameter) ? true : Boolean.valueOf(cacheParameter).booleanValue();
        CacheKey key = new CacheKey(url, columns, showCount, template);
        String html = getHtml(key, macroParameter.get("baseurl"), useCache);
        if (log.isDebugEnabled())
        {
            log.debug("created html content for url [ " + url + " ]");
            log.debug("length [ " + html.length() + " ]");
            log.debug("time [ " + (System.currentTimeMillis() - start) + " ]");
        }
        return html;
    }

    private String getHtml(CacheKey key, String baseurl, boolean useCache) throws IOException
    {
        Cache cache = cacheManager.getCache(JiraIssuesMacro.class.getName());

        boolean flush = !useCache || isCacheParameterSet();
        if (flush)
        {
            if (log.isDebugEnabled())
            {
                log.debug("flushing cache");
            }
            cache.remove(key);
        }

        String result = getFromCache(key, cache);
        if (result == null)
        {
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
            {
                log.debug((System.currentTimeMillis() - start) + ": retrieving new xml content");
            }

            String refreshUrl = getRefreshUrl();
            Map contextMap = MacroUtils.defaultVelocityContext();

            Element channel = fetchChannel(key.url);
            String clickableUrl = makeClickableUrl(key.url);
            if (TextUtils.stringSet(baseurl))
                clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());

            if (log.isDebugEnabled())
            {
                log.debug((System.currentTimeMillis() - start) + ": parsing xml content");
            }

            if (key.showCount)
            {
                result = countHtml(clickableUrl, channel);
            }
            else
            {
                contextMap.put("url", key.url);
                contextMap.put("clickableUrl", clickableUrl);
                contextMap.put("channel", channel);
                contextMap.put("entries", channel.getChildren("item"));
                contextMap.put("columns", prepareDisplayColumns(key.columns));
                contextMap.put("icons", prepareIconMap(channel));
                contextMap.put("refreshUrl", refreshUrl);

                if (log.isDebugEnabled())
                {
                    log.debug((System.currentTimeMillis() - start) + ": transforming to html");
                }

                if (key.template.equals(""))
                    result = VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues.vm", contextMap);
                else
                    result = VelocityUtils.getRenderedTemplate("templates/extra/jira/jiraissues-" + key.template + ".vm", contextMap);

                if (log.isDebugEnabled())
                {
                    log.debug((System.currentTimeMillis() - start) + " done");
                }
            }
            putToCache(key, cache, result);
        }

        return result;
    }

    public void putToCache(CacheKey key, Cache cache, String result) throws IOException
    {
        if (USE_COMPRESSION)
        {
            if (log.isDebugEnabled())
            {
                log.debug("compressing [ " + result.length() + " ] bytes for storage in the cache");
            }
            long start = System.currentTimeMillis();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            GZIPOutputStream out = new GZIPOutputStream(buf);
            out.write(result.getBytes(),0,result.length());
            out.finish();
            out.flush();
            byte[] data = buf.toByteArray();
            if (log.isDebugEnabled())
            {
                log.debug((System.currentTimeMillis() - start) + ": compressed to [ " + data.length + " ]");
            }
            cache.put(key, data);
        }
        else
        {
            cache.put(key, result);
        }
    }

    public String getFromCache(CacheKey key, Cache cache) throws IOException
    {
        if (USE_COMPRESSION)
        {
            try
            {
                byte[] data = (byte[]) cache.get(key);
                if (data == null)
                {
                    return null;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("decompressing [ " + data.length + " ] bytes into html");
                }
                long start = System.currentTimeMillis();
                ByteArrayInputStream bin = new ByteArrayInputStream(data);
                GZIPInputStream in = new GZIPInputStream(bin);
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                IOUtils.copy(in, buf);
                byte[] uncompressedData = buf.toByteArray();
                if (log.isDebugEnabled())
                {
                    log.debug((System.currentTimeMillis() - start) + ": decompressed to [ " + uncompressedData.length + " ]");
                }
                return new String(uncompressedData);
            }
            catch (IOException e)
            {
                log.debug(e);
                // if for any reason the cached data can not be decompressed
                // return null so the application thinks its not cached and
                // continues.  this might happen if the plugin is upgraded
                // from a version that doesn't cache to this version.
                return null;
            }
        }
        else
        {
            return (String) cache.get(key);
        }
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

    private String countHtml(String url, Element channel)
    {
        return "<a href=\"" + url + "\">" + channel.getChildren("item").size() + " issues</a>";
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

    private List prepareDisplayColumns(String columns)
    {
        if (columns == null || columns.equals(""))
        { // No "columns" defined so using the defaults!
            return defaultColumns;
        }
        else
        {
            StringTokenizer tokenizer = new StringTokenizer(columns, ",;");
            List list = new LinkedList();

            while (tokenizer.hasMoreTokens())
            {
                String col = tokenizer.nextToken().toLowerCase().trim();

                if (defaultColumns.contains(col) && !list.contains(col))
                    list.add(col);
            }

            if (list.isEmpty())
                return defaultColumns;
            else
                return list;
        }
    }

    private Map prepareIconMap(Element channel)
    {
        String link = channel.getChild("link").getValue().toString();
        // In pre 3.7 JIRA, the link is just http://domain/context, in 3.7 and later it is the full query URL,
        // which looks like http://domain/context/secure/IssueNaviagtor...
        int index = link.indexOf("/secure/IssueNavigator");
        if (index != -1)
        {
            link = link.substring(0, index);
        }
        String imagesRoot = link + "/images/icons/";
        Map result = new HashMap();

        JiraIconMappingManager iconMappingManager = (JiraIconMappingManager) ContainerManager.getComponent("jiraIconMappingManager");

        for (Iterator iterator = iconMappingManager.getIconMappings().entrySet().iterator(); iterator.hasNext();)
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

    protected Element fetchChannel(String url) throws IOException
    {
        Element channel;
        log.debug("Fetching XML feed " + url + " ...");
        HttpMethod method = null;
        InputStream in = null;
        try
        {
            method = retrieveRemoteUrl(url);
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            in = method.getResponseBodyAsStream();

            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1)
                bytesOut.write(bytes, 0, read);

            byte[] webContent = bytesOut.toByteArray();
            channel = getChannelElement(webContent, url);
        }
        finally
        {
            IOUtils.close(in);
            if (method != null)
            {
                try
                {
                    method.releaseConnection();
                }
                catch (Throwable t)
                {
                    log.error("Error calling HttpMethod.releaseConnection", t);
                }
            }
        }
        return channel;
    }

    private Element getChannelElement(byte[] webContent, String url) throws IOException
    {
        ByteArrayInputStream bufferedIn = null;
        try
        {
            bufferedIn = new ByteArrayInputStream(webContent);
            SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Document jdomDocument = saxBuilder.build(bufferedIn);
            return (Element) XPath.selectSingleNode(jdomDocument, "/rss//channel");
        }
        catch (JDOMException e)
        {
            String filename = "rssoutput" + url.hashCode() + ".txt";
            // store retrieved output in a text file that can be downloaded
            File rssParseError = new File(bootstrapManager.getFilePathProperty(ConfluenceBootstrapConstants.TEMP_DIR_PROP), filename);
            FileUtils.copyFile(new ByteArrayInputStream(webContent), rssParseError);

            String path = "/download/temp/" + filename;
            User user = getRemoteUser();

            if (user != null)
            {
                gateKeeper.addKey(path, user);
            }
            else
            {
                log.error("Could not reference remoteUser when trying to store results of RSS failure in temp dir.");
            }

            log.error("Error while trying to assemble the RSS result!", e);
            throw new IOException(e.getMessage() + " <a href='" + bootstrapManager.getWebAppContextPath() + "/download/temp/" + filename + "'>" + filename + "</a>");
        }
        finally
        {
            IOUtils.close(bufferedIn);
        }
    }


    /**
     * creates a URL including a refresh parameter to flush the cache of a macro (eg. JiraIssues Macro)
     *
     * @return String current url including the refresh parameter
     */
    protected String getRefreshUrl()
    {
        StringBuffer refreshUrl;
        HttpServletRequest request = ServletActionContext.getRequest();
        if (request != null)
        {
            refreshUrl = new StringBuffer(request.getRequestURI());
            String query = request.getQueryString();
            if (TextUtils.stringSet(query))
            {
                refreshUrl.append("?").append(query);

                if (request.getParameter(MACRO_REFRESH) == null)
                    refreshUrl.append("&").append(MACRO_REFRESH).append("=true");
            }
            else
                refreshUrl.append("?").append(MACRO_REFRESH).append("=true");

            return refreshUrl.toString();
        }
        return null;
    }

    /**
     * Checks the current url for a macro refresh parameter
     *
     * @return boolean whether or not the macro cache should be flushed
     */
    protected boolean isCacheParameterSet()
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        if (request != null)
        {
            String reloadParameter = request.getParameter(MACRO_REFRESH);
            if (reloadParameter != null)
            {
                return true;
            }
        }
        return false;
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



