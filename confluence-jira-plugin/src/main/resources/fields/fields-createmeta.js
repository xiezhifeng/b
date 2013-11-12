
var jiraIntegration = window.jiraIntegration || {};

jiraIntegration.createmeta = (function($, _) {
    var CREATE_META_REST_URL = "/rest/api/2/issue/createmeta?";

    function createMetaRequest(applinkId, queryParam, success, error) {
        AppLinks.makeRequest({
            appId: applinkId,
            type: 'GET',
            url: CREATE_META_REST_URL + queryParam,
            dataType: 'json',
            success: success,
            error:function(xhr){
                AJS.logError("Request createmeta error with status=" + xhr.status);
                error(xhr);
            }
        });
    };
    
    function loadProjects(applinkId, success){
        createMetaRequest(applinkId, 'expand=projects', function(data) {
            success(data.projects);
        });
    };
    function loadIssueTypes(applinkId, projectId, success){
        createMetaRequest(applinkId, 'expand=projects.issuetypes.fields&projectIds=' + projectId, function(data) {
            success(data.projects[0].issuetypes);
        });
    };
    
    function loadIssueTypeFields(applinkId, projectId, issueTypeId, success) {
        createMetaRequest(applinkId, 'expand=projects.issuetypes.fields&projectIds=' + projectId + '&issuetypeIds=' + issueTypeId, function(issueFieldData) {
            success(issueFieldData.projects[0].issuetypes[0].fields);
        });
    };
    
    function renderField(field, applinkContextId) {
        if(jiraIntegration.fields.canRender(field)) {
            var renderField = $(jiraIntegration.fields.renderField(null, field));
            var renderContextHandler = jiraIntegration.fields.getRestType(field).renderContextHandler;
            if(renderContextHandler) {
                renderContextHandler(applinkContextId, renderField, field);
            }
            return renderField;
        } else {
            return {
                renderError: {
                    message: "Cannot render with field = "+field.name,
                    typeId: field.schema.system || field.schema.custom || field.schema.customId
                }
            }
        }
    };
    function renderFieldIssuesType(issuesTypeValues) {
//        AJS.$(issuesTypeValues).each(function(){
//            var issueType = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this}));
//            issueType.data("fields", this.fields);
//        });
        var options = _.map(issuesTypeValues, function(val) {
            return {
                value : val.id,
                text: val.name
            };
        });
        return $(jiraIntegration.templates.fields.allowedValuesField({
            jiraType: 'issuetype',
            labelText: 'Issue Type',
            name: 'issuetype',
            extraClasses: 'type-select',
            options: options
        })).removeClass('jira-field');
    };

    return {
        loadProjects: loadProjects,
        loadIssueTypes: loadIssueTypes,
        loadIssueTypeFields: loadIssueTypeFields,
        renderField: renderField,
        renderFieldIssuesType: renderFieldIssuesType
    };
    
}(AJS.$, window._));

