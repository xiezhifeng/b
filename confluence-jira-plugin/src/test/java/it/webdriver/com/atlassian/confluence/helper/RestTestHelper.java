package it.webdriver.com.atlassian.confluence.helper;

import com.atlassian.user.impl.DefaultUser;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

/**
 * TODO: Document this class / interface here
 */
public class RestTestHelper {

    @Nonnull
    public static CloseableHttpResponse postRestResponse(DefaultUser user, String url, String requestEntity) throws IOException
    {
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(requestEntity, Charset.defaultCharset()));
        httpPost.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        return callRestEndPoint(user, httpPost);
    }

    @Nonnull
    public static CloseableHttpResponse callRestEndPoint(final DefaultUser user, final HttpRequestBase httpRequest) throws IOException
    {
        httpRequest.addHeader("Accept", MediaType.APPLICATION_JSON);
        String basicAuth = createAuthorization(user.getName(), user.getPassword());
        httpRequest.addHeader("Authorization", "Basic " + basicAuth);
        return HttpClients.createDefault().execute(httpRequest);

    }

    @Nonnull
    private static String createAuthorization(final String username, final String password)
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