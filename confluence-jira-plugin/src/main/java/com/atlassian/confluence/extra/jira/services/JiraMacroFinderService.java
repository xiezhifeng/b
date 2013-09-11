package com.atlassian.confluence.extra.jira.services;

import java.util.Set;

import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;

public interface JiraMacroFinderService
{
    Set<MacroDefinition> findJiraIssueMacros(AbstractPage page, Predicate<MacroDefinition> filter) throws XhtmlException;
}
