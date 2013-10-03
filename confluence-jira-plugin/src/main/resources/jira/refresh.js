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
        });
    },
    registerRefresh: function(refresh)
    {
        if (!(refresh instanceof RefreshMacro.Refresh))
            throw "Refresh object must be an instance of RefreshMacro.Refresh";
        RefreshMacro.refreshs.push(refresh);
    },
    processRefresh: function(refresh)
    {
        AJS.$.ajax({
            type: 'POST',
            dataType: 'html',
            url: Confluence.getContextPath() + "/plugins/servlet/jiraRefreshRenderer",
            data: {pageId: refresh.pageId, wikiMarkup: refresh.wiki},
            timeout: 120000,
            success: function(reply) {
                RefreshWidget.get(refresh.id).getContentModule().html(reply);
                new RefreshMacro.RefreshCallback(refresh).callback();

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
    this.delay = {hide: 250, reveal: 600};
};

RefreshMacro.CallbackSupport.prototype = {
    errorHandler: function(err)
    {
        $("#refresh-" + this.refresh.id).html("<p>There was a problem rendering this section: " + err + "</p>");
    },
    refreshReplace: function(renderFinished)
    {
        var refreshId = this.refresh.id;
        var revealDelay = this.delay.reveal;
        $("#refresh-" + refreshId).slideUp(this.delay.hide, function() { $("#refresh-module-" + refreshId).slideDown(revealDelay, renderFinished) });
    },
    refreshImmediate: function(xhtml)
    {
        var refreshId = this.refresh.id;
        this.module.html(xhtml);
        $("#refresh-" + refreshId).css("display","none");
        $("#refresh-module-" + refreshId).css("display", "block");
    },
    callback: function() {
        var current = this;
        RefreshMacro.sessionStorage.put(this.refresh.id, RefreshWidget.get(this.refresh.id).getContentModule().html());
        RefreshMacro.renderQueue.push(function(renderFinished) { current.refreshReplace(renderFinished) })
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

$(function() { RefreshMacro.init() });

})(AJS.$);
