package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CreatedAndResolvedChartRenderer implements JiraChartRenderer
{
    @Override

    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        return null;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        return null;
    }

    @Override
    public String getJiraGagetUrl()
    {
        return null;
    }

    @Override
    public String getJiraGagetUrl(HttpServletRequest request)
    {
        return null;
    }
}
