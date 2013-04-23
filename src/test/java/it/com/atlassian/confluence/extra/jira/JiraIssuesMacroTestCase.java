package it.com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import com.atlassian.confluence.plugin.functest.util.ConfluenceBuildUtil;
import com.atlassian.confluence.util.GeneralUtil;

public class JiraIssuesMacroTestCase extends AbstractJiraMacrosPluginTestCase
{

    private static final String XSS_STRING = " - Test *for* <b>XSS ' xss='xss' \\\" xss2=\\\"xss2\\\" </b>end";
    private static final String XSS_STRING_NON_WIKI_QUOTES_ESCAPED = " - Test for <b>XSS ' xss='xss' \\\" xss2=\\\"xss2\\\" </b>end";
    private static final String XSS_STRING_NON_WIKI = " - Test for <b>XSS '' xss=''xss'' \" xss2=\"xss2\" </b>end";
	private static final String XSS_STRING_ESCAPED = "";
	// private static final String XSS_STRING_ESCAPED = " - Test *for* &lt;b&gt;XSS ' xss='xss' \\&quot; xss2=\\&quot;xss2\\&quot; &lt;/b&gt;end";
    private static final String DESCRIPTION_HTML_BY_WIKI_RENDERER = "<p>Description - Test <b>for</b> &lt;b&gt;XSS ' xss='xss' \\\" xss2=\\\"xss2\\\" &lt;/b&gt;end</p>";
    private static final String FREETEXTFIELD_HTML_BY_WIKI_RENDERER = "<p>freetextfield - Test <b>for</b> &lt;b&gt;XSS '' xss=''xss'' \" xss2=\"xss2\" &lt;/b&gt;end</p>";

    private static final boolean ADG_ENABLED = Long.parseLong(GeneralUtil.getBuildNumber()) >= 4000;

   

  
    public void testGetJiraIssuesTrusted() throws JSONException, HttpException, IOException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        

