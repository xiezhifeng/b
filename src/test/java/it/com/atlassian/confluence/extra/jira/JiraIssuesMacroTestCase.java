package it.com.atlassian.confluence.extra.jira;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class JiraIssuesMacroTestCase extends AbstractJiraMacrosPluginTestCase
{
    private static final String URL_TYPE_XML = "jira.issueviews:searchrequest-xml";

    private static final String URL_TYPE_RSS_ISSUES = "jira.issueviews:searchrequest-rss";

    private static final String URL_TYPE_RSS_COMMENTS = "jira.issueviews:searchrequest-comments-rss";

    private String getJiraIssuesXmlUrl()
    {
        return getJiraIssuesXmlUrl(1000);
    }

    private String getJiraIssuesXmlUrl(int resultsPerPage)
    {
        return getJiraIssuesXmlUrl(URL_TYPE_XML, resultsPerPage);
    }

    private String getJiraIssuesXmlUrl(String type, int resultsPerPage)
    {
        return new StringBuffer(jiraWebTester.getTestContext().getBaseUrl().toString())
                .append("sr/").append(type).append("/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC&tempMax=").append(resultsPerPage)
                .toString();
    }

    private String getIssueRetrieverUrl(int page, int resultsPerPage)
    {
        return new StringBuffer(getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']//input[@name='retrieverUrlHtml']", "value").substring(getConfluenceWebTester().getContextPath().length()))
                .append("&page=").append(page)
                .append("&rp=").append(resultsPerPage)
                .toString();
    }

    private void assertJiraIssues(
            int page,
            int total,
            List<JiraIssue> jiraIssues,
            String json) throws JSONException
    {
        JSONObject jsonObject = new JSONObject(json);

        assertEquals(page, jsonObject.get("page"));
        assertEquals(total, jsonObject.get("total"));

        JSONArray jsonArray = jsonObject.getJSONArray("rows");

        assertEquals(jiraIssues.size(), null == jsonArray ? 0 : jsonArray.length());

        if (null != jsonArray)
        {
            for (int i = 0; i < jsonArray.length(); ++i)
            {
                JSONObject jiraIssueInJson = jsonArray.getJSONObject(i);
                JiraIssue jiraIssue = jiraIssues.get(i);

                assertEquals(jiraIssue.key, jiraIssueInJson.get("id"));

                JSONArray jiraIssueCellsJson = jiraIssueInJson.getJSONArray("cell");
                String jiraBaseUrl = jiraWebTester.getTestContext().getBaseUrl().toString();

                /* Take of ending forward slash */
                jiraBaseUrl = jiraBaseUrl.substring(0, jiraBaseUrl.length() - 1);


                assertEquals(
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" ><img src=\"" + jiraBaseUrl + jiraIssue.iconSource + "\" alt=\"" + jiraIssue.iconAltText + "\"/></a>",
                        jiraIssueCellsJson.get(0)
                );
                assertEquals(
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" >" + jiraIssue.key + "</a>",
                        jiraIssueCellsJson.get(1)
                );
                assertEquals(
                        "<a href=\"" + jiraBaseUrl + "/browse/" + jiraIssue.key + "\" >" + jiraIssue.summary + "</a>",
                        jiraIssueCellsJson.get(2)
                );
                assertEquals(
                        jiraIssue.assignee,
                        jiraIssueCellsJson.get(3)
                );
                assertEquals(
                        jiraIssue.reporter,
                        jiraIssueCellsJson.get(4)
                );
                assertEquals(
                        "<img src=\"" + jiraBaseUrl + jiraIssue.priorityIcon + "\" alt=\"" + jiraIssue.priorityAltText + "\"/>",
                        jiraIssueCellsJson.get(5)
                );
                assertEquals(
                        "<img src=\"" + jiraBaseUrl + jiraIssue.statusIcon + "\" alt=\"" + jiraIssue.statusAltText + "\"/> " + jiraIssue.statusAltText,
                        jiraIssueCellsJson.get(6)
                );
                assertEquals(
                        jiraIssue.resolution,
                        jiraIssueCellsJson.get(7)
                );
                assertEquals(
                        jiraIssue.createdDate,
                        jiraIssueCellsJson.get(8)
                );
                assertEquals(
                        jiraIssue.lastUpdatedDate,
                        jiraIssueCellsJson.get(9)
                );
                assertEquals(
                        StringUtils.defaultString(jiraIssue.dueDate),
                        jiraIssueCellsJson.get(10)
                );
            }
        }
    }

    public void testRenderJiraIssuesWithCustomHeight() throws JSONException
    {
        long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesWithCustomHeight",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|height=1000}");

        viewPageById(testPageId);

        assertEquals("1000", getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']/fieldset//input[@name='height']", "value"));
    }

    public void testGetJiraIssuesUntrusted() throws JSONException
    {
        long testPageId = createPage(testSpaceKey, "getJiraIssuesUntrusted",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                1,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource()
        );
    }

    public void testGetJiraIssuesTrusted() throws JSONException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource()
        );
    }

    public void testGetJiraIssuesFromIssuesRssUrl() throws JSONException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesFromIssuesRssUrl",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(URL_TYPE_RSS_ISSUES, 1000) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource()
        );
    }

    public void testGetJiraIssuesFromCommentsRssUrl() throws JSONException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesFromCommentsRssUrl",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(URL_TYPE_RSS_COMMENTS, 1000) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource()
        );
    }

    public void testGetJiraIssuesPaged() throws JSONException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesPaged",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(1) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")
                ),
                getPageSource()
        );

        viewPageById(testPageId);
        gotoPage(getIssueRetrieverUrl(2, 1));

        assertJiraIssues(
                2,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")
                ),
                getPageSource()
        );
    }

    public void testGetJiraIssuesCount() throws JSONException
    {
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesCount",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|count=true}");

        viewPageById(testPageId);
        assertElementPresentByXPath("//div[@class='wiki-content']//span[@class='jiraissues_count']");

        String jsonPath = getElementAttributByXPath("//input[@name='retrieverUrlHtml']", "value");
        jsonPath = jsonPath.substring(getConfluenceWebTester().getContextPath().length());

        gotoPage(jsonPath);

        assertJiraIssues(
                0,
                1,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource()
        );
    }

    public void testCustomTitle()
    {
        String title = "My Custom Title";
        long testPageId = createPage(testSpaceKey, "testCustomTitle",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|title=" + title + "}");

        viewPageById(testPageId);
        assertEquals(title, getElementAttributByXPath("//div[@class='wiki-content']/div[@class='jiraissues_table']//input[@name='title']", "value"));
    }

    public void testDefaultWidthIsOneHundredPercent()
    {
        long testPageId = createPage(testSpaceKey, "testDefaultWidthIsOneHundredPercent",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off}");

        viewPageById(testPageId);
        assertEquals("100%", getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']/fieldset/input[@name='width']", "value"));
    }

    public void testCustomWidthRespected()
    {
        long testPageId = createPage(testSpaceKey, "testCustomWidthRespected",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|width=50%}");

        viewPageById(testPageId);
        assertEquals("50%", getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']/fieldset/input[@name='width']", "value"));
    }

    public void testCustomFixedWidthRespected()
    {
        long testPageId = createPage(testSpaceKey, "testCustomFixedWidthRespected",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|width=500px}");

        viewPageById(testPageId);
        assertEquals("500px", getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']/fieldset/input[@name='width']", "value"));
    }

    private void assertJiraIssuesStatic(
            List<String> columns,
            List<JiraIssue> jiraIssues) throws ParseException
    {
        String jiraBaseUrl = jiraWebTester.getTestContext().getBaseUrl().toString();
        jiraBaseUrl = jiraBaseUrl.substring(0, jiraBaseUrl.length() - 1);

        int columnsSize = columns.size();
        for (int i = 0; i < columnsSize; ++i)
        {
            String column = columns.get(i);

            assertEquals(column, getElementTextByXPath("//div[@class='wiki-content']//table//tr[2]/th[" + (i + 1) + "]"));

            int issueCount = jiraIssues.size();
            for (int j = 0; j < issueCount; ++j)
            {
                JiraIssue jiraIssue = jiraIssues.get(j);
                DateFormat inputDateFormat = new SimpleDateFormat("dd/MMM/yy");
                DateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy");

                if (StringUtils.equals(column, "Type"))
                    assertElementPresentByXPath(
                            "//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/a/img[@src='" + jiraBaseUrl + jiraIssue.iconSource + "']"
                    );

                if (StringUtils.equals(column, "Key"))
                    assertEquals(jiraIssue.key, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/a"));

                if (StringUtils.equals(column, "Summary"))
                    assertEquals(jiraIssue.summary, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/a"));

                if (StringUtils.equals(column, "Assignee"))
                    assertEquals(jiraIssue.assignee, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));

                if (StringUtils.equals(column, "Reporter"))
                    assertEquals(jiraIssue.reporter, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));

                if (StringUtils.equals(column, "Priority"))
                    assertElementPresentByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/img[@src='" + jiraBaseUrl + jiraIssue.priorityIcon + "']");

                if (StringUtils.equals(column, "Status"))
                {
                    assertElementPresentByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/img[@src='" + jiraBaseUrl + jiraIssue.statusIcon + "']");
                    assertEquals(jiraIssue.statusAltText, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));
                }

                if (StringUtils.equals(column, "Resolution"))
                    assertEquals(jiraIssue.resolution, getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));

                if (StringUtils.equals(column, "Created"))
                    assertEquals(
                            outputDateFormat.format(inputDateFormat.parse(jiraIssue.createdDate)),
                            getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));

                if (StringUtils.equals(column, "Updated"))
                    assertEquals(
                            outputDateFormat.format(inputDateFormat.parse(jiraIssue.lastUpdatedDate)),
                            getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));

                if (StringUtils.equals(column, "Due"))
                    assertEquals(
                            null == jiraIssue.dueDate ? "" : outputDateFormat.format(inputDateFormat.parse(jiraIssue.dueDate)), 
                            getElementTextByXPath("//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]"));
            }
        }
    }

    public void testShowLessColumnsInStaticMode() throws ParseException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testShowLessColumns",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|columns=key,summary,assignee}");

        viewPageById(testPageId);

        assertJiraIssuesStatic(
                Arrays.asList("Key", "Summary", "Assignee"),
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                )
        );
    }

    public void testRenderJiraIssuesInStaticMode() throws ParseException
    {
        trustConfluenceApplication();

        long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static}");

        viewPageById(testPageId);

        assertJiraIssuesStatic(
                Arrays.asList("Type", "Key", "Summary", "Assignee", "Reporter", "Priority", "Status", "Resolution", "Created", "Updated", "Due"),
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                )
        );
    }

    public void testCustomTitleInStaticMode()
    {
        trustConfluenceApplication();
        
        String title = "My Custom Title";
        long testPageId = createPage(testSpaceKey, "testCustomTitleInStaticMode",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|renderMode=static|cache=off|title=" + title + "}");

        viewPageById(testPageId);

        assertEquals(title, getElementTextByXPath("//div[@class='wiki-content']//table//tr/th/a"));
    }

    public void testDefaultWidthIsOneHundredPercentInStaticMode()
    {
        long testPageId = createPage(testSpaceKey, "testDefaultWidthIsOneHundredPercentInStaticMode",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static}");

        viewPageById(testPageId);
        assertEquals("width: 100%", getElementAttributByXPath("//div[@class='wiki-content']//table", "style"));
    }

    public void testCustomWidthRespectedInStaticMode()
    {
        long testPageId = createPage(testSpaceKey, "testCustomWidthRespectedInStaticMode",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|width=50%}");

        viewPageById(testPageId);
        assertEquals("width: 50%", getElementAttributByXPath("//div[@class='wiki-content']//table", "style"));
    }

    public void testCustomFixedWidthRespectedInStaticMode()
    {
        long testPageId = createPage(testSpaceKey, "testCustomFixedWidthRespectedInStaticMode",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|width=500px}");

        viewPageById(testPageId);
        assertEquals("width: 500px", getElementAttributByXPath("//div[@class='wiki-content']//table", "style"));
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-137">CONFJIRA-137</a>
     */
    public void testIssueCountRenderedInStaticMode()
    {
        long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static}");

        viewPageById(testPageId);

        String titleText = getElementTextByXPath("//div[@class='wiki-content']//table[@class='grid']//tr/th");

        assertTrue(titleText.indexOf("1 issues") >= 0);
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-141">CONFJIRA-141</a>
     */
    public void testJiraColumnNamesDoubleHtmlEncoded()
    {
        String maliciousColumn = "<script>alert(\"cheese\")</script>";

        long testPageId = createPage(testSpaceKey, "testJiraColumnNamesDoubleHtmlEncoded",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|columns=key," + maliciousColumn + "}");

        viewPageById(testPageId);

        assertEquals(
                maliciousColumn,
                getElementAttributByXPath("//div[@class='wiki-content']//div[@class='jiraissues_table']//fieldset/input[@name='" + StringEscapeUtils.escapeHtml(maliciousColumn) + "']", "value")
        );
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-141">CONFJIRA-141</a>
     */
    public void testJiraColumnNamesProperlyEncodedInJiraIssuesStatic()
    {
        String maliciousColumn = "<script>alert(\"cheese\")</script>";

        long testPageId = createPage(testSpaceKey, "testJiraColumnNamesProperlyEncodedInJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|columns=key," + maliciousColumn + "|renderMode=static}");

        viewPageById(testPageId);

        assertEquals(
                maliciousColumn,
                getElementTextByXPath("//div[@class='wiki-content']//table//tr[2]/th[2]")
        );
    }

    private static class JiraIssue
    {
        public final String iconSource;

        public final String iconAltText;

        public final String key;

        public final String summary;

        public final String createdDate;

        public final String lastUpdatedDate;

        public final String dueDate;

        public final String assignee;

        public final String reporter;

        public final String priorityIcon;

        public final String priorityAltText;

        public final String resolution;

        public final String statusIcon;

        public final String statusAltText;

        private JiraIssue(String iconSource, String iconAltText, String key, String summary, String createdDate, String lastUpdatedDate, String dueDate, String assignee, String reporter, String priorityIcon, String priorityAltText, String resolution, String statusIcon, String statusAltText)
        {
            this.iconSource = iconSource;
            this.iconAltText = iconAltText;
            this.key = key;
            this.summary = summary;
            this.createdDate = createdDate;
            this.lastUpdatedDate = lastUpdatedDate;
            this.dueDate = dueDate;
            this.assignee = assignee;
            this.reporter = reporter;
            this.priorityIcon = priorityIcon;
            this.priorityAltText = priorityAltText;
            this.resolution = resolution;
            this.statusIcon = statusIcon;
            this.statusAltText = statusAltText;
        }
    }
}
