AJS.Editor.JiraConnector = {};

// Register TinyMCE plugin
(function() {

    tinymce.create('tinymce.plugins.JiraLink', {
        init : function(ed) {
            ed.addCommand('mceJiralink', AJS.Editor.JiraConnector.hotKey);
            ed.onPostRender.add(function(ed){
                AJS.$.get(Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers', function(data){
                    AJS.Editor.JiraConnector.servers = data;
                });
                AJS.$('#jiralink').click(function(e) {
                    AJS.trigger('jira.links.macro.dialog.open', {
                        name: 'jira', // macro id
                        openSource: JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorDropdownLink
                    });
                    return AJS.stopEvent(e);
                });
                AJS.$('#insert-menu .macro-jiralink').show();
                ed.addShortcut('ctrl+shift+j', '', 'mceJiralink');
            });
        },
        getInfo : function () {
            return {
                longname : "Confluence Jira Connector",
                author : "Atlassian",
                authorurl : "http://www.atlassian.com",
                version : tinymce.majorVersion + "." + tinymce.minorVersion
            };
        }
    });

    tinymce.PluginManager.add('jiraconnector', tinymce.plugins.JiraLink);
})();

AJS.Editor.Adapter.addTinyMcePluginInit(function(settings) {
    settings.plugins += ",jiraconnector";
    var buttons = settings.theme_advanced_buttons1;
    var index = buttons.indexOf("confimage");
    settings.theme_advanced_buttons1 = buttons.substring(0, index) + "jiralinkButton," + buttons.substring(index);
});

AJS.Editor.JiraConnector.hotKey = function() {
    AJS.trigger('jira.links.macro.dialog.open', {
        name: 'jira',
        openSource: window.JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorHotKey
    });
};
