package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.extra.jira.model.PieChartModel;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class Base64ImageService
{

    private ApplicationLinkService applicationLinkService;

    public Base64ImageService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    public String request(String url, String serverId) throws ResponseException
    {
        try
        {
            final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
            ApplicationLinkRequest request = getApplicationLinkRequest(applicationLink, url);

            ApplicationLinkResponseHandler handler = new ApplicationLinkResponseHandler()
            {
                @Override
                public Object credentialsRequired(Response response) throws ResponseException
                {
                    throw new ResponseException("Required Credentials");
                }

                @Override
                public Object handle(Response response) throws ResponseException
                {
                    try
                    {
                        PieChartModel pieModel = new Gson().fromJson(response.getResponseBodyAsString(), PieChartModel.class);
                        BufferedImage bufferedImage = ImageIO.read(new URL(applicationLink.getRpcUrl() + "/charts?filename=" + pieModel.getLocation()));
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "PNG", os);
                        return Base64.encodeBase64String(os.toByteArray());

                    }
                    catch (IOException e)
                    {
                        throw new ResponseException("Can not retrieve jira chart image", e);
                    }
                }
            };

            String result = (String) request.execute(handler);
            return "data:image/png;base64," + result;

        }
        catch (Exception e)
        {
            throw new ResponseException("Can not retrieve jira chart image");
        }
    }

    private ApplicationLinkRequest getApplicationLinkRequest(ApplicationLink applicationLink, String url) throws CredentialsRequiredException
    {
        ApplicationLinkRequest applicationLinkRequest;
        try
        {
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
            applicationLinkRequest = requestFactory.createRequest(Request.MethodType.GET, url);
        }
        catch (CredentialsRequiredException e)
        {
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
            applicationLinkRequest = requestFactory.createRequest(Request.MethodType.GET, url);
        }
        return applicationLinkRequest;
    }


}
