var JiraTimeline = window.JiraTimeline || {};
JiraTimeline = (function($, _) {
    function JiraTimeline(opts) {
        this.timelineClass = '.jira-timeline';
        this.setupData();

        this.init();
    }

    JiraTimeline.prototype = {
        init: function() {
            if (this.$timelineEl.length) {
                this.loadTimeline();
                this.setupPackingLot();
            }
        },
        loadTimeline: function() {
            // Instantiate our timeline object.
            this.timelineObj = new links.Timeline(this.$timelineEl[0]);

            // Draw our timeline with the created data and options
            this.timelineObj.draw(this.getData().data, {
                width:  this.dataSource.options.width,
                height: this.dataSource.options.height,
                layout: 'box',

                groupsChangeable : true,
                groupsOnRight: false,
                groupsOrder: true,

                editable: false,
                showCustomTime: true,
                editable: true,

                zoomMax: 31104000000
            });
            this.addEventListener();
        },
        setupData: function() {
            this.$timelineEl = $(this.timelineClass);
            var attr = this.$timelineEl.attr('datasource');
            this.dataSource = window[attr];
        },
        getData: function() {
            var me = this;
            if (!this.issueList) {
                var data = [], packing = [];
                _.each(this.dataSource.data, function(item) {
                    item.content = me.formatContentItem(item);
                    if (item.start === undefined) {
                        packing.push(item);
                    } else {
                        data.push(item);
                    }
                });
                this.issueList = {
                    data: data,
                    packing: packing
                };
            }
            return this.issueList;
        },
        formatContentItem: function(item) {
            return Confluence.Templates.Timeline.issue({issue: item});
        },
        setupPackingLot: function() {
            this.$packing = $('<div />', {
                'class': 'jira-timeline-packinglot'
            });
            this.$timelineEl.after(this.$packing);

            if (this.issueList.packing.length) {
                this.loadPackingLot();

            }
        },
        loadPackingLot: function() {
            var item = Confluence.Templates.Timeline.packingItem({ list: this.issueList.packing});
            this.$packing.append(item);

            this.setupDragDrop();
        },
        setupDragDrop: function() {
            $('.packing-item', this.$packing).draggable({
                cursor: 'move',
                revert: 'invalid'
            });

            $('.timeline-groups-text', this.$timelineEl).droppable({
                accept: '.packing-item',
                activeClass: 'timeline-groups-text-active',
                hoverClass: 'timeline-groups-text-hover',
                drop: _.bind(this.onDrop, this)
            });
        },
        onDrop: function(e, ui) {
            var range = this.timelineObj.getVisibleChartRange();
            var newItem = {
                start: new Date((range.start.valueOf() + range.end.valueOf()) / 2),
                content: ui.draggable[0].innerHTML,
                group: e.target.innerHTML
            };

            this.timelineObj.addItem(newItem);

            // Active item has just added
            var count = this.timelineObj.getData().length;
            this.timelineObj.setSelection([{
                'row': count-1
            }]);

            // Remove drag item
            ui.draggable.remove();

            return true;
        },
        convertUpdateIssueToJSON:  function(issue) {
            console.log(issue);

            var issueObject = {
                key: issue.key,
                fields: {}
            };
            var startDate = issue.start.getFullYear() + "-" + (issue.start.getMonth() + 1) + "-" + issue.start.getDate();
            issueObject.fields[this.dataSource.options.startDate] = startDate;
            return JSON.stringify(issueObject);
        },
        updateTimelineIssue: function() {
            var updateIssue = this.issueList.data[this.timelineObj.getSelection()[0].row];
            var updateUrl = Confluence.getContextPath() + "/rest/jiraanywhere/1.0/jira-issue/update-timeline-issue";
            updateUrl = updateUrl + "/" + this.dataSource.options.appId;

            $.ajax({
                type: "POST",
                contentType: "application/json",
                url: updateUrl,
                data: this.convertUpdateIssueToJSON(updateIssue),
                success: function() {
                    console.log("success");
                },
                error: function(xhr) {
                    console.log("error");
                }
            });

        },
        addEventListener: function() {
            var me = this;
            google.visualization.events.addListener(this.timelineObj, 'select', function() {
                console.log(me.timelineObj.getSelection());
            });
            google.visualization.events.addListener(this.timelineObj, 'changed', function() {
                me.updateTimelineIssue();
            });
        }
    };

    google.load("visualization", "1");
    google.setOnLoadCallback(function() {
        var timeline = new JiraTimeline({});
    });
})(AJS.$, window._);


