package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;

import java.util.List;
import java.util.Set;

/**
 * This service responsible for finding all macro definitions in a page or a string
 */
public interface JiraMacroFinderService
{
    /**
     * Find all JIRA Issue Macros in the page satisfying the search filter
     *
     * @param page the page where we want to find the JIRA issues macro
     * @param filter a custom search filter for refining the results
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException;

    Set<MacroDefinition> findJiraSprintMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException;

    /**
     * Find all single JIRA issue macros in the body string
     *
     * @param body the content where we want to find the single JIRA issues macros
     * @param conversionContext the associated Conversion Context
     * @return the list of MacroDefinition instances represent the macro markups for single JIRA issues
     * @throws XhtmlException
     */
    List<MacroDefinition> findSingleJiraIssueMacros(String body, ConversionContext conversionContext) throws XhtmlException;
}
