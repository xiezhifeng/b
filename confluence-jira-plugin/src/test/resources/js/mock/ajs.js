define(function() {
    return {
        InlineDialog: function() {
            console.log('mock object', 'AJS.InlineDialog');
        },

        I18n: {
            getText: function(key) {
                return key;
            }
        },

        contextPath: function() {
            return '/confluence';
        }
    };
});
