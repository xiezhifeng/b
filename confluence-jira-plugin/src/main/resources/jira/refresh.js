(function($) {

var RefreshMacro = {
    REFRESH_STATE_STARTED: 1,
    REFRESH_STATE_DONE: 2,
    REFRESH_STATE_FAILED: 3,
    JIM_SORTABLE:"jim.sortable",
    refreshs: [],
    sortables: [],
    init: function() {
        RefreshWidget.getAll().each(function() {
            RefreshMacro.registerRefresh(this.getRefresh());
        });

        $.each(this.refreshs, function(i, refresh) {
            var widget = RefreshWidget.get(refresh.id);
            widget.getRefreshButton().bind("click", refresh, RefreshMacro.handleRefreshClick);
            widget.getRefreshLink().bind("click", refresh, RefreshMacro.handleRefreshClick);
        });
        if (AJS.DarkFeatures.isEnabled(RefreshMacro.JIM_SORTABLE)) {
            HeaderWidget.getAll().each(function() {
                RefreshMacro.registerSort(this.getSortable());
            });
            $.each(this.sortables, function(i, sort) {
                var widget = HeaderWidget.get(sort.id);
                widget.getHeadersTable().bind("click", sort, RefreshMacro.onHeaderClick);
            });
        }
    },
    onHeaderClick: function(e) {
        var sort = e.data;
        refeshId = sort.id;
        var order = $(this).hasClass("tablesorter-headerDesc") ? "ASC" : "DESC";
        var columnName = $(this).find(".jim-table-header-content").text();
        var wikiMakup =  $("#refresh-wiki-" + refeshId).val();
        var pageId = $("#refresh-page-id-" + refeshId).val();
        var macroPanel = $("#refresh-" + refeshId);
        var refresh = new RefreshMacro.Refresh(refeshId, wikiMakup, pageId, macroPanel.html());
        var refreshWiget = RefreshWidget.get(refeshId);
        refreshWiget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_STARTED);
        RefreshMacro.processRefresh(refresh, columnName, order);
    },
    replaceRefresh: function(oldId, newId) {
        var widget = RefreshWidget.get(oldId);
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_DONE);
        $.each(this.refreshs, function(i, refresh) {
            if (refresh.id === oldId) {
                RefreshMacro.refreshs.splice(i, 1);
                var widget = RefreshWidget.get(newId);
                var newRefresh = widget.getRefresh();
                RefreshMacro.registerRefresh(newRefresh);
                
                RefreshMacro.sortables.splice(i,1);
                var sortWidget = HeaderWidget.get(newId);
                var newSort = sortWidget.getSortable();
               
                RefreshMacro.registerSort(newSort);
                
                widget.getRefreshButton().bind("click", newRefresh, RefreshMacro.handleRefreshClick);
                widget.getRefreshLink().bind("click", newRefresh, RefreshMacro.handleRefreshClick);
                sortWidget.getHeadersTable().bind("click", newSort, RefreshMacro.onHeaderClick);
                return;
            }
        });
    },
    registerRefresh: function(refresh) {
        if (!(refresh instanceof RefreshMacro.Refresh))
            throw AJS.I18n.getText("jiraissues.error.refresh.type");
        RefreshMacro.refreshs.push(refresh);
    },
    registerSort: function(sort) {
        if (!(sort instanceof RefreshMacro.Sortable))
            throw AJS.I18n.getText("jiraissues.error.refresh.type");
        RefreshMacro.sortables.push(sort);
    },
    handleRefreshClick: function(e) {
        var refresh = e.data;
        var widget = RefreshWidget.get(refresh.id);
        widget.getMacroPanel().html(refresh.loadingMsg);
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_STARTED);
        RefreshMacro.processRefresh(refresh);
    },
    processRefresh: function(refresh, columnName, order) {
        var data = {};
        if (arguments.length == 1) {
            data = {pageId: refresh.pageId, wikiMarkup: refresh.wiki};
        } else if (arguments.length == 3) {
            data = {pageId: refresh.pageId, wikiMarkup: refresh.wiki,columnName:columnName, order:order};
        }
        AJS.$.ajax({
            type: "POST",
            dataType: "html",
            url: Confluence.getContextPath() + "/plugins/servlet/jiraRefreshRenderer",
            data: data,
            success: function(reply) {
                var refreshNewId = $(reply).attr("id");
                if (refreshNewId) {
                    refreshNewId = refreshNewId.replace("refresh-module-", "");
                    RefreshWidget.get(refresh.id).getContentModule().replaceWith(reply);
                    new RefreshMacro.CallbackSupport(refresh).callback(refreshNewId);
                } else {
                    new RefreshMacro.CallbackSupport(refresh).errorHandler(reply);
                }
            },
            error: function (xhr, textStatus, errorThrown) {
                new RefreshMacro.CallbackSupport(refresh).errorHandler(errorThrown);
            }
        });
    }
};

RefreshMacro.Refresh = function(id, wiki) {
    this.id = id;
    this.wiki = wiki;
    this.pageId = arguments.length > 2 ? arguments[2] : null;
    this.loadingMsg = arguments.length > 3 ? arguments[3] : null;
};

