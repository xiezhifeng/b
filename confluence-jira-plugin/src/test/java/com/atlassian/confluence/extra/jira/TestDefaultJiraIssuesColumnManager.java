package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.util.i18n.I18NBean;
import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
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
            "subtasks", "fixversion", "timeoriginalestimate", "timeestimate", "statuscategory"
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
    private I18NBean i18NBean;

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
        when(i18nBeanFactory.getI18NBean().getText(any(String.class))).thenReturn("COLUMN NAME");

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
    /**
    * test method getColumnInfo() when the parameter appLink is null
    **/
    public void testColumnInfoRetrievalWithoutAppLink()
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put("cache", "off");
        params.put(": = | TOKEN_TYPE | = :", "BLOCK");
        params.put("url", "http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TST+AND+reporter+%3D+mhrynczak");
        params.put(": = | RAW | = :", "url=http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TST+AND+reporter+%3D+mhrynczak");
        List<JiraColumnInfo> expectedInfo = new ArrayList<JiraColumnInfo>();
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames("", null);
        // expected columnNames = "type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due"
        assertEquals(columnNames, Arrays.asList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due"));
        expectedInfo.add(new JiraColumnInfo("type", "T", Arrays.asList("type"), true));
        expectedInfo.add(new JiraColumnInfo("key", "Key", Arrays.asList("key"), true));
        expectedInfo.add(new JiraColumnInfo("summary", "Summary", Arrays.asList("summary"), true));
        expectedInfo.add(new JiraColumnInfo("assignee", "Assignee", Arrays.asList("assignee"), true));
        expectedInfo.add(new JiraColumnInfo("reporter", "Reporter", Arrays.asList("reporter"), true));
        expectedInfo.add(new JiraColumnInfo("priority", "P", Arrays.asList("priority"), true));
        expectedInfo.add(new JiraColumnInfo("status", "Status", Arrays.asList("status"), true));
        expectedInfo.add(new JiraColumnInfo("resolution", "Resolution", Arrays.asList("resolution"), true));
        expectedInfo.add(new JiraColumnInfo("created", "Created", Arrays.asList("created"), true));
        expectedInfo.add(new JiraColumnInfo("updated", "Updated", Arrays.asList("updated"), true));
        expectedInfo.add(new JiraColumnInfo("due", "Due", Arrays.asList("due"), true));
        assertEquals(expectedInfo, defaultJiraIssuesColumnManager.getColumnInfo(params, Collections.<String, JiraColumnInfo>emptyMap(), null));
    }

    /**
    * test method getColumnsInfoFromJira() when the parameter appLink is null, an empty map should be returned
    **/
    public void testColumnsInfoRetrievalFromJiraWithoutAppLink()
    {
        assertEquals(defaultJiraIssuesColumnManager.getColumnsInfoFromJira(null), Collections.<String, JiraColumnInfo>emptyMap());
    }

    private class DefaultJiraIssuesColumnManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager
    {
        private DefaultJiraIssuesColumnManager()
        {
            super(jiraIssuesSettingsManager, localeManager,i18nBeanFactory, jiraConnectorManager);
        }
    }

    /**
     * test method getColumnsInfoFromJira() when valid JSON from JIRA is passed
     */
    public void testGetColumnsInfoFromJiraWithValidJson() throws Exception
    {
        final ReadOnlyApplicationLink applink = mock(ReadOnlyApplicationLink.class);
        final ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);
        final ApplicationLinkRequestFactory applicationLinkRequestFactory = mock(ApplicationLinkRequestFactory.class);
        final String json = StringUtils.join(IOUtils.readLines(this.getClass().getClassLoader().getResourceAsStream("JiraColumnInfo.json")).toArray());
        when(applink.createAuthenticatedRequestFactory()).thenReturn(applicationLinkRequestFactory);
        when(applicationLinkRequestFactory.createRequest(any(),anyString())).thenReturn(applicationLinkRequest);
        when(applicationLinkRequest.execute()).thenReturn(json);
        final Map<String, JiraColumnInfo> columns = defaultJiraIssuesColumnManager.getColumnsInfoFromJira(applink);

        JiraColumnInfo url = columns.get("customfield_10100");
        assertEquals("URL type column does not have URL title in JSON","URL",url.getTitle());
        assertTrue("URL type column does not have URL column type",url.isUrlColumn());

        JiraColumnInfo issueType = columns.get("issuetype");
        assertEquals("Issue Type column does not have Issue Type title in JSON","Issue Type",issueType.getTitle());
        assertFalse("Issue Type column has URL column type",issueType.isUrlColumn());

        JiraColumnInfo epicName = columns.get("customfield_10005");
        assertEquals("Epic Name type column does not have Epic Name type title in JSON","Epic Name",epicName.getTitle());
        assertFalse("Epic Name type column has URL column type",epicName.isUrlColumn());
    }
}
