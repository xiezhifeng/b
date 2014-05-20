package com.atlassian.confluence.extra.jira;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.applinks.api.event.ApplicationLinkAddedEvent;
import com.atlassian.applinks.api.event.ApplicationLinkDeletedEvent;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

import com.google.common.collect.Maps;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

public class DefaultProjectKeyCache implements ProjectKeyCache, DisposableBean
{
    private static final Logger log = Logger.getLogger(DefaultProjectKeyCache.class);

    private ApplicationLinkService appLinkService;
    private Map<String, ApplicationId> keyToAppMap;
    private Map<ApplicationId, Set<String>> appToKeyMap;

    private EventPublisher eventPublisher;

    public DefaultProjectKeyCache(ApplicationLinkService appLinkService, EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
        this.appLinkService = appLinkService;

        this.keyToAppMap = null;
        this.appToKeyMap = null;
    }

    private void loadMaps()
    {
        log.info("DefaultProjectKeyCache loadMaps");
        checkAndInitializeMap();
        Iterable<ApplicationLink> applicationLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        for (ApplicationLink applicationLink : applicationLinks)
        {
            Map projectKeyAppMap = getProjectKeyAppMap(applicationLink);
            keyToAppMap.putAll(projectKeyAppMap);
            appToKeyMap.put(applicationLink.getId(), projectKeyAppMap.keySet());
        }
    }

    private void checkAndInitializeMap()
    {
        if (keyToAppMap == null)
        {
            keyToAppMap = new HashMap<String, ApplicationId>();
            appToKeyMap = new HashMap<ApplicationId, Set<String>>();
        }
    }

    private Map<String, ApplicationId> getProjectKeyAppMap(final ApplicationLink applicationLink)
    {
        String restUrl = "/rest/gadget/1.0/pickers/projects";
        ApplicationId applicationId = applicationLink.getId();
        try
        {
            AppLinkResponseHandler appLinkResponseHandler = new AppLinkResponseHandler(applicationId);
            applicationLink.createAuthenticatedRequestFactory(Anonymous.class).createRequest(Request.MethodType.GET, restUrl).execute(appLinkResponseHandler);
            return appLinkResponseHandler.getProjectKeyAppMap();
        }
        catch (CredentialsRequiredException e)
        {
            log.warn("No project keys retrieved anonymously from " + applicationLink.getName());
        }
        catch (ResponseException e)
        {
            log.warn("No project keys retrieved anonymously from " + applicationLink.getName());
        }
        return null;
    }

    /**
     * This class is responsible for parsing the response object in JSON format and store them in a map of (projectKey,
     * appId)
     */
    private class AppLinkResponseHandler implements ResponseHandler<Response>
    {
        private Map<String, ApplicationId> projectKeyAppMap;
        private ApplicationId applicationId;

        public AppLinkResponseHandler(ApplicationId applicationId)
        {
            this.applicationId = applicationId;
            this.projectKeyAppMap = Maps.newHashMap();
        }

        /**
         * Triggered when response from {@link com.atlassian.sal.api.net.Request#execute(com.atlassian.sal.api.net.ResponseHandler)}
         * method becomes available. Implementations of this method should handle the response.
         *
         * @param response a response object. Never null.
         * @throws com.atlassian.sal.api.net.ResponseException If the response cannot be retrieved
         */
        @Override
        public void handle(Response response) throws ResponseException
        {
            try
            {
                JSONObject json = new JSONObject(response.getResponseBodyAsString());

                JSONArray array = json.getJSONArray("projects");
                for (int x = 0; x < array.length(); x++)
                {
                    JSONObject project = array.getJSONObject(x);
                    String projectKey = project.getString("key");
                    this.projectKeyAppMap.put(projectKey, this.applicationId);
                }
            }
            catch (JSONException jse)
            {
                log.warn("No project keys retrieved anonymously from " + applicationId.get());
            }
        }

        public Map<String, ApplicationId> getProjectKeyAppMap()
        {
            return this.projectKeyAppMap;
        }
    }

    /**
     * This method is used to get the ApplicationLink object stored in the in-memory map (cache)
     *
     * @return the ApplicationLink object to be retrieved
     */
    public ApplicationLink getAppForKey(String projectKey)
    {
        log.info("getAppForKey");
        if (keyToAppMap == null)
        {
            long startTime = System.currentTimeMillis();
            loadMaps();
            log.info("init time = " + (System.currentTimeMillis() - startTime));
        }
        ApplicationId appId = keyToAppMap.get(projectKey);
        try
        {
            return appLinkService.getApplicationLink(appId);
        }
        catch (TypeNotInstalledException e)
        {
            log.error("can't find application id: " + appId + " for jira project key: " + projectKey, e);
        }
        return null;
    }

    /**
     * This method is used to get the base url by the projectKey Behind the scene, the base url is retrieved from the
     * ApplicationLink object
     */
    public String getBaseUrlForKey(String projectKey)
    {
        ApplicationLink appLink = getAppForKey(projectKey);

        if (appLink != null)
        {
            return appLink.getRpcUrl().toString();
        }
        return null;
    }

    @EventListener
    public void applinkAdded(ApplicationLinkAddedEvent event)
    {
        checkAndInitializeMap();
        keyToAppMap.putAll(getProjectKeyAppMap(event.getApplicationLink()));

    }

    @EventListener
    public void applinkRemoved(ApplicationLinkDeletedEvent event)
    {
        if (keyToAppMap != null && appToKeyMap != null)
        {
            keyToAppMap.keySet().removeAll(appToKeyMap.get(event.getApplicationId()));
        }
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

}
