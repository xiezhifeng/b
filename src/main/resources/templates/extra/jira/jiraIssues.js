jQuery(document).ready(function () {
    var JiraIssues = {
        onSuccessFunction : function(jiraissues_table) {
            // Only adjust the height if the user did not specify a height parameter to the {jiraissues}
            if (!jQuery("fieldset input[name='height']", jiraissues_table).length) {
                var issuesTableElement = jQuery(".bDiv table[id^='jiraissues_table']", jiraissues_table).get(0);
                var flexigrid = issuesTableElement.grid;
                var newHeight = issuesTableElement.clientHeight + jQuery(".hDiv", jiraissues_table).get(0).clientHeight;

                jQuery(".bDiv").css("height", newHeight + "px");
                flexigrid.options.height = newHeight;
                flexigrid.fixHeight(newHeight);
            }
        },

        onErrorFunction: function (jiraissues_table, tableId, jiraissuesError, XMLHttpRequest, textmsg, error) {
            var errorMsg = jiraissuesError + ': ';
            if (XMLHttpRequest.status == '200') {
                errorMsg += textmsg;
            } else {
                errorMsg += XMLHttpRequest.responseText;
            }

            var iFrame = jQuery("iframe.jiraissues_errorMsgSandbox", jiraissues_table);
            var iFrameElement = iFrame.get(0);

            iFrame.load(function() {
                var iframeDocument = iFrameElement.contentWindow || iFrameElement.contentDocument;
                var iframeBody = jQuery((iframeDocument.document ? iframeDocument.document : iframeDocument).body);

                jQuery("a", iframeBody).each(function() {
                    this.target = "_top";
                });

                jQuery('.pPageStat', jiraissues_table).empty().html(iframeBody.text());

                var iFrameContainerElement = jQuery("div.bmDiv", jiraissues_table).get(0);
                iFrame.removeClass("hidden");
                iFrame.css({
                    height: iFrameContainerElement.clientHeight + "px",
                    width: iFrameContainerElement.clientWidth + "px"
                });
            });

            // While this is not exactly XMLHttpRequest.responseText, it is 99% the same error content (caused by invalid URL params specified to JIRA).
            // XMLHttpRequest.responseText contains the <html> and <head> elements and when appended to any element, nothing appears in it -
            // even via jQuery (I cannot set the responseText to a jQuery object and retrieve any meaningful value from it).
            // However, the iframe will load it just fine. Therefore, we ask the iframe to load the error HTML
            iFrameElement.src = jQuery("fieldset input[name='retrieverUrlHtml']", jiraissues_table).val();
            JiraIssues.bigMessageFunction(tableId, iFrame);


            jQuery(jiraissues_table).find('.pReload').removeClass('loading'); // TODO: CONFJIRA-55 may want to change it to an error sign or something
            //		this.loading = false; // need to bring "this" param over if want to do this, but what does this accomplish anyway?
            // Disable all buttons on error.
            jQuery(jiraissues_table).find('.pButton').each(
                function() {
                    $(this).removeClass('pBtnOver');
                    $(this).css({ cursor: 'default', opacity: '0.3' });
                }
            );
            // Make page text field readonly
            jQuery(jiraissues_table).find('span.pcontrol input').attr('readonly', 'true');
        },

        onReloadFunction: function (useCache, jiraissues_table, t) {
            // removing bigMessage box if it existed
            jQuery('.bmDiv', jiraissues_table).remove();
            jQuery('.bmDistance', jiraissues_table).remove();

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

        showTrustWarningsFunction: function (jiraissues_table, data) {
            var trustedDiv = jQuery(jiraissues_table).children(".trusted_warnings");
            if (data.trustedMessage) {
                trustedDiv.find("td:last").html(data.trustedMessage);
                trustedDiv.css('display','block');
            } else {
                trustedDiv.css('display','none');
            }
        },

        preProcessFunction: function (jiraissues_table, tableId, showTrustWarnings, data, noItemMessage) {
            if (showTrustWarnings) {
                JiraIssues.showTrustWarningsFunction(jiraissues_table, data);
            }

            if (data.total == 0) {
                jQuery('.pPageStat', jiraissues_table).html(noItemMessage);
                JiraIssues.bigMessageFunction(tableId, noItemMessage);
                jQuery('.pReload', jiraissues_table).removeClass('loading');
            }
        },

        bigMessageFunction: function (tableId, msg) {
            var bmDistance = jQuery(document.createElement('div')); //create bigmessage distance (used to center box)
            var bmDiv = jQuery(document.createElement('div')); //create bm box
            bmDistance.addClass('bmDistance');
            bmDiv.addClass('bmDiv');

            if (typeof msg == "string") {
                bmDiv.html('<p><strong>' + msg + '</strong></p>');
            } else {
                msg.appendTo(bmDiv);
            }

            var table = jQuery('#' + tableId);
            table.after(bmDiv).after(bmDistance);
        },

        getParamsFrom: function (fieldset) {
            var params = {};
            fieldset.children("input").each(function () {
                params[jQuery(this).attr('name')] = jQuery(this).attr('value');
            });
            return params;
        },

    // retrieves the width of the window (excluding the scrollbar). Handles different browser config.
//    getWindowWidth: function () {
//        if (typeof(window.innerWidth) == 'number')
//        {
//            //Non-IE
//            return window.innerWidth - 16;
//        }
//        else if (document.documentElement && document.documentElement.clientWidth)
//        {
//            //IE 6+ in 'standards compliant mode'
//            return document.documentElement.clientWidth - 16;
//        }
//        else if (document.body && document.body.clientWidth)
//        {
//            //IE 4 compatible
//            return document.body.clientWidth - 16;
//        }
//
//        return 1280; // default
//    },

        initializeColumnWidth: function (columnArray) {
            var columnWidths = {};

            if (!(columnArray && columnArray.length)) {
                return columnWidths;
            }

            // compute for the space that can be allocated, the overhead of the columns are also accounted for
            var spaceRemaining = jQuery(window).width() - (37 + (columnArray.length * 11));
            var hasSummary = false;
            var hasDescription = false;
            var columnsWithWidth = 0;

            var otherColumnWidth = 140;

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
                        columnWidths[columnKey] = 100;
                        spaceRemaining -= 100;
                        break;
                    case "key":
                        columnsWithWidth++;
                        columnWidths[columnKey] = 90;
                        spaceRemaining -= 90;
                        break;
                    case "comments":
                    case "attachments":
                    case "version":
                    case "component":
                    case "resolution":
                        columnsWithWidth++;
                        columnWidths[columnKey] = 80;
                        spaceRemaining -= 80;
                        break;
                    default: // set the column width of anything else to a fixed column width (if there is a summary)
                        columnWidths[columnKey] = otherColumnWidth;
                }
            }

            // set the remaining space to the summary column
            // set a minimum size for the summary column
            if (hasSummary || hasDescription) {
                spaceRemaining -= (otherColumnWidth * (columnArray.length - columnsWithWidth));
                if (hasSummary && hasDescription) {
                    columnWidths.summary = Math.max(spaceRemaining / 2, 250);
                    columnWidths.description = Math.max(spaceRemaining / 2, 250);
                } else if (hasSummary) {
                    columnWidths.summary = Math.max(spaceRemaining, 250);
                } else {
                    columnWidths.description = Math.max(spaceRemaining, 250);
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
    };

    jQuery(".jiraissues_table").each(function (i, jiraissues_table) {
        var fieldset = jQuery(jiraissues_table).children("fieldset");
        fieldset.append('<input type="hidden" name="id" value="' + i + '">');
        var params = JiraIssues.getParamsFrom(fieldset);
        var tableId = 'jiraissues_table_' + params.id;
        jQuery(jiraissues_table).append('<table id="' + tableId + '" style="display:none"></table>');

        jQuery(jiraissues_table).css("width", params["width"]);

        var sortEnabled = params.sortEnabled == "true";

        // get the columns from the input params
        var columns = [];
        fieldset.children(".columns").each(function (i) {
            var nowrapValue = jQuery(this).hasClass("nowrap");

            columns[i] = {
                display: this.name,
                name: this.value,
                nowrap: nowrapValue,
                sortable : sortEnabled,
                align: 'left'
            };
        });

        var columnWidths = JiraIssues.initializeColumnWidth(columns);
        jQuery.each(columns, function (i, column) {
        	column.width = columnWidths[column.name];
        });

        titleDiv = jQuery(document.createElement("div"));
        titleLink = jQuery(document.createElement("a"));

        titleLink.attr("rel", "nofollow");
        titleLink.attr("href", params.clickableUrl);
        titleLink.text(params.title);
        titleLink.appendTo(titleDiv);

        //flexify this
        jQuery('#' + tableId).flexigrid({
            url: params.retrieverUrlHtml,
            method: 'GET',
            dataType: 'json',
            colModel: columns,
            sortname: params.sortField,
            sortorder: params.sortOrder,
            usepager: true,
            title: titleDiv.html(),
            page: parseInt(params.requestedPage, 10), // unfortunately this is ignored
            useRp: false,
            rp: parseInt(params.resultsPerPage, 10),
            showTableToggleBtn: true,
            height: (function() {
                return params.height ? parseInt(params.height, 10) : 480; // Simply return the default height (used to be in JiraIssuesMacro) if none specified. Blame IE..
            })(),
            onSuccess: function() {
                JiraIssues.onSuccessFunction(jiraissues_table);
            },
            onSubmit: function () {
                          JiraIssues.onSubmitFunction(params.useCache, this);
                          return true;
                      },
            preProcess: function (data) {
                            JiraIssues.preProcessFunction(jiraissues_table, tableId, params.showTrustWarnings, data, params.nomsg);
                            return data;
                        },
            onError: function (XMLHttpRequest,textmsg,error) {
                         JiraIssues.onErrorFunction(jiraissues_table, tableId, params.jiraissuesError, XMLHttpRequest, textmsg, error);
                     },
            onReload: function () {
                          JiraIssues.onReloadFunction(params.useCache, jiraissues_table, this);
                          return true;
                      },
            errormsg: params.errormsg,
            pagestat: params.pagestat,
            procmsg: params.procmsg,
            nomsg: params.nomsg
        });
    });

    jQuery(".jiraissues_count").each(function (i, jiraissues_count) {
        var fieldset = jQuery(jiraissues_count).children("fieldset");
        fieldset.append('<input type="hidden" name="id" value="' + i + '">');
        var params = JiraIssues.getParamsFrom(fieldset);
        jQuery.ajax({
            type: 'GET',
            url: params.retrieverUrlHtml,
            data: 'useCache=' + params.useCache + '&rp=' + params.resultsPerPage + '&showCount=true',
            success: function (issueCount) {
                jQuery(jiraissues_count).append('<span id="jiraissues_count_' + params.id + '"><a rel="nofollow" href="' + params.clickableUrl + '">' + issueCount + ' ' + params.issuesWord + '</a></span>');
            },
            error: function (XMLHttpRequest) {
                var errorMsg = params["jiraissuesErrorReceivedErrorFromServer"] + ' ' + XMLHttpRequest.status + ". " + params["jiraissuesErrorCheckLogs"];
                jQuery(jiraissues_count).append("<span class='error'>" + errorMsg + "</span>");
            }
        });
    });

});
