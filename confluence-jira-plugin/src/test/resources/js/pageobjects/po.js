/*
 * @author Ted Piotrowski
 * @date 23/07/2014
 *
 * This file is the similar to a Page objects class in Java. It only contains selectors and these selectors are
 * passed into a jQuery $() constructor during the test to create a reference to the corresponding element. This
 * allows us to bind to elements at any point in the test. Because JS tests mock the server response, all requests
 * are done synchronously so there is no need to use a Poller and TimedQuery's to wait for the DOM to update if loading
 * data from the server.
 */

var PO = PO || {};

PO.Sidebar = {
    container: ".ic-sidebar",
    close: ".ic-action-hide",
    navNext: "#ic-nav-next",
    navPrev: "#ic-nav-previous",
    navIndex: '.ic-nav-x-out-of-y',
    errorContainer: ".ic-error"
};
