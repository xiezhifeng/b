package com.atlassian.confluence.extra.jira;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Assert;
import org.mockito.Mock;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.JiraChartMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.google.common.base.Function;

public class TestJiraChartMacro extends TestCase {
    
    @Mock private I18NBean i18NBean;
    
    @Mock private I18NBeanFactory i18NBeanFactory;
    
    @Mock private ApplicationLinkService applicationLinkService;
    
    @Mock MacroExecutorService executorService;
    
    public void testHappyCase(){
        String border = "false";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("jql", "project = TEST");
        parameters.put("serverId", "to Jira");
        parameters.put("statType", "statType");
        parameters.put("width", "100");
        parameters.put("border", border);
        
        final JQLValidationResult result = new JQLValidationResult();
        Function<Map<String, String>, JQLValidationResult> jqlValidator = new Function<Map<String,String>, JQLValidationResult>() {
            
            @Override
            public JQLValidationResult apply(Map<String, String> arg0) {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                return result;
            }
        };
        
        try {
            doTest(border, parameters, result, jqlValidator);
        } catch (MacroExecutionException e) {
            assertFalse("Unexpected exception", true);
        }
    }
    
    public void testExceptionDuringValidateJQL(){
        String border = "false";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("jql", "project = TEST");
        parameters.put("serverId", "to Jira");
        parameters.put("statType", "statType");
        parameters.put("width", "100");
        parameters.put("border", border);
        
        final JQLValidationResult result = new JQLValidationResult();
        Function<Map<String, String>, JQLValidationResult> jqlValidator = new Function<Map<String,String>, JQLValidationResult>() {
            
            @Override
            public JQLValidationResult apply(Map<String, String> arg0) {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                result.setException(new MacroExecutionException("Fake exception"));
                return result;
            }
        };
        
        try {
            doTest(border, parameters, result, jqlValidator);
        } catch (MacroExecutionException e) {
            return;
        }
        
        assertFalse("Expected exception but cannot get any", true);
    }

    private void doTest(String border, Map<String, String> parameters,
            final JQLValidationResult result,
            Function<Map<String, String>, JQLValidationResult> jqlValidator) throws MacroExecutionException {
        Settings settings = new Settings();
        settings.setBaseUrl("http://fakelink.com");
        
        MockJiraChartMacro testObj = new MockJiraChartMacro(executorService, applicationLinkService,
                i18NBeanFactory, jqlValidator);
        testObj.setSettings(settings);
        
        ConversionContext mockContext = mock(ConversionContext.class);
        Map<String, Object> velocityContext;
        velocityContext = testObj.executePublic(parameters, "", mockContext);
        JQLValidationResult outcomeResult = (JQLValidationResult)velocityContext.get("jqlValidationResult");
        String outcomeServletProxyUrl = (String)velocityContext.get("srcImg");
        String outcomeBorder = String.valueOf(velocityContext.get("border"));
        
        Assert.assertNotNull("Missing the link to Jira Image Servlet proxy", outcomeServletProxyUrl);
        Assert.assertEquals("The border value is incorrect", border, outcomeBorder);
        Assert.assertNotNull("Missing JqlValidationResult", outcomeResult);
        
        Assert.assertArrayEquals(new JQLValidationResult[] {result},
                new JQLValidationResult[] {outcomeResult});;
    }
    
    private class MockJiraChartMacro extends JiraChartMacro {

        public MockJiraChartMacro(MacroExecutorService executorService,
                ApplicationLinkService applicationLinkService,
                I18NBeanFactory i18nBeanFactory,
                Function<Map<String, String>, JQLValidationResult> jqlValidator) {
            super(executorService, applicationLinkService, i18nBeanFactory);
            this.setJqlValidator(jqlValidator);
        }
        
        public Map<String, Object> executePublic(Map<String, String> parameters, String body,
                ConversionContext context) throws MacroExecutionException{
            return this.executeInternal(parameters, body, context);
        }
        
        @Override
        protected Map<String, Object> createVelocityContext() {
            return new HashMap<String, Object>();
        }
    }
}
