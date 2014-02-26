var JiraTimeline = window.JiraTimeline || {};
JiraTimeline = (function($, _) {
    function JiraTimeline(opts) {
        this.timelineClass = '.jira-timeline';
        this.configuration = _.extend({}, {

        }, opts);

        this.init();
    }

    JiraTimeline.prototype = {
        init: function() {
            this.placeHolder = $(this.timelineClass);

            if (this.placeHolder.length) {
                var data = new google.visualization.DataTable();
                data.addColumn('datetime', 'start');
                data.addColumn('datetime', 'end');
                data.addColumn('string', 'content');

                /*data.addRows([
                    [new Date(2010,7,23), , 'Conversation<br>' + '<img src="img/comments-icon.png" style="width:32px; height:32px;">'],
                    [new Date(2010,7,23,23,0,0), , 'Mail from boss<br>' + '<img src="img/mail-icon.png" style="width:32px; height:32px;">'],
                    [new Date(2010,7,24,16,0,0), , 'Report'],
                    [new Date(2010,7,26), new Date(2010,8,2), 'Traject A'],
                    [new Date(2010,7,28), , 'Memo<br>' + '<img src="img/notes-edit-icon.png" style="width:48px; height:48px;">'],
                    [new Date(2010,7,29), , 'Phone call<br>' + '<img src="img/Hardware-Mobile-Phone-icon.png" style="width:32px; height:32px;">'],
                    [new Date(2010,7,31), new Date(2010,8,3), 'Traject B'],
                    [new Date(2010,8,4,12,0,0), , 'Report<br>' + '<img src="img/attachment-icon.png" style="width:32px; height:32px;">']
                ]);*/
                data.addRows(this.getData().data);

                // specify options
                var options = {
                    "width":  "100%",
                    "height": "300px",
                    "style": "box"
                };

                // Instantiate our timeline object.
                timeline = new links.Timeline(this.placeHolder[0]);

                // Draw our timeline with the created data and options
                timeline.draw(data, options);

            }
        },
        getData: function() {
            var attr = this.placeHolder.attr('datasource');
            return window[attr];
        }
    };
})(AJS.$, window._);


