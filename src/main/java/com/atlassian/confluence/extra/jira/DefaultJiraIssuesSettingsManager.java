package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

public class DefaultJiraIssuesSettingsManager implements JiraIssuesSettingsManager
{
    private static final String BANDANA_KEY_COLUMN_MAPPING = "com.atlassian.confluence.extra.jira:customFieldsFor:";

    private static final String BANDANA_KEY_ICON_MAPPING = "atlassian.confluence.jira.icon.mappings";

    private final PlatformTransactionManager platformTransactionManager;

    private final BandanaManager bandanaManager;

    private final CacheManager cacheManager;

    public DefaultJiraIssuesSettingsManager(PlatformTransactionManager platformTransactionManager, BandanaManager bandanaManager, CacheManager cacheManager)
    {
        this.platformTransactionManager = platformTransactionManager;
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
        return (Map<String, String>) new TransactionTemplate(platformTransactionManager).execute(
                new TransactionCallback()
                {
                    public Object doInTransaction(TransactionStatus transactionStatus)
                    {
                        return bandanaManager.getValue(
                                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                                getColumnMapBandanaKey(jiraIssuesUrl)
                        );
                    }
                }
        );
    }

    public void setColumnMap(final String jiraIssuesUrl, final Map<String, String> columnMapping)
    {
        new TransactionTemplate(platformTransactionManager).execute(
                new TransactionCallbackWithoutResult()
                {
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus)
                    {
                        bandanaManager.setValue(
                                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                                getColumnMapBandanaKey(jiraIssuesUrl),
                                new HashMap<String, String>(columnMapping)
                        );
                    }
                }
        );
    }

    private Cache getSortingSettingsCache()
    {
        return cacheManager.getCache(getClass().getName());
    }

    public Sort getSort(String jiraIssuesUrl)
    {
        /*
         * Previously, the sort status are stored as Bandana objects with a timestamp. If it expires (after 1h),
         * the sort settings is calculated again.
         *
         * Now instead of using Bandana (permanent data), since the data stored there is not permanent
         * and expires in 1h by default.
         */
        Sort sort = (Sort) getSortingSettingsCache().get(jiraIssuesUrl);
        return null == sort ? Sort.SORT_UNKNOWN : sort;
    }

    public void setSort(String jiraIssuesUrl, Sort sort)
    {
        getSortingSettingsCache().put(jiraIssuesUrl, sort);
    }


    @SuppressWarnings("unchecked")
    public Map<String, String> getIconMapping()
    {
        return  (Map<String, String>) new TransactionTemplate(platformTransactionManager).execute(
                new TransactionCallback()
                {
                    public Object doInTransaction(TransactionStatus transactionStatus)
                    {
                        return bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, BANDANA_KEY_ICON_MAPPING);
                    }
                }
        );
    }

    public void setIconMapping(final Map<String, String> iconMapping)
    {
        new TransactionTemplate(platformTransactionManager).execute(
                new TransactionCallbackWithoutResult()
                {
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus)
                    {
                        bandanaManager.setValue(
                                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                                BANDANA_KEY_ICON_MAPPING,
                                new HashMap<String, String>(iconMapping)
                        );
                    }
                }
        );
    }
}
