package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CreatedAndResolvedChartRenderer implements JiraChart
{
    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        return null;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        return null;
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return null;
    }

    @Override
    public String getJiraGadgetUrl(HttpServletRequest request)
    {
        return null;
    }

    @Override
    public String getTemplateFileName()
    {
        return null;
    }

    @Override
    public String[] getChartParameters()
    {
        return new String[0];
    }
}
