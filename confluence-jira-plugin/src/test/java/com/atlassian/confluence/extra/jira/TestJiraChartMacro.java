package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import junit.framework.TestCase;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.JQLValidator;
import com.atlassian.confluence.plugins.jiracharts.JiraChartMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.plugins.jiracharts.render.PieChart;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.sal.api.net.ResponseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MacroUtils.class)
public class TestJiraChartMacro extends TestCase
{
    private static final String APPLICATION_ID = "8835b6b9-5676-3de4-ad59-bbe987416662";

    @Mock private I18NBean i18NBean;

    @Mock private I18NBeanFactory i18NBeanFactory;

    @Mock private ReadOnlyApplicationLinkService applicationLinkService;

    @Mock MacroExecutorService executorService;

    @Mock private Base64JiraChartImageService base64JiraChartImageService;

    @Mock private JiraConnectorManager jiraConnectorManager;

    @Mock private JiraChartFactory jiraChartFactory;

    @Mock private JiraExceptionHelper jiraExceptionHelper;

    @Mock private ContextPathHolder contextPathHolder;

    private Map<String, String> parameters;

    @Mock private ReadOnlyApplicationLink applicationLink;

    @Mock private ApplicationLinkRequestFactory requestFactory;

    @Before
    public void init()
    {
        parameters = new HashMap<String, String>();
        parameters.put("jql", "project = TEST");
        parameters.put("serverId", APPLICATION_ID);
        parameters.put("statType", "statType");
        parameters.put("width", "100");
        parameters.put("border", "false");
        parameters.put("chartType", "pie");
    }

    public void testHappyCase() throws TypeNotInstalledException, ResponseException
    {
        final JQLValidationResult result = new JQLValidationResult();
        JQLValidator jqlValidator = new JQLValidator()
        {

            @Override
            public JQLValidationResult doValidate(Map<String, String> arg, boolean isVerifyChartSupported) throws MacroExecutionException
            {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                return result;
            }
        };

        try
        {
            doTest(parameters, result, jqlValidator);
        }
        catch (MacroExecutionException e)
        {
            assertFalse("Unexpected exception", true);
        }
    }

    public void testExceptionDuringValidateJQL() throws TypeNotInstalledException, ResponseException
    {
        final JQLValidationResult result = new JQLValidationResult();
        JQLValidator jqlValidator = new JQLValidator()
        {

            @Override
            public JQLValidationResult doValidate(Map<String, String> parameters, boolean isVerifyChartSupported) throws MacroExecutionException
            {
                result.setAuthUrl("");
                result.setErrorMgs(new ArrayList<String>());
                throw new MacroExecutionException("Fake exception");
            }
        };

        try
        {
            doTest(parameters, result, jqlValidator);
        }
        catch (MacroExecutionException e)
        {
            return;
        }

        assertFalse("Expected exception but cannot get any", true);
    }

    private void doTest(Map<String, String> parameters,
            final JQLValidationResult result,
            JQLValidator jqlValidator) throws MacroExecutionException, TypeNotInstalledException, ResponseException
    {
        Settings settings = new Settings();
        settings.setBaseUrl("http://fakelink.com");

        PowerMockito.mockStatic(MacroUtils.class);
        when(MacroUtils.defaultVelocityContext()).thenReturn(new HashMap<String, Object>());

        i18NBean = mock(I18NBean.class);
        when(i18NBean.getText(anyString())).thenReturn("jirachart.macro.dialog.statistype.statuses");

        i18NBeanFactory = mock(I18NBeanFactory.class);
        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);

        when(applicationLink.getId()).thenReturn(new ApplicationId(APPLICATION_ID));
        when(applicationLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(applicationLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl/jira"));
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        MockJiraChartMacro testObj = new MockJiraChartMacro(executorService, applicationLinkService,
                i18NBeanFactory, jqlValidator, jiraConnectorManager, jiraChartFactory, jiraExceptionHelper);

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
        Assert.assertEquals("The border value is incorrect", parameters.get("border"), outcomeBorder);
        Assert.assertNotNull("Missing JqlValidationResult", outcomeResult);


        Assert.assertArrayEquals(new JQLValidationResult[] {result},
                new JQLValidationResult[] {outcomeResult});
    }

    private class MockJiraChartMacro extends JiraChartMacro
    {

        public MockJiraChartMacro(MacroExecutorService executorService,
                ReadOnlyApplicationLinkService applicationLinkService,
                I18NBeanFactory i18nBeanFactory, JQLValidator jqlValidator,
                JiraConnectorManager jiraConnectorManager, JiraChartFactory jiraChartFactory,
                JiraExceptionHelper jiraExceptionHelper)
        {
            super(executorService, applicationLinkService, i18nBeanFactory, jiraConnectorManager, jiraChartFactory, jiraExceptionHelper, null);
            this.setJqlValidator(jqlValidator);
        }

        public Map<String, Object> executePublic(Map<String, String> parameters, String body,
                ConversionContext context) throws MacroExecutionException, TypeNotInstalledException, ResponseException
        {
            PieChart pieChart = new PieChart(contextPathHolder, base64JiraChartImageService);
            JiraImageChartModel jiraImageChartModel = new JiraImageChartModel();
            jiraImageChartModel.setBase64Image("image");
            jiraImageChartModel.setFilterUrl("url");
            jiraImageChartModel.setLocation("localtion");
            when(base64JiraChartImageService.getBase64JiraChartImageModel(anyString(), anyString())).thenReturn(jiraImageChartModel);
            return pieChart.setupContext(parameters, getJqlValidator().doValidate(parameters, true), context);
        }
    }
}
