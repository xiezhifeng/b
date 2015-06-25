define([
    'jquery',
    'ajs',
    'confluence/jim/macro-browser/editor/dialog/jira-links-dialog-macro-view',
    'confluence/jim/macro-browser/editor/jira-sprint-panel/sprint-panel-view',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/dialog-panel/panel-collection'
],
function(
    $,
    AJS,
    JiraLinksDialogMacroView,
    JiraSprintPanelView,
    config,
    PanelCollection
) {

    'use strict';

    QUnit.module('JIM Dialog test', {
        setup: function() {
            this.panels = new PanelCollection([
                // jira issue/filter panel.
                {
                    id: 'panel-jira-issue-filter',
                    name: AJS.I18n.getText('jira.macro.dialog.panel.issue'),
                    macroId: config.macroIdJiraIssue,
                    tooltipFooter: AJS.I18n.getText('insert.jira.issue.dialog.help.shortcut', 'Cmd'),
                    isSelect: true,
                    tabs: [
                        {
                            id: 'tab-search-issue',
                            isActive: true,
                            name: AJS.I18n.getText('insert.jira.issue.search'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraConnector.Panel.Search
                        },
                        {
                            id: 'tab-create-issue',
                            isActive: false,
                            name: AJS.I18n.getText('insert.jira.issue.create'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraConnector.Panel.Create
                        },
                        {
                            id: 'tab-recent-issue',
                            isActive: false,
                            isAlwaysRefreshWhenActive: true,
                            name: AJS.I18n.getText('insert.jira.issue.recent'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraConnector.Panel.Recent
                        }
                    ]
                },
                // jira sprint panel.
                {
                    id: 'panel-jira-sprint',
                    isSelect: false,
                    name: AJS.I18n.getText('jira.macro.dialog.panel.sprint'),
                    macroId: config.macroIdJiraSprint,
                    tabs: [{
                        id: 'tab-sprint',
                        isActive: true,
                        name: AJS.I18n.getText('jira.macro.dialog.panel.sprint'),
                        PanelContentView: JiraSprintPanelView
                    }]
                },
                {
                    id: 'panel-jira-chart',
                    isSelect: false,
                    name: AJS.I18n.getText('jira.macro.dialog.panel.chart'),
                    macroId: config.macroIdJiraChart,
                    tabs: [
                        {
                            id: 'tab-pie-chart',
                            isActive: true,
                            name: AJS.I18n.getText('jirachart.panel.piechart.title'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraChart.Panel.PieChart
                        },
                        {
                            id: 'tab-created-resolved-chart',
                            isActive: false,
                            name: AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart
                        },
                        {
                            id: 'tab-two-mentional-chart',
                            isActive: false,
                            name: AJS.I18n.getText('jirachart.panel.twodimensionalchart.title'),
                            PanelContentView: Backbone.View.extend({}) // AJS.Editor.JiraChart.Panel.TwoDimensionalChart
                        }
                    ]
                }
            ]);
        },
        teardown: function() {}
    });

    QUnit.test('Can initialize JIM Dialog View', function() {
        var jiraDialogView = new JiraLinksDialogMacroView({panels: this.panels});
        QUnit.ok(!!jiraDialogView);
    });

    QUnit.test('Rendering JIM Dialog View, assert some default selected panel and active tab', function() {
        var jiraDialogView = new JiraLinksDialogMacroView({panels: this.panels});
        var stubFetchData = sinon.stub(jiraDialogView, '_fetchServersData');

        jiraDialogView.render();

        var selectedPanelId = jiraDialogView.$el.find('.page-menu-item.selected').attr('data-panel-id');
        QUnit.equal(selectedPanelId, 'panel-jira-issue-filter');

        var $containerPanel = jiraDialogView.$el.find('.' + selectedPanelId);

        var activeTabId = $containerPanel.find('.tabs-menu .menu-item.active-tab').attr('data-tab-id');
        QUnit.ok(activeTabId, 'tab-search-issue');

        QUnit.ok($containerPanel.find('#' + activeTabId).hasClass('active-pane'));

        stubFetchData.restore();
    });
});
