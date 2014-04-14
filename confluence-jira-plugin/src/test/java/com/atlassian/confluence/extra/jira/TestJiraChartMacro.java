package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartRendererFactory;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.JQLValidator;
import com.atlassian.confluence.plugins.jiracharts.JiraChartMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MacroUtils.class)
public class TestJiraChartMacro extends TestCase
{
    
    @Mock private I18NBean i18NBean;
    
    @Mock private I18NBeanFactory i18NBeanFactory;
    
    @Mock private ApplicationLinkService applicationLinkService;
    
    @Mock MacroExecutorService executorService;

    @Mock private Base64JiraChartImageService base64JiraChartImageService;

    @Mock private JiraConnectorManager jiraConnectorManager;

    @Mock JiraChartRendererFactory jiraChartRendererFactory;

    @Mock private ContextPathHolder contextPathHolder;
    
    public void testHappyCase() throws TypeNotInstalledException
    {
        String border = "false";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("jql", "project = TEST");
        parameters.put("serverId", "to Jira");
        parameters.put("statType", "statType");
        parameters.put("width", "100");
        parameters.put("border", border);
        
        final JQLValidationResult result = new JQLValidationResult();
        JQLValidator jqlValidator = new JQLValidator()
        {
            
            @Override
            public JQLValidationResult doValidate(Map<String, String> arg0) throws MacroExecutionException
            {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                return result;
            }
        };
        
        try
        {
            doTest(border, parameters, result, jqlValidator);
        }
        catch (MacroExecutionException e)
        {
            assertFalse("Unexpected exception", true);
        }
    }
    
    public void testExceptionDuringValidateJQL() throws TypeNotInstalledException
    {
        String border = "false";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("jql", "project = TEST");
        parameters.put("serverId", "to Jira");
        parameters.put("statType", "statType");
        parameters.put("width", "100");
        parameters.put("border", border);
        
        final JQLValidationResult result = new JQLValidationResult();
        JQLValidator jqlValidator = new JQLValidator()
        {
            
            @Override
            public JQLValidationResult doValidate(Map<String, String> parameters) throws MacroExecutionException
            {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                throw new MacroExecutionException("Fake exception");
            }
        };
        
        try
        {
            doTest(border, parameters, result, jqlValidator);
        }
        catch (MacroExecutionException e)
        {
            return;
        }
        
        assertFalse("Expected exception but cannot get any", true);
    }

    private void doTest(String border, Map<String, String> parameters,
            final JQLValidationResult result,
            JQLValidator jqlValidator) throws MacroExecutionException, TypeNotInstalledException
    {
        Settings settings = new Settings();
        settings.setBaseUrl("http://fakelink.com");

        PowerMockito.mockStatic(MacroUtils.class);
        when(MacroUtils.defaultVelocityContext()).thenReturn(new HashMap<String, Object>());
        
        i18NBean = mock(I18NBean.class);
        when(i18NBean.getText(anyString())).thenReturn("jirachart.macro.dialog.statistype.statuses");

        i18NBeanFactory = mock(I18NBeanFactory.class);
        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);
        
        MockJiraChartMacro testObj = new MockJiraChartMacro(executorService, applicationLinkService,
                i18NBeanFactory, jqlValidator, base64JiraChartImageService, jiraConnectorManager, contextPathHolder);
        MockJiraChartMacro testObj = new MockJiraChartMacro(
                executorService, applicationLinkService,
                i18NBeanFactory, jqlValidator, jiraConnectorManager, jiraChartRendererFactory);
        
        ConversionContext mockContext = mock(ConversionContext.class);
        when(mockContext.getOutputType()).thenReturn(ConversionContextOutputType.PREVIEW.name());
        
        Map<String, Object> velocityContext;
        velocityContext = testObj.executePublic(parameters, "", mockContext);
        JQLValidationResult outcomeResult = (JQLValidationResult)velocityContext.get("jqlValidationResult");
        String outcomeServletProxyUrl = (String)velocityContext.get("srcImg");
        String outcomeBorder = String.valueOf(velocityContext.get("showBorder"));
        Boolean outcomeInPreviewMode = (Boolean)velocityContext.get("isPreviewMode");
        
        Assert.assertEquals(outcomeInPreviewMode, true);
        Assert.assertNotNull("Missing the link to Jira Image Servlet proxy", outcomeServletProxyUrl);
        Assert.assertEquals("The border value is incorrect", border, outcomeBorder);
        Assert.assertNotNull("Missing JqlValidationResult", outcomeResult);
        
        
        Assert.assertArrayEquals(new JQLValidationResult[] {result},
                new JQLValidationResult[] {outcomeResult});
    }
    
    private class MockJiraChartMacro extends JiraChartMacro
    {

        public MockJiraChartMacro(MacroExecutorService executorService,
                ApplicationLinkService applicationLinkService,
                I18NBeanFactory i18nBeanFactory, JQLValidator jqlValidator,
                Base64JiraChartImageService base64JiraChartImageService, JiraConnectorManager jiraConnectorManager, ContextPathHolder contextPathHolder)
        {
            super(executorService, applicationLinkService, i18nBeanFactory, base64JiraChartImageService, jiraConnectorManager, contextPathHolder);
            this.setJqlValidator(jqlValidator);
        }
        
        public Map<String, Object> executePublic(Map<String, String> parameters, String body,
                ConversionContext context) throws MacroExecutionException, TypeNotInstalledException
        {
            return this.executeInternal(parameters, body, context);
        }
    }
}
