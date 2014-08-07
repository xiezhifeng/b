package com.atlassian.confluence.extra.jira.util;

import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;

import java.util.regex.Pattern;

public class JiraIssuePredicates
{
    private final static Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\s*([A-Z][A-Z]+)-[0-9]+\\s*");

    public static Predicate<MacroDefinition> isSingleIssue = new Predicate<MacroDefinition>()
        {
            @Override
            public boolean apply(MacroDefinition macroDefinition)
            {
                    String defaultParam = macroDefinition.getDefaultParameterValue();
                    java.util.Map<String, String> parameters = macroDefinition.getParameters();
                    return (defaultParam != null && ISSUE_KEY_PATTERN.matcher(defaultParam).matches()) ||
                           (parameters != null && parameters.get("key") != null);
            }
        };
}
