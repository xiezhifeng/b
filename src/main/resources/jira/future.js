/*
Copyright 2008 Atlassian

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

(function($) {

var FutureMacro = {
    futures: [],
    sessionStorage: {},
    init: function() {
        // this.sessionStorage = window.sessionStorage ? new WindowSessionStorage() : new InputControlSessionStorage();
        this.sessionStorage = new InputControlSessionStorage();

        FutureWidget.getAll().each(function() {
            FutureMacro.registerFuture(this.getFuture());
        });

        $.each(this.futures, function(i, future) {
            var widget = FutureWidget.get(future.id);
            if (future.manual) {
                widget.getManualButton().bind("click", future, FutureMacro.handleManualClick);
            }
            else {
                var cachedResult = FutureMacro.sessionStorage.get(future.id);
                if (cachedResult)
                {
                    (new FutureMacro.FutureCallback(future)).futureImmediate(cachedResult.toString());
                    return;
                }

                if (future.pageId)
                {
                    FutureMacro.processFuture(future);
                }
                /*
                else
                    FutureRenderer.convertWikiToHtmlForFuture(future.wiki, new FutureMacro.FutureCallback(future));
                */
            }
        });

        this.registerPollInterval();
    },
    registerPollInterval: function()
    {
        if (this.pollInterval)
            return;

        this.pollInterval = window.setInterval(function() {  FutureMacro.pollRenderQueue() }, 1000);
    },
    pollInterval: null,
    registerFuture: function(future)
    {
        if (!(future instanceof FutureMacro.Future))
            throw "Future object must be an instance of FutureMacro.Future";
        FutureMacro.futures.push(future);
    },
    clearPollInterval: function()
    {
        window.clearInterval(this.pollInterval);
        this.pollInterval = null;
    },
    pollRenderQueue: function()
    {
        if (this.renderQueue.length == 0)
            return;

        this.clearPollInterval();
        var nextRenderFunction = this.renderQueue.shift();
        nextRenderFunction(function() { FutureMacro.registerPollInterval() } );
    },
    processFuture: function(future)
    {
        AJS.$.ajax({
            type: 'POST',
            dataType: 'html',
            url: Confluence.getContextPath() + "/plugins/servlet/futureRenderer",
            data: {pageId: future.pageId, wikiMarkup: future.wiki},
            timeout: 120000,
            success: function(reply) {
                FutureWidget.get(future.id).getContentModule().html(reply);
                new FutureMacro.FutureCallback(future).callback();

            },
            error: function (xhr, textStatus, errorThrown) {
                new FutureMacro.FutureCallback(future).errorHandler(errorThrown);
            }
        });
    },
    renderQueue: []
};

FutureMacro.Future = function(id, wiki, manual) {
    this.id = id;
    this.wiki = wiki;
    this.manual = manual;
    this.pageId = arguments.length == 4 ? arguments[3] : null;
};

FutureMacro.CallbackSupport = function() {
    this.delay = {hide: 250, reveal: 600};
};

FutureMacro.CallbackSupport.prototype = {
    errorHandler: function(err)
    {
        $("#future-" + this.future.id).html("<p>There was a problem rendering this section: " + err + "</p>");
    },
    futureReplace: function(renderFinished)
    {
        var futureId = this.future.id;
        var revealDelay = this.delay.reveal;
        $("#future-" + futureId).slideUp(this.delay.hide, function() { $("#future-module-" + futureId).slideDown(revealDelay, renderFinished) });
    },
    futureImmediate: function(xhtml)
    {
        var futureId = this.future.id;
        this.module.html(xhtml);
        $("#future-" + futureId).css("display","none");
        $("#future-module-" + futureId).css("display", "block");
    },
    callback: function() {
        var current = this;
        FutureMacro.sessionStorage.put(this.future.id, FutureWidget.get(this.future.id).getContentModule().html());
        FutureMacro.renderQueue.push(function(renderFinished) { current.futureReplace(renderFinished) })
    }
};

FutureMacro.FutureCallback = function(future) {
    this.future = future;
    this.module = $("#future-module-" + future.id);
};

FutureMacro.FutureCallback.prototype = new FutureMacro.CallbackSupport();

FutureMacro.handleManualClick = function(e) {
    var future = e.data;
    $("#future-button-cont-" + future.id).css("display", "none");
    $("#future-" + future.id).css("display", "block");
    FutureMacro.processFuture(future);
};

var FutureWidget = function() {
    if (arguments.length == 1)
        this.id = arguments[0];
};

FutureWidget.prototype.getMacroPanel = function() {
    return $("#future-" + this.id);
};

FutureWidget.prototype.getFuture = function() {
    return new FutureMacro.Future(this.id, this.getWikiMarkup(), this.isManual(), this.getPageId());
};

FutureWidget.prototype.isManual = function() {
    return false;
};

FutureWidget.prototype.getContentModule = function() {
    return $("#future-module-" + this.id);
};

FutureWidget.prototype.getPageId = function() {
    return $("#future-page-id-" + this.id).val();
};

FutureWidget.prototype.getWikiMarkup = function() {
    return $("#future-wiki-" + this.id).val();
};

FutureWidget.prototype.getRenderCache = function() {
    return $("#future-render-cache-" + this.id);
};

FutureWidget.getAll = function() {
    return $("div.future-macro").map(function() {
        var futureId = this.id.replace("future-", "");
        return FutureWidget.get(futureId);
    });
};

FutureWidget.get = function(id) {
    var macro = $("#future-" + id);
    if (!macro)
        return null;

    return macro.hasClass("manual-future") ? new ManualFutureWidget(id) : new FutureWidget(id);
};

var ManualFutureWidget = function(id) { arguments.callee.prototype.constructor.apply(this, [id]) };
ManualFutureWidget.prototype = new FutureWidget();

ManualFutureWidget.prototype.getManualButton = function() {
    return $("#future-button-cont-" + this.id);
};

ManualFutureWidget.prototype.isManual = function() {
    return true;
};

var WindowSessionStorage = function() { };

WindowSessionStorage.prototype.get = function(id) {
    return window.sessionStorage.getItem(id);
};

WindowSessionStorage.prototype.put = function(id, val) {
    window.sessionStorage.setItem(id, val);
};

var InputControlSessionStorage = function() { };

InputControlSessionStorage.prototype.get = function(id) {
    var val = FutureWidget.get(id).getRenderCache().val();
    if (val == null || val == "")
        return null;

    if (val == "!")
        return "";

    return val.toString().substr(1);
};

InputControlSessionStorage.prototype.put = function(id, val) {
    FutureWidget.get(id).getRenderCache().val("!" + val);
};

$(function() { FutureMacro.init() });

})(AJS.$);
