var jiraIntegration = window.jiraIntegration || {};

jiraIntegration.fields = (function($, _) {

    var stringHandler = {
        template : jiraIntegration.templates.fields.stringField,
        getContext : getStringContext,
        getValue : getStringValue
    };
    var userHandler = {
        template : jiraIntegration.templates.fields.userField,
        getContext : getUserContext,
        getValue : getUserValue,
        renderContextHandler : jiraIntegration.contextHandler.userContextHandler
    }
    var textareaHandler = {
        template : jiraIntegration.templates.fields.textareaField,
        getContext : getStringContext,
        getValue : getStringValue
    };
    var numberHandler = {
        template : jiraIntegration.templates.fields.numberField,
        getContext : getStringContext,
        getValue : getStringValue
    };
    var arrayHandler = {
        template : jiraIntegration.templates.fields.arrayField,
        getContext : getArrayContext,
        getValue : getArrayValue
    };
    var allowedValuesHandler = {
        template : jiraIntegration.templates.fields.allowedValuesField,
        getContext : getAllowedValuesContext,
        getValue : getAllowedValuesValue
    };
    var timeTrackingHandler = {
        template : jiraIntegration.templates.fields.timeTrackingField,
        getContext : getTimeTrackingContext,
        getValue : getTimeTrackingValue
    };

    var restTypes = {
        "com.pyxis.greenhopper.jira:gh-epic-label":                       stringHandler,
        "string":                                                         stringHandler,
        "summary":                                                        stringHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:textfield":    stringHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:url":          stringHandler,
        "environment":                                                    stringHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:textarea":     textareaHandler,
        "description":                                                    textareaHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:float":        numberHandler,
        "array":                                                          arrayHandler,
        "labels":                                                         arrayHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:labels":       arrayHandler,
        "resolution":                                                     allowedValuesHandler,
        "fixVersions":                                                    allowedValuesHandler,
        "priority":                                                       allowedValuesHandler,
        "versions":                                                       allowedValuesHandler,
        "components":                                                     allowedValuesHandler,
        "security":                                                       allowedValuesHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:version":      allowedValuesHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:multiversion": allowedValuesHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:project":      allowedValuesHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:select":       allowedValuesHandler,
        //"com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons": allowedValuesHandler,
        "assignee":                                                       userHandler,
        "reporter":                                                       userHandler,
        "com.atlassian.jira.plugin.system.customfieldtypes:userpicker":   userHandler,
        "timetracking":                                                   timeTrackingHandler
    };

    function getBaseContext(typeId, restField, errors) {
        var name = restField.schema.system || "customfield_" + restField.schema.customId;

        return {
            labelText : restField.name,
            name : name,
            isRequired :  restField.required,
            errorTexts : errors[name],
            jiraType : typeId
        }
    }

    function getStringContext(context, restField, issue, values) {
        var name = context.name;
        context.value = ($.isPlainObject(values[name]) ? values[name].name : values[name]) || (issue && issue.fields[name]) || '';
        return context;
    }

    function getStringValue($fieldInput) {
        return $fieldInput.val();
    }

    function getArrayContext(context, restField, issue, values) {
        var name = context.name;
        context.value = (values[name] && values[name].join(',')) || (issue && issue.fields[name] && issue.fields[name].join(','));
        return context;
    }

    function getArrayValue($fieldInput) {
        return _.map($fieldInput.val().split(','), $.trim);
    }

    function getAllowedValuesContext(context, restField, issue, values) {
        var name = context.name;
        var userInputValue = values[name];
        var issueValue = issue && issue.fields[name];
        var selectedValue;

        if (userInputValue) {
            selectedValue = $.isArray(userInputValue) ? _.pluck(userInputValue, 'name') : [ userInputValue.name ];
        } else if (issueValue) {
            selectedValue = $.isArray(issueValue) ? _.pluck(issueValue, 'name') : [ issueValue.name ];
        } else {
            selectedValue = [];
        }
        context.options = _.map(restField.allowedValues, function(val) {
            return {
                value : val.name || val.id,
                text : val.name || val.value,
                selected : _.contains(selectedValue, val.name)
            };
        });
        context.isMultiple = _.contains(restField.operations, 'add');
        return context;
    }

    function getAllowedValuesValue($fieldInput) {
        var val = $fieldInput.val();
        var multiple = $fieldInput.attr('multiple');
        if (multiple) {
            return  $.isArray(val) ?
                    _.map(val, function(val) {
                        return { name: val, id: val };
                    }) :
                    [{ name: val, id: val }];
        }
        return { name : val, id: val };
    }

    function getUserContext(context, restField, issue, values) {
        var name = context.name;
        context.value = (values[name] && values[name].name) || (issue && issue.fields[name] && issue.fields[name].name) || '';
        return context;
    }

    function getUserValue($fieldInput) {
        return {
            name : $fieldInput.val()
        };
    }

    function getTimeTrackingContext(context, restField, issue, values) {
        var name = context.name;
        context.value = (values[name] && values[name].remainingEstimate) || (issue && issue.fields[name] && issue.fields[name].remainingEstimate) || '';
        return context;
    }

    function getTimeTrackingValue($fieldInput) {
        return {
            remainingEstimate : $fieldInput.val()
        };
    }

    /**
     * @param type {string} The JIRA string to expect listed as 'schema.system' or 'schema.customId' in the REST response
     * @param handler {{
     *         template : function(Object) : string,
     *         getContext : function(baseContext, restField, issue, values) :Object,
     *         getValue : function($renderedInput) : Object
     *         canRender : ?function(restField) : boolean
     *     }}
     *     getContext produces an object that the template can read
     *     template produces a string of HTML that contains an <input>, <textarea> or <select> whose name matches baseContext.name
     *     getValue takes in a jQuery object that represents the <input>, <textarea> or <select> form the template.
     *     canRender takes in a restField and returns true if it can be rendered, or false if it can't. This is optional and defaults to always returning true.
     */
    function addFieldHandler(type, handler) {
        if (_.has(restTypes, type) && console && console.warn) {
            console.warn('Redefining handler for type ' + type + ".");
        }
        restTypes[type] = handler;
    }

    function getFieldHandler(restField) {
        return restTypes[restField.schema ? (restField.schema.system || restField.schema.customId) : restField];
    }

    return {
        addFieldHandler : addFieldHandler,
        getFieldHandler : getFieldHandler,
        getRestType : function(restField) {
            var restTypeId = restField.schema.system || restField.schema.custom || restField.schema.customId;
            return restTypes[restTypeId];
        },
        canRender : function(restField) {
            var restTypeId = restField.schema.system || restField.schema.custom || restField.schema.customId;
            var restType = restTypes[restTypeId];

            if (!restType) {
                return false;
            }

            return restField.operations && restField.operations.length && (!restType.canRender || restType.canRender(restField));
        },
        renderField : function(issue, restField, values, errors) {
            var restTypeId = restField.schema.system || restField.schema.custom || restField.schema.customId;
            var restType = restTypes[restTypeId];

            var baseContext = getBaseContext(restTypeId, restField, errors || {});

            var unrenderable = !restType || (restType.canRender && !restType.canRender(restField));
            var noPermission = !restField.operations || !restField.operations.length; // Hopefully this doesn't happen.
            if (unrenderable || noPermission) {
                baseContext.reasonContent = unrenderable ? AJS.I18n.getText('fields.unrenderable', '<a href="' + issue.url + '">', '</a>') :
                                            noPermission ? AJS.I18n.getText('fields.no.permission', '<a href="' + issue.url + '">', '</a>') :
                                            null;
                if (!baseContext.reasonContent) {
                    throw new Error('Invalid unrenderable reason.');
                }
                return jiraIntegration.templates.fields.unrenderableTypeField(baseContext);
            }
            
            return restType.template(restType.getContext(baseContext, restField, issue, values || {}));
        },
        getJSON : function($fieldInput) {
            var typeId = $fieldInput.closest('.jira-field').attr('data-jira-type');
            var handler = typeId && getFieldHandler(typeId);
            return handler && handler.getValue && handler.getValue($fieldInput);
        }
    };
}(AJS.$, window._));