RefreshMacro.CallbackSupport = function(refresh) {
    this.refresh = refresh;
    this.module = $("#refresh-module-" + refresh.id);
};

RefreshMacro.CallbackSupport.prototype = {
    errorHandler: function(err) {
        var widget = RefreshWidget.get(this.refresh.id);
        var errMsg = AJS.format(AJS.I18n.getText("jiraissues.error.refresh"), err);
        if (AJS.DarkFeatures.isEnabled(RefreshMacro.JIM_SORTABLE)){
            widget.getErrorMessagePanel().html(errMsg);
        } else {
            widget.getMacroPanel().html("<p>" + errMsg + "</p>");
        }
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_FAILED);
    },
    callback: function(newId) {
        RefreshMacro.replaceRefresh(this.refresh.id, newId);
    }
};

var RefreshWidget = function() {
    if (arguments.length == 1)
        this.id = arguments[0];
};
var HeaderWidget = function() {
    if (arguments.length == 1)
        this.id = arguments[0];
};

RefreshWidget.prototype.getRefresh = function() {
    return new RefreshMacro.Refresh(this.id, this.getWikiMarkup(), this.getPageId(), this.getMacroPanel().html());
};
HeaderWidget.prototype.getSortable = function() {
    return new RefreshMacro.Sortable(this.id, $("#refresh-page-id-" + this.id).val(), $("#refresh-" + this.id).html());
};
RefreshWidget.get = function(id) {
    var macro = $("#refresh-" + id);
    if (!macro)
        return null;

    return new RefreshWidget(id);
};

HeaderWidget.get = function(id) {
    var macro = $("#refresh-" + id);
    if (!macro)
        return null;
        
    return new HeaderWidget(id);
};

RefreshMacro.Sortable = function(id, pageId, msg) {
    this.id = id;
    this.pageId = pageId;
    this.loadingMsg = msg;
};
HeaderWidget.getAll = function() {
    return $("div.refresh-macro").map(function() {
        var headerId = this.id.replace("refresh-", "");
        return HeaderWidget.get(headerId);
    });
};

RefreshWidget.getAll = function() {
    return $("div.refresh-macro").map(function() {
        var refreshId = this.id.replace("refresh-", "");
        return RefreshWidget.get(refreshId);
    });
};

RefreshWidget.prototype.getErrorMessagePanel = function() {
    return $("#error-message-" + this.id);
};

RefreshWidget.prototype.removeDarkLayer = function() {
    $("#jim-dark-layout-" + this.id).remove();
};

RefreshWidget.prototype.displayDarkLayer = function() {
      var container = $('#refresh-module-' + this.id);
        var position = container.position();
        $('<div />', {
            id: 'jim-dark-layout-' + this.id,
            'class': 'jim-sortable-dark-layout',
            style: 'top:' + position.top + 'px; left: ' + position.left + 'px; width:' + container.width() + 'px; height: ' + container.height() + 'px'
        }).appendTo('#main');
};

RefreshWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id);
};

HeaderWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id).val();
}
RefreshWidget.prototype.getContentModule = function() {
    return $("#refresh-module-" + this.id);
};

RefreshWidget.prototype.getPageId = function() {
    return $("#refresh-page-id-" + this.id).val();
};
HeaderWidget.prototype.getPageId = function() {
    return $("#refresh-page-id-" + this.id).val();
}
RefreshWidget.prototype.getWikiMarkup = function() {
    return $("#refresh-wiki-" + this.id).val();
};

RefreshWidget.prototype.getRefreshButton = function() {
    return $("#refresh-issues-button-" + this.id);
};

HeaderWidget.prototype.getHeadersTable = function() {
    return $("#jira-issues-" + this.id + " .jira-tablesorter-header");
};

RefreshWidget.prototype.getRefreshLink = function() {
    return $("#refresh-issues-link-" + this.id);
};

RefreshWidget.prototype.getJiraIssuesArea = function() {
    return $("#jira-issues-" + this.id);
};

RefreshWidget.prototype.getIssuesCountArea = function() {
    return $("#total-issues-count-" + this.id);
};

RefreshWidget.prototype.updateRefreshVisibility = function(state) {
    if (state === RefreshMacro.REFRESH_STATE_STARTED) {
        if (AJS.DarkFeatures.isEnabled(RefreshMacro.JIM_SORTABLE)) {
            this.displayDarkLayer();
        } else {
            this.getJiraIssuesArea().hide();
            this.getRefreshButton().hide();
            this.getRefreshLink().hide();
            this.getIssuesCountArea().hide();
            this.getMacroPanel().show();
        }
    } else if (state === RefreshMacro.REFRESH_STATE_FAILED) {
        this.getRefreshButton().show();
        this.getRefreshLink().show();
        this.removeDarkLayer();
    } else if (state === RefreshMacro.REFRESH_STATE_DONE) {
        // No need to un-hide elements since they will be replaced
        this.removeDarkLayer();
    }
};

$(function() { RefreshMacro.init() });

})(AJS.$);
