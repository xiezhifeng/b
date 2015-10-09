define('confluence/jim/editor/util/config', [],
function(
) {
    'use strict';

    return {
        macroIdSprint: 'jirasprint',

        NOT_SUPPORTED_BUILD_NUMBER: -1,
        START_JIRA_UNSUPPORTED_BUILD_NUMBER: 6109, // jira version 6.0.8
        END_JIRA_UNSUPPORTED_BUILD_NUMBER: 6155, // jira version 6.1.1

        SUGGESTION_SUFFIX_LENGTH: 4,
        TITLE_MAX_LENGTH: 255,
        DEFAULT_OPTION_VALUE: '-1',

        MAX_TIMES_CHECK_TITLE: 5,
        HELLIP_CHARACTER: '\u2026'
    };
});
