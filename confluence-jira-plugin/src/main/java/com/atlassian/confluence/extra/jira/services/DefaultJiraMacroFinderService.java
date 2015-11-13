package com.atlassian.confluence.extra.jira.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

public class DefaultJiraMacroFinderService implements JiraMacroFinderService
{
    private final XhtmlContent xhtmlContent;

    public DefaultJiraMacroFinderService(XhtmlContent xhtmlContent)
    {
        this.xhtmlContent = xhtmlContent;
    }

    /**
     * Find all JIRA Issue Macros in the page satisfying the search filter
     *
     * @param page   the page where we want to find the JIRA Issues Macros
     * @param filter the custom search filter for refining the results
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    @Override
    public Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException
    {
        Predicate jiraPredicate = JiraIssuePredicates.isJiraIssueMacro;
        if (filter != null)
        {
            jiraPredicate = Predicates.and(jiraPredicate, filter);
        }
        return Sets.newHashSet(findJiraMacros(page, jiraPredicate));
    }

    /**
     * Find all JIRA Issue Macros in the page satisfying the search filter
     *
     * @param contentEntityObject   the page/blogpost/comment where we want to find the JIRA Issues Macros
     * @param filter the custom search filter for refining the results
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    @Override
    public List<MacroDefinition> findJiraMacros(ContentEntityObject contentEntityObject, Predicate<MacroDefinition> filter) throws XhtmlException
    {
        Predicate<MacroDefinition> jiraPredicate = Predicates.or(JiraIssuePredicates.isJiraIssueMacro, JiraIssuePredicates.isSprintMacro);

        if (filter != null)
        {
            jiraPredicate = Predicates.and(filter, jiraPredicate);
        }

        final Predicate<MacroDefinition> jiraMacroPredicate = jiraPredicate;
        final List<MacroDefinition> definitions = Lists.newArrayList();
        MacroDefinitionHandler handler = new MacroDefinitionHandler()
        {
            @Override
            public void handle(MacroDefinition macroDefinition)
            {
                if (jiraMacroPredicate.apply(macroDefinition))
                {
                    definitions.add(macroDefinition);
                }
            }
        };
        xhtmlContent.handleMacroDefinitions(contentEntityObject.getBodyAsString(), new DefaultConversionContext(contentEntityObject.toPageContext()), handler);
        return definitions;
    }

    /**
     * Find all single JIRA issue macros in the body
     *
     * @param body              the content where we want to find the single JIRA issues macros
     * @param conversionContext the associated Conversion Context
     * @return the set of MacroDefinition instances represent the macro markups for single JIRA issues
     * @throws XhtmlException
     */
    @Override
    public List<MacroDefinition> findSingleJiraIssueMacros(String body, ConversionContext conversionContext) throws XhtmlException
    {
        final SingleJiraIssuePredicate singleJiraIssuePredicate = new SingleJiraIssuePredicate();
        final List<MacroDefinition> definitions = Lists.newArrayList();
        MacroDefinitionHandler handler = new MacroDefinitionHandler()
        {
            @Override
            public void handle(MacroDefinition macroDefinition)
            {
                if (singleJiraIssuePredicate.apply(macroDefinition))
                {
                    macroDefinition.setParameter(JiraIssuesMacro.KEY, singleJiraIssuePredicate.getIssueKey());
                    definitions.add(macroDefinition);
                }
            }
        };
        xhtmlContent.handleMacroDefinitions(body, conversionContext, handler);
        return definitions;
    }


    private class SingleJiraIssuePredicate implements Predicate<MacroDefinition>
    {

        private String issueKey;

        @Override
        public boolean apply(MacroDefinition definition)
        {
            boolean isJiraIssue = definition.getName().equals(JiraIssuesMacro.JIRA) || definition.getName().equals(JiraIssuesMacro.JIRAISSUES);
            if (!isJiraIssue)
            {
                return false;
            }

            this.issueKey = JiraUtil.getSingleIssueKey(definition.getParameters());
            if (this.issueKey != null)
            {
                return true;
            }
            return false;
        }

        String getIssueKey()
        {
            return issueKey;
        }
    }
}
