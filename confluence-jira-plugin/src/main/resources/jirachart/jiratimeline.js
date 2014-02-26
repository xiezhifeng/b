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
            var googleData = new google.visualization.DataTable();
            googleData.addColumn('datetime', 'start');
            googleData.addColumn('datetime', 'end');
            googleData.addColumn('string', 'content');
            googleData.addColumn('string', 'group');

            googleData.addRows(this.getData().data);

            // Instantiate our timeline object.
            this.timelineObj = new links.Timeline(this.$timelineEl[0]);

            // Draw our timeline with the created data and options
            this.timelineObj.draw(googleData, {
                width:  this.dataSource.options.width,
                height: this.dataSource.options.height,
                style: this.dataSource.options.style,

                groupsChangeable : true,
                groupsOnRight: false,
                //groupsChangeable: true,
                groupsOrder: true,

                editable: true,
                showCustomTime: true
            });
        },
        setupData: function() {
            this.$timelineEl = $(this.timelineClass);
            var attr = this.$timelineEl.attr('datasource');
            this.dataSource = window[attr];
        },
        getData: function() {
            if (!this.issueList) {
                var data = [], packing = [];
                _.each(this.dataSource.data, function(item) {
                    if (item[0] === undefined) {
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
                revert: true,
                helper: 'clone'
            });

            $('.timeline-groups-text', this.$timelineEl).droppable({
                accept: '.packing-item',
                activeClass: 'timeline-groups-text-active',
                hoverClass: 'timeline-groups-text-hover',
                drop: _.bind(this.onDrop, this)
            });
        },
        onDrop: function(e, ui) {
            var customTime = this.timelineObj.getCustomTime();
            var itemDrag = ui.draggable[0].innerHTML;
            //this.timelineObj.addItem([customTime, undefined, itemDrag, e.target.innerHTML]);
        }
    };

    google.load("visualization", "1");
    google.setOnLoadCallback(function() {
        var timeline = new JiraTimeline({});
    });
})(AJS.$, window._);


