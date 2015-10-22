package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;

import java.util.List;

/**
 * This service responsible for finding all macro definitions in a page or a string
 */
public interface JiraMacroFinderService
{
    /**
     * Find all JIRA Macros in the page satisfying the search filter
     *
     * @param contentEntityObject   the page/blogpost/comment where we want to find the JIRA Issues Macros
     * @param filter the custom search filter for refining the results
     * @return the set of MacroDefinition instances
     * @throws XhtmlException
     */
    List<MacroDefinition> findJiraMacros(ContentEntityObject contentEntityObject, Predicate<MacroDefinition> filter) throws XhtmlException;

}
