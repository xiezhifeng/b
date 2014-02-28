package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestDefaultJiraConnectorManager extends TestCase {

    private DefaultJiraConnectorManager defaultJiraConnectorManager;

    @Mock
    private ApplicationLinkService appLinkService;

    @Mock
    private AuthenticationConfigurationManager authenticationConfigurationManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        defaultJiraConnectorManager = new DefaultJiraConnectorManager();
    }

    public void testGetJiraServerWithoutAppLink()
    {
        assertNull(defaultJiraConnectorManager.getJiraServer(null));
    }

    private class DefaultJiraConnectorManager extends com.atlassian.confluence.extra.jira.DefaultJiraConnectorManager
    {
        private DefaultJiraConnectorManager()
        {
            super(appLinkService, authenticationConfigurationManager);
        }
    }
}
