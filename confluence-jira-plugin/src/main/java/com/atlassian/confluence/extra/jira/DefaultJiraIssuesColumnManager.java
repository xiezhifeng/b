package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.sal.api.net.Request.MethodType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DefaultJiraIssuesColumnManager implements JiraIssuesColumnManager
{
    private static final String REST_URL_FIELD_INFO = "/rest/api/2/field";
    private static final String PROP_KEY_PREFIX = "jiraissues.column.";

    public static final String COLUMN_EPIC_LINK = "epic link";
    public static final String COLUMN_EPIC_LINK_DISPLAY = "epic link display";
    public static final String COLUMN_EPIC_NAME = "epic name";
    public static final String COLUMN_EPIC_STATUS = "epic status";
    public static final String COLUMN_EPIC_COLOUR = "epic colour";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_SUMMARY = "summary";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_RESOLUTION = "resolution";
    public static final String COLUMN_ISSUE_LINKS = "issuelinks";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ENVIRONMENT = "environment";

    private LoadingCache<ReadOnlyApplicationLink, Map<String, JiraColumnInfo>> jiraColumnsCache;

    private final JiraIssuesSettingsManager jiraIssuesSettingsManager;
    private final LocaleManager localeManager;
    private final I18NBeanFactory i18nBeanFactory;
    private final JiraConnectorManager jiraConnectorManager;

    public DefaultJiraIssuesColumnManager(JiraIssuesSettingsManager jiraIssuesSettingsManager, LocaleManager localeManager, I18NBeanFactory i18nBeanFactory, JiraConnectorManager jiraConnectorManager)
    {
        this.jiraIssuesSettingsManager = jiraIssuesSettingsManager;
        this.localeManager = localeManager;
        this.i18nBeanFactory = i18nBeanFactory;
        this.jiraConnectorManager = jiraConnectorManager;
    }

    public I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18nBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18nBeanFactory.getI18NBean();
    }

    public Map<String, String> getColumnMap(String jiraIssuesUrl)
    {
        return jiraIssuesSettingsManager.getColumnMap(jiraIssuesUrl);
    }

    public void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMapping)
    {
        jiraIssuesSettingsManager.setColumnMap(jiraIssuesUrl, columnMapping);
    }

    public boolean isColumnBuiltIn(String columnName)
    {
       return ALL_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());
    }

    public String getCanonicalFormOfBuiltInField(String columnName)
    {
        if (columnName.equalsIgnoreCase("fixversion"))
        {
            return "fixVersion";
        }
        if (columnName.equalsIgnoreCase("fixversions"))
        {
            return "fixVersion";
        }
        if (columnName.equalsIgnoreCase("versions"))
        {
            return "version";
        }
        if (columnName.equalsIgnoreCase("components"))
        {
            return "component";
        }
        if (columnName.equalsIgnoreCase("resolutiondate"))
        {
            return "resolved";
        }
        if (isColumnBuiltIn(columnName))
        {
            return columnName.toLowerCase();
        }
        return columnName;
    }

    public boolean isBuiltInColumnMultivalue(String columnName)
    {
        return ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());
    }

    @Override
    public Map<String, JiraColumnInfo> getColumnsInfoFromJira(ReadOnlyApplicationLink appLink)
    {
        // appLink can be null, it should be checked before calling getUnchecked() on the Cache instance
        return (appLink != null) ? getInternalColumnInfo().getUnchecked(appLink) : Collections.<String, JiraColumnInfo>emptyMap();
    }

    private LoadingCache<ReadOnlyApplicationLink, Map<String, JiraColumnInfo>> getInternalColumnInfo()
    {
        if (jiraColumnsCache == null)
        {
            jiraColumnsCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ReadOnlyApplicationLink, Map<String, JiraColumnInfo>>()
                    {
                        @Override
                        public Map<String, JiraColumnInfo>load(ReadOnlyApplicationLink appLink) throws Exception
                        {
                            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, MethodType.GET, REST_URL_FIELD_INFO);
                            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                            String json = request.execute();

                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<JiraColumnInfo>>() {}.getType();
                            List<JiraColumnInfo> columns = gson.fromJson(json, listType);

                            Map<String, JiraColumnInfo> jiraColumns = new HashMap<String, JiraColumnInfo>();
                            for (JiraColumnInfo column : columns)
                            {
                                jiraColumns.put(column.getKey(), column);
                            }
                            return jiraColumns;
                        }
                    });
        }
        return jiraColumnsCache;
    }

    @Override
    public List<JiraColumnInfo> getColumnInfo(final Map<String, String> params, final Map<String, JiraColumnInfo> columns, final ReadOnlyApplicationLink applink)
    {
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params,"columns", JiraUtil.PARAM_POSITION_1),ImmutableMap.of());
        List<JiraColumnInfo> info = new ArrayList<>();
        JiraServerBean jiraServer = jiraConnectorManager.getJiraServer(applink);
        boolean isJiraSupported = JiraIssueSortableHelper.isJiraSupportedOrder(jiraServer);

        for (String columnName : columnNames)
        {
            String key = getCanonicalFormOfBuiltInField(columnName);

            JiraColumnInfo jiraColumnInfo = getJiraColumnInfo(getColumnMapping(columnName, XML_COLUMN_KEYS_MAPPING), columns);

            List<String> clauseNames = Arrays.asList(key);

            JiraColumnInfo.Schema schema = null;

            boolean isSortable = false;

            if(jiraColumnInfo != null)
            {
                if (isJiraSupported)
                {
                    // Based on field has clause name and navigable to determine whether columns is sortable.
                    clauseNames = jiraColumnInfo.getClauseNames();
                    isSortable = clauseNames != null && !clauseNames.isEmpty() && jiraColumnInfo.isNavigable();
                }
                else
                {
                    // Based on field is a clause name and a navigable to determine whether column is sortable. Otherwise based on support sorting columns.
                    isSortable = (jiraColumnInfo.isCustom() && jiraColumnInfo.isNavigable()) || JiraIssuesColumnManager.SUPPORT_SORTABLE_COLUMN_NAMES.contains(key);
                }
                schema = jiraColumnInfo.getSchema();
            }
            info.add(new JiraColumnInfo(key, getDisplayName(key, columnName), clauseNames, isSortable, schema));
        }
        return info;
    }

    private String getDisplayName(final String key, final String columnName)
    {
        if (key.contains(JiraIssueSortableHelper.SINGLE_QUOTE) || columnName.contains(JiraIssueSortableHelper.SINGLE_QUOTE))
        {
            return columnName;
        }
        String i18nKey = PROP_KEY_PREFIX + key;
        String displayName = getI18NBean().getText(i18nKey);

        if (StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
        {
            displayName = columnName;
        }
        return displayName;
    }

    private JiraColumnInfo getJiraColumnInfo(final String columnName, final Map<String, JiraColumnInfo> columns)
    {
        if (columns == null || StringUtils.isBlank(columnName))
            return null;

        for (JiraColumnInfo jiraColumn : columns.values())
        {
            if (jiraColumn.getTitle().equalsIgnoreCase(columnName) || jiraColumn.getKey().equalsIgnoreCase(columnName))
            {
                return jiraColumn;
            }
        }
        return null;
    }

    @Override
    public String getColumnMapping(String columnKey, Map<String, String> map)
    {
        String key = map.get(columnKey);
        return StringUtils.isNotBlank(key) ? key : columnKey;
    }

    /**
     * Checks a column name derived from applink request is equal to the column name translated by i18n.
     *
     * @param keyForColumnNameToMatch A key which references an i18n translation of column names. This acts as a
     *                                static column name after the initialisation function has run.
     * @param column The name of the column returned by the Applink request.
     * @param oneWayMatch Instead of checking that column equals keyForColumnNameToMatch, checking that
     *                    keyForColumnNameToMatch contains column only.
     * @return
     */
    public static boolean matchColumnNameFromString(String keyForColumnNameToMatch, String column, ImmutableMap<String, ImmutableSet<String>> i18nColumnNames, boolean oneWayMatch) {
        Set<String> columnNamesToMatch = i18nColumnNames.get(keyForColumnNameToMatch);
        for (String columnName : columnNamesToMatch){
            if (oneWayMatch) {
                if(columnName.contains(column)){
                    return true;
                } else if (column.equals(columnName)){
                    return true;
                }
            } else if(columnName.contains(column)){
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks a column name derived from applink request is equal to the column name translated by i18n.
     *
     * @param keyForColumnNameToMatch A key which references an i18n translation of column names. This acts as a
     *                                static column name after the initialisation function has run.
     * @param column The name of the column returned by the Applink request.
     * @return
     */
    public static boolean matchColumnNameFromString(String keyForColumnNameToMatch, String column, ImmutableMap<String, ImmutableSet<String>> i18nColumnNames) {
        return matchColumnNameFromString(keyForColumnNameToMatch, column, i18nColumnNames, false);
    }

    /**
     * Checks to see if a list of column names contains a particular column, which is retrieved from i18n.
     *
     * @param keyForColumnNameToMatch A key which references an i18n translation of column names. This acts as a
     *                                static column name after the initialisation function has run.
     * @param columnNames The name of the column returned by the Applink request.
     * @return
     */
    public static boolean matchColumnNameFromList(String keyForColumnNameToMatch, List<String> columnNames, ImmutableMap<String, ImmutableSet<String>> i18nColumnNames) {
        for (String column : columnNames){
            if (matchColumnNameFromString(keyForColumnNameToMatch, column, i18nColumnNames)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the tranlations for the Jira Issues columns which are specially handled and returns a map of them.
     * Some of these (ex. Summary) cannot be altered in Jira currently, but have included them here incase that changes.
     * @return
     */
    public ImmutableMap getI18nColumnNames() {
        I18NBean i18nBean =  getI18NBean();
        ImmutableMap.Builder<String, ImmutableSet<String>> i18nColumnNamesBuilder = ImmutableMap.builder();

        ImmutableSet.Builder<String> columnEpicLink = ImmutableSet.builder();
        columnEpicLink.add(i18nBean.getText("jiraissue.column.epics.link.upper"));
        columnEpicLink.add(i18nBean.getText("jiraissue.column.epics.link.lower"));
        columnEpicLink.add("Epic Link");
        columnEpicLink.add("epic link");
        i18nColumnNamesBuilder.put(COLUMN_EPIC_LINK, columnEpicLink.build());

        // issue/CONF-31534 Used to get rid of Epic Name column. See JiraIssueSortableHelper
        ImmutableSet.Builder<String> columnEpicLinkDisplay = ImmutableSet.builder();
        columnEpicLink.add(i18nBean.getText("jiraissue.column.epics.link.lower"));
        i18nColumnNamesBuilder.put(COLUMN_EPIC_LINK_DISPLAY, columnEpicLinkDisplay.build());

        ImmutableSet.Builder<String> columnEpicName = ImmutableSet.builder();
        columnEpicName.add(i18nBean.getText("jiraissue.column.epics.name.upper"));
        columnEpicName.add(i18nBean.getText("jiraissue.column.epics.name.lower"));
        columnEpicName.add("Epic Name");
        columnEpicName.add("epic name");
        i18nColumnNamesBuilder.put(COLUMN_EPIC_NAME, columnEpicName.build());

        ImmutableSet.Builder<String> columnEpicColour = ImmutableSet.builder();
        columnEpicColour.add(i18nBean.getText("jiraissue.column.epics.colour.upper"));
        columnEpicColour.add(i18nBean.getText("jiraissue.column.epics.colour.lower"));
        columnEpicColour.add("Epic Colour");
        columnEpicColour.add("Epic Color");
        columnEpicColour.add("epic colour");
        columnEpicColour.add("epic color");
        i18nColumnNamesBuilder.put(COLUMN_EPIC_COLOUR, columnEpicColour.build());

        ImmutableSet.Builder<String> columnEpicStatus = ImmutableSet.builder();
        columnEpicStatus.add(i18nBean.getText("jiraissue.column.epics.status.upper"));
        columnEpicStatus.add(i18nBean.getText("jiraissue.column.epics.status.lower"));
        columnEpicStatus.add("Epic Status");
        columnEpicStatus.add("epic status");
        i18nColumnNamesBuilder.put(COLUMN_EPIC_STATUS, columnEpicStatus.build());

        ImmutableSet.Builder<String> columnType = ImmutableSet.builder();
        columnType.add(i18nBean.getText("jiraissue.column.type"));
        i18nColumnNamesBuilder.put(COLUMN_TYPE, columnType.build());
        ImmutableSet.Builder<String> columnKey = ImmutableSet.builder();
        columnKey.add(i18nBean.getText("jiraissue.column.key"));
        i18nColumnNamesBuilder.put(COLUMN_KEY, columnKey.build());
        ImmutableSet.Builder<String> columnSummary = ImmutableSet.builder();
        columnSummary.add(i18nBean.getText("jiraissue.column.summary"));
        i18nColumnNamesBuilder.put(COLUMN_SUMMARY, columnSummary.build());
        ImmutableSet.Builder<String> columnPriority = ImmutableSet.builder();
        columnPriority.add(i18nBean.getText("jiraissue.column.priority"));
        i18nColumnNamesBuilder.put(COLUMN_PRIORITY, columnPriority.build());
        ImmutableSet.Builder<String> columnStatus = ImmutableSet.builder();
        columnStatus.add(i18nBean.getText("jiraissue.column.status"));
        i18nColumnNamesBuilder.put(COLUMN_STATUS, columnStatus.build());
        ImmutableSet.Builder<String> columnResolution = ImmutableSet.builder();
        columnResolution.add(i18nBean.getText("jiraissue.column.resolution"));
        i18nColumnNamesBuilder.put(COLUMN_RESOLUTION, columnResolution.build());
        ImmutableSet.Builder<String> columnIssuelinks = ImmutableSet.builder();
        columnIssuelinks.add(i18nBean.getText("jiraissue.column.issuelinks"));
        i18nColumnNamesBuilder.put(COLUMN_ISSUE_LINKS, columnIssuelinks.build());
        ImmutableSet.Builder<String> columnDescription = ImmutableSet.builder();
        columnDescription.add(i18nBean.getText("jiraissue.column.description"));
        i18nColumnNamesBuilder.put(COLUMN_DESCRIPTION, columnDescription.build());
        ImmutableSet.Builder<String> columnEnvironment = ImmutableSet.builder();
        columnEnvironment.add(i18nBean.getText("jiraissue.column.environment"));
        i18nColumnNamesBuilder.put(COLUMN_ENVIRONMENT, columnEnvironment.build());

        return i18nColumnNamesBuilder.build();
    }
}
