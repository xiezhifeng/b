package com.atlassian.conflunce.plugins.jiracharts;

import java.util.Map;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

public class JiraChartMacro implements Macro {

    @Override
    public String execute(Map<String, String> arg0, String arg1,
            ConversionContext arg2) throws MacroExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BodyType getBodyType() {
        // TODO Auto-generated method stub
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        // TODO Auto-generated method stub
        return OutputType.BLOCK;
    }

}
