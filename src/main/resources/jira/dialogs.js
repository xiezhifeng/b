//Register TinyMCE plugin
(function() {

    tinymce.create('tinymce.plugins.JiraLink', {
        init : function(ed) {
            ed.addCommand('mceJiralink', AJS.Editor.JiraConnector.hotKey);
            ed.onPostRender.add(function(ed){
                AJS.$.get(Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers', function(data){
                    AJS.Editor.JiraConnector.servers = data;
                });
                AJS.$('#jiralink').click(function(e) {
                    AJS.Editor.JiraConnector.open(true);
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

AJS.Editor.JiraConnector=(function($){
    var dialogTitle = AJS.I18n.getText("insert.jira.issue");
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");

    var modifierKey = function() {
        var isMac = navigator.platform.toLowerCase().indexOf("mac") != -1;
        return isMac ? "Cmd" : "Ctrl";
    };
    var kbHelpText = AJS.I18n.getText("insert.jira.issue.dialog.help.shortcut", modifierKey());
    var jiraAnalyticsProperties;
    var popup;

    var openJiraDialog = function(summaryText){
        if (!popup){
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-connector"});

            popup.addHeader(dialogTitle);
            var panels = AJS.Editor.JiraConnector.Panels;

            for (var i = 0; i < panels.length; i++){
                popup.addPanel(panels[i].title());
                var dlgPanel = popup.getCurrentPanel();
                var panelObj = panels[i];
                panelObj.init(dlgPanel);
            }
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function() {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            popup.addHelpText(kbHelpText);
            // add toolips for help text
            var helpText = $('#jira-connector .dialog-tip').attr('data-title', kbHelpText);
            var tipsyOptions = {
                live: true,
                title: function() {return $(this).attr('data-title')},
                gravity: 's', // Point the arrow to the top
                delayIn: 300,
                delayOut: 0
            };
            helpText.tooltip(tipsyOptions);
            
            popup.addButton(insertText, function(){
                var panel = panels[popup.getCurrentPanel().id];
                panel.insertLink();
                if (jiraAnalyticsProperties) {
                    AJS.Editor.JiraConnector.Analytics.triggerPannelActionEvent(jiraAnalyticsProperties);
                }
                var searchPanel = panels[0];
                if (searchPanel.customizedColumn) {
                    AJS.Editor.JiraConnector.Analytics.triggerCustomizeColumnEvent({
                        columns : searchPanel.customizedColumn
                    });
                }
            }, 'insert-issue-button');
            // disable insert issue button when open popup
            AJS.$('.insert-issue-button').disable();

            popup.addCancel(cancelText, function(){
                AJS.Editor.JiraConnector.closePopup();
            });
            // default to search panel
            popup.gotoPanel(0);
            jiraAnalyticsProperties = {action : 'search'};

            $('#jira-connector .dialog-page-menu button').eq(0).click(function(){
                jiraAnalyticsProperties = {action : 'search'};
            });
            $('#jira-connector .dialog-page-menu button').eq(1).click(function(){
                jiraAnalyticsProperties = {action : 'create_new'};
            });
            $('#jira-connector .dialog-page-menu button').eq(2).click(function(){
                jiraAnalyticsProperties = {action : 'view_recent'};
            });

        }
        popup.show();
        if (summaryText){
            popup.gotoPanel(1);
            var createPanel = AJS.Editor.JiraConnector.Panels[1];
            createPanel.setSummary(summaryText);
        }
        else{
            // always show search
            popup.gotoPanel(0);
        }

    };

   var checkExistAppLinkConfig = function() {
        //call again get list server after admin click config applink
        if (AJS.Editor.JiraConnector.clickConfigApplink) {
            AJS.$.ajax({url:Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers', async:false}).done(function(response) {
                AJS.Editor.JiraConnector.servers = response;
            });
        }
        //check exist config applink
        if (typeof(AJS.Editor.JiraConnector.servers) == 'undefined' || AJS.Editor.JiraConnector.servers.length == 0) {
            //show warning popup with permission of login's user
            AJS.Editor.JiraConnector.warningPopup(AJS.Meta.get("is-admin"));
            return false;
        }
        AJS.Editor.JiraConnector.clickConfigApplink = false;
        // call refresh for applink select control
        if(AJS.Editor.JiraConnector.refreshAppLink) {
            AJS.Editor.JiraConnector.refreshAppLink.call();
            AJS.Editor.JiraConnector.refreshAppLink = false;
        }
        return true;
    };


    return {
        warningPopup : function(isAdministrator){
            //create new dialog
            var warningDialog = new AJS.ConfluenceDialog({width:600, height:400,id: "warning-applink-dialog"});
            //add title dialog
            var warningDialogTitle = AJS.I18n.getText("applink.connector.jira.popup.title");
            warningDialog.addHeader(warningDialogTitle);

            //add body content in panel
            var bodyContent = Confluence.Templates.ConfluenceJiraPlugin.warningDialog({'isAdministrator':isAdministrator});
            warningDialog.addPanel("Panel 1", bodyContent);
            warningDialog.get("panel:0").setPadding(0);
            if(isAdministrator) {
                //add button set connect
                warningDialog.addButton(AJS.I18n.getText("applink.connector.jira.popup.button.admin"), function (warningDialog) {
                    AJS.Editor.JiraConnector.clickConfigApplink = true;
                    warningDialog.hide();
                    tinymce.confluence.macrobrowser.macroBrowserCancel();
                    window.open(Confluence.getContextPath() + "/admin/listapplicationlinks.action");
                }, "create-dialog-create-button app_link");
                //apply class css of form
                warningDialog.popup.element.find(".create-dialog-create-button").removeClass("button-panel-button").addClass("aui-button aui-button-primary");
            } else {
                //add button contact admin
                warningDialog.addButton(AJS.I18n.getText("applink.connector.jira.popup.button.contact.admin"), function (warningDialog) {
                    warningDialog.hide();
                    tinymce.confluence.macrobrowser.macroBrowserCancel();
                    window.open(Confluence.getContextPath() + "/wiki/contactadministrators.action");
                });
            }

            //add button cancel
            warningDialog.addLink(AJS.I18n.getText("insert.jira.issue.button.cancel"), function (warningDialog) {
                warningDialog.hide();
                tinymce.confluence.macrobrowser.macroBrowserCancel();
            });

            warningDialog.show();
            warningDialog.gotoPanel(0);
        },
        closePopup: function(){
            popup.hide();
            tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        open: function(fromRTEMenu) {

            //check exist applink config
            if (!checkExistAppLinkConfig()) {
                return;
            }

            // Store the current selection and scroll position, and get the selected text.
            AJS.Editor.Adapter.storeCurrentSelectionState();
            var summaryText;
            if (fromRTEMenu) {
                summaryText = tinyMCE.activeEditor.selection.getContent({format : 'text'});
                AJS.Editor.JiraConnector.Analytics.triggerPannelTriggerEvent({
                    source : 'editor_dropdown_link'
                });
            }

            var t = tinymce.confluence.macrobrowser,
            node = t.getCurrentNode();
            if (t.isMacroTag(node) && 'jira' == $(node).attr('data-macro-name')) {
                tinymce.confluence.macrobrowser.editMacro(node);
                return;
            }

            openJiraDialog(summaryText);
        },
        edit: function(macro){

            //check for show custom dialog when click in other macro
            if (typeof(macro.params) == 'undefined') {
                AJS.Editor.JiraConnector.open();
                return;
            }

            //check status exist macro and remove all applink.
            if (!checkExistAppLinkConfig()) {
                AJS.Editor.JiraConnector.open();
                return;
            }

            var parseUglyMacro = function(macroTxt) {
                //get first macro parameter and assume its a jql query
                var bar = macroTxt.indexOf("|");
                if (bar >= 0) {
                    return macroTxt.substring(0, bar);
                }
                return macroTxt;
            };

            var getJQLJiraIssues = function(obj) {
                if(obj.hasOwnProperty('jqlQuery')) {
                    return obj['jqlQuery'];
                }

                var positiveIntegerRegex = /^([0-9]\d*)$/;
                var arrayParams = ["count","columns","title","renderMode","cache","width","height","server","serverId","anonymous","baseurl"];
                for (var prop in obj) {
                    if($.inArray(prop, arrayParams) == -1 && obj.hasOwnProperty(prop)) {
                        if(positiveIntegerRegex.test(prop)) {
                            return obj[prop];
                        }
                        return prop += ' = ' + obj[prop];
                    }
                }

                return "";
            }

            var getParamsJiraIssues = function(macro) {
                var params = {};
                if(macro.params['url']) {
                    params['searchStr'] = macro.params['url'];
                    return params;
                }

                //macro param is JQL | Key
                var jqlStr = macro.defaultParameterValue || getJQLJiraIssues(macro.params);
                if (typeof (jqlStr) == 'undefined') {
                    params['searchStr'] = '';
                } else {
                    params['searchStr'] = jqlStr;
                }
                //macro param is server
                if (typeof(macro.params['server']) != 'undefined') {
                    params['serverName'] = macro.params['server'];
                } else {
                    //get server primary
                    for (var i = 0; i < AJS.Editor.JiraConnector.servers.length; i++) {
                        if(AJS.Editor.JiraConnector.servers[i].selected) {
                            params['serverName'] = AJS.Editor.JiraConnector.servers[i].name
                            break;
                        }
                    }
                }
                return params;
            }

            var getParamsJira = function(macro) {
                var params = {};
                var searchStr = macro.defaultParameterValue || macro.params['jqlQuery']
                || macro.params['key']
                || parseUglyMacro(macro.paramStr);
                params['searchStr'] = searchStr;
                params['serverName'] = macro.params['server'];
                return params;
            }

            // parse params from macro data
            var parseParamsFromMacro = function(macro) {
                var params = getParamsJiraIssues(macro);

                /*//macro name is jiraissues
                 if (macro.name == 'jiraissues') {
                 params = getParamsJiraIssues(macro);
                 }
                 //macro name is jira
                 if (macro.name == 'jira') {
                 params = getParamsJira(macro);
                 }*/

                var count = macro.params['count'];
                if (typeof count === "undefined") {
                    count = "false";
                }
                params['count'] = count;

                var columns = macro.params['columns'];
                if (typeof(columns) != 'undefined') {
                    if (columns.length) {
                        params['columns'] = columns;
                    }
                }
                return params;
            };

            var macroParams = parseParamsFromMacro(macro);

            /*if (typeof(macroParams['serverName']) == 'undefined') {
             AJS.Editor.JiraConnector.warningPopup(AJS.Meta.get("is-admin"));
             return;
             }*/

            if (macro && !AJS.Editor.inRichTextMode()) { // select and replace the current macro markup
                $("#markupTextarea").selectionRange(macro.startIndex, macro.startIndex + macro.markup.length);
            }
            openJiraDialog();
            if (macroParams.searchStr) {
                popup.gotoPanel(0);
                var searchPanel = AJS.Editor.JiraConnector.Panels[0];
                // assign macro params to search
                searchPanel.setMacroParams(macroParams);
                searchPanel.doSearch(macroParams['searchStr'], macroParams['serverName']);
            }
        }
    };
})(AJS.$);

AJS.MacroBrowser.setMacroJsOverride('jira', {opener: AJS.Editor.JiraConnector.edit});
AJS.MacroBrowser.setMacroJsOverride('jiraissues', {opener: AJS.Editor.JiraConnector.edit});

AJS.Editor.JiraConnector.Panels = [];

AJS.Editor.JiraConnector.clickConfigApplink = false;

AJS.Editor.JiraConnector.hotKey = function() {
    AJS.Editor.JiraConnector.open(false);
    AJS.Editor.JiraConnector.Analytics.triggerPannelTriggerEvent({
        source : 'editor_hot_key'
    });
}
