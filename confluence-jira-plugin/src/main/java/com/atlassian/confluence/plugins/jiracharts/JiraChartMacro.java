package com.atlassian.confluence.plugins.jiracharts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Function;

/**
 * The macro to display Jira chart 
 * 
 * @author duy.luong
 *
 */
public class JiraChartMacro implements StreamableMacro, EditorImagePlaceholder {
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy";
    private static final String JIRA_SEARCH_URL = "/rest/api/2/search";
    private static final String TEMPLATE_PATH = "templates/jirachart";

    private ApplicationLinkService applicationLinkService;

    private final MacroExecutorService executorService;
    private I18NBeanFactory i18NBeanFactory;
    private Function<Map<String, String>, JQLValidationResult> jqlValidator;
    private Settings settings;

    /**
     * JiraChartMacro constructor
     * 
     * @param executorService
     * @param applicationLinkService
     * @param i18NBeanFactory
     */
    public JiraChartMacro(MacroExecutorService executorService,
            ApplicationLinkService applicationLinkService,
            I18NBeanFactory i18NBeanFactory) {
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkService = applicationLinkService;
    }

    @Override
    public String execute(Map<String, String> parameters, String body,
            ConversionContext context) throws MacroExecutionException {
        Map<String, Object> contextMap = executeInternal(parameters, body, context);
        return VelocityUtils.getRenderedTemplate(
                TEMPLATE_PATH + "/piechart.vm", contextMap);
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters,
            ConversionContext context) {
        try {
            String jql = GeneralUtil.urlDecode(parameters.get("jql"));
            String statType = parameters.get("statType");
            String serverId = parameters.get("serverId");
            String authenticated = parameters.get("isAuthenticated");
            if (jql != null && statType != null && serverId != null) {
                ApplicationLink appLink = applicationLinkService
                        .getApplicationLink(new ApplicationId(serverId));
                if (appLink != null) {
                    // Using anonymous user to display chart place holder
                    UrlBuilder urlBuilder = new UrlBuilder(SERVLET_PIE_CHART);
                    urlBuilder.add("jql", jql).add("statType", statType)
                            .add("appId", serverId).add("chartType", "pie")
                            .add("authenticated", authenticated);
                    
                    String url = urlBuilder.toUrl();
                    return new DefaultImagePlaceholder(url, null, false);
                }
            }
        } catch (TypeNotInstalledException e) {
            log.error("error don't exist applink", e);
        }
        return null;
    }

    @Override
    public Streamable executeToStream(Map<String, String> parameters,
            Streamable body, ConversionContext context)
            throws MacroExecutionException {
        Future<String> futureResult = executorService
                .submit(new StreamableMacroFutureTask(parameters, context,
                        this, AuthenticatedUserThreadLocal.get()));

        return new FutureStreamableConverter.Builder(futureResult, context,
                i18NBeanFactory.getI18NBean())
                .executionErrorMsg("jirachart.error.execution")
                .timeoutErrorMsg("jirachart.error.timeout")
                .interruptedErrorMsg("jirachart.error.interrupted").build();
    }
    
    public Function<Map<String, String>, JQLValidationResult> getJqlValidator() {
        if (jqlValidator == null){
            this.setJqlValidator(new DefaultJqlValidator());
        }
        return jqlValidator;
    }

    public void setJqlValidator(Function<Map<String, String>, JQLValidationResult> jqlValidator) {
        this.jqlValidator = jqlValidator;
    }
    
    /**
     * Purpose of this method is make JiraChartMarco testable
     * 
     * @param parameters
     * @param body
     * @param context
     * @return The Velocity Context
     * @throws MacroExecutionException 
     */
    protected Map<String, Object> executeInternal(Map<String, String> parameters, String body,
            ConversionContext context) throws MacroExecutionException{
        String jql = GeneralUtil.urlDecode(parameters.get("jql"));
        String serverId = parameters.get("serverId");
        
        boolean isReviewMode = ConversionContextOutputType.PREVIEW.name().equalsIgnoreCase(context.getOutputType());
        
        JQLValidationResult result = getJqlValidator().apply(parameters);
        
        if (result.getException() != null){
            log.error("Exception during validtion JQL");
            throw result.getException();
        }

        UrlBuilder urlBuilder = new UrlBuilder(getSettings()
                .getBaseUrl() + SERVLET_PIE_CHART);
        urlBuilder.add("jql", jql).add("statType", parameters.get("statType"))
                .add("appId", serverId).add("chartType", "pie")
                .add("authenticated", !result.isNeedOAuth());

        String width = parameters.get("width");
        if (!StringUtils.isBlank(width) && Integer.parseInt(width) > 0) {
            urlBuilder.add("width", width)
            .add("height", (Integer.parseInt(width) * 2 / 3));
        }
        String url = urlBuilder.toUrl();

        Map<String, Object> contextMap = createVelocityContext();
        contextMap.put("jqlValidationResult", result);
        contextMap.put("srcImg", url);
        contextMap.put("border", Boolean.parseBoolean(parameters.get("border")));
        contextMap.put("isReviewMode", isReviewMode);
        return contextMap;
    }

