//Register TinyMCE plugin
(function() {

    tinymce.create('tinymce.plugins.JiraLink', {
        init : function(ed) {
            ed.addCommand('mceJiralink', AJS.Editor.JiraConnector.hotKey);
            ed.onPostRender.add(function(ed){
                var serversAjax = AJS.$.ajax({url:Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers'}).done(function(response) {
                    AJS.Editor.JiraConnector.servers = response;
                });
                AJS.$('#jiralink').click(function(e) {
                    serversAjax.done( function() {
                        AJS.Editor.JiraConnector.open(AJS.Editor.JiraConnector.source.editorDropdownLink, true);
                        return AJS.stopEvent(e);
                    });
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

AJS.Editor.JiraConnector = (function($) {
    var EMPTY_VALUE = '';
    var dialogTitle = AJS.I18n.getText("insert.jira.issue");
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");

    var modifierKey = function() {
        var isMac = navigator.platform.toLowerCase().indexOf("mac") != -1;
        return isMac ? "Cmd" : "Ctrl";
    };

    var source = {
        macroBrowser: "macro_browser",
        editorBraceKey: "editor_brace_key",
        editorHotKey: "editor_hot_key",
        editorDropdownLink: "editor_dropdown_link",
        instructionalText: "instructional text"
    };

    var kbHelpText = AJS.I18n.getText("insert.jira.issue.dialog.help.shortcut", modifierKey());
    var openDialogSource, labels, popup;

    var panelTriggerWithLabel = function() {

        var $pageLabelsString = $('#createPageLabelsString');
        if ($pageLabelsString.length > 0) {
            labels = $pageLabelsString.val().split(" ").join();
            AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
                source: openDialogSource,
                label: labels
            });
            return;
        }

        $.getJSON(AJS.Meta.get('base-url') + '/rest/ui/1.0/content/' + AJS.Meta.get('page-id') + '/labels', function(data) {
            var labelNames = [];
            $.each(data.labels, function(index, label) {
                labelNames.push(label.name);
            });
            labels = labelNames.join();
            AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
                source: openDialogSource,
                label: labels
            });
        });
    };

    var doAnalytic = function(searchPanel, currentPanel) {
        if(AJS.Editor.JiraAnalytics) {
            AJS.Editor.JiraConnector.analyticPanelActionObject = AJS.Editor.JiraAnalytics.setupAnalyticPanelActionObject(currentPanel, openDialogSource, labels);
            if (searchPanel.customizedColumn) {
                AJS.Editor.JiraAnalytics.triggerCustomizeColumnEvent({
                    columns : searchPanel.customizedColumn
                });
            }
        }
    };

    var handleFocus = function(panel) {
        panel.focusForm && panel.focusForm();
    };

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
            $('#jira-connector .dialog-tip').attr('title', kbHelpText);
            
            popup.addButton(insertText, function(){
                var panelIndex = popup.getCurrentPanel().id;
                var panel = panels[panelIndex];
                var searchPanel = panels[0];
                doAnalytic(searchPanel, panel);
                panel.insertLink();
            }, 'insert-issue-button');
            // disable insert issue button when open popup
            AJS.$('.insert-issue-button').disable();

            popup.addCancel(cancelText, function(){
                AJS.Editor.JiraConnector.closePopup();
            });
            // default to search panel
            popup.gotoPanel(0);

            // prefetch server columns for autocompletion feature
            if (AJS.Editor.JiraConnector.servers) {
                for ( var i = 0; i < AJS.Editor.JiraConnector.servers.length; i++) {
                    var server = AJS.Editor.JiraConnector.servers[i];
                    AppLinks.makeRequest({
                        appId: server.id,
                        type: 'GET',
                        url: '/rest/api/2/field',
                        dataType: 'json',
                        serverIndex : i,
                        success: function(data) {
                            if (data && data.length) {
                                AJS.Editor.JiraConnector.servers[this.serverIndex].columns = data;
                            }
                        },
                        error: function() {
                            AJS.log("Jira Issues Macro: unable to retrieve fields from AppLink: " + server.id);
                        }
                    });
                }
            }

            $('#jira-connector ul.dialog-page-menu').append(Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLink({'id': 'open-jira-chart-dialog', 'label' : AJS.I18n.getText("confluence.extra.jira.jirachart.label")}));

            $('#jira-connector .dialog-page-menu button').click(function() {
                var currentPanel = AJS.Editor.JiraConnector.Panels[popup.getCurrentPanel().id];
                currentPanel.setInsertButtonState && currentPanel.setInsertButtonState();
                handleFocus(currentPanel);
            });

            $('#open-jira-chart-dialog').click(function() {
                AJS.Editor.JiraConnector.closePopup();
                if (AJS.Editor.JiraChart) {
                    AJS.Editor.JiraChart.open();
                }
            });
        }

        //Reset search form when open dialog
        AJS.Editor.JiraConnector.Panels[0].refreshSearchForm();
        if (summaryText) {
            popup.gotoPanel(1);
            var createPanel = AJS.Editor.JiraConnector.Panels[1];
            createPanel.setSummary(summaryText);

            //fix for IE8- need waiting until completed render create issue panel
            setTimeout(function() {
                handleFocus(createPanel);
            }, 0);
        } else {
            // always show search
            popup.gotoPanel(0);
        }
        popup.overrideLastTab();
        popup.show();
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
        open: function(source, isPopulateSummaryText) {
        // this open() method is intended to be called by the macro's code, not by the editor's macro browser
            //check exist applink config
            if (!checkExistAppLinkConfig()) {
                return;
            }

            // Store the current selection and scroll position, and get the selected text.
            AJS.Editor.Adapter.storeCurrentSelectionState();
            openDialogSource = source;
            if (AJS.Editor.JiraAnalytics && openDialogSource) {
                if (openDialogSource === AJS.Editor.JiraConnector.source.instructionalText) {
                    panelTriggerWithLabel();
                } else {
                    labels = null;
                    AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({source: openDialogSource});
                }
            }

            var macroBrowser = tinymce.confluence.macrobrowser,
            node = macroBrowser.getCurrentNode();
            if (macroBrowser.isMacroTag(node) && 'jira' == $(node).attr('data-macro-name')) {
                macroBrowser.editMacro(node);
                // editMacro will call the opener function, which is the edit() function below
                // in the edit() function, we should avoid calling the open() method
                // also see the comment below
                // Otherwise, we will face a deadlock problem
                return;
            }
            AJS.Editor.JiraConnector.openCleanDialog(isPopulateSummaryText);
        },
        openCleanDialog: function(isPopulateSummaryText) {
            var summaryText = isPopulateSummaryText && tinyMCE.activeEditor.selection && tinyMCE.activeEditor.selection.getContent({format : 'text'});
            openJiraDialog(summaryText);
            var searchPanel = AJS.Editor.JiraConnector.Panels[0];
            searchPanel.setMacroParams(null);
        },
        edit: function(macro){
            //check status exist macro and remove all applink.
            if (!checkExistAppLinkConfig()) {
                return;
            }
            //check for show custom dialog when click in other macro
            if (typeof(macro.params) == 'undefined') {
                // WARNING: we must not call AJS.Editor.JiraConnector.open() here
                AJS.Editor.JiraConnector.openCleanDialog(false);
                return;
            }

            //reset source when edit
            openDialogSource = EMPTY_VALUE;
            labels = EMPTY_VALUE;

            var getJQLJiraIssues = function(obj) {
                if(obj.hasOwnProperty('jqlQuery')) {
                    return obj['jqlQuery'];
                }

                var positiveIntegerRegex = /^([0-9]\d*)$/;
                var arrayParams = ["count","columns","title","renderMode","cache","width","height","server","serverId","anonymous","baseurl", "showSummary"];
                for (var prop in obj) {
                    if($.inArray(prop, arrayParams) == -1 && obj.hasOwnProperty(prop)) {
                        if(positiveIntegerRegex.test(prop)) {
                            return obj[prop];
                        }
                        return prop += ' = ' + obj[prop];
                    }
                }

                return "";
            };

            var getParamsJiraIssues = function(macro) {
                var params = {};
                if(macro.params['url']) {
                    params['searchStr'] = macro.params['url'];
                    return params;
                }

                params['maximumIssues'] = macro.params['maximumIssues'];

                //macro param is JQL | Key
                var jqlStr = macro.defaultParameterValue || getJQLJiraIssues(macro.params);
                if (typeof (jqlStr) == 'undefined') {
                    params['searchStr'] = EMPTY_VALUE;
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
                            params['serverName'] = AJS.Editor.JiraConnector.servers[i].name;
                            break;
                        }
                    }
                }
                return params;
            } ;

            // parse params from macro data
            var parseParamsFromMacro = function(macro) {
                var params = getParamsJiraIssues(macro);

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

            if (macro && !AJS.Editor.inRichTextMode()) { // select and replace the current macro markup
                $("#markupTextarea").selectionRange(macro.startIndex, macro.startIndex + macro.markup.length);
            }
            openJiraDialog();
            if (macroParams.searchStr) {
                popup.gotoPanel(0);
                var searchPanel = AJS.Editor.JiraConnector.Panels[0];
                // assign macro params to search
                searchPanel.setMacroParams(macroParams);
                var searchParams = {
                    searchValue: macroParams['searchStr'],
                    serverName: macroParams['serverName'],
                    isJqlQuery: macro.params.hasOwnProperty("jqlQuery"),
                    isAutoSearch: true
                };
                searchPanel.doSearch(searchParams);
            }
        },

        source: source
    };
})(AJS.$);

AJS.MacroBrowser.setMacroJsOverride('jira', {opener: AJS.Editor.JiraConnector.edit});
AJS.MacroBrowser.setMacroJsOverride('jiraissues', {opener: AJS.Editor.JiraConnector.edit});

AJS.Editor.JiraConnector.Panels = [];

AJS.Editor.JiraConnector.clickConfigApplink = false;

AJS.Editor.JiraConnector.hotKey = function() {
    AJS.Editor.JiraConnector.open(AJS.Editor.JiraConnector.source.editorHotKey, true);
};
