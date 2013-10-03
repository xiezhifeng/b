(function($) {

var RefreshMacro = {
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
            }
        });
    },
    registerRefresh: function(refresh) {
        if (!(refresh instanceof RefreshMacro.Refresh))
            throw "Refresh object must be an instance of RefreshMacro.Refresh";
        RefreshMacro.refreshs.push(refresh);
    },
    processRefresh: function(refresh) {
        var widget = RefreshWidget.get(refresh.id);
        widget.getMacroPanel().toggleClass("refresh_hidden");
        widget.getJiraIssuesArea().toggleClass("refresh_hidden");

        AJS.$.ajax({
            type: "POST",
            dataType: "html",
            url: Confluence.getContextPath() + "/plugins/servlet/jiraRefreshRenderer",
            data: {pageId: refresh.pageId, wikiMarkup: refresh.wiki},
            timeout: 120000,
            success: function(reply) {
                var refreshNewId = $(reply).attr("id").replace("refresh-module-", "");
                RefreshWidget.get(refresh.id).getContentModule().replaceWith(reply);
                new RefreshMacro.RefreshCallback(refresh).callback(refreshNewId);
            },
            error: function (xhr, textStatus, errorThrown) {
                new RefreshMacro.RefreshCallback(refresh).errorHandler(errorThrown);
            }
        });
    },
};

RefreshMacro.Refresh = function(id, wiki) {
    this.id = id;
    this.wiki = wiki;
    this.pageId = arguments.length == 3 ? arguments[2] : null;
};

RefreshMacro.CallbackSupport = function() {
};

RefreshMacro.CallbackSupport.prototype = {
    errorHandler: function(err)
    {
        $("#refresh-" + this.refresh.id).html("<p>There was a problem rendering this section: " + err + "</p>");
    },
    callback: function(newId) {
        RefreshMacro.replaceRefresh(this.refresh.id, newId);
    }
};

RefreshMacro.RefreshCallback = function(refresh) {
    this.refresh = refresh;
    this.module = $("#refresh-module-" + refresh.id);
};

RefreshMacro.RefreshCallback.prototype = new RefreshMacro.CallbackSupport();

RefreshMacro.handleRefreshClick = function(e) {
    var refresh = e.data;
    $("#refresh-issues-button-" + refresh.id).css("display", "none");
    $("#refresh-" + refresh.id).css("display", "block");
    RefreshMacro.processRefresh(refresh);
};

var RefreshWidget = function() {
    if (arguments.length == 1)
        this.id = arguments[0];
};

RefreshWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id);
};

RefreshWidget.prototype.getRefresh = function() {
    return new RefreshMacro.Refresh(this.id, this.getWikiMarkup(), this.getPageId());
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

RefreshWidget.getAll = function() {
    return $("div.refresh-macro").map(function() {
        var refreshId = this.id.replace("refresh-", "");
        return RefreshWidget.get(refreshId);
    });
};

RefreshWidget.get = function(id) {
    var macro = $("#refresh-" + id);
    if (!macro)
        return null;

    return new RefreshWidget(id);
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

$(function() { RefreshMacro.init() });

})(AJS.$);