    protected Map<String, Object> createVelocityContext() {
        return MacroUtils.defaultVelocityContext();
    }

    public Settings getSettings() {
        if (settings == null){
            settings = GeneralUtil.getGlobalSettings();
        }
        
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * Contain the actual validation logic. 
     * 
     * @author duy.luong
     *
     */
    private class DefaultJqlValidator implements Function<Map<String, String>, JQLValidationResult>{

        @Override
        public JQLValidationResult apply(Map<String, String> parameters) {
            String jql = GeneralUtil.urlDecode(parameters.get("jql"));
            String appLinkId = parameters.get("serverId");
            
            MacroExecutionException exception = null;
            
            JQLValidationResult result = new JQLValidationResult();
            try {
                ApplicationLink appLink = applicationLinkService
                        .getApplicationLink(new ApplicationId(appLinkId));
                ApplicationLinkRequestFactory requestFactory = appLink
                        .createAuthenticatedRequestFactory();
                if (requestFactory == null)
                    return null;

                UrlBuilder urlBuilder = new UrlBuilder(JIRA_SEARCH_URL);
                urlBuilder.add("jql", jql).add("maxResults", 0);
                String url = urlBuilder.toUrl();

                ApplicationLinkRequest request = requestFactory.createRequest(
                        Request.MethodType.GET, url);
                JiraResponse jiraResponse = request
                        .execute(new ApplicationLinkResponseHandler<JiraResponse>() {
                            @Override
                            public JiraResponse handle(Response response)
                                    throws ResponseException {
                                JiraResponse returnValue = new JiraResponse();
                                int responseStatus = response.getStatusCode();
                                String responseBody = response.getResponseBodyAsString();
                                
                                List<String> errorList = new ArrayList<String>();
                                int totalIssue = 0;
                                try {
                                    JSONObject json = new JSONObject(responseBody);
                                
                                    if (responseStatus >= 400) {
                                        JSONArray errors = json
                                                .getJSONArray("errorMessages");
                                        
                                        for (int i = 0; i < errors.length(); i++) {
                                            errorList.add(errors.getString(i));
                                        }
                                        
                                        returnValue.setErrors(errorList);
                                    }
                                    
                                    if (responseStatus == 200){
                                        // get total count
                                        totalIssue = json.getInt("total");
                                        returnValue.setIssueCount(totalIssue);
                                    }
                                
                                } catch (JSONException ex) {
                                    throw new ResponseException(
                                            "Could not parse json from JIRA",
                                            ex);
                                }
                                
                                return returnValue;
                            }

                            @Override
                            public JiraResponse credentialsRequired(
                                    Response paramResponse)
                                    throws ResponseException {
                                return null;
                            }
                        });

                result.setErrorMgs(jiraResponse.getErrors());
                result.setIssueCount(jiraResponse.getIssueCount());
            } catch (CredentialsRequiredException e) {
                // we need use to input credential
                result.setAuthUrl(e.getAuthorisationURI().toString());
            } catch (ResponseException e) {
                log.error("Exceptino during make a call to JIRA via Applink", e);
                exception = new MacroExecutionException(e);
            } catch (TypeNotInstalledException e) {
                log.error("AppLink is not exits", e);
                exception = new MacroExecutionException("Applink is not exits", e);
            }
            
            result.setException(exception);
            return result;
        }
        
    }
    
    private class JiraResponse{
        
        private List<String> errors;
        
        private int issueCount;

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public int getIssueCount() {
            return issueCount;
        }

        public void setIssueCount(int issueCount) {
            this.issueCount = issueCount;
        }
     }
}
