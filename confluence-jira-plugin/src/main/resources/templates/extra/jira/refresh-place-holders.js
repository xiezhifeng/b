(function($) {
    var Pipe = window.Confluence.Pipe;

    $(function() {
        var $mainContent = $('#main-content'),
            $jimPlaceHolders = $mainContent.find('.jira-issue-placeholder');
        if ($jimPlaceHolders.length > 0) {
            var idTo$Elem = {};
            $jimPlaceHolders.each(function(idx, elem) {
                var $elem = $(elem);
                idTo$Elem[$elem.data('render-id')] = $elem;
            });
            Pipe.on('JIM', function (data) {
                var $elem = idTo$Elem[data.key];
                if ($elem)
                {
                    $elem.replaceWith(data.data)
                }
            })
        }
    });
})(AJS.$);