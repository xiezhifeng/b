define('confluence/jim/jira/jira-issues-view-mode/refresh-table', [
    'jquery',
    'ajs'
], function(
    $,
    AJS
) {
    'use strict';

var RefreshMacro = {
    REFRESH_STATE_STARTED: 1,
    REFRESH_STATE_DONE: 2,
    REFRESH_STATE_FAILED: 3,
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

            //Display spinner if widget is a placeholder
            if (widget.getContentModule().is('[data-client-id]')) {
                RefreshMacro.processRefreshWaiting(refresh);
            }
        });
        HeaderWidget.getAll().each(function() {
            RefreshMacro.registerSort(this.getSortable());
        });
        $.each(this.sortables, function(i, sort) {
            var widget = HeaderWidget.get(sort.id);
            widget.getHeadersTable().bind("click", sort, RefreshMacro.onHeaderClick);
        });
    },
    onHeaderClick: function(e) {
        var refeshId = e.data.id;

        var order = "ASC";
        if ($(this).hasClass("tablesorter-headerAsc")) {
            order = "DESC";
        }

        var columnName = $(this).find(".jim-table-header-content").text();
        var $refreshElement = $(AJS.format("#refresh-module-{0} .refresh-wiki", refeshId));
        var wikiMakup =  $refreshElement.data('wikimarkup');
        var pageId = $refreshElement.data('pageid');
        var macroPanel = $("#refresh-" + refeshId);
        var refresh = new RefreshMacro.Refresh(refeshId, wikiMakup, pageId, macroPanel.html());
        var refreshWiget = RefreshWidget.get(refeshId);
        var useCache = false;
        refreshWiget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_STARTED);
        RefreshMacro.processRefresh(refresh, useCache, columnName, order);
    },

    updateRefreshedElement: function($tableElement, replacedTableHtml) {
        var refreshOldId = $tableElement.attr("id").replace("refresh-module-", "");
        var $replacedTableElement = $(replacedTableHtml);
        var refreshNewId = $replacedTableElement.attr("id") && $replacedTableElement.attr("id").replace("refresh-module-", "") || undefined;
        $.each(this.refreshs, function(i, refresh) {
            if (refresh.id === refreshOldId) {
                if (refreshNewId || $replacedTableElement.hasClass("jim-error-message")) {
                    RefreshWidget.get(refresh.id).getContentModule().replaceWith(replacedTableHtml);
                    new RefreshMacro.CallbackSupport(refresh).callback(refreshNewId);
                } else {
                    //unexpected error
                    new RefreshMacro.CallbackSupport(refresh).errorHandler(replacedTableHtml);
                }
            }
        });
    },
    replaceRefresh: function(oldId, newId) {
        var widget = RefreshWidget.get(oldId);
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_DONE, newId);
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
    processRefreshWithData: function(refresh, clearCache) {
        this.processRefreshWaiting(refresh, clearCache);
        RefreshMacro.processRefresh(refresh, clearCache);
    },
    processRefreshWaiting: function(refresh) {
        var widget = RefreshWidget.get(refresh.id);
        widget.getMacroPanel().html(refresh.loadingMsg);
        widget.updateRefreshVisibility(RefreshMacro.REFRESH_STATE_STARTED, refresh.id);
    },
    handleRefreshClick: function(e) {
        // always clear cache here
        RefreshMacro.processRefreshWithData(e.data, true);
    },
    processRefresh: function(refresh, clearCache, columnName, order) {
        AJS.$.ajax({
            type: "POST",
            dataType: "json",
            contentType: "application/json",
            url: Confluence.getContextPath() + "/rest/jiraanywhere/1.0/jira/renderTable",
            data: AJS.$.toJSON({
                wikiMarkup: encodeURIComponent(refresh.wiki),
                clearCache: clearCache,
                columnName: columnName,
                order: order
            }),
            success: function(response, textStatus) {
                var refreshNewId;
                var reply = response.data;
                // if the reply is from the servlet's error handler, simply render it
                if ($(reply).hasClass('jim-error-message-table')) {
                    RefreshWidget.get(refresh.id).removeDarkLayer();
                    RefreshWidget.get(refresh.id).getContentModule().replaceWith(reply);
                } else {
                    refreshNewId = $(reply).attr("id");
                    if (refreshNewId) {
                        refreshNewId = refreshNewId.replace("refresh-module-", "");
                        RefreshWidget.get(refresh.id).getContentModule().replaceWith(reply);
                        new RefreshMacro.CallbackSupport(refresh).callback(refreshNewId);
                    } else {
                        new RefreshMacro.CallbackSupport(refresh).errorHandler(reply);
                    }
                }
                //CONFDEV-36972 notify to DDR plugin
                AJS.trigger('confluence.extra.jira:jira-table:completed.success', {id: refreshNewId || -1, data: reply, status: textStatus });
            },
            error: function (xhr, textStatus, errorThrown) {
                new RefreshMacro.CallbackSupport(refresh).errorHandler(errorThrown);
                AJS.trigger('confluence.extra.jira:jira-table:completed.error', {id: -1, data: errorThrown, status: textStatus });
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
        var errMsg = AJS.I18n.getText('jiraissues.error.refresh');
        widget.getErrorMessagePanel().html(errMsg);
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
    return $(AJS.format("#refresh-module-{0} .error-message", this.id));
};

RefreshWidget.prototype.removeDarkLayer = function() {
    $("#jim-dark-layout-" + this.id).remove();
};

RefreshWidget.prototype.displayDarkLayer = function() {
    var container = $('#refresh-module-' + this.id);
    var position = container.position();
    $('<div />', {
        id: 'jim-dark-layout-' + this.id,
        'class': 'jim-sortable-dark-layout'
    }).appendTo(container);
};

RefreshWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id);
};

HeaderWidget.prototype.getMacroPanel = function() {
    return $("#refresh-" + this.id).val();
};

RefreshWidget.prototype.getContentModule = function() {
    return $("#refresh-module-" + this.id);
};

RefreshWidget.prototype.getPageId = function() {
    return $("#refresh-wiki-" + this.id).data('pageid');
};
HeaderWidget.prototype.getPageId = function() {
    return $("#refresh-wiki-" + this.id).data('pageid');
};
RefreshWidget.prototype.getWikiMarkup = function() {
    return $(AJS.format("#refresh-module-{0} .refresh-wiki", this.id)).data('wikimarkup');
};

RefreshWidget.prototype.getRefreshButton = function() {
    return $(AJS.format("#refresh-module-{0} .refresh-issues-button", this.id));
};

RefreshWidget.prototype.getLoadingButton = function(id) {
    return $(AJS.format("#refresh-module-{0} .refresh-issues-loading", id || this.id));
};

HeaderWidget.prototype.getHeadersTable = function() {
    return $(AJS.format("#refresh-module-{0} .jira-issues .jira-tablesorter-header", this.id));
};

RefreshWidget.prototype.getRefreshLink = function() {
    return $(AJS.format("#refresh-module-{0} .refresh-issues-link", this.id));
};

RefreshWidget.prototype.getJiraIssuesArea = function() {
    return $(AJS.format("#refresh-module-{0} .jira-issues", this.id));
};

RefreshWidget.prototype.getIssuesCountArea = function() {
    return $(AJS.format("#refresh-module-{0} .total-issues-count", this.id));
};

RefreshWidget.prototype.updateRefreshVisibility = function(state, newId) {
    if (state === RefreshMacro.REFRESH_STATE_STARTED) {
        this.displayDarkLayer();
        this.getErrorMessagePanel().addClass('hidden');
        this.getRefreshLink().text(AJS.I18n.getText("jiraissues.loading"));
        this.getRefreshButton().hide();
        this.getLoadingButton(newId).removeClass('hidden');
    } else if (state === RefreshMacro.REFRESH_STATE_FAILED) {
        // hide loading spinner
        this.getLoadingButton(newId).addClass('hidden')
        this.removeDarkLayer();
        this.getRefreshButton().show();
        this.getRefreshLink().show();
        this.getErrorMessagePanel().removeClass('hidden');
        this.getRefreshLink().text(AJS.I18n.getText("jiraissues.refresh"));
    } else if (state === RefreshMacro.REFRESH_STATE_DONE) {
        // hide loading spinner
        this.getLoadingButton(newId).addClass('hidden')
        this.removeDarkLayer();
        this.getRefreshButton().show();
        this.getRefreshLink().text(AJS.I18n.getText("jiraissues.refresh"));
    }
};

return RefreshMacro;

});


