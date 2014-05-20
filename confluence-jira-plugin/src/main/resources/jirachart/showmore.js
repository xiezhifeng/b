var TwoDimensionalShowLink = (function ($) {

    var displayDarkLayer = function (chartId) {
        var container = $('#two-dimensional-chart-' + chartId);
        var position = container.position();
        $('<div />', {
            id: 'twodimensional-dark-layout-' + chartId,
            'class': 'jim-sortable-dark-layout',
            css: {
                top: position.top + 'px',
                left: position.left + 'px',
                width: container.width() + 'px',
                height: container.height() + 'px'
            }
        }).appendTo(container.parent());
    };

    var removeDarkLayer = function (chartId) {
        $('#twodimensional-dark-layout-' + chartId).remove();
    };

    var handleRefreshClick = function () {
        var chartId =  $(this).attr('data-chart-id');
        displayDarkLayer(chartId);
        var data = {
            pageId: $('#chart-page-id-' + chartId).val(),
            wikiMarkup: $('#chart-wiki-' + chartId).val(),
            isShowMore: $(this).attr('data-is-show-more')
        };

        AJS.$.ajax({
            type: "POST",
            dataType: "html",
            url: Confluence.getContextPath() + "/plugins/servlet/showMoreRenderer",
            data: data,
            success: function(twoDimensional) {
                var newChartId = $(twoDimensional).find(".show-link-container a").attr('data-chart-id');
                $('#two-dimensional-chart-' + chartId).replaceWith(twoDimensional);
                bindShowLinkElement(newChartId);
                removeDarkLayer(chartId);
            }
        });
    };

    var bindShowLinkElement = function(chartId) {
        $("#show-link-" + chartId).on("click", handleRefreshClick);
    };

    var init = function () {
        $('.show-link-container a').each(function() {
            bindShowLinkElement($(this).attr('data-chart-id'));
        });
    };

    return {
        init: init
    };


})(AJS.$);


$(function() {
    TwoDimensionalShowLink.init();
});

