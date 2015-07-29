define('confluence/jim/util/deferred-utils', [
   'jquery',
   'underscore'
],
function(
    $,
    _
) {
    'use strict';

    var util = {

        convertPromiseToAlwaysResolvedDeferred: function(promise) {
            var newDfd = $.Deferred();

            promise.done(function(data) {
                newDfd.resolve(data);
            });

            promise.fail(function() {
                newDfd.resolve(_.toArray(arguments));
            });

            newDfd.jimJiraServerId = promise.jimJiraServerId;

            return newDfd;
        }
    };

    return util;
});


