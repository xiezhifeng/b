/**
 * In old Confluence version, ex: 5.6, does not have "confluence" AMD module
 */
define('confluence/jim/confluence-shim', function() {
    'use strict';
    return window.Confluence;
});