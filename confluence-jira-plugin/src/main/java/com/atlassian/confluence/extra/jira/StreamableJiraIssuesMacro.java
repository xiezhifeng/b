package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.IssueData;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.pipe.KeyedData;
import com.atlassian.confluence.web.pipe.PipeExecutor;
import com.atlassian.confluence.web.pipe.PipeService;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamableJiraIssuesMacro.class);
    private static final String JIM = "JIM";
    private static final String JIM_BATCH_STARTED_MAP = "JIM_BATCH_STARTED_MAP";
    private static final String JIM_RENDER_ID_MAP = "JIM_RENDER_ID_MAP";
    private static final List<String> DEVICETYPES_INCLUDING_RES = Arrays.asList("mobile", "desktop", RenderContextOutputType.DISPLAY, RenderContextOutputType.PREVIEW);
    private static final AtomicInteger CURR_RENDER_ID = new AtomicInteger();
    private static final String RENDER_ID_KEY = "renderId";

    private final JiraMacroFinderService jiraMacroFinderService;
    private final JiraIssueBatchService jiraIssueBatchService;
    private final PageBuilderService pageBuilderService;
    private final PipeService pipeService;

    /**
     * Default constructor to get all necessary beans injected
     */
    public StreamableJiraIssuesMacro(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager,
                                     SettingsManager settingsManager, JiraIssuesColumnManager jiraIssuesColumnManager,
                                     TrustedApplicationConfig trustedApplicationConfig, PermissionManager permissionManager,
                                     ApplicationLinkResolver applicationLinkResolver, JiraIssuesDateFormatter jiraIssuesDateFormatter,
                                     MacroMarshallingFactory macroMarshallingFactory, JiraCacheManager jiraCacheManager,
                                     ImagePlaceHolderHelper imagePlaceHolderHelper, FormatSettingsManager formatSettingsManager,
                                     JiraIssueSortingManager jiraIssueSortingManager, JiraExceptionHelper jiraExceptionHelper,
                                     LocaleManager localeManager, JiraMacroFinderService jiraMacroFinderService,
                                     JiraIssueBatchService jiraIssueBatchService, PageBuilderService pageBuilderService,
                                     PipeService pipeService)
    {
        super(i18NBeanFactory, jiraIssuesManager, settingsManager, jiraIssuesColumnManager, trustedApplicationConfig, permissionManager, applicationLinkResolver, jiraIssuesDateFormatter, macroMarshallingFactory, jiraCacheManager, imagePlaceHolderHelper, formatSettingsManager, jiraIssueSortingManager, jiraExceptionHelper, localeManager);
        this.jiraMacroFinderService = jiraMacroFinderService;
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.pageBuilderService = pageBuilderService;
        this.pipeService = pipeService;
    }

    /**
     * In this method, batch requests for single JIRA issues will be sent
     *
     * @param parameters        the macro parameters
     * @param body              the macro body
     * @param conversionContext the page's conversion context
     * @return the Streamable representing the macro's rendered content
     * @throws MacroExecutionException
     */
    public Streamable executeToStream(final Map<String, String> parameters, final Streamable body,
                                      final ConversionContext conversionContext) throws MacroExecutionException
    {
        if (DEVICETYPES_INCLUDING_RES.contains(conversionContext.getOutputDeviceType()))
        {
            pageBuilderService.assembler().resources().requireWebResource("confluence.extra.jira:web-resources");
        }

        final ContentEntityObject entity = conversionContext.getEntity();
        if (parameters != null && entity != null)
        {
            final String issueKey = JiraUtil.getSingleIssueKey(parameters);
            if (issueKey != null)
            {
                batchingForSingleIssues(conversionContext, entity);
                return new SingleIssueStreamable(conversionContext, issueKey, parameters);
            }
        }
        return new IssuesStreamable(conversionContext, parameters);
    }

    private void batchingForSingleIssues(final ConversionContext conversionContext, ContentEntityObject entity) throws MacroExecutionException
    {
        long entityId = entity.getId();
        Set<Long> batchStartedSet = (Set<Long>) conversionContext.getProperty(JIM_BATCH_STARTED_MAP);
        if (batchStartedSet == null)
        {
            batchStartedSet = new HashSet<Long>();
            conversionContext.setProperty(JIM_BATCH_STARTED_MAP, batchStartedSet);
        }
        if (batchStartedSet.contains(entityId))
        {
            return;
        }
        try
        {
            String content = entity.getBodyContent().getBody();
            // We find all MacroDefinitions for single JIRA issues in the body
            try
            {
                Set<MacroDefinition> macroDefinitions = this.jiraMacroFinderService.findSingleJiraIssueMacros(content, conversionContext);
                Map<String, Map<String, IssueData>> serverIdToIssueKeyToData = Maps.newHashMap();
                for (MacroDefinition macroDefinition : macroDefinitions)
                {
                    String serverId = null;
                    String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                    Map<String, String> params = macroDefinition.getParameters();
                    try
                    {
                        serverId = getServerIdFromKey(macroDefinition.getParameters(), issueKey, conversionContext);
                    }
                    catch (MacroExecutionException e) // suppress this exception
                    {
                        if (LOGGER.isDebugEnabled())
                        {
                            LOGGER.debug(e.toString());
                        }
                    }
                    if (serverId != null)
                    {
                        Map<String, IssueData> issueKeyToData = serverIdToIssueKeyToData.get(serverId);
                        if (issueKeyToData == null)
                        {
                            issueKeyToData = Maps.newHashMap();
                            serverIdToIssueKeyToData.put(serverId, issueKeyToData);
                        }
                        IssueData data =  issueKeyToData.get(issueKey);
                        if (data == null)
                        {
                            data = new IssueData();
                            issueKeyToData.put(issueKey, data);
                            String renderId = String.valueOf(CURR_RENDER_ID.incrementAndGet());
                            setRenderId(conversionContext, serverId, issueKey, renderId);
                            data.setParams(params);
                            data.setRenderId(renderId);
                            data.setKey(issueKey);
                        }
                    }
                }
                PipeExecutor<String> executor = pipeService.getExecutor(JIM);
                pageBuilderService.assembler().resources().requireWebResource("confluence.extra.jira:refresh-place-holders");
                for (Map.Entry<String, Map<String, IssueData>> entry: serverIdToIssueKeyToData.entrySet())
                {
                    executor.schedule(new BackgroundRenderTask(entry.getKey(), entry.getValue(), conversionContext, AuthenticatedUserThreadLocal.get()));
                }
            }
            catch (XhtmlException e)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(e.toString());
                }
                throw new MacroExecutionException(e.getCause());
            }
        }
        finally
        {
            batchStartedSet.add(entityId);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setRenderId(ConversionContext ctx, String serverId, String issueKey, String renderId)
    {
        Map<ServerIdIssueKeyPair, String>  map = (Map<ServerIdIssueKeyPair, String>) ctx.getProperty(JIM_RENDER_ID_MAP);
        if (map == null)
        {
            map = new HashMap<ServerIdIssueKeyPair, String>();
            ctx.setProperty(JIM_RENDER_ID_MAP, map);
        }
        map.put(new ServerIdIssueKeyPair(serverId, issueKey), renderId);
    }

    private static String getRenderId(ConversionContext ctx, String serverId, String issueKey)
    {
        Map<ServerIdIssueKeyPair, String> map = (Map<ServerIdIssueKeyPair, String>) ctx.getProperty(JIM_RENDER_ID_MAP);
        if (map == null)
        {
            return null;
        }
        return map.get(new ServerIdIssueKeyPair(serverId, issueKey));
    }

    private String getServerIdFromKey(Map<String, String> parameters, String issueKey, ConversionContext conversionContext) throws MacroExecutionException
    {
        // We get the server ID through the ApplicationLink object because there can be no serverId and/or server specified
        // in the macro markup
        try
        {
            ApplicationLink applicationLink = this.applicationLinkResolver.resolve(Type.KEY, issueKey, parameters);
            if (applicationLink != null)
            {
                return applicationLink.getId().toString();
            }
        }
        catch (TypeNotInstalledException e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        return null;
    }

    private String renderSingleJiraIssuePlaceHolder(String key, String renderId) {
        Map<String, Object> map = MacroUtils.defaultVelocityContext();
        map.put(KEY, key);
        map.put(RENDER_ID_KEY, renderId);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/singlejiraissuePlaceHolder.vm", map);
    }

    private class BackgroundRenderTask implements Callable<List<KeyedData<String>>>
    {
        private final String serverId;
        private final Map<String, IssueData> issueKeyToData;
        private final ConversionContext ctx;
        private final ConfluenceUser user;

        public BackgroundRenderTask(String serverId,
                                    Map<String, IssueData> issueKeyToData,
                                    ConversionContext ctx,
                                    ConfluenceUser user)
        {
            this.serverId = serverId;
            this.issueKeyToData = issueKeyToData;
            this.ctx = ctx;
            this.user = user;
        }

        @Override
        public List<KeyedData<String>> call() throws Exception
        {
            try
            {
                AuthenticatedUserThreadLocal.set(user);
                List<IssueData> issueDataList = new LinkedList<IssueData>();
                Set<String> issueKeys = issueKeyToData.keySet();
                try
                {
                    Map<String, Object> resultsMap = jiraIssueBatchService.getBatchResults(serverId, issueKeys, ctx);
                    if (resultsMap != null)
                    {
                        @SuppressWarnings("unchecked")
                        Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                        String serverUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);
                        for (Map.Entry<String, Element> elem : elementMap.entrySet())
                        {
                            String issueKey = elem.getKey();
                            IssueData issueData = issueKeyToData.get(issueKey);
                            if (issueData != null)
                            {
                                issueData.setData(elem.getValue());
                                issueData.setServerUrl(serverUrl);
                                issueDataList.add(issueData);
                            }
                        }
                    }
                }
                catch (UnsupportedJiraServerException e) //We called to an old JIRA
                {
                    for (IssueData issueData : issueKeyToData.values())
                    {
                        issueDataList.add(issueData);
                    }
                }
                catch (Throwable e)
                {
                    for (IssueData issueData : issueKeyToData.values())
                    {
                        issueData.setError(e);
                        issueDataList.add(issueData);
                    }
                }
                List<KeyedData<String>> ret = Lists.newLinkedList();
                for (IssueData singleIssueData : issueDataList)
                {
                    String renderedContent;
                    Map<String, String> params = singleIssueData.getParams();
                    Element elem = singleIssueData.getData();
                    String serverUrl = singleIssueData.getServerUrl();
                    Throwable error = singleIssueData.getError();
                    try
                    {
                        if (error != null)
                        {
                            renderedContent = JiraExceptionHelper.renderExceptionMessage(jiraExceptionHelper.explainException(error));
                        }
                        else if (elem != null)
                        {
                            renderedContent = renderSingleJiraIssue(params, ctx, elem, serverUrl);
                        }
                        else if (params != null)
                        {
                            renderedContent = StreamableJiraIssuesMacro.super.execute(params, null, ctx);
                        }
                        else
                        {
                            // Should not happen
                            renderedContent = JiraExceptionHelper.renderExceptionMessage("Unknown data");
                        }
                    }
                    catch (Throwable t)
                    {
                        renderedContent = jiraExceptionHelper.renderException(t);
                    }
                    ret.add(new KeyedData<String>(singleIssueData.getRenderId(), renderedContent));
                }
                return ret;
            }
            finally
            {
                AuthenticatedUserThreadLocal.reset();
            }
        }
    }

    /**
     * Streamable for single jira issue case
     */
    private class SingleIssueStreamable implements Streamable
    {
        private final ConversionContext ctx;
        private final String key;
        private final Map<String, String> params;

        public SingleIssueStreamable(ConversionContext ctx, String key, Map<String, String> params)
        {
            this.ctx = ctx;
            this.key = key;
            this.params = params;
        }

        @Override
        public void writeTo(Writer writer) throws IOException
        {
            try
            {
                long remainingTimeout = ctx.getTimeout().getTime();
                if (remainingTimeout <= 0)
                {
                    writer.write(jiraExceptionHelper.renderSingleIssueTimeoutMessage(key));
                }
                else
                {
                    String renderId = getRenderId(ctx, getServerIdFromKey(params, key, ctx), key);
                    if (renderId != null)
                    {
                        PipeExecutor<String> executor = pipeService.getExecutor(JIM);
                        Optional<String> optionalData = executor.claim(renderId);
                        if (optionalData.isPresent())
                        {
                            writer.write(optionalData.get());
                        }
                        else
                        {
                            writer.write(renderSingleJiraIssuePlaceHolder(key, renderId));
                        }
                    }
                    else
                    {
                        writer.write(StreamableJiraIssuesMacro.super.execute(params, null, ctx));
                    }
                }
            }
            catch (Throwable t)
            {
                writer.write(jiraExceptionHelper.renderSingleIssueException(key, t));
            }
        }
    }

    private class IssuesStreamable implements Streamable
    {
        private final ConversionContext ctx;
        private final Map<String, String> parameters;

        public IssuesStreamable(ConversionContext ctx, Map<String, String> params)
        {
            this.ctx = ctx;
            this.parameters = params;
        }

        @Override
        public void writeTo(Writer writer) throws IOException
        {
            long remainingTimeout = ctx.getTimeout().getTime();
            if (remainingTimeout <= 0)
            {
                writer.write(jiraExceptionHelper.renderTimeoutMessage());
            }
            else
            {
                try
                {
                    writer.write(StreamableJiraIssuesMacro.super.execute(parameters, null, ctx));
                }
                catch (Throwable t)
                {
                    writer.write(jiraExceptionHelper.renderException(t));
                }
            }
        }
    }

    private static final class ServerIdIssueKeyPair
    {
        private final String serverId;
        private final String issueKey;

        public ServerIdIssueKeyPair(String serverId, String issueKey)
        {
            this.serverId = serverId;
            this.issueKey = issueKey;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServerIdIssueKeyPair that = (ServerIdIssueKeyPair) o;

            return serverId.equals(that.serverId) && issueKey.equals(that.issueKey);

        }

        @Override
        public int hashCode()
        {
            int result = serverId.hashCode();
            result = 31 * result + issueKey.hashCode();
            return result;
        }
    }
}
