AJS.Editor.JiraConnector.Panel.Search = function(){
  this.jql_operators = /=|!=|~|>|<|!~| is | in /i;
}
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Search.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Search.prototype, {
            
            title: function(){
                return AJS.I18n.getText("insert.jira.issue.search");
            },
            init: function(panel){
                var servers = AJS.Editor.JiraConnector.servers;
                this.selectedServer = servers[0];
                
                var $ = AJS.$;
                panel.html('<div id="my-jira-search"></div>');
                var thiz = this;
                var container = $('div#my-jira-search');
                this.container = container;
                
                var clearPanel = function(){
                    container.children().not('div.jira-search-form').remove();
                };
                
                var enableSearch = function(){
                    $('input.text', container).enable();
                    $('button', container).enable();
                };
                
                var disableSearch = function(){
                    $('button', container).disable();
                    $('input.text', container).disable();
                };
                
                var authCheck = function(server){
                  // disable insert when authCheck
                    thiz.disableInsert();
                    if (server)
                        thiz.selectedServer = server;
                    if (thiz.selectedServer.authUrl){
                        disableSearch();
                        clearPanel();
                        var oauthForm = thiz.createOauthForm(function(){
                            clearPanel();   
                            enableSearch();
                        });
                        $('.search-help').hide();
                        container.append(oauthForm);
                    }
                    else{
                        clearPanel();
                        enableSearch();
                        $('.search-help').show();
                    }
                };
                
                this.authCheck = authCheck;
                //define object params contain params macro
                
                var doSearch = function(searchStr, serverName){
                  // remove checkbox "Insert all query"
                    //$('div.jql-insert-check').remove();
                    if (searchStr){
                        $('input:text', container).val(searchStr);
                    }
                    // end: update for new plugin
                    if (serverName && serverName != this.selectedServer.name){
                        var servers = AJS.Editor.JiraConnector.servers;
                        for (var i = 0; i < servers.length; i++){
                            if (servers[i].name == serverName){
                                $('option[value="' + servers[i].id + '"]', container).attr('selected', 'selected');
                                $('select', container).change();
                                break;
                            }
                        }
                    }
                    if (this.currentXhr && this.currentXhr.readyState != 4){
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
                                function(xhr){
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
                                }
                            );
                        };
                    
                    var showNoServerMessage = function(isAdmin) {
                        var message;
                        if(isAdmin) {
                            message = AJS.I18n.getText("insert.jira.issue.message.noserver.admin.message") + '<a id="open_applinks" href="' + Confluence.getContextPath() + '/admin/listapplicationlinks.action">' + AJS.I18n.getText("insert.jira.issue.message.noserver.admin.link.title") + '</a>'
                        }
                        else {
                            message = AJS.I18n.getText("insert.jira.issue.message.noserver.user.message") + '<a id="open_applinks" href="' + Confluence.getContextPath() + '/wiki/contactadministrators.action">' + AJS.I18n.getText("insert.jira.issue.message.noserver.user.link.title") + '</a>'
                        }
                      
                        thiz.noServerMsg(container, message);
                    };
                    
                    // url/url xml
                    if(AJS.JiraConnector.JQL.isIssueUrlOrXmlUrl(queryTxt)) {
                        var url = decodeURIComponent(queryTxt);	
                        var jiraParams = AJS.JiraConnector.JQL.getJqlAndServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);                    	
                        var serverIndex = jiraParams["serverIndex"];
                        if(serverIndex != -1) {
                            if(jiraParams["serverIndex"].length > 0) {
                                $('option[value="' + AJS.Editor.JiraConnector.servers[serverIndex].id + '"]', container).attr('selected', 'selected');
                                $('select', container).change();                                
                                performQuery(jiraParams["jqlQuery"], false, null);    
                            }
                            else {
                                // show error msg for no JQL - CONFVN-79
                                clearPanel();
                                thiz.errorMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest"));
                            }
                        }
                        else {
                            clearPanel();
                            thiz.disableInsert();
                            showNoServerMessage(AJS.Meta.get("is-admin"));
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
                
                var searchForm = $('<div class="jira-search-form"><form class="aui">' + 
                        '<fieldset class="inline"><div class="search-input">' + 
                        '<input type="text" class="text one-server" name="jiraSearch"/>' + 
                        '</div>' + 
                        '<button type="button" class="button">' + AJS.I18n.getText("insert.jira.issue.search") + '</button></fieldset></form>' + 
                        '<div class="search-help">' + AJS.I18n.getText("insert.jira.issue.search.text.default") + '</div></div>').appendTo(container);
                
                if (servers.length > 1){
                    var serverSelect = $('<select class="select" tabindex="0"></select>').insertAfter('div.search-input', searchForm);
                    thiz.applinkServerSelect(serverSelect, authCheck);
                    $('input.one-server', searchForm).removeClass('one-server');
                }
                authCheck(this.selectedServer);
                $('button', container).click(function(){doSearch();});
                this.setActionOnEnter($('input', container), doSearch);    
                
                panel.onselect=function(){
                    thiz.validate();
                }
            },
            validate: function() {
                var container = AJS.$('div#my-jira-search');
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
                    if (AJS.$('.oauth-message', container).length){
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
                // get input value from dialog
                var isCount = ((AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-count") ? true : false);              
                var container = AJS.$('div#my-jira-search');
                var columns = $('.jql-display-opts-column-2 input:text', container).val();
              
                var selectedIssueKeys = new Array();
                var unselectIssueKeys = new Array();
                AJS.$('#my-jira-search .my-result.aui input:checkbox[name=jira-issue]').each(function(i){
                    if(AJS.$(this).is(':checked')) {
                        selectedIssueKeys[selectedIssueKeys.length] = AJS.$(this).val();
                    }
                    else {
                        unselectIssueKeys[unselectIssueKeys.length] = AJS.$(this).val();
                    }
                });
              
                // prapare macro param data
                var macroInputParams = {};
              
                if(isCount){
                    macroInputParams['count'] = 'true';
                }
                else {
                    if(typeof(columns) != 'undefined') {
                        columns = columns.replace(/\s/g, '');
                        if(columns.length > 0) {            			
                            macroInputParams["columns"] = columns;
                        }	
                    }
                }
              
                if(selectedIssueKeys.length == 1){
                    // display count when select 1 issue with count
                    macroInputParams['key'] = selectedIssueKeys.toString();
                } 
                else {
                    //add param macro for jql when select all checked
                    if(unselectIssueKeys.length == 0){
                        macroInputParams['jqlQuery'] = this.lastSearch;
                    }
                    else {
                        var keyInJql = 'key in (' + selectedIssueKeys.toString() + ')';
                        macroInputParams['jqlQuery'] = keyInJql;
                    }
                }
                return macroInputParams;
            },
            insertLink: function(){
                var macroInputParams = this.getMacroParamsFromUserInput();
                this.insertIssueLinkWithParams(macroInputParams);
            },
            loadMacroParams: function() {
                var macroParams = this.macroParams;
                if(macroParams) {                
                    if(macroParams['count'] == 'true') {
                        AJS.$('#opt-total').prop('checked', true);
                    }
                    else {
                        AJS.$('#opt-table').prop('checked', true);
                    }	
                    //load columns table
                    if(macroParams['columns'] != null) {
                        var container = AJS.$('div#my-jira-search');
                        AJS.$('.jql-display-opts-column-2 input:text', container).val(macroParams['columns']);
                    }
                }
            },
            addDisplayOptionPanel: function() {
                var displayOptsHtml = '<div class="jql-display-opts-bar data-table">' +
                '<a href="#" class="jql-display-opts-open"><span></span><strong>' + AJS.I18n.getText("insert.jira.issue.option.displayoptions") + '</strong> ' +  AJS.I18n.getText("insert.jira.issue.option.multipleissues") + '</a>' +
                '</div>';
                var displayOptsOverlayHtml = '<div class="jql-display-opts-overlay data-table">' + 
                    '<div class="jql-display-opts-inner">' +
                    '<a href="#" class="jql-display-opts-close"><span></span><strong>' + AJS.I18n.getText("insert.jira.issue.option.displayoptions") + '</strong> ' +  AJS.I18n.getText("insert.jira.issue.option.multipleissues") + '</a>' +
                    '<div class="clearfix">' +
                    '<div class="jql-display-opts-column-1">' +
                    AJS.I18n.getText("insert.jira.issue.option.displayas") +
                        '</div>' +
                        '<div class="jql-display-opts-column-2">' +
                            '<div class="jql-display-opts-option">' +
                                '<input type="radio" class="opt-display" name="insert-advanced" id="opt-total" value="insert-count"><label for="opt-total">' + AJS.I18n.getText("insert.jira.issue.option.count.label") + '</label>' +
                            '</div>' +
                            '<div class="jql-display-opts-description">' +
                              AJS.I18n.getText("insert.jira.issue.option.count.desc") + ' <a href="#">' + AJS.I18n.getText("insert.jira.issue.option.count.sample") + '</a>.' +
                            '</div>' +
                            '<div class="jql-display-opts-option">' +
                                '<input type="radio" class="opt-display" checked="checked" name="insert-advanced" id="opt-table" value="insert-table"><label for="opt-table">' + AJS.I18n.getText("insert.jira.issue.option.table.label") + '</label>' +
                            '</div>' +
                            '<div class="jql-display-opts-description">' +
                              AJS.I18n.getText("insert.jira.issue.option.table.desc") + 
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="clearfix">' +
                        '<div class="jql-display-opts-column-1">' +
                          AJS.I18n.getText("insert.jira.issue.option.columns.label") +
                        '</div>' +
                        '<div class="jql-display-opts-column-2">' +
                            '<div class="columns-display-input">' +
                                '<input type="text" name="columns-display" class="columns-display" value="' + AJS.I18n.getText("insert.jira.issue.option.columns.value") + '">' +
                            '</div>' +
                            '<div class="jql-display-opts-description">' +
                            AJS.I18n.getText("insert.jira.issue.option.columns.desc") + 
                          '</div>' +
                        '</div>' +
                    '</div>' +
                    '</div>'+
                '</div>';
          
                AJS.$("#my-jira-search").append(displayOptsHtml);
                AJS.$("#my-jira-search").append(displayOptsOverlayHtml);
            },
            // bind event for new layout
            bindEventToDisplayOptionPanel: function() {
                var thiz = this;
                var displayOptsCloseBtn = AJS.$('.jql-display-opts-close'),
                displayOptsOpenBtn = AJS.$('.jql-display-opts-open'),
                displayOptsOverlay = AJS.$('.jql-display-opts-overlay'),
                optDisplayRadios = AJS.$('.opt-display'),
                columnsDisplayInput = AJS.$('.columns-display'),
                optTotalRadio = AJS.$('#opt-total'),
                ticketCheckboxAll = AJS.$('#my-jira-search input:checkbox[name=jira-issue-all]'),
                ticketCheckboxes = AJS.$('#my-jira-search input:checkbox[name=jira-issue]'),
                insertButton = AJS.$('.insert-issue-button');

                displayOptsCloseBtn.bind('click',function(){
                    displayOptsOverlay.hide();
                });
                displayOptsOpenBtn.bind('click',function(){
                    displayOptsOverlay.show();
                });
              
                optDisplayRadios.change(function(){
                    if(optTotalRadio.prop('checked')) {
                        columnsDisplayInput.attr('disabled','disabled');
                    } else {
                        columnsDisplayInput.removeAttr('disabled');
                    }
                });
              
                ticketCheckboxAll.bind('click',function(){
                    var all = AJS.$(this);
                    if(all.prop('checked')) {
                        ticketCheckboxes.prop('checked','checked');
                    } else {
                        ticketCheckboxes.removeAttr('checked');
                    }
                    thiz.validate();
                });
              
                ticketCheckboxes.change(function(){
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
                    AJS.$('.columns-display').attr('disabled','disabled');
                    if(AJS.$('input:radio[name=insert-advanced]:checked').val() == "insert-table"){
                        AJS.$('.columns-display').removeAttr('disabled');
                    }
                }
                else {
                    AJS.$("#opt-total").attr('disabled','disabled');
                    AJS.$("#opt-table").attr('disabled','disabled');
                    AJS.$('.columns-display').attr('disabled','disabled');
                    // auto slide down when only check 1 
                    AJS.$('.jql-display-opts-overlay').hide();
                }
            }
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Search());