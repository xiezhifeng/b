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

/**
 * This transformer is used to identify all Single Jira Issues Macro markups and send batch request to JIRA servers to
 * get the results, represented by JiraBatchRequestData objects (each of them contains response from a JIRA server).
 * Once the JiraBatchRequestData objects are built, they will be stored in a ThreadLocal map, which can by accesed by
 * the SingleJiraIssuesThreadLocalAccessor.
 */
public class SingleJiraIssuesToViewTransformer implements Transformer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesToViewTransformer.class);

    private static final String SERVER_ID = "serverId";
    private static final String KEY = "key";
    private static final int MIN_SINGLE_ISSUES_ALLOWED = 5;

    private final JiraMacroFinderService jiraMacroFinderService;

    private final JiraIssueBatchService jiraIssueBatchService;

    /**
     * Constructor
     * @param jiraMacroFinderService responsible for finding all JIRA Issues Macro markups in a string or page
     * @param jiraIssueBatchService responsible for sending a batch request to a JIRA server and retrieve the content
     *                              from the response
     */
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

            // We find all MacroDefinitions for single JIRA issues in the body
            final Set<MacroDefinition> macroDefinitions = jiraMacroFinderService.findSingleJiraIssueMacros(body, conversionContext);

            // If the number of macro definitions is less than MIN_SINGLE_ISSUES_ALLOWED, we stop immediately because it's not worth to do
            // additional work for small results
            if (macroDefinitions.size() < MIN_SINGLE_ISSUES_ALLOWED)
            {
                return body;
            }
            SingleJiraIssuesThreadLocalAccessor.setBatchProcessed(Boolean.TRUE); // Single JIRA issues will be processed in batch
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
