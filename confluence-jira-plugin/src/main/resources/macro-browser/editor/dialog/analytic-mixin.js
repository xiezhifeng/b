define('confluence/jim/macro-browser/editor/dialog/analytic-mixin', [
    'jquery',
    'underscore',
    'ajs',
    'confluence/jim/macro-browser/editor/util/config'
],
function(
    $,
    _,
    AJS,
    config
) {
    'use strict';

    /**
     * Contains all mixin methods regarding to Jira Chart dialog.
     * These methods will extend other object, such as a Backbone View.
     */
    var AnalyticMixin = {
        /**
         * Send analytic event when opening the dialog
         */
        sendOpenDialogAnalyticEvent: function() {
            if (!AJS.Editor.JiraAnalytics) {
                return;
            }

            if (this.openSource === JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.instructionalText) {
                this.sendPanelTriggerWithLabelAnalyticEvent();
            } else {
                AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
                    source: this.openSource
                });
            }
        },

        sendPanelTriggerWithLabelAnalyticEvent: function() {
            var $pageLabelsString = $('#createPageLabelsString');
            if ($pageLabelsString.length) {
                var labels = $pageLabelsString.val().split(' ').join();
                AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
                    source: this.openSource,
                    label: labels
                });

                return;
            }

            service.getLabelsOfPage().done(function(data) {
                var labelNames = [];
                $.each(data.labels, function(index, label) {
                    labelNames.push(label.name);
                });

                AJS.Editor.JiraAnalytics.triggerPannelTriggerEvent({
                    source: this.openSource,
                    label: labelNames.join()
                });
            }.bind(this));
        },

        sendInsertDialogAnalytic: function() {
            if (!AJS.Editor.JiraAnalytics ||
                !this.currentTabContentView) {
                return;
            }

            var currentPanel = this.currentTabContentView;
            AJS.Editor.JiraConnector.analyticPanelActionObject = AJS.Editor.JiraAnalytics.setupAnalyticPanelActionObject(currentPanel, this.openSource, []);

            if (this.panels.getActiveTab() === 'tab-search-issue' &&
                currentPanel.customizedColumn) {
                AJS.Editor.JiraAnalytics.triggerCustomizeColumnEvent({
                    columns : currentPanel.customizedColumn
                });
            }
        }
    };

    return AnalyticMixin;
});

