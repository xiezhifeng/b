package com.atlassian.confluence.plugins.jiracharts;

import java.util.ArrayList;
import java.util.Collections;
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
import com.atlassian.applinks.api.auth.types.OAuthAuthenticationProvider;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
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
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;

public class JiraChartMacro implements StreamableMacro, EditorImagePlaceholder {
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy";
    private static final String JIRA_SEARCH_URL = "/rest/api/2/search";
    private static final String TEMPLATE_PATH = "templates/jirachart";

    private ApplicationLinkService applicationLinkService;

    private final MacroExecutorService executorService;
    private I18NBeanFactory i18NBeanFactory;

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
        String jql = GeneralUtil.urlDecode(parameters.get("jql"));
        String serverId = parameters.get("serverId");

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String oauUrl = getOauUrl(parameters.get("serverId"));
        JQLValidationResult result = validateJQL(jql, serverId);

        UrlBuilder urlBuilder = new UrlBuilder(GeneralUtil.getGlobalSettings()
                .getBaseUrl() + SERVLET_PIE_CHART);
        urlBuilder.add("jql", jql).add("statType", parameters.get("statType"))
                .add("appId", serverId).add("chartType", "pie")
                .add("authenticated", StringUtils.isEmpty(oauUrl));

        String url = urlBuilder.toUrl();

        StringBuffer urlFull = new StringBuffer(url);

        String width = parameters.get("width");
        if (!StringUtils.isBlank(width) && Integer.parseInt(width) > 0) {
            urlFull.append("&width=" + width + "&height="
                    + (Integer.parseInt(width) * 2 / 3));
        }

        contextMap.put("jqlValidationResult", result);
        contextMap.put("srcImg", urlFull.toString());
        contextMap
                .put("border", Boolean.parseBoolean(parameters.get("border")));
        return VelocityUtils.getRenderedTemplate(
                TEMPLATE_PATH + "/piechart.vm", contextMap);
    }

    private JQLValidationResult validateJQL(String jql, String appLinkId)
            throws MacroExecutionException {
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
            List<String> errorResponse = request
                    .execute(new ApplicationLinkResponseHandler<List<String>>() {
                        @Override
                        public List<String> handle(Response response)
                                throws ResponseException {
                            if (response.getStatusCode() >= 400) {
                                try {
                                    JSONObject json = new JSONObject(response
                                            .getResponseBodyAsString());
                                    JSONArray errors = json
                                            .getJSONArray("errorMessages");
                                    List<String> errorList = new ArrayList<String>();
                                    for (int i = 0; i < errors.length(); i++) {
                                        errorList.add(errors.getString(i));
                                    }
                                    return errorList;
                                } catch (JSONException ex) {
                                    throw new ResponseException(
                                            "Could not parse json from JIRA",
                                            ex);
                                }
                            }
                            return Collections.EMPTY_LIST;
                        }

                        @Override
                        public List<String> credentialsRequired(
                                Response paramResponse)
                                throws ResponseException {
                            return null;
                        }
                    });

            result.setErrorMgs(errorResponse);
        } catch (CredentialsRequiredException e) {
            // we need use to input credential
            result.setAuthUrl(e.getAuthorisationURI().toString());
        } catch (ResponseException e) {
            log.error("Exceptino during make a call to JIRA via Applink", e);
            throw new MacroExecutionException(e);
        } catch (TypeNotInstalledException e) {
            log.error("AppLink is not exits", e);
            throw new MacroExecutionException("Applink is not exits", e);
        }

        return result;
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
            String jql = parameters.get("jql");
            String statType = parameters.get("statType");
            String serverId = parameters.get("serverId");
            String authenticated = parameters.get("isAuthenticated");
            if (jql != null && statType != null && serverId != null) {
                ApplicationLink appLink = applicationLinkService
                        .getApplicationLink(new ApplicationId(serverId));
                if (appLink != null) {
                    String url = String.format(SERVLET_PIE_CHART, jql,
                            statType, serverId, authenticated);
                    return new DefaultImagePlaceholder(url, null, false);
                }
            }
        } catch (TypeNotInstalledException e) {
            log.error("error don't exist applink", e);
        }
        return null;
    }

    private String getOauUrl(String appLinkId) {
        try {
            ApplicationLink appLink = applicationLinkService
                    .getApplicationLink(new ApplicationId(appLinkId));
            ApplicationLinkRequestFactory requestFactory = appLink
                    .createAuthenticatedRequestFactory(OAuthAuthenticationProvider.class);
            if (requestFactory == null)
                return null;

            requestFactory.createRequest(Request.MethodType.GET, "");
        } catch (CredentialsRequiredException e) {
            return e.getAuthorisationURI().toString();
        } catch (TypeNotInstalledException e) {
            log.error("AppLink is not exits", e);
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
}
