define('confluence/jim/macro-browser/editor/util/service', [
    'jquery',
    'underscore',
    'ajs'
],
function(
    $,
    _,
    AJS
) {
    'use strict';

    var cacheServerData = null;

    return {
        clearCache: function() {
            cacheServerData = null;
        },

        loadJiraServers: function() {
            var dfd = $.Deferred();

            if (cacheServerData) {
                dfd.resolve(cacheServerData);
                return dfd.promise();
            }

            $.ajax({
                dataType: 'json',
                url: AJS.contextPath() + '/rest/jiraanywhere/1.0/servers'
            })
            .done(function(servers) {
                var primaryServer = _.findWhere(servers, {selected: true});

                cacheServerData = {
                    servers: servers,
                    primaryServer: primaryServer
                };

                dfd.resolve(cacheServerData);
            })
            .fail(dfd.reject);

            return dfd.promise();
        },

        loadBoardsData: function(serverId) {
            return $.ajax({
                dataType: 'json',
                url: AJS.contextPath() + '/rest/jiraanywhere/1.0/jira/agile/' + serverId + '/boards',
                timeout: AJS.Meta.getNumber('connection-timeout')
            });
        },

        loadSprintsData: function(serverId, boardId) {
            return $.ajax({
                dataType: 'json',
                url: AJS.contextPath() + '/rest/jiraanywhere/1.0/jira/agile/' + serverId + '/boards/' + boardId + '/sprints',
                timeout: AJS.Meta.getNumber('connection-timeout')
            });
        },

        getLabelsOfPage: function() {
            var pageId =  AJS.Meta.get('page-id');
            var url = AJS.contextPath() + '/rest/ui/1.0/content/' + pageId + '/labels';

            return $.getJSON(url);
        }
    };
});

