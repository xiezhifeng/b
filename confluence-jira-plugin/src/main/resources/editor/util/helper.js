define('confluence/jim/editor/util/helper', [
    'underscore',
    'ajs',
    'confluence/jim/editor/util/config'
],
function(
    _,
    AJS,
    config
) {
    'use strict';

    return {
        isJiraUnSupportedVersion: function(server) {
            var buildNumber = server.buildNumber;
            return  buildNumber === config.NOT_SUPPORTED_BUILD_NUMBER ||
                    (buildNumber >= config.START_JIRA_UNSUPPORTED_BUILD_NUMBER && buildNumber < config.END_JIRA_UNSUPPORTED_BUILD_NUMBER);
        }
    };
});
