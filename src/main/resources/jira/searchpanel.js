AJS.Editor.JiraConnector.Panel.Search = function() {
    this.jql_operators = /=|!=|~|>|<|!~| is | in /i;
};

AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, {

        title: function() {
            return AJS.I18n.getText("insert.jira.issue.search");
        },
        init: function(panel) {

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
                clearPanel();
                // disable insert when authCheck
                thiz.disableInsert();
                if (server)
                    thiz.selectedServer = server;
                if (thiz.selectedServer.authUrl) {
                    disableSearch();
                    var oauthForm = thiz.createOauthForm(function() {
                        clearPanel();
                        enableSearch();
                    });
                    container.append(oauthForm);
                }
                else{
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
                    var isServerExist = false;
                    var servers = AJS.Editor.JiraConnector.servers;
                    for (var i = 0; i < servers.length; i++){
                        if (servers[i].name == serverName){
                            $('option[value="' + servers[i].id + '"]', container).attr('selected', 'selected');
                            $('select', container).change();
                            isServerExist = true;
                            break;
                        }
                    }

                    if(!isServerExist) {
                        showNoServerMessage(AJS.Meta.get("is-admin"));
                        return;
                    }
                }
                if (this.currentXhr && this.currentXhr.readyState != 4) {
                    return;
                }
                var queryTxt = searchStr || $('input', container).val();

                // analytics stuff
                var type = AJS.JQLHelper.checkQueryType(queryTxt);
                if (type) {
                    AJS.Editor.JiraConnector.Analytics.triggerSearchEvent({
                        type : type,
                        source : 'dialog' 
                    });
                }

                var performQuery = function(jql, single, fourHundredHandler) {
                    $('select', container).disable();
                    disableSearch();
                    thiz.lastSearch = jql;
                    thiz.createIssueTableFromUrl(container, 
                        thiz.selectedServer.id, 
                        '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jql) + '&tempMax=20&field=summary&field=type&field=link',
                        thiz.selectHandler,
                        thiz.insertLinkFromForm,
                        function() { // <-- noRowsHandler
                            thiz.addDisplayOptionPanel();
                            thiz.changeInsertOptionStatus(0);
                            thiz.enableInsert();
                        },
                        function(totalIssues) {
                            thiz.addDisplayOptionPanel();
                            thiz.loadMacroParams();
                            thiz.bindEventToDisplayOptionPanel();
                            thiz.updateTotalIssuesDisplay(totalIssues);
                        },
                        function(xhr) {
                            thiz.disableInsert();
                            if (xhr.status == 400) {
                              if (fourHundredHandler) {
                                  fourHundredHandler();
                              } else {
                                  $('div.data-table', container).remove();
                                  thiz.warningMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest", Confluence.Templates.ConfluenceJiraPlugin.learnMore()));
                              }
                            } else {
                                $('div.data-table', container).remove();
                                thiz.ajaxError(xhr, authCheck);
                            }
                        },
                        true); // <-- add checkbox column
                };

                if(AJS.JQLHelper.isFilterUrl(queryTxt)) {
                    var url = decodeURIComponent(queryTxt);
                    var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
                    if( serverIndex != -1) {
                        var appLinkId = AJS.Editor.JiraConnector.servers[serverIndex].id;
                        AJS.$('option[value="' + appLinkId + '"]', container).attr('selected', 'selected');
                        AJS.$('select', container).change();

                        var onSuccess = function(responseData) {
                            if (responseData.jql) {
                                $('input', container).val(responseData.jql);
                                performQuery(responseData.jql);
                            }
                            else {
                                if(responseData.jql == "") {
                                    $('input', container).val("");
                                }
                                clearPanel();
                                thiz.warningMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest", Confluence.Templates.ConfluenceJiraPlugin.learnMore()));
                            }
                        };

                        var onError = function(xhr) {
                            if(xhr.status = 401) {
                                $('div.data-table', container).remove();
                                thiz.ajaxError(xhr, authCheck);
                            }
                            clearPanel();
                            thiz.warningMsg(container,  AJS.I18n.getText("insert.jira.issue.message.nofilter"));
                        }
                        AJS.JQLHelper.getJqlQueryFromJiraFilter(url, appLinkId, onSuccess, onError);
                    }
                    else {
                        clearPanel();
                        thiz.disableInsert();
                        showNoServerMessage(AJS.Meta.get("is-admin"));
                    }
                }
                // url/url xml
                else if(AJS.JQLHelper.isIssueUrlOrXmlUrl(queryTxt)) {
                    var url = decodeURIComponent(queryTxt); 
                    var jiraParams = AJS.JQLHelper.getJqlAndServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
                    if(processJiraParams(jiraParams)) {
                        $('input', container).val(jiraParams["jqlQuery"]);
                        performQuery(jiraParams["jqlQuery"], false, null);
                    }
                }
                else {
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
                }
            };
            
            this.doSearch = doSearch;
            thiz.addSearchForm();

            var processJiraParams = function(jiraParams) {
                var jql;
                if(jiraParams["serverIndex"] != -1) {
                    AJS.$('option[value="' + AJS.Editor.JiraConnector.servers[jiraParams["serverIndex"]].id + '"]', container).attr('selected', 'selected');
                    AJS.$('select', container).change();
                    if(jiraParams["jqlQuery"].length == 0) {
                        // show error msg for no JQL - CONFVN-79
                        clearPanel();
                        thiz.errorMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest"));
                    } else {
                        jql = jiraParams["jqlQuery"];
                    }
                }
                else {
                    clearPanel();
                    thiz.disableInsert();
                    showNoServerMessage(AJS.Meta.get("is-admin"));
                }
                return jql;
            }

            var showNoServerMessage = function(isAdmin) {
                var message = Confluence.Templates.ConfluenceJiraPlugin.showMessageNoServer({'isAdministrator':isAdmin, 'contentPath':Confluence.getContextPath()})
                thiz.noServerMsg(container, message);
                
                // bind click for call refresh applink select when user click on open applink config 
                var open_applinks = AJS.$("#open_applinks");
                open_applinks.bind('click', function() {
                    AJS.Editor.JiraConnector.clickConfigApplink = true;
                    // refreshAppLink will be used when open dialog
                    AJS.Editor.JiraConnector.refreshAppLink = function() {
                        thiz.refreshSearchForm();
                    }
                });
            };

            //auto convert URL to JQL
            AJS.$("#my-jira-search input:text").bind('paste', function () {
                var element = this;
                setTimeout(function () {
                    var textSearch = AJS.$(element).val();
                    if(AJS.JQLHelper.isFilterUrl(textSearch)) {
                        doSearch();
                    }
                    else if(AJS.JQLHelper.isIssueUrlOrXmlUrl(textSearch)) {
                        var url = decodeURIComponent(textSearch); 
                        var jiraParams = AJS.JQLHelper.getJqlAndServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
                        if(processJiraParams(jiraParams)) {
                            AJS.$(element).val(jiraParams["jqlQuery"]);
                            //for auto search when paste url
                            thiz.doSearch();
                        }
                    }
                }, 100);
            });
            
            $(panel).select(function() {
                thiz.validate();
            });
            // add tipsy tooltip
            var $optionsPanel = AJS.$('.jql-display-opts-open.disabled');
            var tipsyOptions = {
                live: true,
                title: function() {return AJS.$('.jql-display-opts-open.disabled').data('title')},
                gravity: 's', // Point the arrow to the top
                delayIn: 300,
                delayOut: 0
            };
            $optionsPanel.tooltip(tipsyOptions);
        },
        addSearchForm: function() {
            var thiz = this;
            thiz.container.empty();
            var servers = AJS.Editor.JiraConnector.servers;
            thiz.selectedServer = servers[0];
            var isMultiServer = false;
            if (servers.length > 1) {
                isMultiServer = true;
            }
            //get searchform from soy template
            var searchFormSoy = Confluence.Templates.ConfluenceJiraPlugin.searchForm({'isMultiServer':isMultiServer});
            var searchForm = AJS.$(searchFormSoy).appendTo(thiz.container);

            if (servers.length > 1) {
                var serverSelect = $('<select class="select" tabindex="0"></select>').insertAfter('div.search-input', searchForm);
                thiz.applinkServerSelect(serverSelect, thiz.authCheck);
            }
            thiz.authCheck(thiz.selectedServer);
            
            AJS.$('button', thiz.container).click(function() {
                thiz.doSearch();
            });
            thiz.setActionOnEnter($('input', thiz.container), thiz.doSearch);
        },
        refreshSearchForm: function() {
            this.container.empty();
            this.addSearchForm();
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
        customizedColumn : null,
        /*
         * This function is used for splitting a column string to array, ex: Custom Columns, key.
         * Note: If a column include space in need to be put into quotes. Ex: "word1, word2".
         * */
        parseColumnsStringToArray: function(columnsString) {
            var column = "";
            var result = [];
            var currentQuote = null;
            var pushColumnToArray = function (col, arr) {
                col = AJS.$.trim(col);
                if(col != "") {
                    arr.push(col);    
                }
            };
            for (var i=0;i<columnsString.length;i++) {
                var currentChar = columnsString.charAt(i);
                if(currentChar !=="," && currentChar !=="\"" && currentChar !=="'") {
                    column += currentChar;
                }
                // check for the open and close quote
                if(currentChar ==="\"" || currentChar ==="'") {
                    if(currentQuote == null) {
                        currentQuote = currentChar;
                    } else if (currentQuote === currentChar) {
                        //here we see the closed quote character, so we put the column  
                        //into the array and reset every thing
                        pushColumnToArray(column, result);
                        column = "";
                        currentQuote = null;
                    } 
                } else if(currentChar === ",") {
                    if(currentQuote == null) {
                        pushColumnToArray(column, result);
                        column = "";
                    } else {
                        column += currentChar;
                    }
                }
            }
            //add the last column if it was not pushed into array in the loop
            pushColumnToArray(column, result);
            return result;
        },
        /*
         * This function is used for concatting column in an array
         * into the column string of the macro.
         * */
        parseArrayToColumnString: function(columnArray) {
            var result = "";
            for (var i=0;i<columnArray.length;i++) {
                var column = AJS.$.trim(columnArray[i]);
                if (column != "") {
                    if(column.indexOf(",")>=0) {
                        column = "\"" + column + "\"";
                    }
                    if(i != 0) {
                        result += ",";
                    }
                    result += column;
                }
            }
        },
        setMacroParams: function(params) {
            this.macroParams = params;
        },
        getMacroParamsFromUserInput: function() {
            // get value from dialog
            var isCount = ((AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-count") ? true : false);
            var container = this.container;
            var columns = AJS.$('input:text[name=columns-display]', container).val();

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
                // count param only available when have more 1 issue
                if(selectedIssueKeys.length != 1) {
                    macroInputParams['count'] = 'true';
                }
            }
            else if(typeof(columns) != 'undefined') {
                columns = columns.replace(/\s*,\s*/g, ',');
                if(columns.length > 0) {
                    macroInputParams["columns"] = columns;
                }
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
        insertLinkFromForm : function() {
            var thiz = this;
            var container = thiz.container;
            var issueResult = AJS.$('input:checkbox[name=jira-issue]', container);
            if(issueResult.length) {
                var selectedIssueCount = AJS.$('input:checkbox[name=jira-issue]:checked', container).length;
                if(selectedIssueCount > 0) {
                    thiz.insertLink();
                }
            }
        },
        insertLink: function(_searchPanel) {
            var searchPanel;
            if (_searchPanel && typeof _searchPanel.insertIssueLinkWithParams === 'function') {
                searchPanel = _searchPanel;
            } else {
                searchPanel = this;
            }
            var macroInputParams = searchPanel.getMacroParamsFromUserInput();
            searchPanel.insertIssueLinkWithParams(macroInputParams);
        },
        loadMacroParams: function() {
            var macroParams = this.macroParams;
            if (!macroParams) {
                return;
            }
            if(macroParams['count'] == 'true') {
                AJS.$('#opt-total').prop('checked', true);
            }
            else {
                AJS.$('#opt-table').prop('checked', true);
            }
            //load columns table
            if(macroParams['columns'] != null) {
                AJS.$('.jql-display-opts-inner input:text', this.container).val(macroParams['columns']);
            }
        },
        selectHandler : function() {
            var cont = this.container;
            var selectedRow = cont.find('tr.selected');
            if (selectedRow.length) {
                selectedRow.unbind('keydown.space').bind('keydown.space', function(e){
                    if (e.which == 32 || e.keyCode == 32){
                      var inpChk = selectedRow.find('[type=checkbox]');
                      inpChk.trigger('click');
                    }
                });
            }
        },
        addDisplayOptionPanel: function() {
            //get content from soy template
            var thiz = this;
            var displayOptsHtml = Confluence.Templates.ConfluenceJiraPlugin.displayOptsHtml;
            var displayOptsOverlayHtml = Confluence.Templates.ConfluenceJiraPlugin.displayOptsOverlayHtml;
            AJS.$(".jiraSearchResults")
            //.after(displayOptsHtml())
            .after(displayOptsOverlayHtml());
            thiz.setActionOnEnter($('.jql-display-opts-inner input:text'), thiz.insertLink, thiz);
        },
        updateTotalIssuesDisplay: function (totalIssues) {
            var jiraIssuesLink = this.selectedServer.url + '/issues/?jql=' + this.lastSearch;
            //add infor view all
            if(totalIssues > 20) {
                AJS.$(".my-result.aui").after(Confluence.Templates.ConfluenceJiraPlugin.viewAll({'jiraIssuesLink':jiraIssuesLink}));
            }
            //update total issues display
            var totalIssuesText = AJS.I18n.getText('insert.jira.issue.option.count.sample', totalIssues);
            AJS.$('.total-issues-text').html(totalIssuesText);
            // update link for total issues link to jira
            AJS.$('.total-issues-link').attr('href', jiraIssuesLink);
        },
        // bind event for new layout
        bindEventToDisplayOptionPanel: function() {
            var thiz = this;
            var displayOptsBtn = AJS.$('.jql-display-opts-close, .jql-display-opts-open'),
            displayOptsOverlay = AJS.$('.jql-display-opts-overlay'),
            optDisplayRadios = AJS.$('.jql-display-opts-inner .radio'),
            columnsDisplayInput = AJS.$('input:text[name=columns-display]'),
            optTotalRadio = AJS.$('#opt-total'),
            ticketCheckboxAll = AJS.$('#my-jira-search input:checkbox[name=jira-issue-all]'),
            ticketCheckboxes = AJS.$('#my-jira-search input:checkbox[name=jira-issue]');
            
            displayOptsBtn.click(function(e) {
                e.preventDefault();
                if($(this).hasClass("disabled")) {
                    return;
                }
                var isOpenButton = $(this).hasClass('jql-display-opts-open');
                if (isOpenButton) {
                    displayOptsOverlay.animate({
                        bottom: 0
                    }, 500 );
                    jQuery(this).addClass('jql-display-opts-close');
                    jQuery(this).removeClass('jql-display-opts-open');
                } else {
                    displayOptsOverlay.animate({
                        bottom: -152
                    }, 500 );
                    jQuery(this).removeClass('jql-display-opts-close');
                    jQuery(this).addClass('jql-display-opts-open');
                }
            });

            optDisplayRadios.change(function() {
                if(optTotalRadio.prop('checked')) {
                    columnsDisplayInput.attr('disabled','disabled');
                } else {
                    columnsDisplayInput.removeAttr('disabled');
                }
            });

            columnsDisplayInput = AJS.$('input:text[name=columns-display]').change(function(){
                thiz.customizedColumn = columnsDisplayInput.val();
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
            thiz.validate();
        },
        changeInsertOptionStatus: function(selectedIssueCount) {
            // enable insert option
            if(selectedIssueCount > 1) {
                // enable insert option
                AJS.$("#opt-total").removeAttr('disabled');
                AJS.$("#opt-table").removeAttr('disabled');
                AJS.$('input:text[name=columns-display]').attr('disabled','disabled');
                if(AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-table"){
                    AJS.$('input:text[name=columns-display]').removeAttr('disabled');
                }
                AJS.$('.jql-display-opts-open').removeClass("disabled");
            }
            else {
                AJS.$("#opt-total").attr('disabled','disabled');
                AJS.$("#opt-table").attr('disabled','disabled');
                AJS.$('.jql-display-opts-close').click();
                AJS.$('input:text[name=columns-display]').attr('disabled','disabled');
                AJS.$('.jql-display-opts-open').addClass("disabled");
           }
        }
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Search());
