package com.atlassian.confluence.extra.jira;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;



import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.xhtml.MacroMigration;
import com.atlassian.confluence.xhtml.api.MacroDefinition;

public class JiraIssuesMacroMigrator implements MacroMigration
{

    public MacroDefinition migrate(MacroDefinition macro, ConversionContext context)
    {
        String defaultParam = macro.getDefaultParameterValue();
        
        if (defaultParam !=  null)
        {
            // convert default url parameters into explicit url parameters to avoid 
            // confusion when there is JQL query with an '=' character (see https://jira.atlassian.com/browse/CONFDEV-5886)
            try
            {
                URL url = new URL(defaultParam);
                macro.setDefaultParameterValue(null);
                Map<String, String> params = macro.getParameters();
                if (params == null)
                {
                    params = new HashMap<String, String>();
                }
                params.put("url", defaultParam);
                macro.setParameters(params);
            }
            catch(MalformedURLException e)
            {
                //ignore and assume that it isn't a url so we have nothing to do
            }
        }
        return macro;
    }

}
