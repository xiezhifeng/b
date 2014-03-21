package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;

import java.util.Set;

public interface JiraMacroFinderService
{
    /**
     * Find all JIRA Issue Macros in the page satisfying the search filter
     *
     * @param page
     * @param filter
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException;

    /**
     * Find all single JIRA issue macros in the body
     *
     * @param body
     * @param context
     * @return the set of MacroDefinition instances represent the macro markups for single JIRA issues
     * @throws XhtmlException
     */
    Set<MacroDefinition> findSingleJiraIssueMacros(String body, ConversionContext context) throws XhtmlException;
}
