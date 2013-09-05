package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;


import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.event.ApplicationLinkAddedEvent;
import com.atlassian.applinks.api.event.ApplicationLinkDeletedEvent;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

public class DefaultProjectKeyCache implements ProjectKeyCache, DisposableBean
{
    private static final Logger log = Logger.getLogger(DefaultProjectKeyCache.class);
    
    private ApplicationLinkService appLinkService;
    private Map<String, ApplicationId> keyToAppMap;

    private HttpRetrievalService httpRetrievalService;

    private EventPublisher eventPublisher;

    public DefaultProjectKeyCache(ApplicationLinkService appLinkService, BandanaManager bandanaManager, HttpRetrievalService httpRetrievalService, EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
        this.appLinkService = appLinkService;
        this.httpRetrievalService = httpRetrievalService;
        reload();
    }

    private void reload()
    {
        try
        {
            loadProjectKeys();
        }
        catch (Exception e)
        {
            log.error("while loading project keys", e);
        }
        finally
        {
            if(keyToAppMap == null)
            {
                keyToAppMap = new HashMap<String, ApplicationId>();
            }
        }
    }
    
    private void loadProjectKeys() throws IOException, JSONException
    {
        Map<String, List<String>> appProjectMap = retrieveProjectKeysAnonymously();
        Set<String> keySet = appProjectMap.keySet();
        keyToAppMap = new HashMap<String, ApplicationId>();
        for (String appIdStr : keySet)
        {
            List<String> keyList = appProjectMap.get(appIdStr);
            ApplicationId appId = new ApplicationId(appIdStr);
            for (String key : keyList)
            {
                keyToAppMap.put(key, appId);
            }
        }
    }
    
    private  Map<String, List<String>> retrieveProjectKeysAnonymously() throws IOException, JSONException
    {
        Map<String, List<String>> appProjectMap = new HashMap<String, List<String>>();
        
        Iterable<ApplicationLink> applicationLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        for (ApplicationLink applicationLink : applicationLinks)
        {
            try
            {
                getKeysForAppLink(appProjectMap, applicationLink);
            }
            catch (IOException e)
            {
                log.warn("Unable to retrieve project keys anonymously from JIRA applink named " + applicationLink.getName(), e);
            }
        }
        return appProjectMap;
    }

    private void getKeysForAppLink(final Map<String, List<String>> appProjectMap, final ApplicationLink applicationLink)
            throws IOException
    {
        String restUrl = "/rest/gadget/1.0/pickers/projects";
        try
        {
            applicationLink.createAuthenticatedRequestFactory(Anonymous.class).createRequest(Request.MethodType.GET, restUrl).execute(new ResponseHandler<Response>()
            {
                @Override
                public void handle(final Response response) throws ResponseException
                {
                    final List<String> keysForLink = new ArrayList<String>();
                    try
                    {
                        JSONObject json = new JSONObject(response.getResponseBodyAsString());

                        JSONArray array = json.getJSONArray("projects");
                        for (int x = 0; x < array.length(); x++)
                        {
                            JSONObject project = array.getJSONObject(x);
                            String projectKey = project.getString("key");
                            keysForLink.add(projectKey);
                        }
                    }
                    catch(JSONException jse)
                    {
                        log.warn("No project keys retrieved anonymously from " + applicationLink.getName());
                    }
                    appProjectMap.put(applicationLink.getId().toString(), keysForLink);
                }
            });
        }
        catch (CredentialsRequiredException e)
        {
            log.warn("No project keys retrieved anonymously from " + applicationLink.getName());
            appProjectMap.put(applicationLink.getId().toString(), Collections.<String>emptyList());
        }
        catch (ResponseException e)
        {
            log.warn("No project keys retrieved anonymously from " + applicationLink.getName());
            appProjectMap.put(applicationLink.getId().toString(), Collections.<String>emptyList());
        }
    }

    public ApplicationLink getAppForKey(String projectKey) 
    {
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
        reload();
    }
    @EventListener
    public void applinkRemoved(ApplicationLinkDeletedEvent event)
    {
        reload();
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

}
