define('confluence/jim/editor/jirasprint/sprint-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/editor/dialog/abstract-panel-view',
    'confluence/jim/editor/util/config',
    'confluence/jim/editor/util/service',
    'confluence/jim/editor/util/helper',
    'confluence/jim/editor/util/select2-mixin'
],
function(
    $,
    _,
    AJS,
    Backbone,
    AbstractPanelView,
    config,
    service,
    util,
    Select2Mixin
) {
    'use strict';

    var FormDataModel = Backbone.Model.extend({
        defaults: {
            selectedServer: null,
            selectedBoard: null,
            selectedSprint: null,
            isValid: true
        },

        reset: function() {
            _.extend(this.attributes, this.defaults);
        }
    });

    var JiraSprintPanelView = AbstractPanelView.extend({
        events: {
            'change #jira-sprint-servers': '_onSelectServerChanged',
            'change #jira-sprint-board': '_onSelectBoardChanged',
            'change #jira-sprint-sprint': '_onSelectSprintChanged'
        },

        initialize: function() {
            AbstractPanelView.prototype.initialize.apply(this, arguments);

            this.panelTitle = AJS.I18n.getText('jira.sprint.panel.title');
            this.panelId = 'jira-sprint-panel';

            // a map of template functions
            this.template = Confluence.Templates.JiraSprints.Dialog;

            this.on('reload.data', function() {
                this._fillServersData();
            }.bind(this));
        },

        render: function(options) {
            AbstractPanelView.prototype.render.apply(this, arguments);

            var template = this.template.serverBoardSprintTemplate({});
            this.$el.html(template);

            this.servers = [];
            this.primaryServer = null;
            this.boards = [];
            this.sprints = [];

            this.formData = new FormDataModel();
            this.listenTo(this.formData, 'change:selectedServer', this._handleServerChanged);
            this.listenTo(this.formData, 'change:selectedBoard', this._handleBoardChanged);
            this.listenTo(this.formData, 'change:selectedSprint', this._handleSprintChanged);

            this.view = {
                $server: this.$('#jira-sprint-servers'),
                $board: this.$('#jira-sprint-board'),
                $sprint: this.$('#jira-sprint-sprint'),
                $errorMessage: this.$('.error-messages'),
                $createButton: this.dialogView.$el.find('.create-dialog-create-button')
            };

            this.resetAllTextBoxes();
            this._initSelect2Fields();
            this._fillServersData();
        },

        onOpenDialog: function(macroOptions) {
            if (macroOptions && macroOptions.params && macroOptions.name === this.dialogView.macroId) {
                this.macroOptions = macroOptions;

                this._fillServersData().done(function() {
                    this.formData.reset();
                    this.view.$server.select2('val', macroOptions.params['jira-server-id'], true);
                }.bind(this));
            } else {
                this.macroOptions = null;
            }
        },

        _initSelect2Fields: function() {
            this.setupSelect2(this.view.$server,
                    'select2-server-container',
                    'select2-server-dropdown',
                    AJS.I18n.getText('jira.sprint.server.placeholder'),
                    true);

            this.setupSelect2(this.view.$board,
                    'select2-board-container',
                    'select2-board-dropdown',
                    AJS.I18n.getText('jira.sprint.board.placeholder'),
                    true);

            this.setupSelect2(this.view.$sprint,
                    'select2-sprint-container',
                    'select2-sprint-dropdown',
                    AJS.I18n.getText('jira.sprint.sprint.placeholder'),
                    true);

            // update tab index for select boxes otherwise tab key won't work on them
            this.$('.aui-select2-choice').attr('tabindex', 10);
        },

        /**
         * Allow third-party change container and then re-render UI
         * @param options
         */
        setOptionsAndRerender: function(options) {
            // update this.el for current view.
            this.el = options.el;
            this.$el = $(this.el);
            this.delegateEvents();

            this.render();
        },

        fillDataInSelect2: function($select, dfd) {
            this.toggleSelect2Loading($select, true);

            dfd.done(function() {
                this.toggleSelect2Loading($select, false);
                this.view.$errorMessage.empty().addClass('hidden');
            }.bind(this));

            dfd.fail(function(xhr, errorStatus) {
                this.handleRequestError($select, errorStatus);
            }.bind(this));

            return dfd;
        },

        /**
         * Load server data and fill in sprint select box.
         * @private
         */
        _fillServersData: function() {
            // TODO: do need to cache server data
            return this.fillDataInSelect2(this.view.$server, service.loadJiraServers())
                    .done(function(data) {
                        this.servers = data.servers;
                        this.primaryServer = data.primaryServer;

                        if (this.servers.length === 0) {
                            this.renderErrorNoAppLink();
                            return;
                        }

                        var templateSelect2Option = this.template.serverOptions;
                        this.fillDataSelect2(this.view.$server, templateSelect2Option, {servers: this.servers});

                        // only have one server, select it and hide server select2
                        if (this.servers.length === 1) {
                            this.view.$server.parent().addClass('hidden');
                            this.removeDefaultOptionOfSelect2(this.view.$server);

                            // trigger change to load other data, such as board data
                            this.view.$server.val(this.servers[0].id).trigger('change');
                        }
                    }.bind(this));
        },

        /**
         * Load board data and fill in sprint select box.
         * @private
         */
        _fillBoardData: function() {
            var serverId = this.formData.get('selectedServer').id;

            this.fillDataInSelect2(this.view.$board, service.loadBoardsData(serverId))
                    .done(function(boards) {
                        this.boards = boards;

                        var templateSelect2Option = this.template.boardOptions;
                        this.fillDataSelect2(this.view.$board, templateSelect2Option, {boards: boards});

                        if (this.macroOptions) {
                            this.view.$board.select2('val', this.macroOptions.params['board-id'], true);
                        }

                    }.bind(this));
        },

        /**
         * Load sprint data and fill in sprint select box.
         * @private
         */
        _fillSprintData: function() {
            var serverId = this.formData.get('selectedServer').id;
            var boardId = this.formData.get('selectedBoard').id;

            var dfd = service.loadSprintsData(serverId, boardId);
            this.fillDataInSelect2(this.view.$sprint, dfd)
                    .done(function(sprints) {
                        this.sprints = sprints;

                        var templateSelect2Option = this.template.sprintptions;
                        this.fillDataSelect2(this.view.$sprint, templateSelect2Option, {sprints: sprints});

                        if (this.macroOptions) {
                            this.view.$sprint.select2('val', this.macroOptions.params['sprint-id'], true);
                        }

                    }.bind(this));
        },

        _onSelectServerChanged: function() {
            this.formData.set('selectedBoard', null);
            this.formData.set('selectedSprint', null);
            this.formData.set('title', '');

            var serverId = this.view.$server.val();

            var selectedServer = _.findWhere(this.servers, {id: serverId});
            this.formData.set('selectedServer', selectedServer);
        },

        _onSelectBoardChanged: function() {
            var boardId = parseInt(this.view.$board.val(), 10);
            var selectedBoard = _.findWhere(this.boards, {id: boardId});
            this.formData.set('selectedBoard', selectedBoard);
            this.resetSelect2Options(this.view.$sprint);
        },

        _onSelectSprintChanged: function() {
            var sprintId = parseInt(this.view.$sprint.val(), 10);
            var selectedSprint = _.findWhere(this.sprints, {id: sprintId});
            this.formData.set('selectedSprint', selectedSprint);
        },

        _handleServerChanged: function() {
            var selectedServer = this.formData.get('selectedServer');

            if (!selectedServer) {
                return;
            }

            this.formData.set('isValid', this.validateServer(selectedServer));

            if (this.formData.get('isValid')) {
                this._fillBoardData();
                this.dialogView.toggleEnableInsertButton(true);
            } else {
                this.dialogView.toggleEnableInsertButton(false);
            }
        },

        _handleBoardChanged: function() {
            var selectedBoard = this.formData.get('selectedBoard');

            if (!selectedBoard) {
                this.resetSelect2Options(this.view.$board);
                return;
            }

            this._fillSprintData();
        },

        _handleSprintChanged: function() {
            var selectedBoard = this.formData.get('selectedBoard');
            var selectedSprint = this.formData.get('selectedSprint');

            if (!selectedBoard || !selectedSprint) {
                this.resetSelect2Options(this.view.$sprint);
                return;
            }
        },

        getUserInputData: function() {
            var jsonData = null;

            if (this.validate()) {
                jsonData = {};

                // this.formData has all data users input.
                jsonData['jira-server-id'] = this.formData.get('selectedServer').id;
                jsonData['board-id'] = this.formData.get('selectedBoard').id;
                jsonData['sprint-id'] = this.formData.get('selectedSprint').id;
            }

            return jsonData;
        },

        /**
         * Do some validations before submiting the dialog.
         * @returns {boolean}
         */
        validate: function() {
            var valid = true;

            valid = valid && this.validateRequiredFields(this.view.$server, AJS.I18n.getText('jira.sprint.validation.server.required'));
            valid = valid && this.validateRequiredFields(this.view.$board, AJS.I18n.getText('jira.sprint.validation.board.required'));
            valid = valid && this.validateRequiredFields(this.view.$sprint, AJS.I18n.getText('jira.sprint.validation.sprint.required'));

            return valid;
        }
    });

    // extend with some mixins
    _.extend(JiraSprintPanelView.prototype, Select2Mixin);

    return JiraSprintPanelView;
});
