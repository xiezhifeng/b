package com.atlassian.confluence.extra.jira;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;

public class DefaultJiraIssuesSettingsManager implements JiraIssuesSettingsManager
{

    private static final String BANDANA_KEY_COLUMN_MAPPING = "com.atlassian.confluence.extra.jira:customFieldsFor:";
    private static final String BANDANA_KEY_ICON_MAPPING = "atlassian.confluence.jira.icon.mappings";

    private final BandanaManager bandanaManager;

    public DefaultJiraIssuesSettingsManager(final BandanaManager bandanaManager)
    {
        this.bandanaManager = bandanaManager;
    }

    private static String getColumnMapBandanaKey(final String jiraIssuesUrl)
    {
        return new StringBuilder(BANDANA_KEY_COLUMN_MAPPING).append(DigestUtils.md5Hex(jiraIssuesUrl)).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getColumnMap(final String jiraIssuesUrl)
    {
        return (Map<String, String>) this.bandanaManager.getValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(jiraIssuesUrl));


    }

    @Override
    public void setColumnMap(final String jiraIssuesUrl, final Map<String, String> columnMapping)
    {
        this.bandanaManager.setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                getColumnMapBandanaKey(jiraIssuesUrl),
                new HashMap<String, String>(columnMapping)
                );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getIconMapping()
    {
        return (Map<String, String>) this.bandanaManager.getValue(ConfluenceBandanaContext.GLOBAL_CONTEXT, BANDANA_KEY_ICON_MAPPING);
    }

    @Override
    public void setIconMapping(final Map<String, String> iconMapping)
    {
        this.bandanaManager.setValue(
                ConfluenceBandanaContext.GLOBAL_CONTEXT,
                BANDANA_KEY_ICON_MAPPING,
                new HashMap<String, String>(iconMapping)
                );
    }
}
