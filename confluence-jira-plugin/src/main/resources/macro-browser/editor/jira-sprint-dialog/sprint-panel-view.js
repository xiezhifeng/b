define('confluence/jim/macro-browser/editor/jirasprint/sprint-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/dialog/abstract-panel-view',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/util/service'
],
function(
    $,
    _,
    AJS,
    Backbone,
    AbstractPanelView,
    config,
    service
) {
    'use strict';

    var JiraSprintPanelView = AbstractPanelView.extend({
        events: {
            'change .jira-boards': '_onSelectBoardChanged',
            'change .jira-sprints': '_onSelectSprintChanged'
        },

        initialize: function() {
            AbstractPanelView.prototype.initialize.apply(this, arguments);

            this.panelTitle = AJS.I18n.getText('jira.sprint.panel.title');
            this.panelId = 'jira-sprint-panel';

            // a map of template functions
            this.template = Confluence.Templates.JiraSprints.Dialog;
        },

        render: function(options) {
            AbstractPanelView.prototype.render.apply(this, arguments);

            var template = this.template.serverBoardSprintTemplate({});
            this.$el.html(template);

            this.servers = [];
            this.primaryServer = null;
            this.boards = [];
            this.sprints = [];

            // update default values of formData
            _.extend(this.formData.defaults, {
                selectedBoard: null,
                selectedSprint: null
            });
            this.listenTo(this.formData, 'change:selectedBoard', this._handleBoardChanged);
            this.listenTo(this.formData, 'change:selectedSprint', this._handleSprintChanged);
            this.listenTo(this.formData, 'change:selectedServer', function() {
                this.reset();

                if (this.formData.get('isValid')) {
                    this._fillBoardData();
                }
            });

            _.extend(this.view, {
                $boards: this.$('.jira-boards'),
                $sprints: this.$('.jira-sprints'),
                $errorMessage: this.$('.error-messages'),
                $createButton: this.dialogView.$el.find('.create-dialog-create-button')
            });

            this.reset();
            this._initSelect2Fields();
            this.initServerField();
        },

        _initSelect2Fields: function() {
            this.setupSelect2(this.view.$boards,
                    'select2-board-container',
                    'select2-board-dropdown',
                    AJS.I18n.getText('jira.sprint.board.placeholder'),
                    true);

            this.setupSelect2(this.view.$sprints,
                    'select2-sprint-container',
                    'select2-sprint-dropdown',
                    AJS.I18n.getText('jira.sprint.sprint.placeholder'),
                    true);

            // update tab index for select boxes otherwise tab key won't work on them
            this.$('.aui-select2-choice').attr('tabindex', 10);
        },

        reset: function() {
            AbstractPanelView.prototype.reset.apply(this, arguments);

            this.resetSelect2Options(this.view.$boards);
            this.resetSelect2Options(this.view.$sprints);
        },

        /**
         * Load board data and fill in sprint select box.
         * @private
         */
        _fillBoardData: function() {
            var serverId = this.formData.get('selectedServer').id;

            this.fillDataInSelect2(this.view.$boards, service.loadBoardsData(serverId))
                    .done(function(boards) {
                        this.boards = boards;

                        var templateSelect2Option = this.template.boardOptions;
                        this.fillDataSelect2(this.view.$boards, templateSelect2Option, {boards: boards});

                        if (this.macroOptions) {
                            this.setSelect2Value(this.view.$boards, this.macroOptions.params.boardId);
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
            this.fillDataInSelect2(this.view.$sprints, dfd)
                    .done(function(sprints) {
                        this.sprints = sprints;

                        var templateSelect2Option = this.template.sprintOptions;
                        this.fillDataSelect2(this.view.$sprints, templateSelect2Option, {sprints: sprints});

                        if (this.macroOptions) {
                            this.setSelect2Value(this.view.$sprints, this.macroOptions.params.sprintId);
                        }

                    }.bind(this));
        },

        _onSelectBoardChanged: function() {
            this.resetSelect2Options(this.view.$sprints);
            var boardId = parseInt(this.view.$boards.val(), 10);
            var selectedBoard = _.findWhere(this.boards, {id: boardId});
            this.formData.set('selectedBoard', selectedBoard);
        },

        _onSelectSprintChanged: function() {
            var sprintId = parseInt(this.view.$sprints.val(), 10);
            var selectedSprint = _.findWhere(this.sprints, {id: sprintId});
            this.formData.set('selectedSprint', selectedSprint);
        },

        _handleBoardChanged: function() {
            var selectedBoard = this.formData.get('selectedBoard');

            if (!selectedBoard) {
                this.resetSelect2Options(this.view.$boards);
                return;
            }

            this._fillSprintData();
        },

        _handleSprintChanged: function() {
            var selectedBoard = this.formData.get('selectedBoard');
            var selectedSprint = this.formData.get('selectedSprint');

            if (!selectedBoard || !selectedSprint) {
                this.resetSelect2Options(this.view.$sprints);
            }
        },

        getUserInputData: function() {
            var userInputData = null;

            if (this.validate()) {
                // this.formData has all data users input.
                userInputData = {
                    serverId: this.formData.get('selectedServer').id,
                    boardId: this.formData.get('selectedBoard').id,
                    sprintId: this.formData.get('selectedSprint').id,
                    sprintName: this.formData.get('selectedSprint').name
                };
            }

            return userInputData;
        },

        /**
         * Do some validations before submiting the dialog.
         * @returns {boolean}
         */
        validate: function() {
            var valid = true;

            valid = valid && this.validateRequiredFields(this.view.$servers, AJS.I18n.getText('jira.sprint.validation.server.required'));
            valid = valid && this.validateRequiredFields(this.view.$boards, AJS.I18n.getText('jira.sprint.validation.board.required'));
            valid = valid && this.validateRequiredFields(this.view.$sprints, AJS.I18n.getText('jira.sprint.validation.sprint.required'));

            return valid;
        }
    });

    return JiraSprintPanelView;
});
