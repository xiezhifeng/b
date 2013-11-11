
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
    function renderField(field, applinkContextId, errorRender) {
        if(jiraIntegration.fields.canRender(field)) {
            var renderField = $(jiraIntegration.fields.renderField(null, field));
            var renderContextHandler = jiraIntegration.fields.getRestType(field).renderContextHandler;
            if(renderContextHandler) {
                renderContextHandler(applinkContextId, renderField, field);
            }
            return renderField;
        } else {
            errorRender(field, "Cannot render with field = "+field.name);
            AJS.logError("Cannot render with field = "+field.name);
            return null;
        }
    };

    return {
        loadProjects: loadProjects,
        loadIssueTypes: loadIssueTypes,
        renderField: renderField
    };
    
}(AJS.$, window._));

