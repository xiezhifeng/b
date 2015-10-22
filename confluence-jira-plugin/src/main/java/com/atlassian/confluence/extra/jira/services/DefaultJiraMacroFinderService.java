package com.atlassian.confluence.extra.jira.services;

import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import java.util.List;

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

}
