package com.atlassian.confluence.extra.jira.util;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.plugins.sprint.JiraSprintMacro;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
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
            Map<String, String> parameters = macroDefinition.getParameters();
            String issueKey = JiraUtil.getSingleIssueKey(parameters);
            if (StringUtils.isNotEmpty(issueKey))
            {
                macroDefinition.setParameter(JiraIssuesMacro.KEY, issueKey);
                return true;
            }
            return false;
        }
    });

    public static Predicate<MacroDefinition> isTableIssue = Predicates.and(Predicates.not(isSingleIssue), new Predicate<MacroDefinition>()
    {
        @Override
        public boolean apply(MacroDefinition macroDefinition)
        {
            Map<String, String> parameters = macroDefinition.getParameters();
            return StringUtils.isEmpty(parameters.get("count"));
        }
    });

}
