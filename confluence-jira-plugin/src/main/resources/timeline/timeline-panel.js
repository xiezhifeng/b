/*AJS.Editor.Timeline.Panels.Timeline = function() {

    var PIE_CHART_TITLE = AJS.I18n.getText('jirachart.panel.piechart.title');
    return {

        title : PIE_CHART_TITLE,

        init : function(panel) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraTimeline();
            panel.html(contentJiraChart);
        }
    };
};*/

AJS.Editor.Timeline.Panels.Timeline = function() {

    return {
        title: "JIRA TIMELINE PANEL",

        init: function(panel) {

            var $ = AJS.$;
            panel.html('<div id="my-jira-search"></div>');
            var thiz = this;
            var container = $('#my-jira-search');
            this.container = container;

            var doSearch = function(searchParams) {
                var searchValue = searchParams && searchParams.searchValue;
                if (searchValue) {
                    $('input:text', container).val(searchValue);
                }

                if (this.currentXhr && this.currentXhr.readyState != 4) {
                    return;
                }

                var queryTxt = searchValue || $('input', container).val();

                var performQuery = function(jql, single, fourHundredHandler) {
                    thiz.lastSearch = jql;
                    AJS.Editor.JiraConnector.Panel.prototype.createIssueTableFromUrl(container,
                        thiz.selectedServer.id,
                        '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jql) + '&returnMax=true&tempMax=20&field=summary&field=type&field=link',
                        thiz.selectHandler,
                        thiz.insertLinkFromForm,
                        function() { // <-- noRowsHandler
                            thiz.addDisplayOptionPanel();
                            thiz.bindEventToDisplayOptionPanel(); // still enable display option if the jql is legal but no results found
                            thiz.enableInsert();
                        },
                        function() {
                            thiz.addDisplayOptionPanel();
                            thiz.bindEventToDisplayOptionPanel();
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
                        false); // <-- add checkbox column
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
                        thiz.errorMsg(container, AJS.I18n.getText("insert.jira.issue.search.badrequest"));
                    } else {
                        jql = jiraParams["jqlQuery"];
                    }
                }
                else {
                    clearPanel();
                    thiz.disableInsert();
                }
                return jql;
            };
            thiz.processJiraParams = processJiraParams;
        }
    ,

    focusForm: function() {
        AJS.$('input[name="jiraSearch"]', this.container).focus();
    },

    addSearchForm: function() {
        var thiz = this;
        thiz.container.empty();
        var servers = AJS.Editor.JiraConnector.servers;
        thiz.selectedServer = servers[0];
        var searchFormSoy = Confluence.Templates.ConfluenceJiraPlugin.searchTimeForm();
        AJS.$(searchFormSoy).appendTo(thiz.container);

        AJS.$('button', thiz.container).click(function() {
            thiz.doSearch();
        });
    },

    refreshSearchForm: function() {
        this.container.empty();
        this.addSearchForm();
    },

    customizedColumn : null,

    bindEventToDisplayOptionPanel: function() {
        var thiz = this;
        var displayOptsBtn = AJS.$('.jql-display-opts-close, .jql-display-opts-open'),
            displayOptsOverlay = AJS.$('.jql-display-opts-overlay');

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
    },

    setMacroParams: function(params) {
        this.macroParams = params;
    },

    insertLinkFromForm : function() {
        var params = {};
        params.jql = AJS.$('input[name="jiraSearch"]', this.container).val();
        params.width = $('#jira-timeline-width').val();
        params.height = $('#jira-timeline-height').val();
        params.group = $('#jira-timeline-group-by').val();
        tinymce.confluence.macrobrowser.macroBrowserComplete({name: 'timeline', "params": params});
        AJS.Editor.Timeline.close();
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
        var displayOptsOverlayHtml = Confluence.Templates.ConfluenceJiraPlugin.displayOptsOverlayHtmlTimeline;
        AJS.$(".jiraSearchResults").after(displayOptsOverlayHtml());
        //Here we need to bind the submit and return false to prevent the user submission.
        AJS.$("#jiraMacroDlg").unbind("submit").on("submit", function(e) {
                return false;
            }
        );
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

    }
}
};

AJS.Editor.Timeline.Panels.push(new AJS.Editor.Timeline.Panels.Timeline());