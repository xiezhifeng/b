package com.atlassian.confluence.extra.jira.services;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DefaultJiraMacroFinderService implements JiraMacroFinderService
{
    
    private final XhtmlContent xhtmlContent;
    
    public DefaultJiraMacroFinderService(XhtmlContent xhtmlContent)
    {
        this.xhtmlContent = xhtmlContent;
    }
    
    @Override
    public Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException
    {
        final Predicate<MacroDefinition> pred = Predicates.and(filter, new Predicate<MacroDefinition>()
                {
                    public boolean apply(MacroDefinition definition) 
                    {
                        return definition.getName().equals("jira");
                    };
                });
        final Set<MacroDefinition> definitions= Sets.newHashSet();
        MacroDefinitionHandler handler = new MacroDefinitionHandler()
        {
            @Override
            public void handle(MacroDefinition macroDefinition)
            {
                if(pred.apply(macroDefinition))
                    definitions.add(macroDefinition);
            }
        };
        xhtmlContent.handleMacroDefinitions(page.getBodyAsString(), new DefaultConversionContext(page.toPageContext()), handler);
        return definitions;
    }
}
