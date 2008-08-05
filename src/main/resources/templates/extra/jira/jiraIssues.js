jQuery(document).ready(function(){

    jQuery(".jiraissues_table").each(function(i, jiraissues_table){
        var fieldset = jQuery(jiraissues_table).children("fieldset");
        jQuery(fieldset).append('<input type="hidden" name="id" value="'+i+'">');
        var params = JiraIssues.getParamsFrom(fieldset);
        var tableId = 'jiraissues_table_'+params['id'];
        jQuery(jiraissues_table).append('<table id="'+tableId+'" style="display:none"></table>');

        // get the columns from the input params
        var columns = {};
        jQuery(fieldset).children(".columns").each(function(i){
            var name = jQuery(this).attr('name');
            // the index is the number in the name string in the brackets. it starts at index 8 (after "column[") and
            // ends just before the last character ("]")
            columns[name.substring(8,name.length-1)]=jQuery(this).attr('value');
        });

        var columnArray = new Array();
        jQuery.each(columns, function(i, val) {
            columnArray.push(val.toLowerCase());
        });

        var columnWidths = JiraIssues.initializeColumnWidth(columnArray);

        var colModel = [];
        jQuery.each(columns, function(i, val) {
            colModel[i] = {display: val, name : val, width : columnWidths[val.toLowerCase()], sortable : true, align: 'left' };
        });

        //flexify this
        jQuery('#'+tableId).flexigrid({
            url: params['retrieverUrlHtml'],
            method: 'GET',
            dataType: 'json',
            colModel: colModel,
            sortname: params['sortField'],
            sortorder: params['sortOrder'],
            usepager: true,
            title: '<a href="'+params['clickableUrl']+'">'+params['title']+'</a>',
            page: params['requestedPage'], // unfortunately this is ignored
            useRp: false,
            rp: params['resultsPerPage'],
            showTableToggleBtn: true,
            height: 480,
            onSubmit: (function(useCache){ return function(){ JiraIssues.onSubmitFunction(useCache, this); return true; } })(params['useCache']),
            preProcess: (function(jiraissues_table, showTrustWarnings){ return function(data){ JiraIssues.preProcessFunction(jiraissues_table, showTrustWarnings, data); return data; } })(jiraissues_table, params['showTrustWarnings']),
            onError: (function(jiraissues_table,tableId,jiraissuesError){ return function(XMLHttpRequest,textmsg,error){ JiraIssues.onErrorFunction(jiraissues_table,tableId,jiraissuesError,XMLHttpRequest,textmsg,error); } })(jiraissues_table,tableId,params['jiraissuesError']),
            onReload: (function(useCache){ return function(){ JiraIssues.onReloadFunction(useCache, this); return true; } })(params['useCache']),
            errormsg: params['errormsg'],
            pagestat: params['pagestat'],
            procmsg: params['procmsg'],
            nomsg: params['nomsg']
        });
    });

    jQuery(".jiraissues_count").each(function(i, jiraissues_count){
        var fieldset = jQuery(jiraissues_count).children("fieldset");
        jQuery(fieldset).append('<input type="hidden" name="id" value="'+i+'">');
        var params = JiraIssues.getParamsFrom(fieldset);
        jQuery.ajax({
            type: 'GET',
            url: params['retrieverUrlHtml'],
            data: 'useCache='+params['useCache']+'&rp='+params['resultsPerPage']+'&showCount=true',
            success: function(issueCount){
                jQuery(jiraissues_count).append('<span id="jiraissues_count_'+params['id']+'"><a href="'+params['clickableUrl']+'">'+issueCount+' '+params['issuesWord']+'</a></span>');
            }
        });
    });

});


