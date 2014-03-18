package com.atlassian.confluence.extra.jira.transformers;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.transformers.Transformer;
import com.atlassian.confluence.extra.jira.SingleJiraIssuesMapThreadLocal;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.StringSearch;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SingleJiraIssuesToViewTransformer implements Transformer, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesToViewTransformer.class);

    private JiraMacroFinderService jiraMacroFinderService;

    private JiraIssueBatchService jiraIssueBatchService;

    public void setJiraMacroFinderService(JiraMacroFinderService jiraMacroFinderService) {
        this.jiraMacroFinderService = jiraMacroFinderService;
    }

    public void setJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService) {
        this.jiraIssueBatchService = jiraIssueBatchService;
    }

    private <K, V> Map<K, V> copyOf(Map<K, V> map)
    {
        if (map == null)
            return new HashMap<K, V>();
        return new HashMap<K, V>(map);
    }

    @Override
    public String transform(Reader reader, ConversionContext conversionContext) throws XhtmlException {
        String body = "";
        try {
            body = IOUtils.toString(reader);
            StringSearch jiraMarkupSearch = new StringSearch("ac:name=\"jira\"", body);
            if (jiraMarkupSearch.first() == StringSearch.DONE) { // no JIRA markup found
                return body;
            }
            Predicate<MacroDefinition> keyPredicate = new Predicate<MacroDefinition>()
            {
                @Override
                public boolean apply(MacroDefinition macroDefinition)
                {
                    return macroDefinition.getParameters().get("key") != null;
                }
            };
            final Set<MacroDefinition> macroDefinitions = jiraMacroFinderService.findJiraIssueMacros(body, conversionContext, keyPredicate);
            //Map<String, String> macroParameters = copyOf(macroDefinition.getParameters());
            Map<String, String> macroParameters = copyOf(null);
            // we use a Multimap to store the [serverId: set of keys] pairs
            Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();
            for (MacroDefinition macroDefinition : macroDefinitions) {
                jiraServerIdToKeysMap.put(macroDefinition.getParameter("serverId"), macroDefinition.getParameter("key"));
            }
            SingleJiraIssuesMapThreadLocal.flush();

            for (String serverId : jiraServerIdToKeysMap.keySet()) {
                Set<String> keys = (Set<String>) jiraServerIdToKeysMap.get(serverId);
                // make request to the same JIRA server for the whole set of keys and put the individual data of each key into the SingleJiraIssuesMapThreadLocal
                Map<String, String> results = jiraIssueBatchService.getBatchResults(macroParameters, keys);
                SingleJiraIssuesMapThreadLocal.putAll(results);
            }

        } catch (IOException e) {
            log.error(e.toString());
        }
        return body;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SingleJiraIssuesMapThreadLocal.init();
    }

    @Override
    public void destroy() throws Exception {
        SingleJiraIssuesMapThreadLocal.dispose();
    }
}
