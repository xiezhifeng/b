package com.atlassian.confluence.extra.jira.transformers;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.transformers.Transformer;
import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.StringSearch;
import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SingleJiraIssuesToViewTransformer implements Transformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesToViewTransformer.class);

    private static final String SERVER_ID = "serverId";
    private static final String KEY = "key";
    private static final String AC_NAME_JIRA = "ac:name=\"jira\"";

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
            // we search for the presence of the JIRA markup in the body first.
            // If there's none, then we should not proceed
            // We use the ICU4J library's StringSearch class, which implements the Boyer-Moore algorithm
            // for FAST sub-string searching
            StringSearch jiraMarkupSearch = new StringSearch(AC_NAME_JIRA, body);
            if (jiraMarkupSearch.first() == StringSearch.DONE) { // no JIRA markup found
                return body;
            }

            // We find all MacroDefinitions for JIM in the body
            Predicate<MacroDefinition> keyPredicate = new Predicate<MacroDefinition>()
            {
                @Override
                public boolean apply(MacroDefinition macroDefinition)
                {
                    return macroDefinition.getParameters().get(KEY) != null;
                }
            };
            final Set<MacroDefinition> macroDefinitions = jiraMacroFinderService.findJiraIssueMacros(body, conversionContext, keyPredicate);

            // We use a HashMultimap to store the [serverId: set of keys] pairs because duplicate serverId-key pair will not be stored
            Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();

            HashMap<String, Map<String, String>> jiraServerIdToParameters = Maps.newHashMap();
            for (MacroDefinition macroDefinition : macroDefinitions) {
                String serverId = macroDefinition.getParameter(SERVER_ID);
                jiraServerIdToKeysMap.put(serverId, macroDefinition.getParameter(KEY));
                if (jiraServerIdToParameters.get(serverId) == null)
                {
                    jiraServerIdToParameters.put(serverId, copyOf(macroDefinition.getParameters()));
                }
            }
            SingleJiraIssuesThreadLocalAccessor.flush();

            for (String serverId : jiraServerIdToKeysMap.keySet()) {
                Set<String> keys = (Set<String>) jiraServerIdToKeysMap.get(serverId);
                // make request to the same JIRA server for the whole set of keys and putElement the individual data of each key into the SingleJiraIssuesThreadLocalAccessor
                Map<String, String> macroParameters = jiraServerIdToParameters.get(serverId);
                try
                {
                    Map<String, Object> map = jiraIssueBatchService.getBatchResults(macroParameters, keys, conversionContext);
                    Map<String, Element> elementMap = (Map<String, Element>) map.get(JiraIssueBatchService.ELEMENT_MAP);
                    String jiraServerUrl = (String) map.get(JiraIssueBatchService.JIRA_SERVER_URL);
                    SingleJiraIssuesThreadLocalAccessor.putAllElements(serverId, elementMap);
                    SingleJiraIssuesThreadLocalAccessor.putJiraServerUrl(serverId, jiraServerUrl);
                }
                catch (MacroExecutionException e)
                {
                    SingleJiraIssuesThreadLocalAccessor.putException(serverId, e);
                }
            }
        } catch (IOException e) { // this exception should never happen
            LOGGER.error(e.toString());
        }
        return body;
    }
}
