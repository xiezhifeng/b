AJS.Editor.JiraConnector.Panel = function() {};

AJS.Editor.JiraConnector.Panel.prototype = {
        
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
            var $ = AJS.$;
            
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
                
                var textArea = $("#markupTextarea");
                var selection = textArea.selectionRange();
                textArea.selectionRange(selection.start, selection.end);
                textArea.selection(markup);
                selection = textArea.selectionRange();
                textArea.selectionRange(selection.end, selection.end);                
            }
            
            AJS.Editor.JiraConnector.closePopup();            
        },
        disableInsert: function(){        	
            AJS.$('.insert-issue-button').disable();
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
        enableInsert: function(){        	
            AJS.$('.insert-issue-button').enable();
        },
        msg: function(container, messageObject, messageType) {
            if(atlassian && atlassian.message) {
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
                            formattedMessage = atlassian.message.error(templateParameters);
                            break;
                        case 'success': 
                            formattedMessage = atlassian.message.success(templateParameters);
                            break;
                        case 'warning': 
                            formattedMessage = atlassian.message.warning(templateParameters);
                            break;
                        default:
                            formattedMessage = atlassian.message.info(templateParameters);
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
            var errorBlock = AJS.$('<div class="jira-error"></div>').appendTo(container);
            this.msg(errorBlock, messageObject, 'error');
        },
        noServerMsg: function(container, messageObject){
            var dataContainer = $('<div class="data-table jiraSearchResults" ></div>').appendTo(container);
            var messagePanel = AJS.$('<div class="message-panel"/>').appendTo(dataContainer);
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
        setActionOnEnter: function(input, f){
            input.keydown(function(e){
                if (e.which == 13){
                    var keyup = function(e){
                        input.unbind('keyup', keyup);
                        f();
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
            var oauthMessage = '<a class="oauth-init" href="#">' + AJS.I18n.getText("insert.jira.issue.oauth.linktext") + '</a> ' + AJS.I18n.getText("insert.jira.issue.oauth.message") + ' ' + this.selectedServer.name;
            var oauthForm = AJS.$('<div/>');
            if(!(atlassian && atlassian.message)) {
                oauthForm.addClass('oauth-message')
            }
            this.msg(oauthForm, oauthMessage, 'info');
            AJS.$('.oauth-init', oauthForm).click(function(e){
                AppLinks.authenticateRemoteCredentials(server.authUrl, oauthCallbacks.onSuccess, oauthCallbacks.onFailure);
                e.preventDefault();
            });
            return oauthForm;
        },
        
        applinkServerSelect: function(container, onchange){
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
        createIssueTableFromUrl: function(container, appId, url, selectHandler, enterHandler, noRowsHandler, onSuccess, onError){
            var $ = AJS.$;
            $('div.data-table', container).remove();        
            
            var dataContainer = $('<div class="data-table jiraSearchResults" ></div>').appendTo(container);
            var spinnyContainer = $('<div class="loading-data"></div>').appendTo(dataContainer);
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
                    var issues = $('item', data);
                    AJS.$(':disabled', container).enable();
                    if (issues.length){
                        var table = $('<table class="my-result aui"></table>');

                        $('.jiraSearchResults', container).append(table);
                        
                        var columns = [// start: update for new jira plugin
                                       {className: 'issue-checkbox-column',
                                         title:'<input type="checkbox" name="jira-issue-all" checked/>',
                                     renderCell: function(td, issue){
                                       $('<input type="checkbox" name="jira-issue" value="' + issue.key +'" checked />').appendTo(td); 
                                     }
                                     },
                                     // end: update for new jira plugin
                                     {className: 'issue-key-column',
                                        title:'Key',
                                        renderCell: function(td, issue){
                                            $('<span style="background-repeat:no-repeat;background-image: url(\'' + issue.iconUrl + '\');padding-left:20px;padding-bottom:2px;" ></span>').appendTo(td).text(issue.key);
                                        }
                                        },
                                        {className: 'issue-summary-column',
                                         title: 'Summary',
                                         renderCell: function(td, issue){
                                            td.text(issue.summary);
                                         }
                                        }
                                       ];
                        var dataTable = new AJS.DataTable(table, columns);
                        
                        var selection;
                        
                        $(issues).each(function(){
                            var issue = {
                                        iconUrl:$ ('type', this).attr('iconUrl'),
                                        key: $('key', this).text(),
                                        summary: $('summary', this).text(),
                                        url: $('link', this).text()
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
                            onSuccess.call(thiz);
                        }
                    }
                    else{
                        if (noRowsHandler)noRowsHandler();
                        var message = AJS.I18n.getText("insert.jira.issue.search.noresults");
                        var messagePanel = AJS.$('<div class="message-panel"/>');
                        thiz.msg(messagePanel, message, 'info');
                        $('.jiraSearchResults', container).append(messagePanel);
                    }
                    
                },
                error: function(xhr){ 
                    AJS.$(':disabled', container).enable();
                    spinnyContainer.remove();
                    onError.call(thiz,xhr);
                }
            });
        }
};
