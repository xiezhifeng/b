package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class Base64JiraChartImageService
{

    private static final Logger LOG = LoggerFactory.getLogger(Base64JiraChartImageService.class);
    private ApplicationLinkService applicationLinkService;
    private static final String PNG_IMAGE_FORMAT_NAME = "PNG";

    public Base64JiraChartImageService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    public JiraImageChartModel getBase64JiraChartImageModel(String serverId, String gadgetURL) throws ResponseException
    {
        try
        {
            final ApplicationLink applicationLink = JiraConnectorUtils.getApplicationLink(applicationLinkService, serverId);
            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(applicationLink, Request.MethodType.GET, gadgetURL);

            return (JiraImageChartModel) request.execute(new Base64ImageResponseHandler(applicationLink.getDisplayUrl().toString()));

        }
        catch (TypeNotInstalledException e)
        {
            throw new ResponseException("Can not get application link", e);
        }
        catch (Exception e)
        {
            throw new ResponseException("Can not retrieve jira chart image", e);
        }
    }

    static class Base64ImageResponseHandler implements ApplicationLinkResponseHandler
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
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try
            {
                JiraImageChartModel chartModel = new Gson().fromJson(response.getResponseBodyAsString(), JiraImageChartModel.class);
                BufferedImage bufferedImage = ImageIO.read(new URL(baseUrl + "/charts?filename=" + chartModel.getLocation()));

                ImageIO.write(bufferedImage, PNG_IMAGE_FORMAT_NAME,  os);
                chartModel.setBase64Image("data:image/png;base64," + Base64.encodeBase64String(os.toByteArray()));
                return chartModel;
            }
            catch (Exception e)
            {
                throw new ResponseException("Can not retrieve jira chart image", e);
            }
            finally
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                    LOG.debug("Can not close output stream");
                }
            }
        }
    }
}
