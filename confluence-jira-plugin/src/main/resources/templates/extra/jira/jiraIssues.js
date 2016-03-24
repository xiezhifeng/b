/**
 * Should not add more code in this file.
 * We should move to a new place 'jira/jira-issues-view-mode/*.js'
 *
 * All the logic code in this file should be reviewed.
 * Because it was coded at the time JIM was rendered from server side,
 * now Single JIM (not for table yet) is rendered lazily at client-side
 */

jQuery(document).ready(function () {
    var JiraIssues = jQuery.extend(window.JiraIssues || {}, {

        ADG_ENABLED : AJS.Meta.getNumber("build-number") >= 4000,
        ADG_FONT_SIZE_OVER_FLEXIGRID_FONT_SIZE_RATIO : 14/11,

        fixMenusShowingUnderWidgetInIE : function() {
            // http://richa.avasthi.name/blogs/tepumpkin/2008/01/11/ie7-lessons-learned/ for https://developer.atlassian.com/jira/browse/CONFJIRA-166
            if (jQuery.browser.msie) {
                jQuery("ul.ajs-menu-bar li.ajs-menu-item").each(function() {
                    jQuery(this).hover(
                            function() {
                                jQuery("div.ajs-drop-down", jQuery(this)).parents().each(function() {
                                    var $ancestor = jQuery(this);
                                    var position = $ancestor.css("position");
                                    if (position && position != "auto") {
                                        $ancestor.addClass("ajs-menu-bar-z-index-fix-for-ie");
                                    }
                                });
                            },
                            function() {
                                jQuery("div.ajs-drop-down", jQuery(this)).parents().removeClass("ajs-menu-bar-z-index-fix-for-ie");
                            }
                    );
                });
            }
        },

        onSuccessFunction : function(jiraissuesTableDiv) {
            // Only adjust the height if the user did not specify a height parameter to the {jiraissues}
            if (!jQuery("fieldset input[name='height']", jiraissuesTableDiv).length) {
                var $issuesTableElement = jQuery(".bDiv table[id^='jiraissues_table']", jiraissuesTableDiv)[0];
                var flexigrid = $issuesTableElement.grid;
                var newHeight = $issuesTableElement.clientHeight + jQuery(".hDiv", jiraissuesTableDiv)[0].clientHeight;

                jQuery(".bDiv", jiraissuesTableDiv).css("height", newHeight + "px");
                flexigrid.options.height = newHeight;
                flexigrid.fixHeight(newHeight);
            }
        },

        onErrorFunction: function (jiraissuesTableDiv, tableId, jiraissuesError, XMLHttpRequest, textmsg) {
            var $flexigridTable = jQuery("#" + tableId);
            var errorMsg = jiraissuesError + ': ';
            if (XMLHttpRequest.status == '200') {
                errorMsg += textmsg;
            } else{
                errorMsg += XMLHttpRequest.responseText;
            }
            var authHeader = XMLHttpRequest.getResponseHeader("WWW-Authenticate") || "";
            if (XMLHttpRequest.status == "401" && authHeader.indexOf("OAuth") != -1){
                var realmRegEx = /OAuth realm\=\"([^\"]+)\"/;
                var matches = realmRegEx.exec(authHeader);
                if (matches){
                    $flexigridTable.empty();
                    
                    JiraIssues.bigMessageFunction(tableId, '<a class="oauth-init" href="' + matches[1] + '">' + 
                        AJS.I18n.getText("jiraissues.oauth.linktext") + 
                        '</a> ' + AJS.I18n.getText("jiraissues.oauth.table.message") + '</span>');
                    
                    jQuery('.bmDiv', jiraissuesTableDiv).css({"z-index": 2});
                    var oauthCallbacks = {
                            onSuccess: function() {
                                window.location.reload();
                            },
                            onFailure: function() {
                            }
                    };
                    var oauthLink = jQuery('.oauth-init', $flexigridTable.parent());   
                    var authUrl = oauthLink.attr("href");
                    oauthLink.click(function(e){
                        AppLinks.authenticateRemoteCredentials(authUrl, oauthCallbacks.onSuccess, oauthCallbacks.onFailure);
                        e.preventDefault();
                    });  
                    AJS.$('.gBlock').remove();
                }
            }
            else if(XMLHttpRequest.status == "400"){
                JiraIssues.bigMessageFunction(tableId, AJS.I18n.getText("jiraissues.badrequest.possibilities"));
            }
            else{
                var $iFrame = jQuery("iframe.jiraissues_errorMsgSandbox", jiraissuesTableDiv);
    
                $iFrame.load(function() {
                    var iframeDocument = this.contentWindow || this.contentDocument;
                    var $iframeBody = jQuery((iframeDocument.document ? iframeDocument.document : iframeDocument).body);
    
                    jQuery("a", $iframeBody).each(function() {
                        this.target = "_top";
                    });
    
                    jQuery('.pPageStat', jiraissuesTableDiv).empty().text($iframeBody.text());
    
                    var $iFrameContainerElement = jQuery("div.bmDiv", jiraissuesTableDiv)[0];
                    $iFrame.removeClass("hidden");
                    $iFrame.css({
                        'height': $iFrameContainerElement.clientHeight + "px",
                        'width': $iFrameContainerElement.clientWidth + "px"
                    });
                    
                    
                });
    
                // While this is not exactly XMLHttpRequest.responseText, it is 99% the same error content (caused by invalid URL params specified to JIRA).
                // XMLHttpRequest.responseText contains the <html> and <head> elements and when appended to any element, nothing appears in it -
                // even via jQuery (I cannot set the responseText to a jQuery object and retrieve any meaningful value from it).
                // However, the iframe will load it just fine. Therefore, we ask the iframe to load the error HTML
                $iFrame[0].src = jQuery("fieldset input[name='retrieverUrlHtml']", jiraissuesTableDiv).val();
                JiraIssues.bigMessageFunction(tableId, $iFrame);
            }


            jQuery(jiraissuesTableDiv).find('.pReload').removeClass('loading'); // TODO: CONFJIRA-55 may want to change it to an error sign or something
            $flexigridTable[0].grid.loading = false; 
            // Disable all buttons on error.
            jQuery(jiraissuesTableDiv).find('.pButton').each(
                function() {
                    jQuery(this).removeClass('pBtnOver');
                    jQuery(this).css({ cursor: 'default', opacity: '0.3' });
                }
            );
            // Make page text field readonly
            jQuery(jiraissuesTableDiv).find('span.pcontrol input').attr('readonly', 'true');
            
            
        },

        onReloadFunction: function (useCache, jiraissuesTableDiv, t) {
            // removing bigMessage box if it existed
            jQuery('.bmDiv', jiraissuesTableDiv).remove();
            jQuery('.bmDistance', jiraissuesTableDiv).remove();

            t.onSubmit = function () {
                JiraIssues.reloadOnSubmitFunction(useCache, t);
                return true;
            };
        },
        reloadOnSubmitFunction: function (useCache, t) {
            t.params = [{
                name: 'useCache',
                value: false
            }];
            t.onSubmit = function () {
                JiraIssues.onSubmitFunction(useCache, t);
                return true;
            };
        },
        onSubmitFunction: function (useCache, t) {
            t.params = [{
                name: 'useCache',
                value: useCache
            }];
        },

        showTrustWarningsFunction: function (jiraissuesTableDiv, data) {
            var $trustedDiv = jQuery(jiraissuesTableDiv).children(".trusted_warnings");
            if (data.trustedMessage) {
                $trustedDiv.find("td:last").html(data.trustedMessage);
                $trustedDiv.css('display','block');
            } else {
                $trustedDiv.css('display','none');
            }
        },

        preProcessFunction: function (jiraissuesTableDiv, tableId, showTrustWarnings, data, noItemMessage) {
            if (showTrustWarnings) {
                JiraIssues.showTrustWarningsFunction(jiraissuesTableDiv, data);
            }
            
            if (data.total == 0) {
                jQuery('.pPageStat', jiraissuesTableDiv).html(noItemMessage);
                JiraIssues.bigMessageFunction(tableId, noItemMessage);
                jQuery('.pReload', jiraissuesTableDiv).removeClass('loading');
                return;
            }
        },

        bigMessageFunction: function (tableId, msg) {
            var $bmDistance = jQuery('<div></div>'); //create bigmessage distance (used to center box)
            var $bmDiv = jQuery('<div></div>'); //create bm box
            $bmDistance.addClass('bmDistance');
            $bmDiv.addClass('bmDiv');

            if (typeof msg == "string") {
                $bmDiv.html('<p><strong>' + msg + '</strong></p>');
            } else {
                msg.appendTo($bmDiv);
            }

            var $table = jQuery('#' + tableId);
            $table.after($bmDiv).after($bmDistance);
        },

        getParamsFrom: function ($fieldset) {
            var params = {};
            $fieldset.children("input").each(function () {
                params[jQuery(this).attr('name')] = jQuery(this).attr('value');
            });
            return params;
        },


        // tableParent is a jquery object, used to calculate our max width
        initializeColumnWidth: function ($tableParent, columnArray) {
            var columnWidths = {},
                autoAdjustColumnWidthForAdg = function(originalWidth) {
                    return JiraIssues.ADG_ENABLED ? Math.round(originalWidth * JiraIssues.ADG_FONT_SIZE_OVER_FLEXIGRID_FONT_SIZE_RATIO) : originalWidth;
                };

            if (!(columnArray && columnArray.length)) {
                return columnWidths;
            }

            // compute for the space that can be allocated, the overhead of the columns are also accounted for
            var tableOverhead = 37, // approx scroll bar width + table padding + border + margin
                columnOverhead = 11, // approx cell padding + border + margin
                spaceRemaining = $tableParent.width() - (tableOverhead + (columnArray.length * columnOverhead)),
                hasSummary = false,
                hasDescription = false,
                columnsWithWidth = 0,
                otherColumnWidth = autoAdjustColumnWidthForAdg(140);

            // set the widths for columns with default column width
            for (var i=0, length = columnArray.length; i < length; i++) {
            	var columnKey = columnArray[i].name;

                switch (columnKey) {
                    case "summary":
                        hasSummary = true;
                        columnsWithWidth++;
                        break;
                    case "description":
                        hasDescription = true;
                        columnsWithWidth++;
                        break;
                    case "type":
                        columnsWithWidth++;
                        columnWidths[columnKey] = 30;
                        spaceRemaining -= 30;
                        break;
                    case "priority":
                        columnsWithWidth++;
                        columnWidths[columnKey] = 50;
                        spaceRemaining -= 50;
                        break;
                    case "status":
                        columnsWithWidth++;
                        columnWidths[columnKey] = autoAdjustColumnWidthForAdg(100);
                        spaceRemaining -= autoAdjustColumnWidthForAdg(100);
                        break;
                    case "key":
                        columnsWithWidth++;
                        columnWidths[columnKey] = autoAdjustColumnWidthForAdg(90);
                        spaceRemaining -= autoAdjustColumnWidthForAdg(90);
                        break;
                    case "comments":
                    case "attachments":
                    case "version":
                    case "component":
                    case "resolution":
                        columnsWithWidth++;
                        columnWidths[columnKey] = autoAdjustColumnWidthForAdg(80);
                        spaceRemaining -= autoAdjustColumnWidthForAdg(80);
                        break;
                    default: // set the column width of anything else to a fixed column width (if there is a summary)
                        columnWidths[columnKey] = otherColumnWidth;
                }
            }

            // give all remaining space to the stretchable columns
            if (hasSummary || hasDescription) {
                spaceRemaining -= (otherColumnWidth * (columnArray.length - columnsWithWidth));
                var minWidth = 250;
                if (hasSummary && hasDescription) {
                    columnWidths.summary = Math.max(spaceRemaining / 2, minWidth);
                    columnWidths.description = Math.max(spaceRemaining / 2, minWidth);
                } else if (hasSummary) {
                    columnWidths.summary = Math.max(spaceRemaining, minWidth);
                } else {
                    columnWidths.description = Math.max(spaceRemaining, minWidth);
                }
            // adjust the size for other columns if there is no summary column
            } else if (!hasSummary && !hasDescription && (columnArray.length > columnsWithWidth)) {
                otherColumnWidth = spaceRemaining / (columnArray.length - columnsWithWidth);

                // adjust the size the columns with
                for (i=0; i < length; i++) {
                    if (!{resolution: true, key: true, type: true, priority: true, status: true}[columnArray[i]]) {
                        columnWidths[columnArray[i]] = otherColumnWidth;
                    }
                }
            }

            return columnWidths;
        }
    });

    JiraIssues.fixMenusShowingUnderWidgetInIE();

    jQuery(".jiraissues_table").each(function (i, jiraissuesTableDiv) {
        var $jiraissuesTableDiv = jQuery(jiraissuesTableDiv),
            $fieldset = $jiraissuesTableDiv.children("fieldset"),
            params = JiraIssues.getParamsFrom($fieldset),
            tableId = 'jiraissues_table_' + i;

        $jiraissuesTableDiv.append('<table id="' + tableId + '" style="display:none"></table>');
        $jiraissuesTableDiv.css("width", params["width"]);

        // get the columns from the input params
        var columns = [];
        $fieldset.children(".columns").each(function (i) {
            var $nowrapValue = jQuery(this).hasClass("nowrap");
            columns[i] = {
                display: this.name,
                name: this.value,
                nowrap: $nowrapValue,
                sortable : true,
                align: 'left'
            };
        });

        var columnWidths = JiraIssues.initializeColumnWidth($jiraissuesTableDiv,columns);
        jQuery.each(columns, function (i, column) {
        	column.width = columnWidths[column.name];
        });

        var $titleDiv = jQuery("<div></div>");
        jQuery("<a></a>")
                .attr({
                    rel: "nofollow",
                    href: params.clickableUrl
                })
                .text(params.title)
                .appendTo($titleDiv);

        //flexify this
        jQuery('#' + tableId).flexigrid({
            url: params.retrieverUrlHtml,
            method: 'GET',
            dataType: 'json',
            colModel: columns,
            sortname: params.sortField,
            sortorder: params.sortOrder,
            usepager: true,
            title: $titleDiv.html(),
            page: parseInt(params.requestedPage, 10), // unfortunately this is ignored
            useRp: false,
            rp: parseInt(params.resultsPerPage, 10),
            showTableToggleBtn: true,
            height: (function() {
                return params.height ? parseInt(params.height, 10) : 480; // Simply return the default height (used to be in JiraIssuesMacro) if none specified. Blame IE..
            })(),
            onSuccess: function() {
                JiraIssues.onSuccessFunction(jiraissuesTableDiv);
            },
            onSubmit: function () {
                          JiraIssues.onSubmitFunction(params.useCache, this);
                          return true;
                      },
            preProcess: function (data) {
                            JiraIssues.preProcessFunction(jiraissuesTableDiv, tableId, params.showTrustWarnings, data, params.nomsg);
                            return data;
                        },
            onError: function (XMLHttpRequest,textmsg,error) {
                         JiraIssues.onErrorFunction(jiraissuesTableDiv, tableId, params.jiraissuesError, XMLHttpRequest, textmsg, error);
                     },
            onReload: function () {
                          JiraIssues.onReloadFunction(params.useCache, jiraissuesTableDiv, this);
                          return true;
                      },
            errormsg: params.errormsg,
            pagestat: params.pagestat,
            procmsg: params.procmsg,
            nomsg: params.nomsg
        });
    });

    jQuery(".jiraissues_count").each(function (i, jiraissuesCountSpan) {
        var $jiraissuesCountSpan = jQuery(jiraissuesCountSpan);
        
        jQuery.ajax({
            cache: false,
            type: 'GET',
            url: $jiraissuesCountSpan.find(".url").text(),
            data: {
                useCache : $jiraissuesCountSpan.find(".use-cache").text(),
                rp : $jiraissuesCountSpan.find(".rp").text(),
                showCount : "true"
            },
            success: function (issueCount) {
                var resultLink = $jiraissuesCountSpan.find(".result");
                if(issueCount == 1)
                    resultLink.text(AJS.format(AJS.I18n.getText("jiraissues.issue.word"), issueCount));
                else
                    resultLink.text(AJS.format(AJS.I18n.getText("jiraissues.issues.word"), issueCount));
                resultLink.removeClass("hidden");
                jQuery(".calculating, .error, .data", $jiraissuesCountSpan).remove();
            },
            error: function (XMLHttpRequest) {
                var $errorSpan = jQuery(".error", $jiraissuesCountSpan).removeClass("hidden"),
                    authHeader = XMLHttpRequest.getResponseHeader("WWW-Authenticate") || "",
                    isOauthRequired = false;

                if (XMLHttpRequest.status === 401 && authHeader.indexOf("OAuth") != -1) {
                    var realmRegEx = /OAuth realm\=\"([^\"]+)\"/,
                        matches = realmRegEx.exec(authHeader);

                    if (matches) {
                        $errorSpan.empty().append(
                            AJS.$("<a/>", {
                                "href" : matches[1],
                                "class" : "oauth-init"
                            }).text(
                                    AJS.I18n.getText("jiraissues.oauth.linktext")
                            ).click(function() {
                                    AppLinks.authenticateRemoteCredentials(matches[1], function() {
                                        window.location.reload();
                                    }, function() {

                                    });
                                    return false;
                            })
                        ).append(
                                AJS.$("<span/>", {
                                    "text" : " "  + AJS.I18n.getText("jiraissues.oauth.table.message")
                                })
                        );

                        isOauthRequired = true;
                    }
                }

                if (!isOauthRequired)
                    $errorSpan.text(AJS.format($errorSpan.text(), XMLHttpRequest.status));

                jQuery(".calculating, .result, .data", $jiraissuesCountSpan).remove();
            }
        });
    });

});
