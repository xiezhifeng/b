package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.user.impl.DefaultUser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

/**
 * TODO: Document this class / interface here
 */
public class RestTestHelper
{
    public static JSONObject getTestCreateAppLinkSubmission(String remoteUrl, DefaultUser user, boolean primary, boolean trustEachOther, boolean sharedUserBase, UUID applicationId, boolean createTwoWayLink)
            throws JSONException
    {
        JSONObject inputAppLink = new JSONObject();
        inputAppLink.put("id", applicationId);
        inputAppLink.put("typeId", "jira");
        inputAppLink.put("name", remoteUrl);
        inputAppLink.put("rpcUrl", remoteUrl);
        inputAppLink.put("displayUrl", remoteUrl);
        inputAppLink.put("isPrimary", primary);

        JSONObject configFormValuesInput = new JSONObject();
        configFormValuesInput.put("trustEachOther", trustEachOther);
        configFormValuesInput.put("shareUserbase", sharedUserBase);

        JSONObject input = new JSONObject();
        input.put("applicationLink", inputAppLink);
        input.put("username", user.getName());
        input.put("password", user.getPassword());
        input.put("createTwoWayLink", createTwoWayLink);
        input.put("customRpcURL", false);
        input.put("rpcUrl", remoteUrl);
        input.put("configFormValues", configFormValuesInput);

        return input;
    }


    @Nonnull
    public static CloseableHttpResponse getRestResponse(DefaultUser user, String url)
    {
        return callRestEndPoint(user, new HttpGet(url));
    }

    @Nonnull
    public static CloseableHttpResponse postRestResponse(DefaultUser user, String url, String requestEntity)
    {
        final HttpPost httpPost = new HttpPost(url);
        try
        {
            httpPost.setEntity(new StringEntity(requestEntity));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        httpPost.addHeader("Content-Type", "application/json");
        return callRestEndPoint(user, httpPost);
    }

    @Nonnull
    public static CloseableHttpResponse putRestResponse(DefaultUser user, String url, String requestEntity)
    {
        final HttpPut httpPut = new HttpPut(url);
        if (StringUtils.isNotEmpty(requestEntity))
        {
            try
            {
                httpPut.setEntity(new StringEntity(requestEntity));
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e);
            }
        }
        httpPut.addHeader("Content-Type", "application/json");
        return callRestEndPoint(user, httpPut);
    }

    @Nonnull
    public static CloseableHttpResponse callRestEndPoint(final DefaultUser user, final HttpRequestBase httpRequest)
    {
        httpRequest.addHeader("Accept", MediaType.APPLICATION_JSON);
        if (user != null)
        {
            String basicAuth = createAuthorization(user.getName(), user.getPassword());
            httpRequest.addHeader("Authorization", "Basic " + basicAuth);
        }

        try
        {
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpRequest);
            return response;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static String createAuthorization(final String username, final String password)
    {
        return new String(Base64.encodeBase64((username + ":" + password).getBytes()));
    }

    @Nonnull
    public static DefaultUser getDefaultUser()
    {
        DefaultUser defaultUser = new DefaultUser("admin", "admin", "");
        defaultUser.setPassword("admin");
        return defaultUser;
    }
}