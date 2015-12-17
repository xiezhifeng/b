define('confluence/jim/macro-browser/editor/util/select2-mixin', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/util/config'
],
function(
    $,
    _,
    AJS,
    Backbone,
    config
) {
    'use strict';

    /**
     * Contains all mixin methods regarding to select2.
     * These methods will extend other object, such as a Backbone View.
     */
    var Select2Mixin = {
        /**
         * Convert a select element into select2 element
         *
         * @param {Object} options
         * @param {jQuery object} options.$el
         * @param {string} options.placeholderText
         * @param {boolean} options.isRequired
         * @param {object} options.overrideSelect2Ops
         */
        setupSelect2: function(options) {
            var defaultOptsSelect2 = {
                // minimumInputLength: 3,
                maximumSelectionSize: 1,
                placeholder: options.placeholderText,
                width: '300px',
                containerCssClass: 'select2-container-' + options.$el.attr('id'),
                dropdownCssClass: 'select2-dropdown-' + options.$el.attr('id')
            };
            var optsSelect2 = defaultOptsSelect2;

            if (options.overrideSelect2Ops) {
                optsSelect2 = $.extend({}, optsSelect2, options.overrideSelect2Ops);
            }

            options.$el.auiSelect2(optsSelect2);

            if (options.isRequired) {
                // clear empty option when opening select2 first name
                options.$el.on('select2-opening', function() {
                    options.$el.find('option[value="' + config.DEFAULT_OPTION_VALUE + '"]').addClass('hidden');
                });
            }
        },

        fillDataSelect2: function($el, data) {
            this.toggleSelect2Loading($el, false);
            this.toggleEnableSelect2($el, false);
            this.resetAndAddDefaultOption($el);

            var markup = this.template.selectOptions({items: data});
            $el.append(markup);

            this.toggleEnableSelect2($el, true);
        },

        resetAndAddDefaultOption: function($el) {
            $el.empty();

            var markup = this.template.defaultOption({
                defaultValue: config.DEFAULT_OPTION_VALUE
            });
            $el.append(markup);
            $el.auiSelect2('val', config.DEFAULT_OPTION_VALUE);
        },

        toggleEnableSelect2: function($el, isEnable) {
            if (isEnable) {
                $el.auiSelect2('enable', true);
            } else {
                $el.auiSelect2('enable', false);
            }
        },

        toggleSelect2Loading: function($el, isLoading, isForInputType) {
            this.resetSelect2Options($el);

            if (isLoading) {
                $el.addClass('loading');

                if (!isForInputType) {
                    // add loading icon on the right of the select
                    $el.after('<span class="aui-icon aui-icon-wait">Loading...</span>');
                    this.toggleEnableSelect2($el, false);

                    // add loading option
                    var markup = this.template.loadingOption();
                    $el.append(markup);

                    $el.auiSelect2('val', 'loading');
                }

            } else {
                $el.removeClass('loading');

                if (!isForInputType) {
                    $el.parent().find('.aui-icon-wait').remove();
                    this.toggleEnableSelect2($el, true);
                }
            }
        },

        resetSelect2Options: function($el) {
            $el.removeClass('loading');
            $el.parent().find('.aui-icon-wait').remove();
            this.toggleEnableSelect2($el, true);

            $el.empty();
            $el.auiSelect2('data', null);
        },

        selectFirstValueInSelect2: function($el) {
            this.removeEmptyOptionInSelect2($el);
            $el.auiSelect2('val', null, true);
        },

        removeEmptyOptionInSelect2: function($el) {
            $el.find('option[value="' + config.DEFAULT_OPTION_VALUE + '"]').remove();
        },

        setSelect2Value: function($el, value) {
            // if we set value which is null or '' or no existed for select2,
            // select2 will reset all its options.
            var $option = $el.find('option[value="' + value + '"]');
            if (value && $option.length) {
                $el.select2('val', value, true);
                return true;
            }

            return false;
        }

    };

    return Select2Mixin;
});


