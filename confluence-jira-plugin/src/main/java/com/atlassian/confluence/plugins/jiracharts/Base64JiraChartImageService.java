package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.extra.jira.model.JiraChartModel;
import com.atlassian.confluence.extra.jira.model.Locatable;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jiracharts.model.JiraChartParams;
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
import java.util.Map;

public class Base64JiraChartImageService
{

    private static final Logger LOG = LoggerFactory.getLogger(Base64JiraChartImageService.class);
    private ApplicationLinkService applicationLinkService;
    private static final String PNG_IMAGE_FORMAT_NAME = "PNG";

    public Base64JiraChartImageService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    public String getBase64JiraChartImage(Map<String, String> params) throws ResponseException
    {
        try
        {
            final ApplicationLink applicationLink = JiraConnectorUtils.getApplicationLink(applicationLinkService, params.get("serverId"));
            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(applicationLink, Request.MethodType.GET, JiraChartParams.buildJiraGadgetUrl(params));

            String result = (String) request.execute(new Base64ImageResponseHandler(applicationLink.getRpcUrl().toString()));
            return "data:image/png;base64," + result;
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
                Locatable chartLocatable = new Gson().fromJson(response.getResponseBodyAsString(), JiraChartModel.class);
                BufferedImage bufferedImage = ImageIO.read(new URL(baseUrl + "/charts?filename=" + chartLocatable.getLocation()));

                ImageIO.write(bufferedImage, PNG_IMAGE_FORMAT_NAME,  os);
                return Base64.encodeBase64String(os.toByteArray());
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
