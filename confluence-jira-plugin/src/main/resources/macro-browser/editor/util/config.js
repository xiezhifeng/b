define('confluence/jim/macro-browser/editor/util/config', [],
function(
) {
    'use strict';

    return {
        macroIdSprint: 'jirasprint',

        NOT_SUPPORTED_BUILD_NUMBER: -1,
        START_JIRA_UNSUPPORTED_BUILD_NUMBER: 6109, // jira version 6.0.8
        END_JIRA_UNSUPPORTED_BUILD_NUMBER: 6155, // jira version 6.1.1

        DEFAULT_OPTION_VALUE: '-1'
    };
});
