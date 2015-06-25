// Mocks for AJS

(function (AJS) {
    var _meta = {
        "user-display-name" : "A. D. Ministrator",
        "remote-user" : "admin",
        "current-user-avatar-url" : "/path/to/user/avatar",
        "latest-page-id" : 123456789,
        "page-id" : 123456789,
        "content-type" : "page"
    };

    $.fn.tooltip = function(){};

    AJS.contextPath = function () { return "/confluence"; };

    AJS.Meta = {
        get: function(key) {
            return _meta[key];
        }
    };

    AJS.Analytics = {
        triggerPrivacyPolicySafeEvent: function () {
            return false;
        }
    };

    AJS.debug = function(msg) { return false; };

    AJS.I18n.keys = {
        'inline.comments.resolved.menu.show.replies': '{0} replies',
        'inline.comments.resolved.menu.show.reply': '{0} reply',
        'inline.comments.resolved.menu.hide.replies': 'Hide replies ({0})',
        'inline.comments.resolved.view.menu': 'Resolved feedback'
    };

    AJS.DarkFeatures = {
        isEnabled: function(key) {
            return true; // enable all dark features!!!
        }
    };

    AJS.whenIType = function() {
        return {
            execute: function() {}
        }
    };

    /*
     * Copied these templates over from Confluence Core because our templates call out to them and I do not want to
     * compile all of Confluence SOY templates like we do for AUI and SOY Plugin.
     */
    window.Confluence = window.Confluence || {};
    var Confluence = window.Confluence;

   /* Confluence.Templates = Confluence.Templates || {};
    Confluence.Templates.JiraIssueMacro = Confluence.Templates.JiraIssueMacro || {};
    Confluence.Templates.JiraIssueMacro = Confluence.Templates.JiraIssueMacro || {};
    Confluence.Templates.JiraIssueMacro.Dialog = Confluence.Templates.JiraIssueMacro.Dialog || {};
    Confluence.Templates.JiraIssueMacro.Dialog.dialog = function() {
        return '';
    };

    Confluence.Templates.JiraIssueMacro.Dialog.warningDialogNoAppLink = function() {
        return '';
    };

    Confluence.Templates.JiraIssueMacro.Dialog.serverBoardSprintTemplate = function() {
        return '';
    };*/

    /*
     * Need to mock out MutationObserver because it is not supported in PhantomJS that's used by Karma test runner
     */
    MutationObserver = function() {
        return this;
    };
    MutationObserver.prototype.observe = function() {
        return false;
    };
    MutationObserver.prototype.disconnect = function() {
        return false;
    };

    // soy template of AUI
    window.aui = {
        dialog: {
            dialog2: function() {
                return "<section></section>";
            }
        }
    };

    AJS.Editor = {};
    AJS.Editor.JiraConnector = {};
    AJS.Editor.JiraConnector.servers = [{
       id: '123'
    }];

    // phatomjs does not have Function.prototype.bind
    if (!Function.prototype.bind) {
        Function.prototype.bind = function(oThis) {
            if (typeof this !== 'function') {
                // closest thing possible to the ECMAScript 5
                // internal IsCallable function
                throw new TypeError('Function.prototype.bind - what is trying to be bound is not callable');
            }

            var aArgs   = Array.prototype.slice.call(arguments, 1),
                    fToBind = this,
                    fNOP    = function() {},
                    fBound  = function() {
                        return fToBind.apply(this instanceof fNOP
                                        ? this
                                        : oThis,
                                aArgs.concat(Array.prototype.slice.call(arguments)));
                    };

            if (this.prototype) {
                // native functions don't have a prototype
                fNOP.prototype = this.prototype;
            }
            fBound.prototype = new fNOP();

            return fBound;
        };
    }

}(window.AJS || (AJS = {})));