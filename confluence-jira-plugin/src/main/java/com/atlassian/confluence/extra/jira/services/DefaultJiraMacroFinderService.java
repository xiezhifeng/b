package com.atlassian.confluence.extra.jira.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

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
     * @param page
     * @param filter
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    @Override
    public Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException
    {
        Predicate<MacroDefinition> jiraPredicate = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals(JiraIssuesMacro.JIRA);
            }

            ;
        };

        if (filter != null)
        {
            jiraPredicate = Predicates.and(filter, jiraPredicate);
        }

        final Predicate<MacroDefinition> jiraMacroPredicate = jiraPredicate;
        final Set<MacroDefinition> definitions = Sets.newHashSet();
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
        xhtmlContent.handleMacroDefinitions(page.getBodyAsString(), new DefaultConversionContext(page.toPageContext()), handler);
        return definitions;
    }

    /**
     * Find all single JIRA issue macros in the body
     *
     * @param body
     * @param context
     * @return the set of MacroDefinition instances represent the macro markups for single JIRA issues
     * @throws XhtmlException
     */
    @Override
    public Set<MacroDefinition> findSingleJiraIssueMacros(String body, ConversionContext context) throws XhtmlException
    {
        Predicate<MacroDefinition> jiraPredicate = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals(JiraIssuesMacro.JIRA);
            }
        };

        Predicate<MacroDefinition> keyPredicate = new Predicate<MacroDefinition>()
        {
            @Override
            public boolean apply(MacroDefinition macroDefinition)
            {
                return macroDefinition.getParameters().get(JiraIssuesMacro.KEY) != null;
            }
        };

        if (keyPredicate != null)
        {
            jiraPredicate = Predicates.and(jiraPredicate, keyPredicate);
        }

        final Predicate<MacroDefinition> jiraMacroPredicate = jiraPredicate;
        final Set<MacroDefinition> definitions = Sets.newHashSet();
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
        xhtmlContent.handleMacroDefinitions(body, context, handler);
        return definitions;
    }
}
