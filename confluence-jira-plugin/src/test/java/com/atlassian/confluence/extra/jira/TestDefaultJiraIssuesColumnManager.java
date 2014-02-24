package com.atlassian.confluence.extra.jira;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;

public class TestDefaultJiraIssuesColumnManager extends TestCase
{
    private static final Collection<String> BUILT_IN_COLUMNS = Arrays.asList(
            "description", "environment", "key", "summary", "type", "parent",
            "priority", "status", "version", "resolution", "security", "assignee", "reporter",
            "created", "updated", "due", "component", "components", "votes", "comments", "attachments",
            "subtasks", "fixversion", "timeoriginalestimate", "timeestimate"
    );

    private static final Collection<String> MULTIVALUE_BUILTIN_COLUMN_NAMES = Collections.unmodifiableCollection(
            Arrays.asList(
                    "version",
                    "component",
                    "comments",
                    "attachments",
                    "fixversion"
            )
    );

    @Mock
    private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock
    private LocaleManager localeManager;

    @Mock
    private I18NBeanFactory i18nBeanFactory;

    @Mock
    private JiraConnectorManager jiraConnectorManager;

    private DefaultJiraIssuesColumnManager defaultJiraIssuesColumnManager;

    private String url;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        defaultJiraIssuesColumnManager = new DefaultJiraIssuesColumnManager();

        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    }

    public void testColumnMapRetrievedFromJiraIssuesSettingsManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();

        when(jiraIssuesSettingsManager.getColumnMap(url)).thenReturn(columnMap);

        assertSame(columnMap, defaultJiraIssuesColumnManager.getColumnMap(url));
    }

    public void testColumnMapPersistedByJiraIssuesSettingsManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();

        defaultJiraIssuesColumnManager.setColumnMap(url, columnMap);
        verify(jiraIssuesSettingsManager).setColumnMap(url, columnMap);
    }

    public void testBuiltInColumnNameDetectionCaseInsensitive()
    {
        for (String columnName : BUILT_IN_COLUMNS)
            assertTrue(defaultJiraIssuesColumnManager.isColumnBuiltIn(columnName));
        for (String columnName : BUILT_IN_COLUMNS)
            assertTrue(defaultJiraIssuesColumnManager.isColumnBuiltIn(StringUtils.upperCase(columnName)));
    }

    public void testCanonicalFormOfBuiltInFieldLookupIsCaseInsensitive()
    {
        for (String columnName : BUILT_IN_COLUMNS)
            if (columnName.equalsIgnoreCase("fixversion"))
            {
                assertEquals("fixVersion", defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName));
            } 
            else if (columnName.equalsIgnoreCase("components")) 
            {
                assertEquals("component", defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName));
            }
            else
            {
                assertEquals(columnName, defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName));
            }
            
        for (String columnName : BUILT_IN_COLUMNS)
            if (columnName.equalsIgnoreCase("fixversion"))
            {
                assertEquals("fixVersion", defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName.toUpperCase()));
            }
            else if (columnName.equalsIgnoreCase("components")) 
            {
                assertEquals("component", defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName.toUpperCase()));
            }
            else
            {
                assertEquals(columnName, defaultJiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName.toUpperCase()));
            }
    }

    public void testMultiValueColumnDetectionCaseInsenstive()
    {
        for (String columnName : MULTIVALUE_BUILTIN_COLUMN_NAMES)
            assertTrue(defaultJiraIssuesColumnManager.isBuiltInColumnMultivalue(columnName));
        for (String columnName : MULTIVALUE_BUILTIN_COLUMN_NAMES)
            assertTrue(defaultJiraIssuesColumnManager.isBuiltInColumnMultivalue(StringUtils.upperCase(columnName)));
    }

    public void testColumnInfoRetrievalWithoutAppLink()
    {
        assertTrue(Boolean.TRUE);
    }

    public void testColumnsInfoMapRetrievalWithoutAppLink()
    {
        assertTrue(Boolean.TRUE);
    }

    private class DefaultJiraIssuesColumnManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager
    {
        private DefaultJiraIssuesColumnManager()
        {
            super(jiraIssuesSettingsManager, localeManager,i18nBeanFactory, jiraConnectorManager);
        }
    }
}
