define('confluence/jim/jira/jira-issues-view-mode/fix-ui', [
    'jquery',
    'ajs'
], function(
    $,
    AJS
) {
    'use strict';

    var exportModule = {
         fixBreakIconInOldConf: function() {
            // CONF with AUI <= 5.4.4 does not support icon font in warning message box.
            var isCONFNotSupportWarningIconFont = exportModule.compareVersion(AJS.version, '5.4.4') <= 0;
            if (!isCONFNotSupportWarningIconFont) {
                return;
            }

            $('.jim-error-message').each(function() {
                var $this = $(this);
                var $message = $this;

                if ($this.hasClass('jim-error-message-table')) {
                    $message = $this.find('.aui-message');
                }

                $message
                        .addClass('warning')
                        .prepend('<span class="aui-icon icon-warning"></span>');
            });
        },

        compareVersion: function(left, right) {
            if (typeof left !== 'string' ||
                typeof right !== 'string') {
                return false;
            }

            var a = left.split('.');
            var b = right.split('.');
            var i = 0;
            var len = Math.max(a.length, b.length);

            for (; i < len; i++) {
                if ((a[i] && !b[i] && parseInt(a[i]) > 0) ||
                        (parseInt(a[i]) > parseInt(b[i]))) {
                    return 1;
                } else if ((b[i] && !a[i] && parseInt(b[i]) > 0) ||
                            (parseInt(a[i]) < parseInt(b[i]))) {
                    return -1;
                }
            }

            return 0;
        }
    };

    return exportModule;
});
