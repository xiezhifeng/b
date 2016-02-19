AppLinks = AJS.$.extend(window.AppLinks || {}, {
	makeRequest: function(options){
		var context = contextPath || AJS.contextPath();
		
		// only set the app parameter if we know for sure this is form data
		if (options.processData){
    		if (options.appId){
    			options.data = AJS.$.extend(options.data || {}, {
    				appId: options.appId
    			});
    		}
    		else if (options.appType){
    			options.data = AJS.$.extend(options.data || {}, {
    				appType: options.appType
    			});
    		}
    		
    		options.data = AJS.$.extend(options.data || {}, {
                path: options.url
            });
		}
		else{
		    var appPath = options.url;
		    options = AJS.$.extend(options, {beforeSend: function(xhr){
		        if (options.appId){
	                xhr.setRequestHeader('X-AppId', options.appId);
	            }
	            else if (options.appType){
	                xhr.setRequestHeader('X-AppType', options.appType);
	            }
	            xhr.setRequestHeader('X-AppPath', appPath);
		    }});
		}
		
		
		options = AJS.$.extend(options, {url: context + '/plugins/servlet/applinks/proxy'});
		return AJS.$.ajax(options);
	},
	createProxyGetUrl: function(options){
		var context = '';
		if (options.includeContext){
			context = contextPath || AJS.contextPath();
		}
		var url = context + '/plugins/servlet/applinks/proxy';
		if (options.appId){
			url += '?appId=' + encodeURIComponent(options.appId);
		}
		else if (options.appType){
			url += '?appType=' + encodeURIComponent(options.appType);
		}
		else{
			AJS.log('You need to specify an appType or appId');
			return '';
		}
		// path can be added manually later (i.e. quick search)
		if (options.path){
			url += '&path=' + encodeURIComponent(options.path);
		}
		return url;
	}
});

