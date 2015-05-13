AJS.JiraIssues = {
        Remote: {}
};
var appLinksI18n = {entries:{}};
jQuery(document).ready(function () {
    AJS.JiraIssues = jQuery.extend(AJS.JiraIssues || {}, {

        bindOAuthLink: function($link, onSuccess){
            var oauthCallbacks = {
                onSuccess: function() {
                    onSuccess();
                },
                onFailure: function() {
                }
            };
            var authUrl = $link.attr("href");
            $link.click(function(e){
                AppLinks.authenticateRemoteCredentials(authUrl, oauthCallbacks.onSuccess, oauthCallbacks.onFailure);
                e.preventDefault();
            });
        },
        getOAuthRealm: function(xhr){
            var authHeader = xhr.getResponseHeader("WWW-Authenticate") || "";
            var realmRegEx = /OAuth realm\=\"([^\"]+)\"/;
            var matches = realmRegEx.exec(authHeader);
            if (matches){
                return matches[1];
            }
            else{
                return null;
            }
        }
    });

    jQuery("a.static-oauth-init").each(function(){
        AJS.JiraIssues.bindOAuthLink(jQuery(this), function(){
            window.location.reload();
        });
    });

    jQuery("a.anonymous-init").each(function(value, link){
        var redirectPage = encodeURIComponent(window.location.pathname.replace(Confluence.getContextPath(), ''));
        var url = Confluence.getContextPath() + '/login.action?os_destination=' + redirectPage;
        AJS.$(link).attr('href', url);
    });

    var loadIssuesForAppId = function(appId){
        var issues = AJS.JiraIssues.Remote[appId];
        var keyClause = '';
        for (var i = 0; i < issues.length; i++){
            keyClause = keyClause + (issues[i].key + (i < issues.length - 1 ? ',' : ''));
        }

        var createQueryUrl = function(keyClause){
            var jqlQuery = 'issuekey in (' + keyClause + ')';
            var jiraUrl = '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=' + encodeURIComponent(jqlQuery) +'&returnMax=true';
            var localUrl = contextPath + '/plugins/servlet/issue-retriever?appId=' + appId + '&url=' + encodeURIComponent(jiraUrl) +
                '&columns=summary&columns=type&columns=resolution&columns=status';
            return localUrl;
        };

        var attachIssueData = function(data){
            var items = AJS.$('item', data);
            items.each(function(){
                var link = AJS.$('link', this).text();
                var key = AJS.$('key', this).text();
                var summary = AJS.$('summary', this).text();
                var type = AJS.$('type', this);
                var resolution = AJS.$('resolution', this);
                var resolved = resolution.attr("id") != "-1";
                var status = AJS.$('status', this);

                var macros = AJS.$('.unknown-jira-issue.' + key);
                for (var i = 0; i < macros.length; i++){
                    var keyRef = AJS.$('<a style="background-image: url(\'' + type.attr('iconUrl') + '\')" href="' + link + '"></a>');
                    keyRef.text(key);

                    var statusSpan = AJS.$('<span class="jira-status"></span>');
                    statusSpan.text(status.text().toUpperCase());

                    var issueSpan = AJS.$('<span class="jira-issue' + (resolved ? ' resolved' : '') +'" ></span>');
                    issueSpan.append(keyRef);
                    issueSpan.append(document.createTextNode(' - ' + summary + ' - '));
                    issueSpan.append(statusSpan);

                    AJS.$(macros[i]).replaceWith(issueSpan);
                }
            });
        };
        var localUrl = createQueryUrl(keyClause);

        AJS.$.ajax({
            url: localUrl,
            success: attachIssueData,
            error: function(xhr){
                if (xhr.status == 401){

                    var oauthRealm = AJS.JiraIssues.getOAuthRealm(xhr);

                    if (oauthRealm){
                        var seen = {};
                        AJS.$(issues).each(function(){
                            if (!seen[this.key]){
                                seen[this.key] = true;
                                var oauthLink = AJS.$('<span class="oauth-msg"> - <a class="oauth-init" href="' + oauthRealm + '">' +
                                        AJS.I18n.getText("jiraissues.oauth.linktext") +
                                        '</a> ' + AJS.I18n.getText("jiraissues.oauth.single.message") + '</span>');
                                AJS.$('.unknown-jira-issue.' + this.key).addClass('single-issue-oauth').append(oauthLink);
                            }
                        });
                        AJS.JiraIssues.bindOAuthLink(AJS.$('.single-issue-oauth a.oauth-init'), function(){
                            window.location.reload();
                        });
                    }
                }
                else if (xhr.status == 400 && issues.length > 1){
                    // jira (proxied via conf) can return this if one of the issue keys is invalid.
                    // in that case, we make a request for each key
                    AJS.$(issues).each(function(){
                        var key = this.key;
                        var keyUrl = createQueryUrl(key);
                        AJS.$.ajax({
                            url: keyUrl,
                            success: attachIssueData,
                            error: function(xhr){
                               var linkLocation = AJS.$('.unknown-jira-issue.' + key);
                               linkLocation.removeClass('single-issue-oauth');
                               AJS.$(".oauth-msg", linkLocation).remove();
                               linkLocation.addClass('jira-error');
                            }
                        });
                    });
                }
            }
        });
    };

    AJS.$('.unknown-jira-issue').each(function(i, item){
        var $item = AJS.$(item);
        var applinkId = $item.attr('data-app-link');
        var issueKey = $item.attr('data-key');
        AJS.JiraIssues.Remote[applinkId] = AJS.JiraIssues.Remote[applinkId] || [];
        AJS.JiraIssues.Remote[applinkId].push({
            key: issueKey
        });
    });

    for (var appId in AJS.JiraIssues.Remote){
        loadIssuesForAppId(appId);
    }

});