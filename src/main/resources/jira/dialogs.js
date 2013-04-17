
//Register TinyMCE plugin
(function() {

    tinymce.create('tinymce.plugins.JiraLink', {
        init : function(ed) {
            var title = AJS.I18n.getText("insert.jira.issue");
            ed.addCommand('mceJiralink', AJS.Editor.JiraConnector.open);
            var cb = ed.onPostRender.add(function(ed){
                var cm = ed.controlManager;
            	AJS.$('#insert-menu .macro-jiralink').hide();
            	AJS.$.get(Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers', function(data){
            		if(data[0].id){
        				AJS.Editor.JiraConnector.servers = data;
        				AJS.$('#jiralink').click(function(e){
        					AJS.Editor.JiraConnector.open(true);
        					return AJS.stopEvent(e);
        				});
            		} else{
            			AJS.$('#jiralink').click(function(e){
        					AJS.Editor.JiraConnector.warningPopup(data[0].isAdministrator);
        					return AJS.stopEvent(e);
        				});
            		}
            		//alway show link jira macro in insert-menu
            		AJS.$('#insert-menu .macro-jiralink').show();
            		ed.addShortcut('ctrl+shift+j', '', 'mceJiralink');
            	});
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
    }
    var kbHelpText = AJS.I18n.getText("insert.jira.issue.dialog.help.shortcut", modifierKey());
    var popup;
    
    var openJiraDialog = function(summaryText){
        if (!popup){
	        popup = new AJS.Dialog(840, 590, 'jira-connector');
	        popup.addHeader(dialogTitle);
	        var panels = AJS.Editor.JiraConnector.Panels;
	       
	        for (var i = 0; i < panels.length; i++){
	            popup.addPanel(panels[i].title());
	            var dlgPanel = popup.getCurrentPanel();
	            var panelObj = panels[i];
	            panelObj.init(dlgPanel);
	        }
            popup.addHelpText(kbHelpText);
	        popup.addButton(insertText, function(){
	            var panel = panels[popup.getCurrentPanel().id];
	            panel.insertLink();
	        }, 'insert-issue-button');
	        
	        popup.addCancel(cancelText, function(){
	            AJS.Editor.JiraConnector.closePopup();
	        });
	        popup.gotoPanel(0);
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
    
    return {
    	warningPopup : function(isAdministrator){
    		//render bodycontent
    		var warningDialogTitle = "Connect Confluence To Jira";
    		var warningDialog = new AJS.Dialog({width:600, height:400});
    		warningDialog.addHeader(warningDialogTitle);

    		//add body content in panel
    		var bodyContent;
			if(isAdministrator) {
				bodyContent = "<div id='warning-body'>"
				+ "<p>If you connect Confluence to Jira you can..</p>"
				+ "<p><a id='open_applinks' target='_blank' href='" + Confluence.getContextPath() + "/admin/listapplicationlinks.action'>Click here to set this up</a></p>" 
				+ "</div>";
    		} else {
    			bodyContent = "<div id='warning-body'>"
    				+ "<p>If you connect Confluence to Jira you can..</p>"
    				+ "<p>Your administrator can set this up. <a id='open_applinks' target='_blank' href='mailto:admin@atlassian.com'>Click here to contact your admin</a></p>" 
    				+ "</div>"; 
    		}	
    		warningDialog.addPanel("Panel 1", bodyContent);
            warningDialog.get("panel:0").setPadding(0);

    		warningDialog.addButton("Cancel", function (dialog) {
    			warningDialog.hide();
                tinymce.confluence.macrobrowser.macroBrowserCancel();           
            });
    		
    		AJS.bind("show.dialog", function(e, data) {
    			var open_applinks = AJS.$("#warning-body #open_applinks");
    			open_applinks.bind('click',function(){
        			warningDialog.hide();
                    tinymce.confluence.macrobrowser.macroBrowserCancel();           
            	});
			});
    		
    		warningDialog.show();
    		warningDialog.gotoPanel(0);    	       

    	},
        closePopup: function(){
            popup.hide();
            tinymce.confluence.macrobrowser.macroBrowserCancel();           
        },
		open: function(fromRTEMenu){

            // Store the current selection and scroll position, and get the selected text.
           
            AJS.Editor.Adapter.storeCurrentSelectionState();
            var summaryText;
            if(fromRTEMenu) {
            	summaryText = tinyMCE.activeEditor.selection.getContent({format : 'text'});
            } 
            
            var t = tinymce.confluence.macrobrowser,
            node = t.getCurrentNode();
            if (t.isMacroTag(node) && 'jira' == $(node).attr('data-macro-name')){
                tinymce.confluence.macrobrowser.editMacro(node);
                return;
            }
            
			openJiraDialog(summaryText);
        },
        edit: function(macro){
        	//check for show custom dialog when click in other macro
        	if(typeof(macro.params) == 'undefined') {
    	        AJS.Editor.JiraConnector.open();
        		return;
        	}
        	
            var parseUglyMacro = function(macroTxt){
                //get first macro parameter and assume its a jql query
                var bar = macroTxt.indexOf("|");
                if (bar >= 0){
                    return macroTxt.substring(0, bar);
                }
                return macroTxt;
            };
            // parse params from macro data
            var parseParamsFromMacro = function(macro) {
            	var params = {};
            	
                var searchStr = macro.defaultParameterValue || macro.params['jqlQuery'] 
                || macro.params['key'] 
                || parseUglyMacro(macro.paramStr);                
                params['searchStr'] = searchStr;
                
                params['serverName'] = macro.params['server'];
                
                var count = macro.params['count'];
                if(typeof count === "undefined") {
                	count = "false";
                }
                params['count'] = count;
                
                var columns = macro.params['columns'];
                if(typeof(columns) != 'undefined') {
                	if(columns.length) {
                		params['columns'] = columns
                	}
                }

            	return params;            	
            };            
          
            var macroParams = parseParamsFromMacro(macro);
            
            if (macro && !AJS.Editor.inRichTextMode()) { // select and replace the current macro markup
                $("#markupTextarea").selectionRange(macro.startIndex, macro.startIndex + macro.markup.length);
            }
            openJiraDialog();
            if (macroParams.searchStr){
                popup.gotoPanel(0);
                var searchPanel = AJS.Editor.JiraConnector.Panels[0];                
                // assign macro params to search
                searchPanel.setMacroParams(macroParams);
                
                searchPanel.doSearch(macroParams['searchStr'], macroParams['serverName']);
            }
        }   
	}
})(AJS.$);

AJS.MacroBrowser.setMacroJsOverride('jira', {opener: AJS.Editor.JiraConnector.edit});
AJS.Editor.JiraConnector.Panels= [];