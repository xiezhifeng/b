define([
    'confluence/jim/editor-plugins/jira-paste-link'
], function(
    jiraPasteLink
) {
    'use strict';

    module('test jira-paste-link module', {
        beforeEach: function() {
        },

        afterEach: function() {
        }
    });

    test('Can load jira-paste-link module.', function() {
        ok(typeof jiraPasteLink === 'object');
    });

    test('Test "_getMatchedServerFromLink" method should get correct server object, with overlap server url', function() {
        var servers = [
            {
                id: 1,
                name: 'server 1',
                url: 'https://jira-dev.com/jira1'
            },
            {
                id: 2,
                name: 'server 2',
                url: 'https://jira-dev.com/jira'
            },
            {
                id: 3,
                name: 'server 1',
                url: 'https://jira-dev.com/jira123'
            },

        ];

        var url = 'https://jira-dev.com/jira123/issue/1';
        var matchServer = jiraPasteLink._getMatchedServerFromLink(url, servers);

        ok(matchServer.id === 3);
    });

    test('Test "_getMatchedServerFromLink" method should get correct server object, with non-overlap server url', function() {
        var servers = [
            {
                id: 1,
                name: 'server 1',
                url: 'https://jira-dev.com:123/jira1'
            },
            {
                id: 2,
                name: 'server 2',
                url: 'https://jira-dev.com:456/jira'
            },
            {
                id: 3,
                name: 'server 1',
                url: 'https://jira-dev.com:789/jira123'
            },

        ];

        var url = 'https://jira-dev.com:456/jira/issue/1';
        var matchServer = jiraPasteLink._getMatchedServerFromLink(url, servers);

        ok(matchServer.id === 2);
    });

    test('Test "_getMatchedServerFromLink" method should return null if there is no match server', function() {
        var servers = [
            {
                id: 1,
                name: 'server 1',
                url: 'https://jira-dev.com:123/jira1'
            },
            {
                id: 2,
                name: 'server 2',
                url: 'https://jira-dev.com:456/jira'
            },
            {
                id: 3,
                name: 'server 1',
                url: 'https://jira-dev.com:789/jira123'
            },

        ];

        var url = 'https://jira-dev.com:456/jira1/issue/1';
        var matchServer = jiraPasteLink._getMatchedServerFromLink(url, servers);

        ok(matchServer === null);
    });
});
