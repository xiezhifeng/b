AJS.Editor.JiraConnector.Panel.Search = function() {
    this.jql_operators = /=|!=|~|>|<|!~| is | in /i;
};
AJS.Editor.JiraConnector.Select2 = AJS.Editor.JiraConnector.Select2 || {};

AJS.Editor.JiraConnector.Select2.getKeyColumnsSelectedOptions = function(jiraColumnSelectBox) {
    var result = [];
    var objects = jiraColumnSelectBox.select2('data');
    for(var i = 0; i < objects.length; i++) {
        result[i] = objects[i].id;
    }
    return result.join();
};

AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, {
        defaultColumns : "key,summary,type,created,updated,due,assignee,reporter,priority,status,resolution",
        DEFAULT_MAX_ISSUES_VAL : 20,
        MAXIMUM_MAX_ISSUES_VAL : 1000,
        MINIMUM_MAX_ISSUES_VAL : 1,
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
                if (AJS.Editor.JiraAnalytics) {
                    var type = AJS.JQLHelper.checkQueryType(queryTxt);
                    if (type) {
                        AJS.Editor.JiraAnalytics.triggerSearchEvent({
                            type : type,
                            source : 'dialog' 
                        });
                    }
                }

                var performQuery = function(jql, single, fourHundredHandler) {
                    var $columnSelector = container.find("#jiraIssueColumnSelector");
                    var selectedColumns = $columnSelector.val() && $columnSelector.val().join();
                    $('select', container).disable();
                    disableSearch();
                    thiz.lastSearch = jql;
                    thiz.createIssueTableFromUrl(container, 
                        thiz.selectedServer.id, 
                        '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jql) + '&returnMax=true&tempMax=20&field=summary&field=type&field=link',
                        thiz.selectHandler,
                        thiz.insertLinkFromForm,
                        function() { // <-- noRowsHandler
                            thiz.addDisplayOptionPanel();
                            thiz.loadMacroParams(selectedColumns);
                            thiz.bindEventToDisplayOptionPanel(true); // still enable display option if the jql is legal but no results found
                            thiz.enableInsert();
                        },
                        function(totalIssues) {
                            thiz.addDisplayOptionPanel();
                            thiz.loadMacroParams(selectedColumns);
                            thiz.bindEventToDisplayOptionPanel();
                            thiz.updateTotalIssuesDisplay(totalIssues);
                            thiz.checkAutoSelectColumns();
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

                var successGetJqlFromJiraFilterHandler = function(responseData) {
                    if(responseData.errors) {
                        clearPanel();
                        thiz.warningMsg(container,  AJS.I18n.getText("insert.jira.issue.message.nofilter"));
                    }
                    else if (responseData.jql) {
                        $('input', container).val(responseData.jql);
                        performQuery(responseData.jql);
                    }
                    else {
                        clearPanel();
                        thiz.warningMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest", Confluence.Templates.ConfluenceJiraPlugin.learnMore()));
                    }
                };

                if(AJS.JQLHelper.isFilterUrl(queryTxt)) {
                    var url = decodeURIComponent(queryTxt);
                    var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
                    if( serverIndex != -1) {
                        var appLinkId = AJS.Editor.JiraConnector.servers[serverIndex].id;
                        AJS.$('option[value="' + appLinkId + '"]', container).attr('selected', 'selected');
                        AJS.$('select', container).change();

                        var filterJql = AJS.JQLHelper.getFilterFromFilterUrl(url);
                        if (filterJql) {
                            $('input', container).val(filterJql);
                            performQuery(filterJql);
                        }
                        else {
                            clearPanel();
                            thiz.warningMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest", Confluence.Templates.ConfluenceJiraPlugin.learnMore()));
                        }
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
                        if (AJS.JQLHelper.isSingleKeyJQLExp(queryTxt)) {
                            performQuery('key = ' + queryTxt, true);
                        } else if (AJS.JQLHelper.isMultipleSingleKeyJQLExp(queryTxt)) {
                            performQuery('key in (' + queryTxt + ')', true);
                        } else {
                            performQuery('summary ~ "' + queryTxt + '" OR description ~ "' + queryTxt + '"', false, null);
                        }
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
            };

            var showNoServerMessage = function(isAdmin) {
                var message = Confluence.Templates.ConfluenceJiraPlugin.showMessageNoServer({'isAdministrator':isAdmin, 'contextPath':Confluence.getContextPath()});
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
            thiz.setActionOnEnter($('input.text', thiz.container), thiz.doSearch);
        },
        refreshSearchForm: function() {
            this.container.empty();
            this.addSearchForm();
        },
        validate: function(acceptNoResult) {
            var container = this.container;
            var issueResult = AJS.$('input:checkbox[name=jira-issue]', container);
            var searchPanel = AJS.Editor.JiraConnector.Panel.Search.prototype;
            if(issueResult.length || acceptNoResult) {
                var selectedIssueCount = AJS.$('input:checkbox[name=jira-issue]:checked', container).length;
                if(selectedIssueCount > 0 || acceptNoResult) {
                    searchPanel.enableInsert();
                }
                else {
                    searchPanel.disableInsert();
                }
                searchPanel.changeInsertOptionStatus(selectedIssueCount, acceptNoResult);
            }
            else {
                if (AJS.$('.jira-oauth-message-marker', container).length) {
                    searchPanel.authCheck(this.selectedServer);
                }
                AJS.$('input', container).focus();
                searchPanel.disableInsert();
            }
            
            if (searchPanel.isInsertDisabled()) {
                return;
            }
            
            searchPanel.validateMaxIssues();
        },
        validateMaxIssues : function(e) {
            
            var $element = AJS.$('#jira-maximum-issues');
            
            function clearMaxIssuesWarning() {
                $element.next('#jira-max-number-error').remove();
            }
            function disableMaxIssuesTextBox() {
                $element.attr('disabled','disabled');
            }
            function enableMaxIssuesTextBox() {
                $element.removeAttr('disabled');
            }
            function showMaxIssuesWarning() {
                clearMaxIssuesWarning();
                $element.after(Confluence.Templates.ConfluenceJiraPlugin.warningValMaxiumIssues());
            }
            
            switch ($("input:radio[name=insert-advanced]:checked").val()) {
                case "insert-single" :
                case "insert-count" :
                    clearMaxIssuesWarning();
                    disableMaxIssuesTextBox();
                    break;
                case "insert-table" :
                    var searchPanel = AJS.Editor.JiraConnector.Panel.Search.prototype;
                    enableMaxIssuesTextBox();
                    var value = $element.val();
                    if ($.trim(value) === '') {
                        if (e && e.type === 'keyup') {
                            clearMaxIssuesWarning();
                            break;
                        }
                        if (e && e.type === 'blur') {
                            value = searchPanel.MAXIMUM_MAX_ISSUES_VAL;
                            $element.val(value);
                            break;
                        }
                    }
                    if (AJS.$.isNumeric(value) && (searchPanel.MINIMUM_MAX_ISSUES_VAL <= value && value <= searchPanel.MAXIMUM_MAX_ISSUES_VAL)){
                        clearMaxIssuesWarning();
                        searchPanel.enableInsert();
                    } else {
                        // disable insert button when validate fail
                        showMaxIssuesWarning();
                        searchPanel.disableInsert();
                    }
                    break;
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

        checkAndSetDefaultValueMaximumIssues : function (options){
            if (!options) {
                AJS.log("Cannot set default value for Maximum Issues");
                return;
            }

            var element = options.element || AJS.$('#jira-maximum-issues');
            var defaultVal = options.defaultVal || this.MAXIMUM_MAX_ISSUES_VAL;
            var value = AJS.$.trim(element.val());
            if (value === ''){
                // set default value if user did not input anything
                element.val(defaultVal);
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
                if (!AJS.Editor.JiraConnector.Panel.Search.jiraColumnSelectBox) {
                    macroInputParams.columns = this.defaultColumns;
                    return;
                }
                macroInputParams.columns = AJS.Editor.JiraConnector.Select2.getKeyColumnsSelectedOptions(AJS.Editor.JiraConnector.Panel.Search.jiraColumnSelectBox);
                if (!macroInputParams.columns) {
                    macroInputParams.columns = this.defaultColumns;
                }
            }

            var currentRadioValue = AJS.$('input:radio[name=insert-advanced]:checked').val();
            if (currentRadioValue === 'insert-single') {
                macroInputParams['key'] = selectedIssueKeys.toString();
            }
            else if (unselectIssueKeys.length == 0) {
                // add param macro for jql when select all checked
                macroInputParams['jqlQuery'] = this.lastSearch + ' '; // the trailing empty space to invalidate previous cache
            } else {
                var keyInJql = 'key in (' + selectedIssueKeys.toString() + ')';
                macroInputParams['jqlQuery'] = keyInJql;
            }

            // CONF-30116
            if (currentRadioValue === 'insert-table'){
                var maxIssues = AJS.$('#jira-maximum-issues').val();
                macroInputParams["maximumIssues"] = maxIssues;
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
            return true;
        },
        loadMacroParams: function(selectedColumns) {
            var macroParams = this.macroParams;
            if (!macroParams) {
                // init new default macro params
                this.prepareColumnInput(selectedColumns || this.defaultColumns);
                this.checkAndSetDefaultValueMaximumIssues({defaultVal : 20});
            } else {
                macroParams.columns = selectedColumns || macroParams.columns || this.defaultColumns;
                // CONF-30116
                if (!macroParams.maximumIssues){
                    // disable textbox if there is not display table option
                    AJS.$('#jira-maximum-issues').attr('disabled','disabled');
                }
                
                this.checkAndSetDefaultValueMaximumIssues({defaultVal : 20});
                if (macroParams["count"] == "true") {
                    AJS.$("#opt-total").prop("checked", true);
                } else {
                    AJS.$("#opt-table").prop("checked", true);
                    // CONF-30116
                    
                    AJS.$('#jira-maximum-issues').removeAttr('disabled');
                    var maximumIssues = macroParams["maximumIssues"] || this.DEFAULT_MAX_ISSUES_VAL;
                }
                this.prepareColumnInput(macroParams["columns"]);
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
            AJS.$(".jiraSearchResults").after(displayOptsOverlayHtml());
            //Here we need to bind the submit and return false to prevent the user submission.
            AJS.$("#jiraMacroDlg").unbind("submit").on("submit", function(e) {
                    return false;
                }
            );
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
        prepareColumnInput : function(selectedColumnString) {
            var selectedColumnValues = selectedColumnString
                    .split(/\s*,\s*/);
            // make selectedColumnValues lower case
            for ( var i in selectedColumnValues) {
                var val = selectedColumnValues[i];
                if (val && typeof val.toLowerCase === 'function') {
                    selectedColumnValues[i] = val.toLowerCase();
                }
            }
            var server = this.selectedServer;
            var columnAlias = {
                issuekey : 'key', 
                duedate : 'due', 
                issuetype : 'type'
            };
            var initColumnInputField = function(data) {
                var dataMap = [];
                var columnInputField = AJS.$("#jiraIssueColumnSelector");
                var unselectedOptionHTML = "";
                var selectedOptionHTML = "";
                //build html string for unselected columns
                for ( var i = 0; i < data.length; i++) {
                    // apply the alias so it can work with the current column manager in back end :(
                    // TODO improve the whole column handling logic at some point
                    if (columnAlias[data[i].id]) {
                        data[i].id = columnAlias[data[i].id];
                    }
                    var key;
                    if (data[i].custom === true) {
                        key = data[i].name.toLowerCase();
                    } else {
                        key = data[i].id.toLowerCase();
                    }
                    var displayValue = data[i].name;
                    var selected = "";
                    var optionTemplate = AJS.template("<option value='{value}'>{displayValue}</option>");
                    dataMap[key] = displayValue;
                    
                    if (AJS.$.inArray(key, selectedColumnValues) < 0) {
                        unselectedOptionHTML += optionTemplate.fill({"value": key, "displayValue": displayValue});
                    } 
                }
                //below lines is used for processing alias keys cases (key, due, type). If not, we cannot find
                //values of "due","type" in the return Jira columns
                dataMap["key"] = "Key";
                //build html option string for selected columns.
                //The reason we need to do this: we need to provide the selected columns in options with appropriate order
                //to select2 component. If we don't do this, it will load the selected columns following the order of
                //columns returned by Jira
                for(var i = 0; i < selectedColumnValues.length; i++) {
                    var selectedOptionTemplate = AJS.template("<option selected='true' value='{value}'>{displayValue}</option>");
                    var key = selectedColumnValues[i].toLowerCase();
                    var displayValue =  dataMap[key];
                    if(displayValue != null)  {
                        selectedOptionHTML += selectedOptionTemplate.fill({"value": key, "displayValue": displayValue});
                    }
                }
                var finalOptionString =  selectedOptionHTML + unselectedOptionHTML;
                columnInputField.html(finalOptionString);
                columnInputField.auiSelect2({
                    width: "415px",
                    containerCssClass: "select2-container-jira-issue-columns"
                });
                AJS.Editor.JiraConnector.Panel.Search.jiraColumnSelectBox = columnInputField;
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
                    });
        },
        expandDisplayOptPanel: function() {
            var displayOptsOverlay = AJS.$('.jql-display-opts-overlay');
            var currentHeighOfOptsOverlay = displayOptsOverlay.height();
            var topMarginDisplayOverlay = 40;
            displayOptsOverlay.css("top", "");
            //here we need to calculate the current bottom position and set
            //to displayOptsOverlay. IF NOT, it does not have the original "from" bottom
            //position to start the animation and it will cause the Flash effect.
            
            var currentBottomPosition =  -(currentHeighOfOptsOverlay - topMarginDisplayOverlay);
            displayOptsOverlay.css("bottom", currentBottomPosition + "px");
            displayOptsOverlay.animate({
                bottom: 0
            }, 500 );
        },
        minimizeDisplayOptPanel: function() {
            var displayOptsOverlay = AJS.$('.jql-display-opts-overlay');
            //Need to get the current top value and set to the displayOptOverlay
            //because it needs the "from" top value to make the animation smoothly 
            displayOptsOverlay.css("top", displayOptsOverlay.position().top + "px");
            displayOptsOverlay.css("bottom", "");
            displayOptsOverlay.animate({
                top: 420
            }, 500 );

        },
        disableAutoSelectColumns : function() {
            AJS.Editor.JiraConnector.Panel.Search.jiraColumnSelectBox.auiSelect2("enable", false);
        },
        enableAutoSelectColumns : function() {
            AJS.Editor.JiraConnector.Panel.Search.jiraColumnSelectBox.auiSelect2("enable", true);
        },
        checkAutoSelectColumns : function() {
            if (AJS.$('#opt-table').prop('checked')) {
                this.enableAutoSelectColumns();
            } else {
                this.disableAutoSelectColumns();
                this.enableInsert();
            }
        },
        // bind event for new layout
        bindEventToDisplayOptionPanel: function(acceptNoResult) {
            var thiz = this;
            var displayOptsBtn = AJS.$('.jql-display-opts-close, .jql-display-opts-open'),
            displayOptsOverlay = AJS.$('.jql-display-opts-overlay'),
            optDisplayRadios = AJS.$('.jql-display-opts-inner .radio'),
            optTableRadio = AJS.$('#opt-table'),
            ticketCheckboxAll = AJS.$('#my-jira-search input:checkbox[name=jira-issue-all]'),
            ticketCheckboxes = AJS.$('#my-jira-search input:checkbox[name=jira-issue]');
            var $maxiumIssues = AJS.$('#jira-maximum-issues');

            // CONF-30116
            $maxiumIssues.on("blur keyup", AJS.Editor.JiraConnector.Panel.Search.prototype.validateMaxIssues);

            displayOptsOverlay.css("top", "420px");
            
            displayOptsBtn.click(function(e) {
                e.preventDefault();
                if($(this).hasClass("disabled")) {
                    return;
                }
                var isOpenButton = $(this).hasClass('jql-display-opts-open');
                
                if (isOpenButton) {
                    thiz.expandDisplayOptPanel();
                   
                    jQuery(this).addClass('jql-display-opts-close');
                    jQuery(this).removeClass('jql-display-opts-open');
                } else {
                    thiz.minimizeDisplayOptPanel();
                    jQuery(this).removeClass('jql-display-opts-close');
                    jQuery(this).addClass('jql-display-opts-open');
                }
            });
            optDisplayRadios.change(function() {
                thiz.checkAutoSelectColumns();
                thiz.validateMaxIssues();
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
                var ticketUncheckedLength = AJS.$('#my-jira-search input:checkbox[name=jira-issue]:not(:checked)').length;
                if(ticketUncheckedLength > 0) {
                    ticketCheckboxAll.removeAttr('checked');
                } else {
                    ticketCheckboxAll.prop('checked','checked');
                }
                thiz.validate();
            });
            thiz.validate(acceptNoResult);
        },
        /*
         * Change radio button value base on an action on issue checkboxes
         * */
        changeInsertOptionStatus: function(selectedIssueCount, handleNoRow) {
            var thiz = this;
            var radioSingle = AJS.$('#opt-single');
            var radioCount = AJS.$('#opt-total');
            var radioTable = AJS.$('#opt-table');
            var checkboxes = AJS.$('#my-jira-search input:checkbox[name=jira-issue]:checked');
            var isCheckedAll = AJS.$('#my-jira-search input:checkbox[name=jira-issue-all]').attr('checked') === 'checked';
            var isSingleIssueChecked = checkboxes.length === 1;
            var isMultipleIssuesChecked = checkboxes.length > 1;
            var isNothingChecked = checkboxes.length === 0;
            var singleKeyJQL = AJS.JQLHelper.isSingleKeyJQLExp(AJS.$('#my-jira-search input[name=jiraSearch]').val());
            
            var enableSingleIssueMode = function () {
                radioCount.attr('disabled','disabled');
                radioTable.attr('disabled','disabled');
                radioSingle.removeAttr('disabled').click();
                setTimeout(function(){
                    radioSingle.removeAttr('disabled').click();
                },100);
            };
            
            var enableMultipleIssuesMode = function () {
                radioSingle.attr('disabled','disabled');
                radioCount.removeAttr('disabled');
                if (AJS.$('input[name=insert-advanced]:checked').val() === 'insert-single') {
                    radioTable.removeAttr('disabled').click();
                    setTimeout(function(){
                        radioTable.removeAttr('disabled').click();
                    },100);
                }
            };
            
            var enableAllDisplayOptions = function () {
                radioTable.removeAttr('disabled','disabled');
                radioCount.removeAttr('disabled','disabled');
                radioSingle.removeAttr('disabled','disabled');
                var currentRadioValue = AJS.$('input:radio[name=insert-advanced]:checked').val();
                if (currentRadioValue === 'insert-single') {
                    radioTable.click();
                    setTimeout(function(){
                        radioTable.click();
                    },100);
                }
            };
            
            var disableOptionPanel = function() {
                radioSingle.attr('disabled','disabled');
                radioCount.attr('disabled','disabled');
                radioTable.attr('disabled','disabled');
                AJS.$('.jql-display-opts-close').click();
                AJS.$('.jql-display-opts-open').addClass("disabled");
            };
            
            AJS.$('.jql-display-opts-open').removeClass("disabled");
            radioSingle.removeAttr('disabled');
            radioCount.removeAttr('disabled');
            radioTable.removeAttr('disabled');
            
            if (isCheckedAll && isSingleIssueChecked && singleKeyJQL) { // single issue key, for eg: key = wbs-1
                enableSingleIssueMode();
            } else if (isCheckedAll && isSingleIssueChecked && !singleKeyJQL) { // valid jql that returns only 1 result
                enableAllDisplayOptions();
            } else if (isMultipleIssuesChecked) {
                enableMultipleIssuesMode();
            } else if (isSingleIssueChecked && !isCheckedAll) { // single result is check when jql returns multiple result
                enableSingleIssueMode();
            } else if (handleNoRow) {
                enableMultipleIssuesMode();
            } else if (isNothingChecked) {
                disableOptionPanel();
            }
        },

        analyticName: "search"

});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Search());
