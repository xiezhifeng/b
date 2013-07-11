/*
 * Facade service for analytics event handling
 * */
AJS.Editor.JiraAnalytics = {

    // All supported events
    events : {
        paste : {
            key : 'confluence.jira.plugin.paste'
        },
        panelAction : {
            key : 'confluence.jira.plugin.panel.action'
        },
        search : {
            key : 'confluence.jira.plugin.search'
        },
        trigger : {
            key : 'confluence.jira.plugin.trigger'
        },
        customizeColumn : {
            key : 'confluence.jira.plugin.column.customize'
        }
    },

    linkTypes : {
        jqlDirect : 'direct_jql',
        jql : 'jql_link',
        xml : 'xml_link',
        rss : 'rss_link',
        filter : 'filter_link'
    },

    triggerPasteEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.paste.key,
            properties : properties
        });
    },

    triggerPannelActionEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.panelAction.key,
            properties : properties
        });
    },

    triggerSearchEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.search.key,
            properties : properties
        });
    },

    triggerMarkupEvent : function(properties) {
        this.triggerSearchEvent(properties);
    },

    triggerPannelTriggerEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.trigger.key,
            properties : properties
        });
    },

    triggerCustomizeColumnEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.customizeColumn.key,
            properties : properties
        });
    }
};

// Use AOP to detect and trigger event upon jira panel open action
(function($){

$.aop.before({target: AJS.MacroBrowser, method: 'loadMacroInBrowser'},
  function(metadata, target) {
    if (metadata && metadata[0] && metadata[0].pluginKey == 'confluence.extra.jira') {
        AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
            source : 'macro_browser'
        });
    }
  }
);
$.aop.before({target: tinymce.confluence.macrobrowser, method: 'macroBrowserToolbarButtonClicked'},
  function(metadata, target) {
    if (metadata && metadata[0] && metadata[0].presetMacroMetadata && metadata[0].presetMacroMetadata.pluginKey == 'confluence.extra.jira') {
        AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
            source : 'editor_brace_key'
        });
    }
  }
);

// Use AOP to analyze manual insert via wiki markup dialog
AJS.bind("init.rte", function() {
    $.aop.before({target : tinyMCE.activeEditor, method : 'execCommand'}, function(metadata, target) {
        if (metadata && metadata[0] == 'mceInsertContent' && metadata[2]) {
            var placeHolder = metadata[2];
            
            // Force to parse placeHolder as HTML only, since we only expect HTML here
            var macro = $('<div></div>').html(placeHolder).children().first();
            var macroName = macro.attr('data-macro-name');
            
            var validJIMNames = ['jira', 'jiraissues'];
            if ($.inArray(macroName, validJIMNames) == -1) {
                return;
            }
            
            var jiraAnalytics = AJS.Editor.JiraConnector.Analytics;
            var analyticsData = {source : 'wiki_markup'};
            var macroParams = macro.attr('data-macro-parameters').split('|');
            
            for ( var i = 0; i < macroParams.length; i++) {
                var param = $.trim(macroParams[i]);
                if (param.indexOf('jql') == 0 || param.indexOf('jqlQuery') == 0) {
                    analyticsData.type = jiraAnalytics.linkTypes.jqlDirect;
                    break;
                } else if (param.indexOf('url') == 0) {
                    var url = $.trim(param.substring(param.indexOf('=') + 1, param.length));
                    analyticsData.type = AJS.Editor.JiraConnector.JQL.checkQueryType(url);
                    break;
                } else if (param.indexOf('http') == 0) {
                    var url = param;
                    analyticsData.type = AJS.Editor.JiraConnector.JQL.checkQueryType(url);
                    break;
                }
            }
            if (typeof analyticsData.type === 'undefined') {
                analyticsData.type = jiraAnalytics.linkTypes.jqlDirect;
            }
            jiraAnalytics.triggerMarkupEvent(analyticsData);
        }
    });
});

})(AJS.$);