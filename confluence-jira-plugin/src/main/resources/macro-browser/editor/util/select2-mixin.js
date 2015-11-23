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
        setupSelect2: function($el, containerCSS, dropDownCSS, placeholderText, isRequired) {
            var opts = {
                width: '300px',
                containerCssClass: containerCSS,
                dropdownCssClass: dropDownCSS,
                formatResult: function(result, label, query) {
                    label.attr('title', result.text);
                    return $.fn.select2.defaults.formatResult.apply(this, arguments);
                }
            };

            $el.auiSelect2(opts);

            if (isRequired) {
                // clear empty option when opening select2 first name
                $el.on('select2-opening', function() {
                    $el.find('option[value="' + config.DEFAULT_OPTION_VALUE + '"]').addClass('hidden');
                });
            }

            // set placeholder
            this.$(dropDownCSS + ' .select2-input').attr('placeholder', placeholderText);
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

        toggleSelect2Loading: function($el, isLoading) {
            this.resetSelect2Options($el);

            if (isLoading) {
                $el.after('<span class="aui-icon aui-icon-wait">Loading...</span>');
                this.toggleEnableSelect2($el, false);

                var markup = this.template.loadingOption();
                $el.append(markup);

                this.selectFirstValueInSelect2($el);
            } else {
                $el.parent().find('.aui-icon-wait').remove();
                this.toggleEnableSelect2($el, true);
            }
        },

        resetSelect2Options: function($el) {
            this.toggleEnableSelect2($el, true);
            $el.parent().find('.aui-icon-wait').remove();

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