        final long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING_ESCAPED, "10/Feb/09", "08/Feb/12", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource(),
                true
        );
    }

    

    public void testGetJiraIssuesFromIssuesRssUrl() throws JSONException, HttpException, IOException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());

        final long testPageId = createPage(testSpaceKey, "testGetJiraIssuesFromIssuesRssUrl",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(10000,URL_TYPE_RSS_ISSUES, 1000) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING_ESCAPED, "10/Feb/09", "08/Feb/12", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource(),
                true
        );
    }

    public void testGetJiraIssuesFromCommentsRssUrl() throws JSONException, HttpException, IOException
    {
		// trustConfluenceApplication();
		// enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testGetJiraIssuesFromCommentsRssUrl",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(10000, URL_TYPE_RSS_COMMENTS, 1000) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1000));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING_ESCAPED, "11/Feb/09", "11/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
 new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "11/Feb/09", "11/Feb/09", null, "admin", "admin",
				"/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource(),
                true
        );
    }

    public void testGetJiraIssuesPaged() throws JSONException, HttpException, IOException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testGetJiraIssuesPaged",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(10000, 1) + "|cache=off}");

        viewPageById(testPageId);

        gotoPage(getIssueRetrieverUrl(1, 1));

        assertJiraIssues(
                1,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING_ESCAPED, "10/Feb/09", "08/Feb/12", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")
                ),
                getPageSource(),
                true
        );

        viewPageById(testPageId);
        gotoPage(getIssueRetrieverUrl(2, 1));

        assertJiraIssues(
                2,
                2,
                Arrays.asList(
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")
                ),
                getPageSource(),
                true
        );
    }

    public void testGetJiraIssuesCount() throws JSONException
    {
        final long testPageId = createPage(testSpaceKey, "testGetJiraIssuesCount",
                "{jiraissues:anonymous=true|url=" + getJiraIssuesXmlUrl() + "|cache=off|count=true}");

        viewPageById(testPageId);
        assertElementPresentByXPath("//div[@class='wiki-content']//span[@class='jiraissues_count']");

        String jsonPath = getElementTextByXPath("//span[@class='hidden data url']");
        jsonPath = jsonPath.substring(getContextPath().length());

        gotoPage(jsonPath);

        assertJiraIssues(
                0,
                1,
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING_ESCAPED, "10/Feb/09", "08/Feb/12", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                ),
                getPageSource(),
                false
        );
    }

    public void testCustomTitle()
    {
        final String title = "My Custom Title";
        final long testPageId = createPage(testSpaceKey, "testCustomTitle",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|title=" + title + "}");

        viewPageById(testPageId);
        assertEquals(title, getElementAttributByXPath("//div[@class='wiki-content']/div[@class='jiraissues_table']//input[@name='title']", "value"));
    }

    
   
    

    private void assertWarning(String text) throws ParseException
    {
        String warning;
        if (ADG_ENABLED) {
			warning = getElementTextByXPath("//div[@class='wiki-content']//div[@class='aui-message warning shadowed information-macro']");
		} else {
			warning = getElementTextByXPath("//div[@class='wiki-content']//div[@class='panelMacro noteMacro  has-icon ']");
		}
        
        assertTrue("Expected to contain [" + text + "]; Actual warning is [" + warning + "]", warning.contains(text));
    }


    private void assertJiraIssuesStatic(
            List<String> columns,
            List<JiraIssue> jiraIssues) throws ParseException
    {
        String jiraBaseUrl = jiraWebTester.getTestContext().getBaseUrl().toString();
        jiraBaseUrl = jiraBaseUrl.substring(0, jiraBaseUrl.length() - 1);

        final int columnsSize = columns.size();
        for (int i = 0; i < columnsSize; ++i)
        {
            final String column = columns.get(i);

            assertEquals(column, getElementTextByXPath("//div[@class='wiki-content']//table//tr[2]/th[" + (i + 1) + "]"));

            final int issueCount = jiraIssues.size();
            for (int j = 0; j < issueCount; ++j)
            {
                final JiraIssue jiraIssue = jiraIssues.get(j);
                final DateFormat inputDateFormat = new SimpleDateFormat("dd/MMM/yy");
                final DateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy");

                if (StringUtils.equals(column, "Type")) {
					assertElementPresentByXPath(
                            "//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]/a/img[@src='" + jiraBaseUrl + jiraIssue.iconSource + "']"
                    );
				}

                final String cellXPath = "//div[@class='wiki-content']//table//tr[" + (j + 3) +  "]/td[" + (i + 1) + "]";
                
                if (StringUtils.equals(column, "Key")) {
					assertEquals(column, jiraIssue.key, getElementTextByXPath(cellXPath + "/a"));
				}

                if (StringUtils.equals(column, "Summary")) {
					assertEquals(column, jiraIssue.summary, getElementTextByXPath(cellXPath + "/a"));
				}

                if (StringUtils.equals(column, "description")) {
					assertEquals(column, jiraIssue.description, getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "freetextfield")) {
					assertEquals(column, jiraIssue.freeTextField, getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Assignee")) {
					assertEquals(column, jiraIssue.assignee, getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Reporter")) {
					assertEquals(column, jiraIssue.reporter, getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Priority")) {
					assertElementPresentByXPath(cellXPath + "/img[@src='" + jiraBaseUrl + jiraIssue.priorityIcon + "']");
				}

                if (StringUtils.equals(column, "Status"))
                {
                    assertElementPresentByXPath(cellXPath + "/img[@src='" + jiraBaseUrl + jiraIssue.statusIcon + "']");
                    assertEquals(column, jiraIssue.statusAltText, getElementTextByXPath(cellXPath));
                }

                if (StringUtils.equals(column, "Resolution")) {
					assertEquals(column, jiraIssue.resolution, getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Created")) {
					assertEquals(column,
                            outputDateFormat.format(inputDateFormat.parse(jiraIssue.createdDate)),
                            getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Updated")) {
					assertEquals(column,
                            outputDateFormat.format(inputDateFormat.parse(jiraIssue.lastUpdatedDate)),
                            getElementTextByXPath(cellXPath));
				}

                if (StringUtils.equals(column, "Due")) {
					assertEquals(column,
                            null == jiraIssue.dueDate ? "" : outputDateFormat.format(inputDateFormat.parse(jiraIssue.dueDate)),
                                    getElementTextByXPath(cellXPath));
				}
            }
        }
    }

    public void testShowLessColumnsInStaticMode() throws ParseException, HttpException, IOException, JSONException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testShowLessColumns",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|columns=key,summary,assignee}");

        viewPageById(testPageId);

        assertJiraIssuesStatic(
                Arrays.asList("Key", "Summary", "Assignee"),
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING, "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")
                )
        );
    }

    public void testRenderJiraIssuesInStaticMode() throws ParseException, HttpException, IOException, JSONException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static}");

        viewPageById(testPageId);

        assertJiraIssuesStatic(
                Arrays.asList("Type", "Key", "Summary", "Assignee", "Reporter", "Priority", "Status", "Resolution", "Created", "Updated", "Due"),
                Arrays.asList(
                        new JiraIssue("/images/icons/newfeature.gif", "New Feature", "TP-2", "New Feature 01" + XSS_STRING, "10/Feb/09", "08/Feb/12", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open"),
                        new JiraIssue("/images/icons/bug.gif", "Bug", "TP-1", "Bug 01", "10/Feb/09", "10/Feb/09", null, "admin", "admin", "/images/icons/priority_major.gif", "Major", "Unresolved", "/images/icons/status_open.gif", "Open")

                )
        );
    }

    public void testApplinkedJIRADisplaysFormattedFieldsInStaticMode() throws ParseException, HttpException, IOException, JSONException
    {
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|columns=Key,summary,description,freetextfield}");

        viewPageById(testPageId);

        assertJiraIssuesStatic(
                Arrays.asList("Key", "Summary", "description", "freetextfield"),
                Arrays.asList(
                        new JiraIssue("TP-2", "New Feature 01" + XSS_STRING, "Description" + XSS_STRING_NON_WIKI_QUOTES_ESCAPED, "freetextfield" + XSS_STRING_NON_WIKI),
                        new JiraIssue("TP-1", "Bug 01", "", "")
                )
        );
    }

    public void testNonApplinkedJIRAEscapesFieldsInStaticMode() throws ParseException, HttpException, IOException, JSONException
    {
        trustConfluenceApplication();
        // enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static|columns=Key,summary,description,freetextfield}");

        viewPageById(testPageId);
        
        /** Note: This warning only appears for administrators. We don't test whether it's hidden for lower users. */
        assertWarning("The contents will be displayed as HTML.");

        assertJiraIssuesStatic(
                Arrays.asList("Key", "Summary", "description", "freetextfield"),
                Arrays.asList(
                        new JiraIssue("TP-2", "New Feature 01" + XSS_STRING, DESCRIPTION_HTML_BY_WIKI_RENDERER, FREETEXTFIELD_HTML_BY_WIKI_RENDERER),
                        new JiraIssue("TP-1", "Bug 01", "", "")
                )
        );
    }

    
    public void testCustomTitleInStaticMode()
    {
        trustConfluenceApplication();

        final String title = "My Custom Title";
        final long testPageId = createPage(testSpaceKey, "testCustomTitleInStaticMode",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|renderMode=static|cache=off|title=" + title + "}");

        viewPageById(testPageId);

        assertEquals(title, getElementTextByXPath("//div[@class='wiki-content']//h2[@class='issues-subheading']/a"));
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-137">CONFJIRA-137</a>
     */
    public void testIssueCountRenderedInStaticMode()
    {
        final long testPageId = createPage(testSpaceKey, "testRenderJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|anonymous=true|renderMode=static}");

        viewPageById(testPageId);

        final String titleText = getElementTextByXPath("//div[@class='wiki-content']//h2[@class='issues-subheading']");

        assertTrue(titleText.indexOf("1 issues") >= 0);
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-141">CONFJIRA-141</a>
     */
    public void testJiraColumnNamesDoubleHtmlEncoded()
    {
        final String maliciousColumn = "<script>alert(\"cheese\")</script>";

        final long testPageId = createPage(testSpaceKey, "testJiraColumnNamesDoubleHtmlEncoded",
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
        final String maliciousColumn = "<script>alert(\"cheese\")</script>";

        final long testPageId = createPage(testSpaceKey, "testJiraColumnNamesProperlyEncodedInJiraIssuesStatic",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|columns=key," + maliciousColumn + "|renderMode=static}");

        viewPageById(testPageId);

        assertEquals(
                maliciousColumn,
                getElementTextByXPath("//div[@class='wiki-content']//table//tr[2]/th[2]")
        );
    }

    private boolean doesConfluenceHaveXsrfProtectionMechanism()
    {
        return getConflenceBuildNumber() > 1599 && !ConfluenceBuildUtil.containsSudoFeature();
    }

    public void testAddIssuesIconMappingProtectedFromXsrfExploits()
    {
        if (doesConfluenceHaveXsrfProtectionMechanism()) {
			assertResourceXsrfProtected("/admin/addiconmapping.action");
		}
    }

    public void testRemoveIssuesIconMappingProtectedFromXsrfExploits()
    {
        if (doesConfluenceHaveXsrfProtectionMechanism()) {
			assertResourceXsrfProtected("/admin/removeiconmapping.action");
		}
    }


    public void testCustomFieldDateValueNicelyFormattedInStaticMode() throws HttpException, IOException, JSONException
    {
        //restoreJiraData("CONFJIRA-162.xml");
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());

        final long testPageId = createPage(testSpaceKey, "testCustomFieldDateValueNicelyFormattedInStaticMode",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(10011) + "|cache=off|columns=Date CustomField|renderMode=static}");

        viewPageById(testPageId);
        assertElementPresentByXPath("//div[@class='wiki-content']//table//td[text()='25/Dec/09']");
    }

    public void testMultipleFixVersionsCollapsed() throws HttpException, IOException, JSONException
    {
        //restoreJiraData("CONFJIRA-184.xml");
        trustConfluenceApplication();
        enableTrustedAuthWithAppLink(setupAppLink());
        
        final long testPageId = createPage(testSpaceKey, "testMultipleFixVersionsCollapsed",
                "{jiraissues:url=" + getJiraIssuesXmlUrl(10010) + "|cache=off|columns=fixVersion|renderMode=static}");

        viewPageById(testPageId);
        assertElementPresentByXPath("//div[@class='wiki-content']//table//td[text()='1.0, 2.0']");
    }
    
    
    static class JiraIssue
    {
        public final String iconSource;

        public final String iconAltText;

        public final String key;

        public final String summary;
        
        public final String freeTextField;
        
        public final String description;

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
            this.description = null;
            this.freeTextField = null;
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

        private JiraIssue(String key, String summary, String description, String freeTextField)
        {
            this.key = key;
            this.summary = summary;
            this.freeTextField = freeTextField;
            this.description = description;

            this.iconSource = null;
            this.iconAltText = null;
            this.createdDate = null;
            this.lastUpdatedDate = null;
            this.dueDate = null;
            this.assignee = null;
            this.reporter = null;
            this.priorityIcon = null;
            this.priorityAltText = null;
            this.resolution = null;
            this.statusIcon = null;
            this.statusAltText = null;
        }
    }


    
}