var JiraIssues = {
    onErrorFunction: function(jiraissues_table,tableId,jiraissuesError,XMLHttpRequest,textmsg,error){
        var errorMsg = jiraissuesError+': ';
        if (XMLHttpRequest.status=='200')
            errorMsg += textmsg;
        else
            errorMsg += XMLHttpRequest.responseText;

        jQuery(jiraissues_table).find('.pPageStat').html(errorMsg);
        JiraIssues.bigMessageFunction(tableId,errorMsg);
        jQuery(jiraissues_table).find('.pReload').removeClass('loading'); // TODO: CONFJIRA-55 may want to change it to an error sign or something
        //		this.loading = false; // need to bring "this" param over if want to do this, but what does this accomplish anyway?
    },

    onReloadFunction: function(useCache,t){
        t.onSubmit = (function(useCache,t){ return function(){ JiraIssues.reloadOnSubmitFunction(useCache,t); return true; } })(useCache,t);
    },
    reloadOnSubmitFunction: function(useCache,t){
        t.params = [{name:'useCache',value:false}];
        t.onSubmit = (function(useCache,t){ return function(){ JiraIssues.onSubmitFunction(useCache,t); return true; } })(useCache,t);
    },
    onSubmitFunction: function(useCache,t){
        t.params = [{name:'useCache',value:useCache}];
    },

    showTrustWarningsFunction: function(jiraissues_table, data){
        var trustedDiv = jQuery(jiraissues_table).children(".trusted_warnings");
        if(data.trustedMessage)
        {
            jQuery(trustedDiv).find("td:last").html(data.trustedMessage);
            jQuery(trustedDiv).css('display','block');
        }
        else
            jQuery(trustedDiv).css('display','none');
    },

    preProcessFunction: function(jiraissues_table,showTrustWarnings,data){
        if(showTrustWarnings)
            JiraIssues.showTrustWarningsFunction(jiraissues_table, data);

    // right now this will get overwritten anyway... see CONFJIRA-46
        // note: removed tableId from this function's params because this is out
        // if(data.total==0)
        //    bigMessageFunction(tableId,"no items");
    },

    bigMessageFunction: function(tableId,msg){
        jQuery('#'+tableId).html('<tbody><tr><td><strong>'+msg+'</strong></td></tr></tbody>');
    },

    getParamsFrom: function(fieldset) {
        var params = {};
        jQuery(fieldset).children("input").each(function(){
            params[jQuery(this).attr('name')] = jQuery(this).attr('value');
        });
        return params;
    },

    // retrieves the width of the window (excluding the scrollbar). Handles different browser config.
    getWindowWidth: function(){
        if (typeof(window.innerWidth) == 'number')
        {
            //Non-IE
            return window.innerWidth - 16;
        }
        else if (document.documentElement && document.documentElement.clientWidth)
        {
            //IE 6+ in 'standards compliant mode'
            return document.documentElement.clientWidth - 16;
        }
        else if (document.body && document.body.clientWidth)
        {
            //IE 4 compatible
            return document.body.clientWidth - 16;
        }

        return 1280; // default
    },

    initializeColumnWidth: function (columnArray){
        var columnWidths = {};
        if (columnArray == undefined || columnArray.length <= 0) return columnWidths;

        // compute for the space that can be allocated, the overhead of the columns are also accounted for
        var spaceRemaining = JiraIssues.getWindowWidth() - (37 + (columnArray.length*11));
        var hasSummary = false;
        var hasDescription = false;
        var columnsWithWidth = 0;

        var otherColumnWidth = 140;

        // set the widths for columns with default column width
        for (var i=0; i<columnArray.length; i++)
        {
            if ("summary" == columnArray[i])
            {
                hasSummary = true;
                columnsWithWidth++;
            }
            else if ("description" == columnArray[i])
            {
                hasDescription = true;
                columnsWithWidth++;
            }
            else if ("type" == columnArray[i])
            {
                columnsWithWidth++;
                columnWidths[columnArray[i]] = 30;
                spaceRemaining -= 30;
            }
            else if ("priority" == columnArray[i])
            {
                columnsWithWidth++;
                columnWidths[columnArray[i]] = 50;
                spaceRemaining -= 50;
            }
            else if ("status" == columnArray[i])
            {
                columnsWithWidth++;
                columnWidths[columnArray[i]] = 100;
                spaceRemaining -= 100;
            }
            else if ("key" == columnArray[i])
            {
                columnsWithWidth++;
                columnWidths[columnArray[i]] = 90;
                spaceRemaining -= 90;
            }
            else if ("resolution" == columnArray[i])
            {
                columnsWithWidth++;
                columnWidths[columnArray[i]] = 80;
                spaceRemaining -= 80;
            }
            else
            {
                // set the column width of anything else to a fixed column width (if there is a summary)
                columnWidths[columnArray[i]] = otherColumnWidth;
            }
        }

        // set the remaining space to the summary column
        // set a minimum size for the summary column
        if (hasSummary || hasDescription)
        {
            spaceRemaining -= (otherColumnWidth * (columnArray.length - columnsWithWidth));
            if (hasSummary && hasDescription)
            {
                columnWidths["summary"] = Math.max(spaceRemaining/2, 250);
                columnWidths["description"] = Math.max(spaceRemaining/2, 250);
            }
            else if (hasSummary)
            {
                columnWidths["summary"] = Math.max(spaceRemaining, 250);
            }
            else
            {
                columnWidths["description"] = Math.max(spaceRemaining, 250);
            }
        }

        // adjust the size for other columns if there is no summary column
        if (!hasSummary && !hasDescription && (columnArray.length > columnsWithWidth))
        {
            otherColumnWidth = spaceRemaining / (columnArray.length - columnsWithWidth);

            // adjust the size the columns with
            for (var i=0; i<columnArray.length; i++)
            {
                if ("resolution" != columnArray[i] && "key" != columnArray[i] && "type" != columnArray[i] &&
                    "priority" != columnArray[i] && "status" != columnArray[i])
                {
                    columnWidths[columnArray[i]] = otherColumnWidth;
                }
            }
        }

        return columnWidths;
    }
};