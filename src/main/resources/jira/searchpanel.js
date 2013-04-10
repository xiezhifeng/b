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
                        thiz.createIssueTableFromUrl(container, 
                                thiz.selectedServer.id, 
                                '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jql) + '&tempMax=20&field=summary&field=type&field=link',
                                thiz.setSelectedIssue, 
                                thiz.insertLink,
                                thiz.disableInsert, 
                                function(){
                        			// start: update for new jira plugin - insert advanced layout, remove old insert all
                        			thiz.addInsertAdvancedLayout();
                        			thiz.loadInsertAdvancedEvent();
                        			thiz.loadMacroParams();
                        	
                                    if (!single){
                                        //var checked = (searchStr && !single) ? true : false;
                                        //container.append('<div class="jql-insert-check"><input type="checkbox" name="as-jql" value="as-jql" />' + AJS.I18n.getText("insert.jira.issue.asjql") + '</div>');
                                        //if (checked) $('input:checkbox',container).attr('checked','true');
                                    	thiz.lastSearch = jql;
                                    }
                                 // end: update for new jira plugin - insert advanced layout
                                },
                                function(xhr){
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
                    thiz.onselect();
                }
                
            },
            setMacroParams: function(params) {
            	this.macroParams = params;
            },            
            insertLink: function(){            	
            	// start: update for new jira plugin
            	//get select & unselect issue keys
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
            	
            	var isCount = ((AJS.$('div.jql-display-opts-column-2  input:radio:checked').val() == "insert-count") ? true : false);
            	
                var container = AJS.$('div#my-jira-search');
            	var columns = $('.jql-display-opts-column-2 input:text', container).val();
            	console.log("columns = " + columns);
            	
            	this.insertJiraIssueLink(isCount, selectedIssueKeys, unselectIssueKeys, this.lastSearch, columns);
            	// end: update for new jira plugin
            },
            onselect: function(){
                var container = AJS.$('div#my-jira-search');
                var selectedrow = AJS.$('tr.selected', container);
                if (selectedrow.length){
                    this.enableInsert();
                    selectedrow.focus();
                }               
                else{
                    if (AJS.$('.oauth-message', container).length){
                        this.authCheck(this.selectedServer);
                    }
                    AJS.$('input', container).focus();
                    this.disableInsert();
                }
            },            
            loadMacroParams: function() {
            	var macroParams = this.macroParams;            	
            	if(macroParams) {
            		if(macroParams.countStr == "true") {
                		$('input[name=insert-advanced][value="insert-count"]').prop('checked', true);
                	}
                	else {
                		$('input[name=insert-advanced][value="insert-table"]').prop('checked', true);
                	}	
            		//load columns table
            		if(macroParams.columns != null) {
            			var container = AJS.$('div#my-jira-search');
            			$('.jql-display-opts-column-2 input:text', container).val(macroParams.columns);
            		}
            	}
            },
            addInsertAdvancedLayout: function() {
            	var displayOptsHtml = '<div class="jql-display-opts-bar data-table">' +    			 
    			'<a href="#" class="jql-display-opts-open"><span></span><strong>' + AJS.I18n.getText("insert.jira.issue.option.displayoptions") + '</strong>' +  AJS.I18n.getText("insert.jira.issue.option.multipleissues") + '</a>' +
    			'</div>';
    			var displayOptsOverlayHtml = '<div class="jql-display-opts-overlay data-table">' + 
    				'<div class="jql-display-opts-inner">' +
                    '<a href="#" class="jql-display-opts-close"><span></span>' + AJS.I18n.getText("insert.jira.issue.option.displayoptions") + '</a>' +
                    '<div class="clearfix">' +
                    '<div class="jql-display-opts-column-1">' +
                    AJS.I18n.getText("insert.jira.issue.option.displayas") +
                        '</div>' +
                        '<div class="jql-display-opts-column-2">' +
                            '<div class="jql-display-opts-option">' +
                                '<input type="radio" class="opt-display" name="insert-advanced" id="opt-total" value="insert-count"><label for="opt-total">' + AJS.I18n.getText("insert.jira.issue.option.totalissuecount") + '</label>' +
                            '</div>' +
                            '<div class="jql-display-opts-description">' +
                            	AJS.I18n.getText("insert.jira.issue.option.totalissuecountdesc") + ' <a href="#">' + AJS.I18n.getText("insert.jira.issue.option.totalissuecountsample") + '</a>.' +
                            '</div>' +
                            '<div class="jql-display-opts-option">' +
                                '<input type="radio" class="opt-display" checked="checked" name="insert-advanced" id="opt-table" value="insert-table"><label for="opt-table">' + AJS.I18n.getText("insert.jira.issue.option.table") + '</label>' +
                            '</div>' +
                            '<div class="jql-display-opts-description">' +
                            	AJS.I18n.getText("insert.jira.issue.option.tabledesc") + 
                            '</div>' +                                        
                        '</div>' +
                    '</div>' +
                    '<div class="clearfix">' +
                        '<div class="jql-display-opts-column-1">' +
                        	AJS.I18n.getText("insert.jira.issue.option.columnstodisplay") +
                        '</div>' +
                        '<div class="jql-display-opts-column-2">' +
                            '<div class="columns-display-input">' +
                                '<input type="text" name="columns-display" class="columns-display" value="">' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>'+
            '</div>';
    			
    			jQuery("#my-jira-search").append(displayOptsHtml);
    			jQuery("#my-jira-search").append(displayOptsOverlayHtml);    			
            },
            // bind event for new layout
            loadInsertAdvancedEvent: function() {
            	jQuery('.jql-display-opts-close').bind('click',function(){
            		var button = jQuery(this),
            			overlay = button.parents('.jql-display-opts-overlay');

            		overlay.hide();
            	});
            	jQuery('.jql-display-opts-open').bind('click',function(){
            		var button = jQuery(this),
            			overlay = jQuery('.jql-display-opts-overlay');

            		overlay.show();
            	});
            	jQuery('.opt-display').change(function(){
            		var columnsDisplay = jQuery('.columns-display');
            		if(jQuery('#opt-total').prop('checked')) {
            			columnsDisplay.attr('disabled','disabled');
            		} else {
            			columnsDisplay.removeAttr('disabled');
            		}
            	});            	
            	jQuery('#my-jira-search .jiraSearchResults input:checkbox[name=jira-issue-all]').bind('click',function(){            		
            		var all = jQuery(this);
            		var items = jQuery('#my-jira-search .jiraSearchResults input:checkbox[name=jira-issue]');

            		if(all.prop('checked')) {
            			items.prop('checked','checked');
            		} else {
            			items.removeAttr('checked');
            		}
            	});        	
            }
        });
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Search());