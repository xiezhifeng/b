package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import junit.framework.TestCase;
import org.apache.commons.codec.digest.DigestUtils;
import org.mockito.Mock;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

public class TestDefaultJiraIssuesSettingsManager extends TestCase
{
    @Mock private BandanaManager bandanaManager;

    @Mock private CacheManager cacheManager;

    @Mock private Cache cache;

    private DefaultJiraIssuesSettingsManager defaultJiraIssuesSettingsManager;

    private String url;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(cacheManager.getCache(DefaultJiraIssuesSettingsManager.class.getName())).thenReturn(cache);

        defaultJiraIssuesSettingsManager = new DefaultJiraIssuesSettingsManager();
        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    }

    private String getColumnMapBandanaKey(String jiraIssuesUrl)
    {
        return new StringBuilder("com.atlassian.confluence.extra.jira:customFieldsFor:").append(DigestUtils.md5Hex(jiraIssuesUrl)).toString();
    }

    public void testGetColumnMappingNotDefinedIfNoneSet()
    {
        assertNull(defaultJiraIssuesSettingsManager.getColumnMap(url));
    }

    public void testGetColumnMappingIfOneSet()
    {
        Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("a", "b");

        when(bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, getColumnMapBandanaKey(url))).thenReturn(columnMap);
        assertEquals(columnMap, defaultJiraIssuesSettingsManager.getColumnMap(url));
    }

    public void testColumnMapPersistedByBandana()
    {
        Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("a", "b");

        defaultJiraIssuesSettingsManager.setColumnMap(url, columnMap);

        verify(bandanaManager).setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(url),
                columnMap
        );
    }

    public void testGetIconMappingNotDefinedIfNoneSet()
    {
        assertNull(defaultJiraIssuesSettingsManager.getIconMapping());
    }

    public void testGetIconMappingIfOneSet()
    {
        Map<String, String> iconMap = new HashMap<String, String>();
        iconMap.put("Bug", "bug.gif");

        when(bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, "atlassian.confluence.jira.icon.mappings")).thenReturn(iconMap);

        assertEquals(iconMap, defaultJiraIssuesSettingsManager.getIconMapping());
    }

    public void testIconMappingPersistedByBandana()
    {
        Map<String, String> iconMap = new HashMap<String, String>();
        iconMap.put("Bug", "bug.gif");

        defaultJiraIssuesSettingsManager.setIconMapping(iconMap);
        verify(bandanaManager).setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                "atlassian.confluence.jira.icon.mappings",
                iconMap
        );
    }

    private class DefaultJiraIssuesSettingsManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesSettingsManager
    {
        private DefaultJiraIssuesSettingsManager()
        {
            super(bandanaManager, cacheManager);
        }
    }
}
