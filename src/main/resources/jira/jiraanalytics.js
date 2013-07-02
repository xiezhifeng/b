/*
 * Facade service for analytics event handling
 * */
AJS.Editor.JiraConnector.Analytics = {

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
            name : AJS.Editor.JiraConnector.Analytics.events.paste.key,
            properties : properties
        });
    },

    triggerPannelActionEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraConnector.Analytics.events.panelAction.key,
            properties : properties
        });
    },

    triggerSearchEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraConnector.Analytics.events.search.key,
            properties : properties
        });
    },

    triggerMarkupEvent : function(properties) {
        this.triggerSearchEvent(properties);
    },

    triggerPannelTriggerEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraConnector.Analytics.events.trigger.key,
            properties : properties
        });
    },

    triggerCustomizeColumnEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraConnector.Analytics.events.customizeColumn.key,
            properties : properties
        });
    }
};

// Use AOP to detect and trigger event upon jira panel open action
(function($){

$.aop.before({target: AJS.MacroBrowser, method: 'loadMacroInBrowser'},
  function(metadata, target) {
    if (metadata && metadata[0] && metadata[0].pluginKey == 'confluence.extra.jira') {
        AJS.Editor.JiraConnector.Analytics.triggerPannelTriggerEvent({
            source : 'macro_browser'
        });
    }
  }
);
$.aop.before({target: tinymce.confluence.macrobrowser, method: 'macroBrowserToolbarButtonClicked'},
  function(metadata, target) {
    if (metadata && metadata[0] && metadata[0].presetMacroMetadata && metadata[0].presetMacroMetadata.pluginKey == 'confluence.extra.jira') {
        AJS.Editor.JiraConnector.Analytics.triggerPannelTriggerEvent({
            source : 'editor_brace_key'
        });
    }
  }
);

})(AJS.$);