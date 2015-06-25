package it.com.atlassian.confluence.plugins.webdriver;

import com.atlassian.confluence.plugins.model.JiraProjectModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.confluence.test.properties.TestProperties.isOnDemandMode;

public class AbstractJiraODTest extends AbstractJiraTest
{
    protected static final String PROJECT_TSTT = "Test Project";
    protected static final String PROJECT_TP = "Test Project 1";
    protected static final String PROJECT_TST = "Test Project 2";

    protected Map<String, JiraProjectModel> onDemandJiraProjects = new HashMap<String, JiraProjectModel>();

    protected Map<String, String> internalJiraProjects = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(PROJECT_TSTT, "10011");
            put(PROJECT_TP, "10000");
            put(PROJECT_TST, "10010");
        }
    });

    protected String getProjectId(String projectName)
    {
        if ( isOnDemandMode() )
        {
            return onDemandJiraProjects.get(projectName).getProjectId();
        }

        return internalJiraProjects.get(projectName);
    }
}
