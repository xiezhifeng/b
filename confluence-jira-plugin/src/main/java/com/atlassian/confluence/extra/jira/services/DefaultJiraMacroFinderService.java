package com.atlassian.confluence.extra.jira.services;

import java.util.Set;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

public class DefaultJiraMacroFinderService implements JiraMacroFinderService
{

    private final XhtmlContent xhtmlContent;

    private static final String JIRA = "jira";
    private static final String KEY = "key";

    public DefaultJiraMacroFinderService(XhtmlContent xhtmlContent)
    {
        this.xhtmlContent = xhtmlContent;
    }

    /**
     * Find all JiraIssueMacro definitions in the page
     * @param page
     * @param filter
     * @return
     * @throws XhtmlException
     */
    @Override
    public Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter)
            throws XhtmlException
    {
        Predicate<MacroDefinition> pred = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals(JIRA);
            };
        };

        if (filter != null)
        {
            pred = Predicates.and(filter, pred);
        }

        final Predicate<MacroDefinition> jiraMacroPredicate = pred;
        final Set<MacroDefinition> definitions = Sets.newHashSet();
        MacroDefinitionHandler handler = new MacroDefinitionHandler()
        {
            @Override
            public void handle(MacroDefinition macroDefinition)
            {
                if (jiraMacroPredicate.apply(macroDefinition))
                    definitions.add(macroDefinition);
            }
        };
        xhtmlContent.handleMacroDefinitions(page.getBodyAsString(), new DefaultConversionContext(page.toPageContext()),
                handler);
        return definitions;
    }

    /**
     * Find all JiraIssueMacro definitions in the page body
     * @param body
     * @param context
     * @return
     * @throws XhtmlException
     */
    @Override
    public Set<MacroDefinition> findSingleJiraIssueMacros(String body, ConversionContext context)
            throws XhtmlException
    {
        Predicate<MacroDefinition> jiraPredicate = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals(JIRA);
            }
        };

        Predicate<MacroDefinition> keyPredicate = new Predicate<MacroDefinition>()
        {
            @Override
            public boolean apply(MacroDefinition macroDefinition)
            {
                return macroDefinition.getParameters().get(KEY) != null;
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
                    definitions.add(macroDefinition);
            }
        };
        xhtmlContent.handleMacroDefinitions(body, context, handler);
        return definitions;
    }
}
