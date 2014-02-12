package com.atlassian.confluence.extra.jira;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DefaultJiraIssuesColumnManager implements JiraIssuesColumnManager
{
    private static final String REST_URL_FIELD_INFO = "/rest/api/2/field";
    private static final String PROP_KEY_PREFIX = "jiraissues.column.";

    private Cache<ApplicationLink, Map<String, JiraColumnInfo>> jiraColumnsCache;

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
    public Map<String, JiraColumnInfo> getColumnsInfoFromJira(ApplicationLink appLink)
    {
        return getInternalColumnInfo().getUnchecked(appLink);
    }

    private Cache<ApplicationLink, Map<String, JiraColumnInfo>> getInternalColumnInfo()
    {
        if (jiraColumnsCache == null)
        {
            jiraColumnsCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ApplicationLink, Map<String, JiraColumnInfo>>()
                    {
                        @Override
                        public Map<String, JiraColumnInfo>load(ApplicationLink appLink) throws Exception
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
    public List<JiraColumnInfo> getColumnInfo(final Map<String, String> params, final Map<String, JiraColumnInfo> columns, final ApplicationLink applink)
    {
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params,"columns", JiraUtil.PARAM_POSITION_1));
        List<JiraColumnInfo> info = new ArrayList<JiraColumnInfo>();
        JiraServerBean jiraServer = jiraConnectorManager.getJiraServer(applink);
        boolean isJiraSupported = JiraIssueSortableHelper.isJiraSupportedOrder(jiraServer);

        for (String columnName : columnNames)
        {
            String key = getCanonicalFormOfBuiltInField(columnName);

            if (isJiraSupported)
            {
                JiraColumnInfo jiraColumnInfo = getJiraColumnInfo(getColumnMapping(columnName), columns);

                if (jiraColumnInfo != null)
                {
                    // Based on field has clause name and navigable to determine whether columns is sortable.
                    List<String> clauseNames = jiraColumnInfo.getClauseNames();
                    boolean isSortable = clauseNames != null && !clauseNames.isEmpty() && jiraColumnInfo.isNavigable();
                    info.add(new JiraColumnInfo(key, getDisplayName(key, columnName), clauseNames, isSortable));
                }
            }
            else
            {
                // Based on field is a clause name and a navigable to determine whether column is sortable. Otherwise based on support sorting columns.
                JiraColumnInfo jiraColumnInfo  = getJiraColumnInfo(key, columns);
                boolean isNavigable = jiraColumnInfo != null ? jiraColumnInfo.isNavigable() : false;
                boolean isCustomField = jiraColumnInfo != null ? jiraColumnInfo.isCustom() : false;
                boolean isSortable = (isCustomField && isNavigable) || JiraIssuesColumnManager.SUPPORT_SORTABLE_COLUMN_NAMES.contains(key);
                info.add(new JiraColumnInfo(key, getDisplayName(key, columnName), Arrays.asList(key), isSortable)); 
            }
        }
        return info;
    }

    private String getDisplayName(final String key, final String columnName)
    {
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
    public String getColumnMapping(String columnKey)
    {
        String key = COLUMN_KEYS_MAPPING.get(columnKey);
        return StringUtils.isNotBlank(key) ? key : columnKey;
    }
}
