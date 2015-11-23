/*
 * Facade service for analytics event handling
 * */
AJS.Editor.JiraAnalytics = {

    // All supported events
    events : {
        paste : {
            key : 'confluence.jira.plugin.paste'
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

    getDisplayType: function(panel) {
        var display = "single";
        if (panel.container.find("#opt-table").is(":checked")) {
            display = "table";
        } else if (panel.container.find("#opt-total").is(":checked")) {
            display = "count";
        }

        return display;
    },

    setupAnalyticPanelActionObject : function(panel, source, label) {
        return {
            name: panel.analyticPanelActionName,
            properties: this.setupPanelActionProperties(panel, source, label)
        };
    },

    setupPanelActionProperties : function(panel, source, label) {
        var properties = {};
        if (source === AJS.Editor.JiraConnector.source.instructionalText) {
            if (panel.analyticPanelActionName === 'confluence.jira.plugin.issuecreated') {
                properties.issueType = panel.container.find('select[name="issuetype"] :selected').text();
            } else if (panel.analyticPanelActionName === 'confluence.jira.plugin.searchadded') {
                properties.display = this.getDisplayType(panel);
            }
            properties.label = label;
        }
        return properties;
    },

    triggerPasteEvent : function(properties) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push({
            name : AJS.Editor.JiraAnalytics.events.paste.key,
            properties : properties
        });
    },

    triggerPannelActionEvent : function(analyticPanelActionObject) {
        AJS.EventQueue = AJS.EventQueue || [];
        AJS.EventQueue.push(analyticPanelActionObject);
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
            source : AJS.Editor.JiraConnector.source.macroBrowser
        });
    }
  }
);
$.aop.before({target: tinymce.confluence.macrobrowser, method: 'macroBrowserToolbarButtonClicked'},
  function(metadata, target) {
    if (metadata && metadata[0] && metadata[0].presetMacroMetadata && metadata[0].presetMacroMetadata.pluginKey == 'confluence.extra.jira') {
        AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
            source : AJS.Editor.JiraConnector.source.editorBraceKey
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
            var macro = AJS.$('<div></div>').html(placeHolder).children().first();
            var macroName = macro.attr('data-macro-name');
            
            var validJIMNames = ['jira', 'jiraissues'];
            if ($.inArray(macroName, validJIMNames) == -1) {
                return;
            }
            
            var jiraAnalytics = AJS.Editor.JiraAnalytics;
            var analyticsData = {source : 'wiki_markup'};
            if (!macro.attr('data-macro-parameters')) {
                return;
            }
            var macroParams = macro.attr('data-macro-parameters').split('|');
            
            for ( var i = 0; i < macroParams.length; i++) {
                var param = $.trim(macroParams[i]);
                if (param.indexOf('jql') == 0 || param.indexOf('jqlQuery') == 0) {
                    analyticsData.type = jiraAnalytics.linkTypes.jqlDirect;
                    break;
                } else if (param.indexOf('url') == 0) {
                    var url = $.trim(param.substring(param.indexOf('=') + 1, param.length));
                    analyticsData.type = AJS.JQLHelper.checkQueryType(url);
                    break;
                } else if (param.indexOf('http') == 0) {
                    var url = param;
                    analyticsData.type = AJS.JQLHelper.checkQueryType(url);
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