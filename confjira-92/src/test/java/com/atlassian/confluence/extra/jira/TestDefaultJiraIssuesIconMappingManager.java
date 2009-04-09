package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestDefaultJiraIssuesIconMappingManager extends TestCase
{
    private static final Map<String, String> DEFAULT_ICON_MAPPING;

    static
    {
        Map<String, String> mutableIconMapping = new HashMap<String, String>();

        mutableIconMapping.put("Bug", "bug.gif");
        mutableIconMapping.put("New Feature", "newfeature.gif");
        mutableIconMapping.put("Task", "task.gif");
        mutableIconMapping.put("Sub-task", "issue_subtask.gif");
        mutableIconMapping.put("Improvement", "improvement.gif");

        mutableIconMapping.put("Blocker", "priority_blocker.gif");
        mutableIconMapping.put("Critical", "priority_critical.gif");
        mutableIconMapping.put("Major", "priority_major.gif");
        mutableIconMapping.put("Minor", "priority_minor.gif");
        mutableIconMapping.put("Trivial", "priority_trivial.gif");

        mutableIconMapping.put("Assigned", "status_assigned.gif");
        mutableIconMapping.put("Closed", "status_closed.gif");
        mutableIconMapping.put("In Progress", "status_inprogress.gif");
        mutableIconMapping.put("Need Info", "status_needinfo.gif");
        mutableIconMapping.put("Open", "status_open.gif");
        mutableIconMapping.put("Reopened", "status_reopened.gif");
        mutableIconMapping.put("Resolved", "status_resolved.gif");
        mutableIconMapping.put("Unassigned", "status_unassigned.gif");

        DEFAULT_ICON_MAPPING = Collections.unmodifiableMap(mutableIconMapping);
    }

    @Mock
    private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    private DefaultJiraIssuesIconMappingManager defaultJiraIssuesIconMappingManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(DEFAULT_ICON_MAPPING);
        defaultJiraIssuesIconMappingManager = new DefaultJiraIssuesIconMappingManager();
    }

    public void testDefaultMappingReturnedIfNoneCustomized()
    {
        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(null);
        assertEquals(DEFAULT_ICON_MAPPING, defaultJiraIssuesIconMappingManager.getBaseIconMapping());
    }

    public void testCustomMappingReturnedIfOneCustomized()
    {
        Map<String, String> customIconMapping = new HashMap<String, String>(DEFAULT_ICON_MAPPING);
        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(customIconMapping);

        assertSame(customIconMapping, defaultJiraIssuesIconMappingManager.getBaseIconMapping());
    }

    public void testGetFullIconMappingFromJiraThreePointSeven()
    {
        String link = "http://localhost:1990/jira";
        Map<String, String> customBaseIconMap = new HashMap<String, String>();
        Map<String, String> fullIconMap = new HashMap<String, String>();

        customBaseIconMap.put("Bug", "bug.gif");
        fullIconMap.put("Bug", link + "/images/icons/bug.gif");

        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(customBaseIconMap);
        assertEquals(fullIconMap, defaultJiraIssuesIconMappingManager.getFullIconMapping(link));
    }

    public void testGetFullIconMappingFromJiraThreePointSevenOnwards()
    {
        String link = "http://localhost:1990/jira/secure/IssueNavigator/foo/bar/baz";
        Map<String, String> customBaseIconMap = new HashMap<String, String>();
        Map<String, String> fullIconMap = new HashMap<String, String>();

        customBaseIconMap.put("Bug", "bug.gif");
        fullIconMap.put("Bug", "http://localhost:1990/jira/images/icons/bug.gif");

        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(customBaseIconMap);
        assertEquals(fullIconMap, defaultJiraIssuesIconMappingManager.getFullIconMapping(link));
    }

    public void testGetFullIconMappingWithCustomizedMappingWithAbsoluteUrls()
    {
        String link = "http://localhost:1990/jira/secure/IssueNavigator/foo/bar/baz";
        Map<String, String> customBaseIconMap = new HashMap<String, String>();
        Map<String, String> fullIconMap = new HashMap<String, String>();

        customBaseIconMap.put("Bug", "http://localhost:1991/jira/images/icons/bug.gif");
        fullIconMap.put("Bug", "http://localhost:1991/jira/images/icons/bug.gif");

        when(jiraIssuesSettingsManager.getIconMapping()).thenReturn(customBaseIconMap);
        assertEquals(fullIconMap, defaultJiraIssuesIconMappingManager.getFullIconMapping(link));
    }

    public void testMappingAdditionSaved()
    {
        Map<String, String> customIconMapping = new HashMap<String, String>(DEFAULT_ICON_MAPPING);
        customIconMapping.put("Foo", "bar.gif");

        defaultJiraIssuesIconMappingManager.addBaseIconMapping("Foo", "bar.gif");
        verify(jiraIssuesSettingsManager).setIconMapping(customIconMapping);
    }

    public void testMappingRemovalSaved()
    {
        Map<String, String> customIconMapping = new HashMap<String, String>(DEFAULT_ICON_MAPPING);
        customIconMapping.remove("Bug");

        defaultJiraIssuesIconMappingManager.removeBaseIconMapping("Bug");
        verify(jiraIssuesSettingsManager).setIconMapping(customIconMapping);
    }

    public void testNoMappingIfIssueTypeNotMapped()
    {
        assertFalse(defaultJiraIssuesIconMappingManager.hasBaseIconMapping("Foobarbaz"));
    }

    public void testHasMappingIfIssueTypeMapped()
    {
        assertTrue(defaultJiraIssuesIconMappingManager.hasBaseIconMapping("Bug"));
    }

    public void testIconFileNameIsNullForUnmappedIssueType()
    {
        assertNull(defaultJiraIssuesIconMappingManager.getIconFileName("Foobarbaz"));
    }

    public void testIconFileNameNotNullForMappedIssueType()
    {
        assertNotNull(defaultJiraIssuesIconMappingManager.getIconFileName("Bug"));
    }

    private class DefaultJiraIssuesIconMappingManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesIconMappingManager
    {
        private DefaultJiraIssuesIconMappingManager()
        {
            super(jiraIssuesSettingsManager);
        }
    }
}
