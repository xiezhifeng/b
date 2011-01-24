package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.StateAware;

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
            getKeysForAppLink(appProjectMap, applicationLink);            
        }
        return appProjectMap;
    }

    private void getKeysForAppLink(Map<String, List<String>> appProjectMap, ApplicationLink applicationLink)
            throws IOException
    {
        String restUrl = "rest/gadget/1.0/pickers/projects";
        List<String> keysForLink = new ArrayList<String>();
        String rpcUrl = applicationLink.getRpcUrl().toString();
        HttpRequest req = httpRetrievalService.getDefaultRequestFor(rpcUrl + (rpcUrl.endsWith("/") ? "" : "/") + restUrl);
        HttpResponse resp = httpRetrievalService.get(req);
        
        if (!resp.isFailed())
        {
            String encoding = getResponseEncoding(resp);
            
            String jsonResp = IOUtils.toString(resp.getResponse(), encoding);
            try
            {
                JSONObject json = new JSONObject(jsonResp);
                
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
                log.warn("Unable to retrieve project keys anonymously from JIRA applink named " + applicationLink.getName(), jse);
            }
        }
        appProjectMap.put(applicationLink.getId().toString(), keysForLink);
    }

    private String getResponseEncoding(HttpResponse resp)
    {
        String contentType = resp.getContentType();
        String[] split = contentType != null ? contentType.split(";") : new String[0];
        String encoding = "utf-8";
        if (split.length > 1)
        {
            if (split[1].startsWith("charset="))
            {
                encoding = split[1].substring(8);
            }
        }
        return encoding.trim();
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
