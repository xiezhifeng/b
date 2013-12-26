package com.atlassian.confluence.extra.jira;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JiraConnectorUtils.class)
public class TestDefaultJiraIssuesColumnManager extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(TestDefaultJiraIssuesColumnManager.class);
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

    private class DefaultJiraIssuesColumnManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager
    {
        private DefaultJiraIssuesColumnManager()
        {
            super(jiraIssuesSettingsManager);
        }
    }
    
    public void testGetColumnsInfoFromJira() throws CredentialsRequiredException, ResponseException
    {
        PowerMockito.mockStatic(JiraConnectorUtils.class);
        
        ApplicationLink applicationLink = mock(ApplicationLink.class);
        ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);
        
        when(JiraConnectorUtils.getApplicationLinkRequest(applicationLink, MethodType.GET, "/rest/api/2/field")).thenReturn(applicationLinkRequest);
        when(applicationLink.getId()).thenReturn(new ApplicationId(UUID.randomUUID().toString()));
        
        List<JiraColumnInfo> columns = new ArrayList<JiraColumnInfo>();
        columns.add(new JiraColumnInfo("summary", "Summary", true));
        columns.add(new JiraColumnInfo("key", "Key", true));
        columns.add(new JiraColumnInfo("effectversion", "Effective Version", false));
        Gson gson = new Gson();
        String json = gson.toJson(columns);
        
        when(applicationLinkRequest.execute()).thenReturn(json);
        Map<String, JiraColumnInfo> jiraColumns = defaultJiraIssuesColumnManager.getColumnsInfoFromJira(applicationLink);
        for (JiraColumnInfo column : columns)
        {
            Assert.assertTrue(jiraColumns.containsKey(column.getKey()));
        }
    }

    public void testGetColumnsInfoFromJiraHasSameDataStructure() throws CredentialsRequiredException, ResponseException
    {
        PowerMockito.mockStatic(JiraConnectorUtils.class);
        
        ApplicationLink applicationLink = mock(ApplicationLink.class);
        ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);
        
        when(JiraConnectorUtils.getApplicationLinkRequest(applicationLink, MethodType.GET, "/rest/api/2/field")).thenReturn(applicationLinkRequest);
        when(applicationLink.getId()).thenReturn(new ApplicationId(UUID.randomUUID().toString()));
        String fieldsJson = "[" + "{\"id\":\"customfield_10560\",\"name\":\"Reviewers\",\"custom\":true,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"array\",\"items\":\"user\",\"custom\":\"com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker\",\"customId\":10560}}," + "{\"id\":\"summary\",\"name\":\"Summary\",\"custom\":false,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"string\",\"system\":\"summary\"}}"+"]";
        Gson gson = new Gson();
        List<JiraColumnInfo> columns = gson.fromJson(fieldsJson, new TypeToken<List<JiraColumnInfo>>() {}.getType());
        
        when(applicationLinkRequest.execute()).thenReturn(fieldsJson);
        Map<String, JiraColumnInfo> jiraColumns = defaultJiraIssuesColumnManager.getColumnsInfoFromJira(applicationLink);
        
        for (JiraColumnInfo column : columns)
        {
            Assert.assertTrue(jiraColumns.containsKey(column.getKey()));
        }
    }
}
