package com.atlassian.confluence.extra.jira.util;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.plugins.sprint.JiraSprintMacro;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public class JiraIssuePredicates
{
    public final static Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\s*([A-Z][A-Z]+)-[0-9]+\\s*");

    public static Predicate<MacroDefinition> isJiraIssueMacro = new Predicate<MacroDefinition>()
    {
        @Override
        public boolean apply(MacroDefinition macroDefinition)
        {
            return StringUtils.equals(macroDefinition.getName(), JiraIssuesMacro.JIRA)
                    || StringUtils.equals(macroDefinition.getName(), JiraIssuesMacro.JIRAISSUES);
        }
    };

    public static Predicate<MacroDefinition> isSprintMacro = new Predicate<MacroDefinition>()
    {
        @Override
        public boolean apply(MacroDefinition macroDefinition)
        {
            return StringUtils.equals(macroDefinition.getName(), JiraSprintMacro.JIRASPRINT);
        }
    };

    public static Predicate<MacroDefinition> isSingleIssue = Predicates.and(isJiraIssueMacro, new Predicate<MacroDefinition>()
    {
        @Override
        public boolean apply(MacroDefinition macroDefinition)
        {
            String defaultParam = macroDefinition.getDefaultParameterValue();
            java.util.Map<String, String> parameters = macroDefinition.getParameters();
            return (defaultParam != null && ISSUE_KEY_PATTERN.matcher(defaultParam).matches()) ||
                    (parameters != null && parameters.get("key") != null);
        }
    });


}
