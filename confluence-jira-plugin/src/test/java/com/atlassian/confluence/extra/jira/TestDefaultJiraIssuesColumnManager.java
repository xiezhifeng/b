package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.util.i18n.I18NBean;
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

    private static final String PROP_KEY_PREFIX = "jiraissues.column.";

    @Mock
    private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock
    private LocaleManager localeManager;

    @Mock
    private I18NBeanFactory i18nBeanFactory;
    @Mock private I18NBean i18NBean;

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
        when(i18nBeanFactory.getI18NBean()).thenReturn(i18NBean);
        when(i18nBeanFactory.getI18NBean(any(Locale.class))).thenReturn(i18NBean);

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
    /*
    * test method getColumnInfo() when the parameter appLink is null
    * */
    public void testColumnInfoRetrievalWithoutAppLink()
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "off");
        params.put(": = | TOKEN_TYPE | = :", "BLOCK");
        params.put("url", "http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TST+AND+reporter+%3D+mhrynczak");
        params.put(": = | RAW | = :", "url=http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TST+AND+reporter+%3D+mhrynczak");
        List<JiraColumnInfo> expectedInfo = new ArrayList<JiraColumnInfo>();
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames("");
        for (String columnName : columnNames)
        {
            expectedInfo.add(new JiraColumnInfo(columnName, getDisplayName(columnName, columnName), Arrays.asList(columnName), true));
        }
        assertEquals(expectedInfo, defaultJiraIssuesColumnManager.getColumnInfo(params, Collections.<String, JiraColumnInfo>emptyMap(), null));
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

    private String getDisplayName(final String key, final String columnName)
    {
        if (key.contains(JiraIssueSortableHelper.SINGLE_QUOTE) || columnName.contains(JiraIssueSortableHelper.SINGLE_QUOTE))
        {
            return columnName;
        }
        String i18nKey = PROP_KEY_PREFIX + key;
        String displayName = i18NBean.getText(i18nKey);

        if (StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
        {
            displayName = columnName;
        }
        return displayName;
    }
}
