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

    public DefaultJiraMacroFinderService(XhtmlContent xhtmlContent)
    {
        this.xhtmlContent = xhtmlContent;
    }

    @Override
    public Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter)
            throws XhtmlException
    {
        Predicate<MacroDefinition> pred = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals("jira");
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

    @Override
    public Set<MacroDefinition> findJiraIssueMacros(String body, ConversionContext context, Predicate<MacroDefinition> filter)
            throws XhtmlException
    {
        Predicate<MacroDefinition> pred = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals("jira");
            };
        };

        if (filter != null)
        {
            pred = Predicates.and(pred, filter);
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
        xhtmlContent.handleMacroDefinitions(body, context, handler);
        return definitions;
    }
}
