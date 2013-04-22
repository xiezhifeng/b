AJS.Editor.JiraConnector.Panel.Search = function(){
	this.jql_operators = /=|!=|~|>|<|!~| is | in /i;
	this.issueKey = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
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
                var searchFormtest = Confluence.Templates.SoyDialog.searchForm;
                var searchForm = $(searchFormtest()).appendTo(container);
                
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
            	//get content from soy template
            	var displayOptsHtml = Confluence.Templates.SoyDialog.displayOptsHtml;
        		var displayOptsOverlayHtml = Confluence.Templates.SoyDialog.displayOptsOverlayHtml;
        		AJS.$(".jiraSearchResults").after(displayOptsHtml());
        		AJS.$(".jiraSearchResults").after(displayOptsOverlayHtml());    			
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