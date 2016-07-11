AJS.Editor.JiraConnector.Panel = function() {};

AJS.Editor.JiraConnector.Panel.prototype = {
        SHOW_MESSAGE_ON_TOP: false, // message is default in the bottom

        /**
         * Insert a JIRA macro linking to the supplied issue key.
         * 
         * @param key the issue key
         */
        insertIssueLink: function(key){
            this.insertIssueLinkWithParams({"key": key});
        },
        
        /**
         * Insert a JIRA macro linking to the supplied jql query 
         * 
         * @param jql the query to be used.
         */
        insertJqlLink: function(jql) {
            this.insertIssueLinkWithParams({"jqlQuery": jql});
        },
        
        insertIssueLinkWithParams: function(params) {
            
            var insertMacroAtSelectionFromMarkup = function (macro){
                tinymce.confluence.macrobrowser.macroBrowserComplete(macro);
            };
            params["server"] = this.selectedServer.name;
            params["serverId"] = this.selectedServer.id;
            
            if (AJS.Editor.inRichTextMode()) {
                insertMacroAtSelectionFromMarkup({name: 'jira', "params": params});
            } else {
                var markup = '{jira:';
                for (var key in params) {
                    markup = markup + key + '=' + params[key] + '|';
                }
                
                if (markup.charAt(markup.length - 1) == '|') {
                    markup = markup.substr(0, markup.length - 1);
                }
                
                var textArea = AJS.$("#markupTextarea");
                var selection = textArea.selectionRange();
                textArea.selectionRange(selection.start, selection.end);
                textArea.selection(markup);
                selection = textArea.selectionRange();
                textArea.selectionRange(selection.end, selection.end);
            }

            //make analytic
            if(AJS.Editor.JiraAnalytics && AJS.Editor.JiraConnector.analyticPanelActionObject) {
                AJS.Editor.JiraAnalytics.triggerPannelActionEvent(AJS.Editor.JiraConnector.analyticPanelActionObject);
                AJS.Editor.JiraConnector.analyticPanelActionObject = null;
            }
            AJS.Editor.JiraConnector.closePopup();
        },
        disableInsert: function() {
            AJS.$('.insert-issue-button').disable();
        },
        isInsertDisabled: function(){
            return AJS.$('.insert-issue-button').is(':disabled');
        },
        getOAuthRealm: function(xhr){
            var authHeader = xhr.getResponseHeader("WWW-Authenticate") || "";
            var realmRegEx = /OAuth realm\=\"([^\"]+)\"/;
            var matches = realmRegEx.exec(authHeader);
            if (matches){
                return matches[1];
            }
            else{
                return null;
            }
        },
        enableInsert: function() {
            AJS.$('.insert-issue-button').enable();
        },
        handleInsertWaiting: function(isWaiting) {
            var $insertButton = AJS.$('.insert-issue-button');
            return isWaiting ? $insertButton.before(aui.icons.icon({icon: 'wait'})) : $insertButton.prev('.aui-icon.aui-icon-wait').remove();
        },
        msg: function(container, messageObject, messageType) {
            if (aui && aui.message) {
                try {
                    var auiMessageContainer = AJS.$('<div class="aui-message-container"/>');
                    var message = messageObject;
                    if(messageObject.html) {
                        message = messageObject.html();
                    }
                    var templateParameters = {'content' : message};
                    var formattedMessage;
                    switch(messageType) {
                        case 'error':
                            formattedMessage = aui.message.error(templateParameters);
                            break;
                        case 'success':
                            formattedMessage = aui.message.success(templateParameters);
                            break;
                        case 'warning':
                            formattedMessage = aui.message.warning(templateParameters);
                            break;
                        default:
                            formattedMessage = aui.message.info(templateParameters);
                    }
                    auiMessageContainer.append(formattedMessage);
                    messageObject = auiMessageContainer;
                } catch(e) {
                    if(AJS && AJS.logError) {
                        AJS.logError('jira-connector', e);
                    }
                }
            }
            container.append(messageObject);
        },
        errorMsg: function(container, messageObject){
            this.removeError(container);
            var errorBlock = this.SHOW_MESSAGE_ON_TOP ?
                    AJS.$('<div class="jira-error"></div>').prependTo(container) : AJS.$('<div class="jira-error"></div>').appendTo(container);
            this.msg(errorBlock, messageObject, 'error');
        },
        warningMsg: function(container, messageObject){
            this.removeError(container);
            var warningBlock = this.SHOW_MESSAGE_ON_TOP ?
                    AJS.$('<div class="jira-error"></div>').prependTo(container) : AJS.$('<div class="jira-error"></div>').appendTo(container);
            this.msg(warningBlock, messageObject, 'warning');
        },
        noServerMsg: function(container, messageObject){
            var dataContainer = AJS.$('<div class="data-table jiraSearchResults" ></div>').appendTo(container);
            var messagePanel = this.SHOW_MESSAGE_ON_TOP ?
                    AJS.$('<div class="message-panel"/>').prependTo(dataContainer) : AJS.$('<div class="message-panel"/>').appendTo(dataContainer);
            this.msg(messagePanel, messageObject, 'info');
        },
        ajaxError: function(xhr, onOauthFail){
            if (xhr.status == 401){
                var authUrl = this.getOAuthRealm(xhr);
                this.selectedServer.authUrl = authUrl;
                onOauthFail.call(this);
            }
            else{
                this.errorMsg(this.container, AJS.I18n.getText("insert.jira.issue.proxy.error") + ':' + xhr.status);
            }
        },
        
        removeError: function(container){
            AJS.$('div.jira-error', container).remove();
        },
        setActionOnEnter: function(input, f, source){
            input.unbind('keydown').keydown(function(e){
                if (e.which == 13){
                    var keyup = function(e){
                        input.unbind('keyup', keyup);
                        f(source);
                        return AJS.stopEvent(e);
                    };
                    input.keyup(keyup);
                    return AJS.stopEvent(e);
                }
            });
        },
        
        createOauthForm: function(success){
            var server = this.selectedServer;
            var oauthCallbacks = {
                onSuccess: function() {
                    server.authUrl = null;
                    success(server);
                },
                onFailure: function() {
                }
            };
            var oauthMessage = '<a class="oauth-init" href="#">' + AJS.I18n.getText("insert.jira.issue.oauth.linktext") + '</a> ' + AJS.I18n.getText("insert.jira.issue.oauth.message") + ' ' + AJS.escapeHtml(this.selectedServer.name);
            var oauthForm = AJS.$('<div class="jira-oauth-message-marker"/>');
            if(!(aui && aui.message)) {
                oauthForm.addClass('oauth-message');
            }
            this.msg(oauthForm, oauthMessage, 'info');
            AJS.$('.oauth-init', oauthForm).click(function(e){
                AppLinks.authenticateRemoteCredentials(server.authUrl, oauthCallbacks.onSuccess, oauthCallbacks.onFailure);
                e.preventDefault();
            });
            return oauthForm;
        },
        
        applinkServerSelect: function(container, onchange){
            AJS.Editor.JiraConnector.serversAjax.done(function() {
                var servers = AJS.Editor.JiraConnector.servers;
                AJS.$(servers).each(function(){
                    var option = '<option ';
                    if (this.selected){
                        selectedServer = this;
                        option += 'selected="selected"';
                    }
                    option += 'value="' + this.id + '"></option>';
                    option = AJS.$(option);
                    option.text(this.name);
                    AJS.$(container).append(option);
                    option.data('jiraapplink', this);

                });
                AJS.$(container).change(function(e){
                    var option = AJS.$('option:selected', container);
                    var server = option.data('jiraapplink');
                    onchange(server);
                });
            });
        },
        showSpinner: function (element, radius, centerWidth, centerHeight){
            AJS.$.data(element, "spinner", Raphael.spinner(element, radius, "#666"));
            // helps with centering the spinner
            if (centerWidth) AJS.$(element).css('marginLeft', -radius * 1.2);
            if (centerHeight) AJS.$(element).css('marginTop', -radius * 1.2);
        },

        hideSpinner: function (element){
            AJS.$(element).css('marginTop','');
            AJS.$(element).css('marginLeft', '');
            var spinner = AJS.$.data(element, "spinner");
            if (spinner) {
                spinner();
                delete spinner;
                AJS.$.data(element, "spinner", null);
            }

        },
        
        setSelectedIssue: function(issue){
            this.selectedIssue = issue;
            this.enableInsert();
        },
        
        insertSelected: function(){
            if (this.selectedIssue){
                this.insertIssueLink(this.selectedIssue.key);
            }
        },
        createIssueTableFromUrl: function(container, appId, url, selectHandler, enterHandler, noRowsHandler, onSuccess, onError, isShowCheckBox){
            AJS.$('div.data-table', container).remove();
            
            var dataContainer = AJS.$('<div class="data-table jiraSearchResults" ></div>').appendTo(container);
            var spinnyContainer = AJS.$('<div class="loading-data"></div>').appendTo(dataContainer);
            this.removeError(container);
            this.showSpinner(spinnyContainer[0], 50, true, true);

            var thiz = this;
            this.currentXhr = AppLinks.makeRequest({
                appId: appId,
                type: 'GET',
                url: url,
                dataType: 'xml',
                success: function(data){
                    spinnyContainer.remove();
                    var issues = AJS.$('item', data);
                    AJS.$(':disabled', container).enable();
                    if (issues.length){
                        var table = AJS.$('<table class="my-result aui"></table>');

                        AJS.$('.jiraSearchResults', container).append(table);
                        var columns = [];
                        if(isShowCheckBox) {
                            var checkBoxColumn = {
                                    className : 'issue-checkbox-column',
                                    title : '<input type="checkbox" name="jira-issue-all" checked/>',
                                    renderCell : function(td, issue) {
                                        var issueCheckbox = Confluence.Templates.ConfluenceJiraPlugin.issueCheckbox({'issueKey':issue.key});
                                        AJS.$(issueCheckbox).appendTo(td);
                                    }
                                };
                                columns.push(checkBoxColumn);
                        }
                        var defaultColumns = [
                                       {
                                           className: 'issue-key-column',
                                           title:AJS.I18n.getText("jiraissues.column.key"),
                                           renderCell: function(td, issue) {
                                               var issueKey = Confluence.Templates.ConfluenceJiraPlugin.issueKey({'issueIconUrl': issue.iconUrl,'issueKey':issue.key});
                                               AJS.$(issueKey).appendTo(td);
                                           }
                                       },
                                       {
                                           className: 'issue-summary-column',
                                           title: AJS.I18n.getText("jiraissues.column.summary"),
                                           renderCell: function(td, issue){
                                               td.text(issue.summary);
                                            }
                                        }
                                       ];
                        columns = columns.concat(defaultColumns);
                        var dataTable = new AJS.DataTable(table, columns);

                        AJS.$(issues).each(function(){
                            var issue = {
                                        iconUrl: AJS.$('type', this).attr('iconUrl'),
                                        key: AJS.$('key', this).text(),
                                        summary: AJS.$('summary', this).text(),
                                        url: AJS.$('link', this).text()
                            };
                            dataTable.addRow(issue);
                        });

                        table.bind('row-action', function(e, data){
                            enterHandler.call(thiz, data);
                        });
                        table.bind('row-select', function(e, data){
                            selectHandler.call(thiz, data);
                        });
                        dataTable.selectRow(0);
                        if (onSuccess) {
                            var totalIssues = AJS.$('issue', data).attr('total');
                            onSuccess.call(thiz, totalIssues);
                        }
                    }
                    else{
                        if (noRowsHandler)noRowsHandler();
                        var message = AJS.I18n.getText("insert.jira.issue.search.noresults");
                        var messagePanel = AJS.$('<div class="message-panel"/>');
                        thiz.msg(messagePanel, message, 'info');
                        AJS.$('.jiraSearchResults', container).append(messagePanel);
                    }
                    
                },
                error: function(xhr){ 
                    AJS.$(':disabled', container).enable();
                    spinnyContainer.remove();
                    onError.call(thiz,xhr);
                }
            });
        },
        retrieveJson: function(appId, url, onSuccess, onError) {
            AppLinks.makeRequest({
                appId: appId,
                type: 'GET',
                url: url,
                dataType: 'json',
                success: onSuccess,
                error: onError
            });
        }
};
