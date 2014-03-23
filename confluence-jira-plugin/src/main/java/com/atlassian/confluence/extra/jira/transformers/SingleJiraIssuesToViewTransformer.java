package com.atlassian.confluence.extra.jira.transformers;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.transformers.Transformer;
import com.atlassian.confluence.extra.jira.SingleJiraIssuesThreadLocalAccessor;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.atlassian.confluence.extra.jira.util.MapUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
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

public class SingleJiraIssuesToViewTransformer implements Transformer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesToViewTransformer.class);

    private static final String SERVER_ID = "serverId";
    private static final String KEY = "key";
    private static final String AC_NAME_JIRA = "ac:name=\"jira\"";
    private static final String AC_NAME_JIRA_ISSUES = "ac:name=\"jiraissues\"";

    private final JiraMacroFinderService jiraMacroFinderService;

    private final JiraIssueBatchService jiraIssueBatchService;

    public SingleJiraIssuesToViewTransformer(JiraMacroFinderService jiraMacroFinderService, JiraIssueBatchService jiraIssueBatchService)
    {
        this.jiraMacroFinderService = jiraMacroFinderService;
        this.jiraIssueBatchService = jiraIssueBatchService;
    }

    @Override
    public String transform(Reader reader, ConversionContext conversionContext) throws XhtmlException
    {
        String body = "";
        try
        {
            body = IOUtils.toString(reader);
            // we search for the presence of the JIRA markup in the body first.
            // If there's none, then we should not proceed
            // We use the ICU4J library's StringSearch class, which implements the Boyer-Moore algorithm
            // for FAST sub-string searching
            StringSearch jiraMarkupSearch = new StringSearch(AC_NAME_JIRA, body);
            if (jiraMarkupSearch.first() == StringSearch.DONE)
            { // no ac:name="jira" found
                StringSearch jiraIssuesMarkupSearch = new StringSearch(AC_NAME_JIRA_ISSUES, body);
                if (jiraIssuesMarkupSearch.first() == StringSearch.DONE) // no ac:name="jiraissue" found
                {
                    return body;
                }
            }

            // We find all MacroDefinitions for single JIRA issues in the body
            final Set<MacroDefinition> macroDefinitions = jiraMacroFinderService.findSingleJiraIssueMacros(body, conversionContext);

            // We use a HashMultimap to store the [serverId: set of keys] pairs because duplicate serverId-key pair will not be stored
            Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();

            HashMap<String, Map<String, String>> jiraServerIdToParameters = Maps.newHashMap();
            for (MacroDefinition macroDefinition : macroDefinitions)
            {
                String serverId = macroDefinition.getParameter(SERVER_ID);
                jiraServerIdToKeysMap.put(serverId, macroDefinition.getParameter(KEY));
                if (jiraServerIdToParameters.get(serverId) == null)
                {
                    jiraServerIdToParameters.put(serverId, MapUtil.copyOf(macroDefinition.getParameters()));
                }
            }
            for (String serverId : jiraServerIdToKeysMap.keySet())
            {
                Set<String> keys = (Set<String>) jiraServerIdToKeysMap.get(serverId);
                // make request to the same JIRA server for the whole set of keys and putElement the individual data of each key into the SingleJiraIssuesThreadLocalAccessor
                JiraBatchRequestData jiraBatchRequestData = new JiraBatchRequestData();
                try
                {
                    Map<String, Object> resultsMap = jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
                    if (resultsMap != null)
                    {
                        Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                        String jiraServerUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);
                        // Store the results to TheadLocal maps for later use
                        jiraBatchRequestData.setElementMap(elementMap);
                        jiraBatchRequestData.setServerUrl(jiraServerUrl);
                    }
                }
                catch (MacroExecutionException macroExecutionException)
                {
                    jiraBatchRequestData.setException(macroExecutionException);
                }
                catch (UnsupportedJiraServerException unsupportedJiraServerException)
                {
                    jiraBatchRequestData.setException(unsupportedJiraServerException);
                }
                finally
                {
                    SingleJiraIssuesThreadLocalAccessor.putJiraBatchRequestData(serverId, jiraBatchRequestData);
                }
            }
        }
        catch (IOException e)
        {
            // this exception should never happen
            LOGGER.error(e.toString());
        }
        return body;
    }
}
