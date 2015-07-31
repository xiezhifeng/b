define('confluence/jim/util/retry-caller', [
   'ajs',
    'jquery',
   'underscore'
],
function(
    AJS,
    $,
    _
) {
    'use strict';

     /**
     * Automatically retrying failing Deferred calls.
     * If maxium of attemps is reached, it will rejected with list of errors of all attemps.
     *
     * @example:
     * var getWeatherNewsFromAService = function (options) {...};
     * retryCaller(getWeatherNewsFromAService, {
     *     context: this,
     *     args: {url: http://weather-service.com/rest/gettoday},
     *     tester: function (error) {
     *         return (error === 'network timeout');
     *     }
     * })
     *
     * @param  {function} func - a master function which is attempted to called several times until it is resolved
      * or reach maximum calling times.
     * @param  {object} options:
     * @param  {object} options.context - a context for resolving Deferred.
     * @param  {array} options.args - list of arguments for calling master function.
     * @param  {array} options.delays - list of delay times to retry to call master function.
     * @param  {function} options.tester - each times the underlying fucntions fails,
      * pass the error value to the checking function.
     * If it is return true, continue to retry the underlying function.
     * Otherwise, reject the master deferred.
     * @return {Promise}
     */
    var retryCaller = function (func, options) {
        options = options || {};
        var args = options.args;
        var tester = (typeof options.tester === 'function') ? options.tester : function () {return true;};
        var delays = options.delays || [0.1, 0.3, 0.5, 0.7, 1];
        var lengDelays = delays.length;
        var name = options.name || '';

        var deferred = $.Deferred(),
            promise = deferred.promise();

        var context = options.context || deferred;

        var rejectedErrors = [],
            attemptCount = 0;

        /**
         * 'error' will be run each time a promise returned by the underlying function is rejected.
         * @param {object} rejectedValue - rejected value.
         */
        var error = function (rejectedValue) {
            rejectedErrors.push(rejectedValue);

            // If the number of allowed attempts has been reached, reject the master deferred
            // with the original reject value.
            if (attemptCount === lengDelays) {
                return deferred.rejectWith(context, [rejectedErrors]);
            }

            // We still have some attempts left, so call the underlying function again.

            if (tester(rejectedValue)) {
                // current reject is error, continue to attempt.
                call();
            } else {
                // current reject is not error, resolve it.
                deferred.resolveWith(context, [rejectedValue]);
            }
        };

        var call = function () {
             // If the number of allowed attempts has been reached, reject the master deferred
            // with the original reject value.
            if (attemptCount === lengDelays) {
                AJS.debug('retry-caller: "', name ,'" - rejected due to exceed maximum time (', lengDelays, ')');
                return deferred.rejectWith(context, [context, 'exceed-maximum-called-times', '']);
            }

            AJS.debug('retry-caller: "', name ,'" - #', attemptCount, ', timeout = ', delays[attemptCount]);

            // Wait for the next delay time to elapse before calling the underlying function.
            var timeout = delays[attemptCount++];

            var exectFunc = function() {
                $.when(func.apply(context, args))
                .then(
                    function() {
                        if (tester.apply(context, arguments)) {
                            call();
                        } else {
                            // If the underlying function runs without error,
                            // pass its return value along to the master deferred.
                            deferred.resolveWith(context, arguments);
                        }
                    },
                    // If the underlying function hits an error,
                    // call our error function,
                    // which will either arrange for a retry or reject the master deferred.
                    error
                );

            };

            _.delay(exectFunc, timeout);
        };

        // call master function first time.
        call();

        return promise;
    };

    return retryCaller;
});


