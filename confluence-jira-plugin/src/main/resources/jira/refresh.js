(function($) {

var RefreshMacro = {
    REFRESH_STATE_STARTED: 1,
    REFRESH_STATE_DONE: 2,
    REFRESH_STATE_FAILED: 3,
    refreshs: [],
    init: function() {
        RefreshWidget.getAll().each(function() {
            RefreshMacro.registerRefresh(this.getRefresh());
        });

        $.each(this.refreshs, function(i, refresh) {
            var widget = RefreshWidget.get(refresh.id);
            widget.getRefreshButton().bind("click", refresh, RefreshMacro.handleRefreshClick);
            widget.getRefreshLink().bind("click", refresh, RefreshMacro.handleRefreshClick);
        });
    },
    replaceRefresh: function(oldId, newId) {
        $.each(this.refreshs, function(i, refresh) {
            if (refresh.id === oldId) {
                RefreshMacro.refreshs.splice(i, 1);
                var widget = RefreshWidget.get(newId);
                var newRefresh = widget.getRefresh();
                RefreshMacro.registerRefresh(newRefresh);

                widget.getRefreshButton().bind("click", newRefresh, RefreshMacro.handleRefreshClick);
                widget.getRefreshLink().bind("click", newRefresh, RefreshMacro.handleRefreshClick);
                return;
            }
        });
    },
    registerRefresh: function(refresh) {
        if (!(refresh instanceof RefreshMacro.Refresh))
            throw "Refresh object must be an instance of RefreshMacro.Refresh";
        RefreshMacro.refreshs.push(refresh);
    },
    handleRefreshClick: function(e) {
        var refresh = e.data;
        var widget = RefreshWidget.get(refresh.id);
        widget.getMacroPanel().html(refresh.loadingMsg);
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_STARTED);
        RefreshMacro.processRefresh(refresh);
    },
    processRefresh: function(refresh) {
        AJS.$.ajax({
            type: "POST",
            dataType: "html",
            url: Confluence.getContextPath() + "/plugins/servlet/jiraRefreshRenderer",
            data: {pageId: refresh.pageId, wikiMarkup: refresh.wiki},
            timeout: 120000,
            success: function(reply) {
                var refreshNewId = $(reply).attr("id").replace("refresh-module-", "");
                RefreshWidget.get(refresh.id).getContentModule().replaceWith(reply);
                new RefreshMacro.CallbackSupport(refresh).callback(refreshNewId);
            },
            error: function (xhr, textStatus, errorThrown) {
                new RefreshMacro.CallbackSupport(refresh).errorHandler(errorThrown);
            }
        });
    },
};

RefreshMacro.Refresh = function(id, wiki) {
    this.id = id;
    this.wiki = wiki;
    this.pageId = arguments.length > 2 ? arguments[2] : null;
    this.loadingMsg = arguments.length > 3 ? arguments[3] : null;;
};

RefreshMacro.CallbackSupport = function(refresh) {
    this.refresh = refresh;
    this.module = $("#refresh-module-" + refresh.id);
};

RefreshMacro.CallbackSupport.prototype = {
    errorHandler: function(err) {
        var widget = RefreshWidget.get(this.refresh.id);
        var errMsg = AJS.format(AJS.I18n.getText("jiraissues.error.refresh"), err);
        widget.getMacroPanel().html("<p>" + errMsg + "</p>");
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

RefreshWidget.prototype.getRefresh = function() {
    return new RefreshMacro.Refresh(this.id, this.getWikiMarkup(), this.getPageId(), this.getMacroPanel().html());
};

RefreshWidget.get = function(id) {
    var macro = $("#refresh-" + id);
    if (!macro)
        return null;

    return new RefreshWidget(id);
};

RefreshWidget.getAll = function() {
    return $("div.refresh-macro").map(function() {
        var refreshId = this.id.replace("refresh-", "");
        return RefreshWidget.get(refreshId);
    });
};

RefreshWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id);
};

RefreshWidget.prototype.getContentModule = function() {
    return $("#refresh-module-" + this.id);
};

RefreshWidget.prototype.getPageId = function() {
    return $("#refresh-page-id-" + this.id).val();
};

RefreshWidget.prototype.getWikiMarkup = function() {
    return $("#refresh-wiki-" + this.id).val();
};

RefreshWidget.prototype.getRefreshButton = function() {
    return $("#refresh-issues-button-" + this.id);
};

RefreshWidget.prototype.getRefreshLink = function() {
    return $("#refresh-issues-link-" + this.id);
};

RefreshWidget.prototype.getJiraIssuesArea = function() {
    return $("#jira-issues-" + this.id);
};

RefreshWidget.prototype.updateRefreshVisibility = function(state) {
    if (state === RefreshMacro.REFRESH_STATE_STARTED) {
        this.getJiraIssuesArea().addClass("refresh_hidden");
        this.getRefreshButton().addClass("refresh_hidden");
        this.getRefreshLink().addClass("refresh_hidden");
        this.getMacroPanel().removeClass("refresh_hidden");
    } else if (state === RefreshMacro.REFRESH_STATE_FAILED) {
        this.getRefreshButton().removeClass("refresh_hidden");
        this.getRefreshLink().removeClass("refresh_hidden");
    } else if (state === RefreshMacro.REFRESH_STATE_DONE) {
        // No need to un-hide elements since they will be replaced 
    }
};

$(function() { RefreshMacro.init() });

})(AJS.$);
