package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utilities for JIRA Issues Macro.
 */
public class JiraIssuesUtils
{
    private static final Logger log = Logger.getLogger(JiraIssuesUtils.class);

    static final String BANDANA_CUSTOM_FIELDS_PREFIX = "com.atlassian.confluence.extra.jira:customFieldsFor:";
    static final String BANDANA_SORTING_PREFIX = "com.atlassian.confluence.extra.jira:sorting:";
    static final String SAX_PARSER_CLASS = "org.apache.xerces.parsers.SAXParser";
    private static final int MILLIS_PER_HOUR = 3600000;
    
    private PlatformTransactionManager transactionManager;
    private BandanaManager bandanaManager;
    private JiraIconMappingManager jiraIconMappingManager;
    private TrustedTokenAuthenticator trustedTokenAuthenticator;
    private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;
    private HttpRetrievalService httpRetrievalService;

    public PlatformTransactionManager getTransactionManager()
    {
        if (transactionManager == null)
        {
            transactionManager = (PlatformTransactionManager) ContainerManager.getComponent("transactionManager");
        }
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    public void setBandanaManager(BandanaManager bandanaManager)
    {
        this.bandanaManager = bandanaManager;
    }

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    public void setTrustedTokenFactory(TrustedTokenFactory trustedTokenFactory)
    {
        this.trustedTokenAuthenticator = new TrustedTokenAuthenticator(trustedTokenFactory);
    }

    public void setTrustedConnectionStatusBuilder(TrustedConnectionStatusBuilder trustedConnectionStatusBuilder)
    {
        this.trustedConnectionStatusBuilder = trustedConnectionStatusBuilder;
    }

    public void setHttpRetrievalService(HttpRetrievalService httpRetrievalService)
    {
        this.httpRetrievalService = httpRetrievalService;
    }

    public Map getColumnMap(String jiraIssuesUrl)
    {
        ConfluenceBandanaContext globalContext = new ConfluenceBandanaContext();        
        Object cachedObject = bandanaManager.getValue(globalContext, customFieldsBandanaKeyForUrl(jiraIssuesUrl));
        if (cachedObject != null)
        {
            return (Map) cachedObject;
        }
        else
        {
            return null;
        }
    }
    
    public void putColumnMap(final String jiraIssuesUrl, final Map columnMap) // TODO what effect does final have here
    {
        TransactionTemplate template = new TransactionTemplate(getTransactionManager());
        template.execute(new TransactionCallback()
        {
            public Object doInTransaction(TransactionStatus transactionStatus)
            {
                ConfluenceBandanaContext globalContext = new ConfluenceBandanaContext();
                bandanaManager.setValue(globalContext, customFieldsBandanaKeyForUrl(jiraIssuesUrl), columnMap);
                return null;
            }
        });

    }

    public Boolean getSortSetting(String jiraIssuesUrl)
    {
        ConfluenceBandanaContext globalContext = new ConfluenceBandanaContext();
        Object cachedObject = bandanaManager.getValue(globalContext, sortingBandanaKeyForUrl(jiraIssuesUrl));
        if (cachedObject != null)
        {
            SortSettingCacheObject sortSetting = (SortSettingCacheObject) cachedObject;
            if((new Date().getTime())-sortSetting.getTimeRefreshed()<=MILLIS_PER_HOUR)
                return Boolean.valueOf(sortSetting.isEnableSort());
        }
        return null;
    }

    public void putSortSetting(final String jiraIssuesUrl, final boolean enableSort)
    {
        TransactionTemplate template = new TransactionTemplate(getTransactionManager());
        final SortSettingCacheObject sortSetting = new SortSettingCacheObject();
        sortSetting.setEnableSort(enableSort);
        sortSetting.setTimeRefreshed(new Date().getTime());

        template.execute(new TransactionCallback()
        {
            public Object doInTransaction(TransactionStatus transactionStatus)
            {
                ConfluenceBandanaContext globalContext = new ConfluenceBandanaContext();
                bandanaManager.setValue(globalContext, sortingBandanaKeyForUrl(jiraIssuesUrl), sortSetting);
                return null;
            }
        });
    }

    /**
     * @param url jira issues url
     * @return custom fields prefix + the md5 encoded url (the url is md5 encoded to keep the key length under 100 characters)
     */
    private String customFieldsBandanaKeyForUrl(String url)
    {
        return BANDANA_CUSTOM_FIELDS_PREFIX + DigestUtils.md5Hex(url);
    }

    /**
     * @param url jira issues url
     * @return sorting prefix + the md5 encoded url (the url is md5 encoded to keep the key length under 100 characters)
     */
    private String sortingBandanaKeyForUrl(String url)
    {
        return BANDANA_SORTING_PREFIX + DigestUtils.md5Hex(url);
    }
    
    public String getColumnMapKeyFromUrl(String url)
    {
        if (url.indexOf("?") > 0)
        {
            return url.substring(0,url.indexOf("?"));
        }
        else
        {
            return url;
        }
    }

    public Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
    {
        HttpRequest req = httpRetrievalService.getDefaultRequestFor(url);
        if (useTrustedConnection)
        {
            req.setAuthenticator(trustedTokenAuthenticator);
        }

        HttpResponse resp = httpRetrievalService.get(req);
        try
        {
            if (resp.getStatusCode() != 200)
            {
                // tempMax is invalid CONFJIRA-49
                if (resp.getStatusCode() == 403)
                {
                    throw new IllegalArgumentException(resp.getStatusMessage());
                }
                else
                {
                    // we're not sure how to handle any other error conditions at this point
                    throw new RuntimeException(resp.getStatusMessage());
                }
            }
            Element channelElement = getChannelElement(resp.getResponse());

            TrustedConnectionStatus trustedConnectionStatus = null;
            if (useTrustedConnection)
            {
                trustedConnectionStatus = trustedConnectionStatusBuilder.getTrustedConnectionStatus(resp);
            }

            return new Channel(channelElement, trustedConnectionStatus);
        }
        finally
        {
            resp.finish();
        }
    }

    public Map prepareIconMap(Element channel)
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
                result.put((String) entry.getKey(), icon);
            else
                result.put((String) entry.getKey(), imagesRoot + icon);
        }

        return result;
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
    
    public String findIconUrl( Element xmlItemField, Map iconMap )
    {
        String iconUrl = "";
        
        if( xmlItemField != null ) 
        {
            String value = xmlItemField.getValue();
            
            // first look for icon in user-set mapping, and then check in the xml returned from jira
            iconUrl = (String) iconMap.get(value);
            if(StringUtils.isBlank(iconUrl) )
                iconUrl = StringUtils.defaultString(xmlItemField.getAttributeValue("iconUrl"));
        }
        
        return iconUrl;
    }


    /*
    * fetchChannel needs to return its result plus a trusted connection status. This is a value class to allow this.
    */
    public final static class Channel
    {
        private final Element element;
        private final TrustedConnectionStatus trustedConnectionStatus;

        protected Channel(Element element, TrustedConnectionStatus trustedConnectionStatus)
        {
            this.element = element;
            this.trustedConnectionStatus = trustedConnectionStatus;
        }

        public Element getElement()
        {
            return element;
        }

        public TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return trustedConnectionStatus;
        }

        public boolean isTrustedConnection()
        {
            return trustedConnectionStatus != null;
        }
    }
}
