AJS.Editor.JiraConnector.Panel.Search = function(){
    this.jql_operators = /=|!=|~|>|<|!~| is | in /i;
    this.issueKey = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
}
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, {

        title: function() {
            return AJS.I18n.getText("insert.jira.issue.search");
        },
        init: function(panel) {
            var servers = AJS.Editor.JiraConnector.servers;
            this.selectedServer = servers[0];

            var $ = AJS.$;
            panel.html('<div id="my-jira-search"></div>');
            var thiz = this;
            var container = $('#my-jira-search');
            this.container = container;

            var clearPanel = function() {
                container.children(":not(div.jira-search-form)").remove();
            };

            var enableSearch = function() {
                $('input.text', container).enable();
                $('button', container).enable();
            };

            var disableSearch = function() {
                $('button', container).disable();
                $('input.text', container).disable();
            };

            var authCheck = function(server) {
                if (server)
                    thiz.selectedServer = server;
                if (thiz.selectedServer.authUrl) {
                    disableSearch();
                    clearPanel();
                    var oauthForm = thiz.createOauthForm(function() {
                        clearPanel();
                        enableSearch();
                    });
                    container.append(oauthForm);
                }
                else{
                    clearPanel();
                    enableSearch();
                    $('.search-help').show();
                }
            };

            this.authCheck = authCheck;

            var doSearch = function(searchStr, serverName) {
                if (searchStr) {
                    $('input:text', container).val(searchStr);
                }
                if (serverName && serverName != this.selectedServer.name) {
                    var servers = AJS.Editor.JiraConnector.servers;
                    for (var i = 0; i < servers.length; i++){
                        if (servers[i].name == serverName){
                            $('option[value="' + servers[i].id + '"]', container).attr('selected', 'selected');
                            $('select', container).change();
                            break;
                        }
                    }
                }
                if (this.currentXhr && this.currentXhr.readyState != 4) {
                    return;
                }
                var queryTxt = searchStr || $('input', container).val();

                var performQuery = function(jql, single, fourHundredHandler) {
                    $('select', container).disable();
                    disableSearch();
                    thiz.lastSearch = jql;
                    thiz.createIssueTableFromUrl(container, 
                        thiz.selectedServer.id, 
                        '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jql) + '&tempMax=20&field=summary&field=type&field=link',
                        function() {},
                        thiz.insertLink, 
                        thiz.enableInsert, // <-- noRowsHandler: enable insert button for no issue 
                        function() {
                            thiz.addDisplayOptionPanel();
                            thiz.loadMacroParams();
                            thiz.bindEventToDisplayOptionPanel();
                        },
                        function(xhr) {
                            thiz.disableInsert();
                            if (xhr.status == 400) {
                              if (fourHundredHandler) {
                                  fourHundredHandler();
                              } else {
                                  $('div.data-table', container).remove();
                                  thiz.errorMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest"));
                              }
                            } else {
                                $('div.data-table', container).remove();
                                thiz.ajaxError(xhr, authCheck);
                            }
                    });
                };

                if (queryTxt.match(thiz.jql_operators)) {
                    performQuery(queryTxt, false, null);
                } else {
                    // issue keys are configurable in JIRA so we can't reliably detect one here instead issue two queries. 
                    // The first will be as an issue key, and if JIRA returns a 400 then it did not recognise the key so 
                    // we then try the second.
                    performQuery('issuekey in (' + queryTxt + ')', true, function() {
                        performQuery('summary ~ "' + queryTxt + '" OR description ~ "' + queryTxt + '"', false, null);
                    });
                }
            };
            this.doSearch = doSearch;

            //get searchform from soy template
            var searchFormtest = Confluence.Templates.ConfluenceJiraPlugin.searchForm;
            var searchForm = $(searchFormtest()).appendTo(container);

            if (servers.length > 1) {
                var serverSelect = $('<select class="select" tabindex="0"></select>').insertAfter('div.search-input', searchForm);
                thiz.applinkServerSelect(serverSelect, authCheck);
                $('input.one-server', searchForm).removeClass('one-server');
            }
            authCheck(this.selectedServer);
            $('button', container).click(function(){doSearch();});
            this.setActionOnEnter($('input', container), doSearch);    

            $(panel).select(function() {
                thiz.validate();
            });
        },
        validate: function() {
            var container = this.container;
            var issueResult = AJS.$('input:checkbox[name=jira-issue]', container);

            if(issueResult.length) {
                var selectedIssueCount = AJS.$('input:checkbox[name=jira-issue]:checked', container).length;
                if(selectedIssueCount == 0) {
                    this.disableInsert();
                }
                else {
                    this.enableInsert();
                }
                this.changeInsertOptionStatus(selectedIssueCount);
            }
            else {
                if (AJS.$('.jira-oauth-message-marker', container).length) {
                    this.authCheck(this.selectedServer);
                }
                AJS.$('input', container).focus();
                this.disableInsert();
            }
        },
        setMacroParams: function(params) {
            this.macroParams = params;
        },
        getMacroParamsFromUserInput: function() {
            // get value from dialog
            var isCount = ((AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-count") ? true : false);
            var container = this.container;
            var selectedIssueKeys = new Array();
            var unselectIssueKeys = new Array();
            AJS.$('#my-jira-search .my-result.aui input:checkbox[name=jira-issue]').each(function(i) {
            	var checkbox = AJS.$(this);
                if(checkbox.is(':checked')) {
                    selectedIssueKeys[selectedIssueKeys.length] = checkbox.val();
                }
                else {
                    unselectIssueKeys[unselectIssueKeys.length] = checkbox.val();
                }
            });

            // prapare macro param data
            var macroInputParams = {};

            if(isCount) {
                macroInputParams['count'] = 'true';
            }
            else {
                macroInputParams["columns"] = AJS.$("#jiraIssueColumnSelector").val().join(",");
            }

            if(selectedIssueKeys.length == 1) {
                // display count when select 1 issue with count
                macroInputParams['key'] = selectedIssueKeys.toString();
            }
            //add param macro for jql when select all checked
            else if (unselectIssueKeys.length == 0) {
                macroInputParams['jqlQuery'] = this.lastSearch;
            }
            else {
                var keyInJql = 'key in (' + selectedIssueKeys.toString() + ')';
                macroInputParams['jqlQuery'] = keyInJql;
            }

            return macroInputParams;
        },
        insertLink: function() {
            var macroInputParams = this.getMacroParamsFromUserInput();
            this.insertIssueLinkWithParams(macroInputParams);
        },
        loadMacroParams: function() {
            
            var macroParams = this.macroParams;
            if (!macroParams) {
                this.prepareColumnInput("issuekey, summary, issuetype, created, updated, duedate, assignee, reporter, priority, status, resolution");
            	return;
            }
            if(macroParams["count"] == "true") {
                AJS.$("#opt-total").prop("checked", true);
            }
            else {
                AJS.$("#opt-table").prop("checked", true);
            }
            this.prepareColumnInput(macroParams["columns"]);
        },
        addDisplayOptionPanel: function() {
            //get content from soy template
            var displayOptsHtml = Confluence.Templates.ConfluenceJiraPlugin.displayOptsHtml;
            var displayOptsOverlayHtml = Confluence.Templates.ConfluenceJiraPlugin.displayOptsOverlayHtml;
            AJS.$(".jiraSearchResults").after(displayOptsHtml()).after(displayOptsOverlayHtml());
            
        },
        prepareColumnInput: function(selectedColumnString) {
            
            var selectedColumnValues = [];
            var selectedColumnMap = {};
            selectedColumnString = selectedColumnString.replace(/\s+/g, '');    
            selectedColumnValues = selectedColumnString.split(",");
            for(var i = 0; i < selectedColumnValues.length; i++) {
                selectedColumnMap[selectedColumnValues[i]] = true;
            }            
            var server = this.selectedServer;
            var initColumnInputField = function(data) {
                var columnInputField = AJS.$("#jiraIssueColumnSelector");
                columnInputField.html("");
                var optionStrings = "";
                
                for (var i=0; i<data.length; i++) {
                    var columnValue = data[i].id;
                    if(selectedColumnMap[columnValue] == true) {
                        optionStrings += "<option selected='true' value='" + data[i].id + "'>" + data[i].name + "</option>";
                        
                    } else {
                        optionStrings += "<option value='" + data[i].id + "'>" + data[i].name + "</option>";    
                    }
                }
                columnInputField.html(optionStrings);
               
                
                if (columnInputField.hasClass("chzn-done")) {
                    columnInputField.trigger("liszt:updated");
                } else {
                    //TODO: The Chosen plugin cannot support 100% width as it should. 
                    columnInputField.chosen({"selected_values_in_order": selectedColumnValues});
                }
                
            };
            if (server.columns && server.columns.length > 0) {
                initColumnInputField(server.columns);
                return;
            }
            this.retrieveJson(server.id, "/rest/api/2/field",
                function(data) {
                    if (data && data.length) {
                        server.columns = data;
                        initColumnInputField(server.columns);
                    }
                }
            );

          

        },
        // bind event for new layout
        bindEventToDisplayOptionPanel: function() {
            var thiz = this;
            var displayOptsCloseBtn = AJS.$('.jql-display-opts-close'),
            displayOptsOpenBtn = AJS.$('.jql-display-opts-open'),
            displayOptsOverlay = AJS.$('.jql-display-opts-overlay'),
            optDisplayRadios = AJS.$('.jql-display-opts-inner .radio'),
            columnsDisplayInput = AJS.$('input:text[name=columns-display]'),
            optTotalRadio = AJS.$('#opt-total'),
            ticketCheckboxAll = AJS.$('#my-jira-search input:checkbox[name=jira-issue-all]'),
            ticketCheckboxes = AJS.$('#my-jira-search input:checkbox[name=jira-issue]'),
            insertButton = AJS.$('.insert-issue-button');

            displayOptsCloseBtn.click(function() {
            	displayOptsOverlay.hide();
            });
            displayOptsOpenBtn.click(function() {
            	displayOptsOverlay.show()
            });

            optDisplayRadios.change(function() {
                if(optTotalRadio.prop('checked')) {
                    AJS.$("#jiraIssueColumnSelector").attr('disabled', true).trigger("liszt:updated");
                } else {
                    AJS.$("#jiraIssueColumnSelector").removeAttr('disabled').trigger("liszt:updated");;
                }
            });

            ticketCheckboxAll.bind('click',function() {
                var all = AJS.$(this);
                if(all.prop('checked')) {
                    ticketCheckboxes.prop('checked','checked');
                } else {
                    ticketCheckboxes.removeAttr('checked');
                }
                thiz.validate();
            });

            ticketCheckboxes.change(function() {
                thiz.validate();
                var ticketUncheckedLength = AJS.$('#my-jira-search input:checkbox[name=jira-issue]:not(:checked)').length;
                if(ticketUncheckedLength > 0) {
                    ticketCheckboxAll.removeAttr('checked');
                } else {
                    ticketCheckboxAll.prop('checked','checked');
                }
            });
            
            thiz.changeInsertOptionStatus();
            thiz.validate();
        },
        changeInsertOptionStatus: function(selectedIssueCount) {
            // enable insert option
            if(selectedIssueCount != 1) {
                // enable insert option
                AJS.$("#opt-total").removeAttr('disabled');
                AJS.$("#opt-table").removeAttr('disabled');
                AJS.$('input:text[name=columns-display]').attr('disabled','disabled');
                if(AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-table"){
                    AJS.$("#jiraIssueColumnSelector").removeAttr('disabled').trigger("liszt:updated");
                }
            }
            else {
                AJS.$("#opt-total").attr('disabled','disabled');
                AJS.$("#opt-table").attr('disabled','disabled');
                AJS.$("#jiraIssueColumnSelector").attr('disabled', true).trigger("liszt:updated");
                // auto slide down when only check 1 
                AJS.$('.jql-display-opts-overlay').hide();
            }
        }
    });
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Search());
