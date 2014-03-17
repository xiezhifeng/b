package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.transformers.Transformer;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

public class SingleJiraIssuesToViewTransformer implements Transformer {
    private XhtmlContent xhtmlContent;

    public void setXhtmlContent(XhtmlContent xhtmlContent) {
        this.xhtmlContent = xhtmlContent;
    }

    @Override
    public String transform(Reader reader, ConversionContext conversionContext) throws XhtmlException {

        Predicate<MacroDefinition> jiraPredicate = new Predicate<MacroDefinition>()
        {
            public boolean apply(MacroDefinition definition)
            {
                return definition.getName().equals("jira");
            };
        };

        Predicate<MacroDefinition> keyPredicate = new Predicate<MacroDefinition>()
        {
            @Override
            public boolean apply(MacroDefinition def)
            {
                return def.getParameters().get("key") != null;
            }
        };

        final Predicate<MacroDefinition> singleJiraIssueMacroPredicate = Predicates.and(jiraPredicate, keyPredicate);
        final Set<MacroDefinition> definitions = Sets.newHashSet();
        MacroDefinitionHandler handler = new MacroDefinitionHandler()
        {
            @Override
            public void handle(MacroDefinition macroDefinition)
            {
                if (singleJiraIssueMacroPredicate.apply(macroDefinition))
                    definitions.add(macroDefinition);
            }
        };

        try {
            xhtmlContent.handleMacroDefinitions(IOUtils.toString(reader), conversionContext,
                    handler);
            SingleJiraIssuesMapThreadLocal.init();
            Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();
            for (MacroDefinition macroDefinition : definitions) {
                SingleJiraIssuesMapThreadLocal.put(macroDefinition.getParameter("serverId"), macroDefinition.getParameter("key"));
            }
        } catch (IOException e) {
            return "";
        }

        return null;
    }
}
