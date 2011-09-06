package com.atlassian.confluence.extra.jira;

import java.util.Map;
import java.util.Set;



import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.xhtml.MacroMigration;
import com.atlassian.confluence.xhtml.api.MacroDefinition;

public class JiraIssuesMacroMigrator implements MacroMigration
{

    public MacroDefinition migrate(MacroDefinition macro, ConversionContext context)
    {
        
        // convert default url parameters into explicit url parameters to avoid 
        // confusion when there is JQL query with an '=' character (see https://jira.atlassian.com/browse/CONFDEV-5886)
        Map<String, String> parameters = macro.getParameters();
        if (parameters != null)
        {
            Set<String> keySet = parameters.keySet();
            for (String key : keySet)
            {
                if (key.startsWith("http://") || key.startsWith("https://"))
                {
                    String val = parameters.remove(key);
                    String url = key + '=' + val;
                    parameters.put("url", url);
                    break;
                }
            }
        }
        return macro;
    }

}
