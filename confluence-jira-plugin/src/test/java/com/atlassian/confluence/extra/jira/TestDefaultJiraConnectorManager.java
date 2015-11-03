package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestDefaultJiraConnectorManager extends TestCase {

    private DefaultJiraConnectorManager defaultJiraConnectorManager;

    @Mock
    private ReadOnlyApplicationLinkService appLinkService;

    @Mock
    private AuthenticationConfigurationManager authenticationConfigurationManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        defaultJiraConnectorManager = new DefaultJiraConnectorManager(appLinkService, authenticationConfigurationManager);
    }

    public void testGetJiraServerWithoutAppLink()
    {
        assertNull(defaultJiraConnectorManager.getJiraServer(null));
    }
}
