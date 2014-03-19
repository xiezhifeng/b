package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Group;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.json.JsonBoolean;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractApplinkedWebDriverTest extends AbstractWebDriverTest
{
    public enum ApplinkAuthenticationType
    {
        NONE,
        BASIC_ACCESS,
        OAUTH,
        OAUTH_2LO,
        OAUTH_2LO_WITH_IMPERSONATION,
        TRUSTED_APPS
    }

    protected Map<String, String> applinkIds = new HashMap<String, String>();
    protected String jiraBaseUrl = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    protected String confBaseUrl = System.getProperty("baseurl.confluence", "http://localhost:1990/confluence");

    private static final Logger log = LoggerFactory.getLogger(AbstractApplinkedWebDriverTest.class);

    final HttpClient client = new HttpClient();

    // Sets up the sysadmin user to have correct permissions to use coathanger features
    // This is a hack until under management is properly implemented in CONFDEV-20880
    @Before
    public void setupUsers() throws Exception
    {
        // Hack - set correct user group while UserManagementHelper is still being fixed (CONFDEV-20880). This logic should be handled by using Group.USERS
        Group userGroup = TestProperties.isOnDemandMode() ? Group.ONDEMAND_ALACARTE_USERS : Group.CONF_ADMINS;

        // Setup User.ADMIN to have all permissions
        userHelper.createGroup(Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, Group.DEVELOPERS);
        userHelper.addUserToGroup(User.ADMIN, userGroup);

        userHelper.synchronise();
        // Hack - the synchronise method doesn't actually sync the directory on OD so we just need to wait... Should also be addressed in CONFDEV-20880
        Thread.sleep(10000);
    }

    @After
    public void deleteAllTestApplinks()
    {
        if (!TestProperties.isOnDemandMode())
        {
            deleteApplink(confBaseUrl, jiraBaseUrl);
            deleteApplink(jiraBaseUrl, confBaseUrl);
        }
    }

    protected void setupAppLinks(ApplinkAuthenticationType applinkAuthType, User user) throws InterruptedException
    {
        applinkIds = createAllApplinks();

        if (!TestProperties.isOnDemandMode())
        {
            addAuthenticationProvider(applinkAuthType, applinkIds.get(jiraBaseUrl), user);
            addConsumer(applinkAuthType, applinkIds.get(confBaseUrl));

            // Hack - allow time for ApplicationLinkService to register newly created applinks (only for BTF instances where applinks are created ad hoc)
            // Can be removed once CONFDEV-21771 is resolved
            Thread.sleep(10000);
        }
    }

    private String getAuthenticationParams()
    {
        return "os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    private void deleteApplink(String sourceUrl, String destUrl)
    {
        String applinkId = getApplinkId(sourceUrl, destUrl);

        if (applinkId != null)
        {
            String queryString = "?id=" + applinkId + "&reciprocate=false&" + getAuthenticationParams();
            final DeleteMethod deleteMethod = new DeleteMethod(sourceUrl + "/rest/applinks/2.0/applicationlink/" + applinkId + queryString);

            try
            {
                Assert.assertEquals(HttpStatus.SC_OK, client.executeMethod(deleteMethod));
            }
            catch (Exception e)
            {
                log.error("Error deleting applinks from instance: " + sourceUrl, e);
            }
        }
    }

    private Map<String, String> createAllApplinks()
    {
        applinkIds.put(jiraBaseUrl, createApplink(confBaseUrl, jiraBaseUrl, "jira"));
        applinkIds.put(confBaseUrl, createApplink(jiraBaseUrl, confBaseUrl, "confluence"));

        return applinkIds;
    }

    private String createApplink(String sourceUrl, String destUrl, String destType)
    {
        String applinkId = getApplinkId(sourceUrl, destUrl);

        if (applinkId == null)
        {
            final PutMethod putMethod = new PutMethod(sourceUrl + "/rest/applinks/2.0/applicationlink?" + getAuthenticationParams());

            JsonObject applicationLink = new JsonObject();
            applicationLink.setProperty("typeId", destType);
            applicationLink.setProperty("name", "test" + destType);
            applicationLink.setProperty("displayUrl", destUrl);
            applicationLink.setProperty("rpcUrl", destUrl);
            applicationLink.setProperty("isPrimary", new JsonBoolean(true));

            try
            {
                final StringRequestEntity requestEntity = new StringRequestEntity(applicationLink.serialize(), "application/json", "UTF-8");
                putMethod.setRequestHeader("Accept", "application/json");
                putMethod.setRequestEntity(requestEntity);
                Assert.assertEquals(HttpStatus.SC_CREATED, client.executeMethod(putMethod));

                final JSONObject jsonObj = new JSONObject(putMethod.getResponseBodyAsString());
                String selfLink = jsonObj.getJSONArray("resources-created").getJSONObject(0).getString("href");
                applinkId = getIdFromSelfLink(selfLink);
            }
            catch (Exception e)
            {
                log.error("Error creating application link from " + sourceUrl + " to " + destUrl, e);
            }
        }

        return applinkId;
    }

    private String getApplinkId(String sourceUrl, String destUrl)
    {
        final GetMethod getMethod = new GetMethod(sourceUrl + "/rest/applinks/2.0/applicationlink?" + getAuthenticationParams());
        JSONArray applinkArray = new JSONArray();
        String applinkId = null;
        getMethod.setRequestHeader("Accept", "application/json");

        try
        {
            Assert.assertEquals(HttpStatus.SC_OK, client.executeMethod(getMethod));
            applinkArray = (new JSONObject(getMethod.getResponseBodyAsString())).getJSONArray("applicationLinks");
        }
        catch (Exception e)
        {
            log.error("Error retrieving applinks for instance at: " + sourceUrl, e);
        }

        for (int i = 0; i < applinkArray.length(); i++)
        {
            try
            {
                final String url = applinkArray.getJSONObject(i).getString("displayUrl");
                Assert.assertNotNull(url);
                if (url.equals(destUrl))
                {
                    applinkId = applinkArray.getJSONObject(i).getString("id");
                    break;
                }
            }
            catch (JSONException e)
            {
                log.error("Error parsing JSON object for applink rpcUrl", e);
            }
        }

        return applinkId;
    }

    private String getIdFromSelfLink(String selfLink)
    {
        Pattern p = Pattern.compile(".+?/rest/applinks/2.0/applicationlink/(.*)$");
        Matcher m = p.matcher(selfLink);
        m.find();
        return m.group(1);
    }

    private void addAuthenticationProvider(ApplinkAuthenticationType applinkAuthType, String destApplinkId, User user)
    {
        final PutMethod providerMethod = new PutMethod(confBaseUrl + "/rest/applinks/2.0/applicationlink/" + destApplinkId + "/authentication/provider?id=" + destApplinkId + "&" + getAuthenticationParams());
        String providerClass;
        String moduleClass = null;
        JsonObject config = new JsonObject();
        switch (applinkAuthType)
        {
            case NONE:
                log.info("Application link authentication type specified as none. No application link authentication has been configured.");
                return;
            case BASIC_ACCESS:
                providerClass = "com.atlassian.applinks.api.auth.types.BasicAuthenticationProvider";
                config.setProperty("username", user.getUsername());
                config.setProperty("password", user.getPassword());
                break;
            case OAUTH:
                providerClass = "com.atlassian.applinks.api.auth.types.OAuthAuthenticationProvider";
                moduleClass = "com.atlassian.applinks.core.auth.oauth.OAuthAuthenticatorProviderPluginModule";
                break;
            case OAUTH_2LO:
                providerClass = "com.atlassian.applinks.api.auth.types.OAuthAuthenticationProvider";
                moduleClass = "com.atlassian.applinks.core.auth.oauth.twolo.TwoLeggedOAuthAuthenticatorProviderPluginModule";
                break;
            case OAUTH_2LO_WITH_IMPERSONATION:
                providerClass = "com.atlassian.applinks.api.auth.types.OAuthAuthenticationProvider";
                moduleClass = "com.atlassian.applinks.core.auth.oauth.twolo.impersonation.TwoLeggedOAuthWithImpersonationAuthenticatorProviderPluginModule";
                break;
            case TRUSTED_APPS:
                // Does not currently work. Use either OAuth or Basic Access to get a working applink
                providerClass = "com.atlassian.applinks.api.auth.types.TrustedAppsAuthenticationProvider";
                break;
            default:
                log.error("No supported application link authentication type specified. No application link authentication has been configured.");
                return;
        }

        JsonObject authenticationProvider = new JsonObject();
        authenticationProvider.setProperty("provider", providerClass);
        authenticationProvider.setProperty("config", config);
        if (moduleClass != null)
        {
            authenticationProvider.setProperty("module", moduleClass);
        }

        try
        {
            final StringRequestEntity requestEntity = new StringRequestEntity(authenticationProvider.serialize(), "application/json", "UTF-8");

            providerMethod.setRequestHeader("Accept", "application/json");
            providerMethod.setRequestEntity(requestEntity);
            Assert.assertEquals(HttpStatus.SC_CREATED, client.executeMethod(providerMethod));
        }
        catch (Exception e)
        {
            log.error("Error adding OAuth authentication provider to instance: " + confBaseUrl, e);
        }
    }


    private void addConsumer(ApplinkAuthenticationType applinkAuthType, String destApplinkId)
    {
        // Consumer only required for OAuth
        switch (applinkAuthType)
        {
            case OAUTH:
                sendConsumerRequest(destApplinkId, false, false);
                break;
            case OAUTH_2LO:
                sendConsumerRequest(destApplinkId, true, false);
                break;
            case OAUTH_2LO_WITH_IMPERSONATION:
                sendConsumerRequest(destApplinkId, true, true);
                break;
            default:
                // No consumer required for other applinks
        }
    }

    private void sendConsumerRequest(String destApplinkId, boolean twoLOAllowed, boolean twoLOImpersonationAllowed)
    {
        final PutMethod consumerMethod = new PutMethod(jiraBaseUrl + "/rest/applinks/2.0/applicationlink/" + destApplinkId + "/authentication/consumer?id=" + destApplinkId + "&autoConfigure=true&" + getAuthenticationParams());
        JsonObject consumerEntity = new JsonObject();
        consumerEntity.setProperty("twoLOAllowed", new JsonBoolean(twoLOAllowed));
        consumerEntity.setProperty("twoLOImpersonationAllowed", new JsonBoolean(twoLOImpersonationAllowed));

        try
        {
            final StringRequestEntity requestEntity = new StringRequestEntity(consumerEntity.serialize(), "application/json", "UTF-8");

            consumerMethod.setRequestHeader("Accept", "application/json");
            consumerMethod.setRequestEntity(requestEntity);
            Assert.assertEquals(HttpStatus.SC_CREATED, client.executeMethod(consumerMethod));
        }
        catch (Exception e)
        {
            log.error("Error adding consumer to instance: " + jiraBaseUrl, e);
        }
    }
}
