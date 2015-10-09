define('confluence/jim/editor/util/select2-mixin', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/editor/util/config'
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
        template: Confluence.Templates.JiraSprints.Dialog,

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
                $el.on('select2-opening', function() {
                    if ($el.val() === config.DEFAULT_OPTION_VALUE) {
                        $el.find('option[value="' + config.DEFAULT_OPTION_VALUE + '"]').addClass('hidden');
                    }
                });
            }

            // set placeholder
            this.$(dropDownCSS + ' .select2-input').attr('placeholder', placeholderText);
        },

        removeDefaultOptionOfSelect2: function($el) {
            $el.find('option[value=' + config.DEFAULT_OPTION_VALUE + ']').remove();
        },

        fillDataSelect2: function($el, templateName, option) {
            this.resetSelect2Options($el);

            var markup = templateName(option);
            $el.append(markup);

            this.toggleEnableSelect2($el, true);
        },

        toggleEnableSelect2: function($el, isEnable) {
            if (isEnable) {
                $el.auiSelect2('enable', true);
            } else {
                $el.auiSelect2('enable', false);
            }
        },

        toggleSelect2Loading: function($el, isLoading) {
            if (isLoading) {
                this.resetSelect2Options($el);
                this.toggleEnableSelect2($el, false);

                var markup = this.template.loadingOption();
                $el.append(markup);

                $el.auiSelect2('val', 'loading');

            } else {
                this.resetSelect2Options($el);
                this.toggleEnableSelect2($el, true);
            }
        },

        resetSelect2Options: function($el) {
            $el.empty();
            $el.auiSelect2('val', config.DEFAULT_OPTION_VALUE);
        },

        resetAndAddDefaultOption: function($el) {
            $el.empty();

            var markup = this.template.defaultOption({
                defaultValue: config.DEFAULT_OPTION_VALUE
            });
            $el.append(markup);
            $el.auiSelect2('val', config.DEFAULT_OPTION_VALUE);
        }
    };

    return Select2Mixin;
});


