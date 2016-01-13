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

            // update default values of formData
            _.extend(this.formData.defaults, {
                selectedBoard: null,
                selectedSprint: null
            });

            this.listenTo(this.formData, 'change:selectedServer', function() {
                this.reset();

                if (this.formData.get('isValid')) {
                    this._fillBoardData('');
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
            var fillBoardData = _.debounce(this._fillBoardData.bind(this), 500);

            // setup board select
            this.setupSelect2({
                $el: this.view.$boards,
                placeholderText: AJS.I18n.getText('jira.sprint.board.placeholder'),
                isRequired: true,
                overrideSelect2Ops: {
                    query: function(query) {
                        fillBoardData(query.term, query.callback);
                    }
                }
            });

            // setup sprint select
            this.setupSelect2({
                $el: this.view.$sprints,
                placeholderText: AJS.I18n.getText('jira.sprint.sprint.placeholder'),
                isRequired: true
            });
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
        _fillBoardData: function(queryTerm, callback) {
            var serverId = this.formData.get('selectedServer').id;

            this.toggleSelect2Loading(this.view.$boards, true, true);

            var promise = service.loadBoardsData(serverId, queryTerm)
            .done(function(boards) {
                this.toggleSelect2Loading(this.view.$boards, false, true);
                this.dialogView.toggleEnableInsertButton(true);

                // format data to adapt select2 requirement
                _.each(boards, function(board) {
                    board.text = board.name;
                });

                var data = {
                    results: boards,
                    text: 'name'
                };

                if (callback) {
                    callback(data);
                } else {
                    // re-open from macro placeholder
                    if (this.macroOptions) {
                        var boardId = this.macroOptions.params.boardId;
                        var boardName = this.macroOptions.params.boardName;
                        boardName = boardName || boardId;

                        this.view.$boards.select2('data', {
                            id: boardId,
                            text: boardName,
                            name: boardName
                        }, true);
                    }
                }

                return data;
            }.bind(this))
            .fail(function(xhr, errorString, errorStatus) {
                this.resetSelect2Options(this.view.$sprints);
                this.handleAjaxRequestError(this.view.$boards, errorStatus);
            }.bind(this));

            return promise;
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

                    this.fillDataSelect2(this.view.$sprints, sprints);

                    if (this.macroOptions) {
                        this.setSelect2Value(this.view.$sprints, this.macroOptions.params.sprintId);
                    }

                }.bind(this));
        },

        _onSelectBoardChanged: function() {
            var selectedBoard = this.view.$boards.select2('data');

            if (!selectedBoard) {
                return;
            }

            this.formData.set('selectedBoard', selectedBoard);
            this._fillSprintData();
        },

        _onSelectSprintChanged: function() {
            var selectedSprint = this.view.$sprints.select2('data');

            if (!selectedSprint.name) {
                selectedSprint.name = selectedSprint.text;
            }

            this.formData.set('selectedSprint', selectedSprint);
        },

        getUserInputData: function() {
            var userInputData = null;

            if (this.validate()) {
                // this.formData has all data users input.
                userInputData = {
                    serverId: this.formData.get('selectedServer').id,
                    boardId: this.formData.get('selectedBoard').id,
                    boardName: this.formData.get('selectedBoard').name,
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
