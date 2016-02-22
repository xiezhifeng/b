package com.atlassian.confluence.extra.jira;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.codec.digest.DigestUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;

public class TestDefaultJiraIssuesSettingsManager extends TestCase
{
    @Mock BandanaManager bandanaManager;

    private DefaultJiraIssuesSettingsManager defaultJiraIssuesSettingsManager;

    private String url;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        this.defaultJiraIssuesSettingsManager = new DefaultJiraIssuesSettingsManager();
        this.url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    }

    private static String getColumnMapBandanaKey(final String jiraIssuesUrl)
    {
        return new StringBuilder("com.atlassian.confluence.extra.jira:customFieldsFor:").append(DigestUtils.md5Hex(jiraIssuesUrl)).toString();
    }

    public void testGetColumnMappingNotDefinedIfNoneSet()
    {
        assertNull(this.defaultJiraIssuesSettingsManager.getColumnMap(this.url));
    }

    public void testGetColumnMappingIfOneSet()
    {
        final Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("a", "b");

        when(this.bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, getColumnMapBandanaKey(this.url))).thenReturn(columnMap);
        assertEquals(columnMap, this.defaultJiraIssuesSettingsManager.getColumnMap(this.url));
    }

    public void testColumnMapPersistedByBandana()
    {
        final Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("a", "b");

        this.defaultJiraIssuesSettingsManager.setColumnMap(this.url, columnMap);

        verify(this.bandanaManager).setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(this.url),
                columnMap
                );
    }

    public void testGetIconMappingNotDefinedIfNoneSet()
    {
        assertNull(this.defaultJiraIssuesSettingsManager.getIconMapping());
    }

    public void testGetIconMappingIfOneSet()
    {
        final Map<String, String> iconMap = new HashMap<String, String>();
        iconMap.put("Bug", "bug.gif");

        when(this.bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, "atlassian.confluence.jira.icon.mappings")).thenReturn(iconMap);

        assertEquals(iconMap, this.defaultJiraIssuesSettingsManager.getIconMapping());
    }

    public void testIconMappingPersistedByBandana()
    {
        final Map<String, String> iconMap = new HashMap<String, String>();
        iconMap.put("Bug", "bug.gif");

        this.defaultJiraIssuesSettingsManager.setIconMapping(iconMap);
        verify(this.bandanaManager).setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                "atlassian.confluence.jira.icon.mappings",
                iconMap
                );
    }

    private class DefaultJiraIssuesSettingsManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesSettingsManager
    {
        DefaultJiraIssuesSettingsManager()
        {
            super(TestDefaultJiraIssuesSettingsManager.this.bandanaManager);
        }
    }
}
