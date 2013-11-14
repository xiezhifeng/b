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

public class Base64JiraChartImageService
{

    private ApplicationLinkService applicationLinkService;
    private static String PNG_IMAGE_FORMAT_NAME = "PNG";

    public Base64JiraChartImageService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    public String getBase64JiraChartImage(String url, String serverId) throws ResponseException
    {
        try
        {
            final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
            ApplicationLinkRequest request = getApplicationLinkRequest(applicationLink, url);
            String result = (String) request.execute(new Base64ImageResponseHandler(applicationLink.getRpcUrl().toString()));
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

    class Base64ImageResponseHandler implements ApplicationLinkResponseHandler
    {
        private String baseUrl;

        Base64ImageResponseHandler(String baseUrl)
        {
            this.baseUrl = baseUrl;
        }

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
                BufferedImage bufferedImage = ImageIO.read(new URL(baseUrl + "/charts?filename=" + pieModel.getLocation()));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, PNG_IMAGE_FORMAT_NAME,  os);
                return Base64.encodeBase64String(os.toByteArray());
            }
            catch (Exception e)
            {
                throw new ResponseException("Can not retrieve jira chart image", e);
            }
        }
    }
}
