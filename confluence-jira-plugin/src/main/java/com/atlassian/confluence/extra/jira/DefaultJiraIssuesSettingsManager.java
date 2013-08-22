package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

public class DefaultJiraIssuesSettingsManager implements JiraIssuesSettingsManager
{
    private static final Logger LOG = Logger.getLogger(DefaultJiraIssuesSettingsManager.class);

    private static final String BANDANA_KEY_COLUMN_MAPPING = "com.atlassian.confluence.extra.jira:customFieldsFor:";

    private static final String BANDANA_KEY_ICON_MAPPING = "atlassian.confluence.jira.icon.mappings";

    private final BandanaManager bandanaManager;

    private final CacheManager cacheManager;

    public DefaultJiraIssuesSettingsManager(BandanaManager bandanaManager, CacheManager cacheManager)
    {
        this.bandanaManager = bandanaManager;
        this.cacheManager = cacheManager;
    }

    private String getColumnMapBandanaKey(String jiraIssuesUrl)
    {
        return new StringBuilder(BANDANA_KEY_COLUMN_MAPPING).append(DigestUtils.md5Hex(jiraIssuesUrl)).toString();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getColumnMap(final String jiraIssuesUrl)
    {
        
        return (Map<String, String>)bandanaManager.getValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(jiraIssuesUrl));
                       
        
    }

    public void setColumnMap(final String jiraIssuesUrl, final Map<String, String> columnMapping)
    {
       
        bandanaManager.setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(jiraIssuesUrl),
                new HashMap<String, String>(columnMapping)
        );
                    
        
    }

    private Cache getSortingSettingsCache()
    {
        return cacheManager.getCache(getClass().getName());
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getIconMapping()
    {
        return (Map<String, String>) bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, BANDANA_KEY_ICON_MAPPING);
    }

    public void setIconMapping(final Map<String, String> iconMapping)
    {
       
        bandanaManager.setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                BANDANA_KEY_ICON_MAPPING,
                new HashMap<String, String>(iconMapping)
        );
                    
    }
}
