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

    return {
        loadJiraServers: function() {
            var dfd = $.Deferred();

            $.ajax({
                dataType: 'json',
                url: AJS.contextPath() + '/rest/jiraanywhere/1.0/servers'
            })
            .done(function(servers) {
                var primaryServer = _.findWhere(servers, {selected: true});

                dfd.resolve({
                    servers: servers,
                    primaryServer: primaryServer
                });
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
        }
    };
});

