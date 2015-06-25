define('confluence/jim/macro-browser/editor/dialog-panel/panel-collection', [
    'underscore',
    'ajs',
    'backbone'
],
function(
    _,
    AJS,
    Backbone
) {
    'use strict';

    var PanelModel = Backbone.Model.extend({
        defaults: {
            name: '',
            id: '',
            macroId: '',
            tooltipFooter: '',
            isSelect: false,
            tabs: []
        }
    });

    var PanelCollection = Backbone.Collection.extend({
        model: PanelModel,

        getSelectedPanel: function() {
            return this.findWhere({isSelect: true});
        },

        getActiveTab: function() {
            var selectedPanel = this.getSelectedPanel();
            if (selectedPanel) {
                return _.findWhere(selectedPanel.get('tabs'), {isActive: true});
            }
        },

        _setSelectPanelByPropName: function(propName, value) {
            this.resetSelectedPanelAndActiveTab();

            var selectedPanel = null;

            this.each(function(panel) {
                panel.set('isSelect', panel.get(propName) === value);

                if (panel.get('isSelect')) {
                    selectedPanel = panel;

                    // always set the fist tab is active by default.
                    panel.get('tabs')[0].isActive = true;
                }

            }, this);

            return selectedPanel;
        },

        setSelectedForPanelByMacroId: function(macroId) {
            this._setSelectPanelByPropName('macroId', macroId);
            this.trigger('selected.panel.changed');
        },

        setSelectedForPanelById: function(panelId, isTriggerEvent) {
            var oldPanel = this.getSelectedPanel();

            var selectedPanel = this._setSelectPanelByPropName('id', panelId);

            if (isTriggerEvent && oldPanel && oldPanel.id !== this.getSelectedPanel().id) {
                this.trigger('selected.panel.changed');
            }

            return selectedPanel;
        },

        setActiveForTab: function(panelId, tabId, isTriggerEvent) {
            var oldTab = this.getActiveTab();

            var selectedPanel = this.setSelectedForPanelById(panelId, false);
            if (selectedPanel) {
                _.each(selectedPanel.get('tabs'), function(tab) {
                    tab.isActive = tab.id === tabId;
                });
            }

            if (isTriggerEvent && oldTab && oldTab.id !== this.getActiveTab().id) {
                this.trigger('selected.tab.changed');
            }
        },

        setActiveForTabByTabId: function(tabId, isTriggerEvent) {
            var oldTab = this.getActiveTab();

            this.each(function(panel) {
                panel.isSelect = false;

                var tabs = panel.get('tabs');
                _.each(tabs, function(tab) {
                    if (tab.id === tabId) {
                        tab.isActive = true;
                        panel.isSelect = true;
                    } else {
                        tab.isActive = false;
                    }
                });
            });

            if (isTriggerEvent && oldTab && oldTab.id !== this.getActiveTab().id) {
                this.trigger('selected.tab.changed');
            }
        },

        resetSelectedPanelAndActiveTab: function() {
            this.each(function(panel) {
                panel.set('isSelect', false);

                _.each(panel.get('tabs'), function(tab) {
                    tab.isActive = false;
                });
            });
        },

        resetCachedView: function() {
            this.each(function(panel) {
                _.each(panel.get('tabs'), function(tab) {
                    if (tab.cachedView) {
                        if (tab.cachedView.destroy) {
                            tab.cachedView.destroy();
                        }

                        // for legacy panels
                        if (tab.cachedView.container) {
                            tab.cachedView.container.remove();
                        }
                    }

                    tab.cachedView = null;
                });
            });
        }
    });

    return PanelCollection;
});
