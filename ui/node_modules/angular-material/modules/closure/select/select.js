/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.select');
goog.require('ngmaterial.components.backdrop');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.select
 */

/***************************************************

 ### TODO ###
 - [ ] Abstract placement logic in $mdSelect service to $mdMenu service

 ***************************************************/

SelectDirective['$inject'] = ["$mdSelect", "$mdUtil", "$mdConstant", "$mdTheming", "$mdAria", "$parse", "$sce", "$injector"];
SelectMenuDirective['$inject'] = ["$parse", "$mdUtil", "$mdConstant", "$mdTheming"];
OptionDirective['$inject'] = ["$mdButtonInkRipple", "$mdUtil", "$mdTheming"];
SelectProvider['$inject'] = ["$$interimElementProvider"];
var SELECT_EDGE_MARGIN = 8;
var selectNextId = 0;
var CHECKBOX_SELECTION_INDICATOR =
  angular.element('<div class="md-container"><div class="md-icon"></div></div>');

angular.module('material.components.select', [
    'material.core',
    'material.components.backdrop'
  ])
  .directive('mdSelect', SelectDirective)
  .directive('mdSelectMenu', SelectMenuDirective)
  .directive('mdOption', OptionDirective)
  .directive('mdOptgroup', OptgroupDirective)
  .directive('mdSelectHeader', SelectHeaderDirective)
  .provider('$mdSelect', SelectProvider);

/**
 * @ngdoc directive
 * @name mdSelect
 * @restrict E
 * @module material.components.select
 *
 * @description Displays a select box, bound to an `ng-model`. Selectable options are defined using
 * the <a ng-href="/api/directive/mdOption">md-option</a> element directive. Options can be grouped
 * using the <a ng-href="/api/directive/mdOptgroup">md-optgroup</a> element directive.
 *
 * When the select is required and uses a floating label, then the label will automatically contain
 * an asterisk (`*`). This behavior can be disabled by using the `md-no-asterisk` attribute.
 *
 * By default, the select will display with an underline to match other form elements. This can be
 * disabled by applying the `md-no-underline` CSS class.
 *
 * @param {expression} ng-model Assignable angular expression to data-bind to.
 * @param {expression=} ng-change Expression to be executed when the model value changes.
 * @param {boolean=} multiple When present, allows for more than one option to be selected.
 *  The model is an array with the selected choices. **Note:** This attribute is only evaluated
 *  once; it is not watched.
 * @param {expression=} md-on-close Expression to be evaluated when the select is closed.
 * @param {expression=} md-on-open Expression to be evaluated when opening the select.
 * Will hide the select options and show a spinner until the evaluated promise resolves.
 * @param {expression=} md-selected-text Expression to be evaluated that will return a string
 * to be displayed as a placeholder in the select input box when it is closed. The value
 * will be treated as *text* (not html).
 * @param {expression=} md-selected-html Expression to be evaluated that will return a string
 * to be displayed as a placeholder in the select input box when it is closed. The value
 * will be treated as *html*. The value must either be explicitly marked as trustedHtml or
 * the ngSanitize module must be loaded.
 * @param {string=} placeholder Placeholder hint text.
 * @param {boolean=} md-no-asterisk When set to true, an asterisk will not be appended to the
 * floating label. **Note:** This attribute is only evaluated once; it is not watched.
 * @param {string=} aria-label Optional label for accessibility. Only necessary if no placeholder or
 * explicit label is present.
 * @param {string=} md-container-class Class list to get applied to the `.md-select-menu-container`
 * element (for custom styling).
 *
 * @usage
 * With a placeholder (label and aria-label are added dynamically)
 * <hljs lang="html">
 *   <md-input-container>
 *     <md-select
 *       ng-model="someModel"
 *       placeholder="Select a state">
 *       <md-option ng-value="opt" ng-repeat="opt in neighborhoods2">{{ opt }}</md-option>
 *     </md-select>
 *   </md-input-container>
 * </hljs>
 *
 * With an explicit label
 * <hljs lang="html">
 *   <md-input-container>
 *     <label>State</label>
 *     <md-select
 *       ng-model="someModel">
 *       <md-option ng-value="opt" ng-repeat="opt in neighborhoods2">{{ opt }}</md-option>
 *     </md-select>
 *   </md-input-container>
 * </hljs>
 *
 * Using the `md-select-header` element directive
 *
 * When a developer needs to put more than just a text label in the `md-select-menu`, they should
 * use one or more `md-select-header`s. These elements can contain custom HTML which can be styled
 * as desired. Use cases for this element include a sticky search bar and custom option group
 * labels.
 *
 * <hljs lang="html">
 *   <md-input-container>
 *     <md-select ng-model="someModel">
 *       <md-select-header>
 *         <span> Neighborhoods - </span>
 *       </md-select-header>
 *       <md-option ng-value="opt" ng-repeat="opt in neighborhoods2">{{ opt }}</md-option>
 *     </md-select>
 *   </md-input-container>
 * </hljs>
 *
 * ## Selects and object equality
 * When using a `md-select` to pick from a list of objects, it is important to realize how javascript handles
 * equality. Consider the following example:
 * <hljs lang="js">
 * angular.controller('MyCtrl', function($scope) {
 *   $scope.users = [
 *     { id: 1, name: 'Bob' },
 *     { id: 2, name: 'Alice' },
 *     { id: 3, name: 'Steve' }
 *   ];
 *   $scope.selectedUser = { id: 1, name: 'Bob' };
 * });
 * </hljs>
 * <hljs lang="html">
 * <div ng-controller="MyCtrl">
 *   <md-select ng-model="selectedUser">
 *     <md-option ng-value="user" ng-repeat="user in users">{{ user.name }}</md-option>
 *   </md-select>
 * </div>
 * </hljs>
 *
 * At first one might expect that the select should be populated with "Bob" as the selected user.
 * However, this is not true. To determine whether something is selected,
 * `ngModelController` is looking at whether `$scope.selectedUser == (any user in $scope.users);`;
 *
 * Javascript's `==` operator does not check for deep equality (ie. that all properties
 * on the object are the same), but instead whether the objects are *the same object in memory*.
 * In this case, we have two instances of identical objects, but they exist in memory as unique
 * entities. Because of this, the select will have no value populated for a selected user.
 *
 * To get around this, `ngModelController` provides a `track by` option that allows us to specify a
 * different expression which will be used for the equality operator. As such, we can update our
 * `html` to make use of this by specifying the `ng-model-options="{trackBy: '$value.id'}"` on the
 * `md-select` element. This converts our equality expression to be
 * `$scope.selectedUser.id == (any id in $scope.users.map(function(u) { return u.id; }));`
 * which results in Bob being selected as desired.
 *
 * **Note:** We do not support AngularJS's `track by` syntax. For instance
 *  `ng-options="user in users track by user.id"` will not work with `md-select`.
 *
 * Working HTML:
 * <hljs lang="html">
 * <div ng-controller="MyCtrl">
 *   <md-select ng-model="selectedUser" ng-model-options="{trackBy: '$value.id'}">
 *     <md-option ng-value="user" ng-repeat="user in users">{{ user.name }}</md-option>
 *   </md-select>
 * </div>
 * </hljs>
 */
function SelectDirective($mdSelect, $mdUtil, $mdConstant, $mdTheming, $mdAria, $parse, $sce,
    $injector) {
  var keyCodes = $mdConstant.KEY_CODE;
  var NAVIGATION_KEYS = [keyCodes.SPACE, keyCodes.ENTER, keyCodes.UP_ARROW, keyCodes.DOWN_ARROW];

  return {
    restrict: 'E',
    require: ['^?mdInputContainer', 'mdSelect', 'ngModel', '?^form'],
    compile: compile,
    controller: function() {
    } // empty placeholder controller to be initialized in link
  };

  function compile(element, attr) {
    // add the select value that will hold our placeholder or selected option value
    var valueEl = angular.element('<md-select-value><span></span></md-select-value>');
    valueEl.append('<span class="md-select-icon" aria-hidden="true"></span>');
    valueEl.addClass('md-select-value');
    if (!valueEl[0].hasAttribute('id')) {
      valueEl.attr('id', 'select_value_label_' + $mdUtil.nextUid());
    }

    // There's got to be an md-content inside. If there's not one, let's add it.
    var mdContentEl = element.find('md-content');
    if (!mdContentEl.length) {
      element.append(angular.element('<md-content>').append(element.contents()));
    }
    mdContentEl.attr('role', 'presentation');


    // Add progress spinner for md-options-loading
    if (attr.mdOnOpen) {

      // Show progress indicator while loading async
      // Use ng-hide for `display:none` so the indicator does not interfere with the options list
      element
        .find('md-content')
        .prepend(angular.element(
          '<div>' +
          ' <md-progress-circular md-mode="indeterminate" ng-if="$$loadingAsyncDone === false" md-diameter="25px"></md-progress-circular>' +
          '</div>'
        ));

      // Hide list [of item options] while loading async
      element
        .find('md-option')
        .attr('ng-show', '$$loadingAsyncDone');
    }

    if (attr.name) {
      var autofillClone = angular.element('<select class="md-visually-hidden"></select>');
      autofillClone.attr({
        'name': attr.name,
        'aria-hidden': 'true',
        'tabindex': '-1'
      });
      var opts = element.find('md-option');
      angular.forEach(opts, function(el) {
        var newEl = angular.element('<option>' + el.innerHTML + '</option>');
        if (el.hasAttribute('ng-value')) newEl.attr('ng-value', el.getAttribute('ng-value'));
        else if (el.hasAttribute('value')) newEl.attr('value', el.getAttribute('value'));
        autofillClone.append(newEl);
      });

      // Adds an extra option that will hold the selected value for the
      // cases where the select is a part of a non-angular form. This can be done with a ng-model,
      // however if the `md-option` is being `ng-repeat`-ed, AngularJS seems to insert a similar
      // `option` node, but with a value of `? string: <value> ?` which would then get submitted.
      // This also goes around having to prepend a dot to the name attribute.
      autofillClone.append(
        '<option ng-value="' + attr.ngModel + '" selected></option>'
      );

      element.parent().append(autofillClone);
    }

    var isMultiple = $mdUtil.parseAttributeBoolean(attr.multiple);

    // Use everything that's left inside element.contents() as the contents of the menu
    var multipleContent = isMultiple ? 'multiple' : '';
    var selectTemplate = '' +
      '<div class="md-select-menu-container" aria-hidden="true" role="presentation">' +
      '<md-select-menu role="presentation" {0}>{1}</md-select-menu>' +
      '</div>';

    selectTemplate = $mdUtil.supplant(selectTemplate, [multipleContent, element.html()]);
    element.empty().append(valueEl);
    element.append(selectTemplate);

    if (!attr.tabindex){
      attr.$set('tabindex', 0);
    }

    return function postLink(scope, element, attr, ctrls) {
      var untouched = true;
      var isDisabled, ariaLabelBase;

      var containerCtrl = ctrls[0];
      var mdSelectCtrl = ctrls[1];
      var ngModelCtrl = ctrls[2];
      var formCtrl = ctrls[3];
      // grab a reference to the select menu value label
      var valueEl = element.find('md-select-value');
      var isReadonly = angular.isDefined(attr.readonly);
      var disableAsterisk = $mdUtil.parseAttributeBoolean(attr.mdNoAsterisk);

      if (disableAsterisk) {
        element.addClass('md-no-asterisk');
      }

      if (containerCtrl) {
        var isErrorGetter = containerCtrl.isErrorGetter || function() {
          return ngModelCtrl.$invalid && (ngModelCtrl.$touched || (formCtrl && formCtrl.$submitted));
        };

        if (containerCtrl.input) {
          // We ignore inputs that are in the md-select-header (one
          // case where this might be useful would be adding as searchbox)
          if (element.find('md-select-header').find('input')[0] !== containerCtrl.input[0]) {
            throw new Error("<md-input-container> can only have *one* child <input>, <textarea> or <select> element!");
          }
        }

        containerCtrl.input = element;
        if (!containerCtrl.label) {
          $mdAria.expect(element, 'aria-label', element.attr('placeholder'));
        }

        scope.$watch(isErrorGetter, containerCtrl.setInvalid);
      }

      var selectContainer, selectScope, selectMenuCtrl;

      findSelectContainer();
      $mdTheming(element);

      var originalRender = ngModelCtrl.$render;
      ngModelCtrl.$render = function() {
        originalRender();
        syncLabelText();
        syncAriaLabel();
        inputCheckValue();
      };

      attr.$observe('placeholder', ngModelCtrl.$render);

      if (containerCtrl && containerCtrl.label) {
        attr.$observe('required', function (value) {
          // Toggle the md-required class on the input containers label, because the input container is automatically
          // applying the asterisk indicator on the label.
          containerCtrl.label.toggleClass('md-required', value && !disableAsterisk);
        });
      }

      mdSelectCtrl.setLabelText = function(text) {
        mdSelectCtrl.setIsPlaceholder(!text);

        // Whether the select label has been given via user content rather than the internal
        // template of <md-option>
        var isSelectLabelFromUser = false;

        if (attr.mdSelectedText && attr.mdSelectedHtml) {
          throw Error('md-select cannot have both `md-selected-text` and `md-selected-html`');
        }

        if (attr.mdSelectedText || attr.mdSelectedHtml) {
          text = $parse(attr.mdSelectedText || attr.mdSelectedHtml)(scope);
          isSelectLabelFromUser = true;
        } else if (!text) {
          // Use placeholder attribute, otherwise fallback to the md-input-container label
          var tmpPlaceholder = attr.placeholder ||
              (containerCtrl && containerCtrl.label ? containerCtrl.label.text() : '');

          text = tmpPlaceholder || '';
          isSelectLabelFromUser = true;
        }

        var target = valueEl.children().eq(0);

        if (attr.mdSelectedHtml) {
            // Using getTrustedHtml will run the content through $sanitize if it is not already
            // explicitly trusted. If the ngSanitize module is not loaded, this will
            // *correctly* throw an sce error.
            target.html($sce.getTrustedHtml(text));
        } else if (isSelectLabelFromUser) {
          target.text(text);
        } else {
          // If we've reached this point, the text is not user-provided.
          target.html(text);
        }
      };

      mdSelectCtrl.setIsPlaceholder = function(isPlaceholder) {
        if (isPlaceholder) {
          valueEl.addClass('md-select-placeholder');
          if (containerCtrl && containerCtrl.label) {
            containerCtrl.label.addClass('md-placeholder');
          }
        } else {
          valueEl.removeClass('md-select-placeholder');
          if (containerCtrl && containerCtrl.label) {
            containerCtrl.label.removeClass('md-placeholder');
          }
        }
      };

      if (!isReadonly) {
        element
          .on('focus', function(ev) {
            // Always focus the container (if we have one) so floating labels and other styles are
            // applied properly
            containerCtrl && containerCtrl.setFocused(true);
          });

        // Attach before ngModel's blur listener to stop propagation of blur event
        // to prevent from setting $touched.
        element.on('blur', function(event) {
          if (untouched) {
            untouched = false;
            if (selectScope._mdSelectIsOpen) {
              event.stopImmediatePropagation();
            }
          }

          if (selectScope._mdSelectIsOpen) return;
          containerCtrl && containerCtrl.setFocused(false);
          inputCheckValue();
        });
      }

      mdSelectCtrl.triggerClose = function() {
        $parse(attr.mdOnClose)(scope);
      };

      scope.$$postDigest(function() {
        initAriaLabel();
        syncLabelText();
        syncAriaLabel();
      });

      function initAriaLabel() {
        var labelText = element.attr('aria-label') || element.attr('placeholder');
        if (!labelText && containerCtrl && containerCtrl.label) {
          labelText = containerCtrl.label.text();
        }
        ariaLabelBase = labelText;
        $mdAria.expect(element, 'aria-label', labelText);
      }

      scope.$watch(function() {
        return selectMenuCtrl.selectedLabels();
      }, syncLabelText);

      function syncLabelText() {
        if (selectContainer) {
          selectMenuCtrl = selectMenuCtrl || selectContainer.find('md-select-menu').controller('mdSelectMenu');
          mdSelectCtrl.setLabelText(selectMenuCtrl.selectedLabels());
        }
      }

      function syncAriaLabel() {
        if (!ariaLabelBase) return;
        var ariaLabels = selectMenuCtrl.selectedLabels({mode: 'aria'});
        element.attr('aria-label', ariaLabels.length ? ariaLabelBase + ': ' + ariaLabels : ariaLabelBase);
      }

      var deregisterWatcher;
      attr.$observe('ngMultiple', function(val) {
        if (deregisterWatcher) deregisterWatcher();
        var parser = $parse(val);
        deregisterWatcher = scope.$watch(function() {
          return parser(scope);
        }, function(multiple, prevVal) {
          if (multiple === undefined && prevVal === undefined) return; // assume compiler did a good job
          if (multiple) {
            element.attr('multiple', 'multiple');
          } else {
            element.removeAttr('multiple');
          }
          element.attr('aria-multiselectable', multiple ? 'true' : 'false');
          if (selectContainer) {
            selectMenuCtrl.setMultiple(multiple);
            originalRender = ngModelCtrl.$render;
            ngModelCtrl.$render = function() {
              originalRender();
              syncLabelText();
              syncAriaLabel();
              inputCheckValue();
            };
            ngModelCtrl.$render();
          }
        });
      });

      attr.$observe('disabled', function(disabled) {
        if (angular.isString(disabled)) {
          disabled = true;
        }
        // Prevent click event being registered twice
        if (isDisabled !== undefined && isDisabled === disabled) {
          return;
        }
        isDisabled = disabled;
        if (disabled) {
          element
            .attr({'aria-disabled': 'true'})
            .removeAttr('tabindex')
            .off('click', openSelect)
            .off('keydown', handleKeypress);
        } else {
          element
            .attr({'tabindex': attr.tabindex, 'aria-disabled': 'false'})
            .on('click', openSelect)
            .on('keydown', handleKeypress);
        }
      });

      if (!attr.hasOwnProperty('disabled') && !attr.hasOwnProperty('ngDisabled')) {
        element.attr({'aria-disabled': 'false'});
        element.on('click', openSelect);
        element.on('keydown', handleKeypress);
      }

      var ariaAttrs = {
        role: 'listbox',
        'aria-expanded': 'false',
        'aria-multiselectable': isMultiple && !attr.ngMultiple ? 'true' : 'false'
      };

      if (!element[0].hasAttribute('id')) {
        ariaAttrs.id = 'select_' + $mdUtil.nextUid();
      }

      var containerId = 'select_container_' + $mdUtil.nextUid();
      selectContainer.attr('id', containerId);
      // Only add aria-owns if element ownership is NOT represented in the DOM.
      if (!element.find('md-select-menu').length) {
        ariaAttrs['aria-owns'] = containerId;
      }
      element.attr(ariaAttrs);

      scope.$on('$destroy', function() {
        $mdSelect
          .destroy()
          .finally(function() {
            if (containerCtrl) {
              containerCtrl.setFocused(false);
              containerCtrl.setHasValue(false);
              containerCtrl.input = null;
            }
            ngModelCtrl.$setTouched();
          });
      });



      function inputCheckValue() {
        // The select counts as having a value if one or more options are selected,
        // or if the input's validity state says it has bad input (eg string in a number input)
        // we must do this on nextTick as the $render is sometimes invoked on nextTick.
        $mdUtil.nextTick(function () {
          containerCtrl && containerCtrl.setHasValue(selectMenuCtrl.selectedLabels().length > 0 || (element[0].validity || {}).badInput);
        });
      }

      function findSelectContainer() {
        selectContainer = angular.element(
          element[0].querySelector('.md-select-menu-container')
        );
        selectScope = scope;
        if (attr.mdContainerClass) {
          var value = selectContainer[0].getAttribute('class') + ' ' + attr.mdContainerClass;
          selectContainer[0].setAttribute('class', value);
        }
        selectMenuCtrl = selectContainer.find('md-select-menu').controller('mdSelectMenu');
        selectMenuCtrl.init(ngModelCtrl, attr.ngModel);
        element.on('$destroy', function() {
          selectContainer.remove();
        });
      }

      function handleKeypress(e) {
        if ($mdConstant.isNavigationKey(e)) {
          // prevent page scrolling on interaction
          e.preventDefault();
          openSelect(e);
        } else {
          if (shouldHandleKey(e, $mdConstant)) {
            e.preventDefault();

            var node = selectMenuCtrl.optNodeForKeyboardSearch(e);
            if (!node || node.hasAttribute('disabled')) return;
            var optionCtrl = angular.element(node).controller('mdOption');
            if (!selectMenuCtrl.isMultiple) {
              selectMenuCtrl.deselect(Object.keys(selectMenuCtrl.selected)[0]);
            }
            selectMenuCtrl.select(optionCtrl.hashKey, optionCtrl.value);
            selectMenuCtrl.refreshViewValue();
          }
        }
      }

      function openSelect() {
        selectScope._mdSelectIsOpen = true;
        element.attr('aria-expanded', 'true');

        $mdSelect.show({
          scope: selectScope,
          preserveScope: true,
          skipCompile: true,
          element: selectContainer,
          target: element[0],
          selectCtrl: mdSelectCtrl,
          preserveElement: true,
          hasBackdrop: true,
          loadingAsync: attr.mdOnOpen ? scope.$eval(attr.mdOnOpen) || true : false
        }).finally(function() {
          selectScope._mdSelectIsOpen = false;
          element.focus();
          element.attr('aria-expanded', 'false');
          ngModelCtrl.$setTouched();
        });
      }

    };
  }
}

function SelectMenuDirective($parse, $mdUtil, $mdConstant, $mdTheming) {
  // We want the scope to be set to 'false' so an isolated scope is not created
  // which would interfere with the md-select-header's access to the
  // parent scope.
  SelectMenuController['$inject'] = ["$scope", "$attrs", "$element"];
  return {
    restrict: 'E',
    require: ['mdSelectMenu'],
    scope: false,
    controller: SelectMenuController,
    link: {pre: preLink}
  };

  // We use preLink instead of postLink to ensure that the select is initialized before
  // its child options run postLink.
  function preLink(scope, element, attr, ctrls) {
    var selectCtrl = ctrls[0];

    element.addClass('_md');     // private md component indicator for styling

    $mdTheming(element);
    element.on('click', clickListener);
    element.on('keypress', keyListener);

    function keyListener(e) {
      if (e.keyCode == 13 || e.keyCode == 32) {
        clickListener(e);
      }
    }

    function clickListener(ev) {
      var option = $mdUtil.getClosest(ev.target, 'md-option');
      var optionCtrl = option && angular.element(option).data('$mdOptionController');
      if (!option || !optionCtrl) return;
      if (option.hasAttribute('disabled')) {
        ev.stopImmediatePropagation();
        return false;
      }

      var optionHashKey = selectCtrl.hashGetter(optionCtrl.value);
      var isSelected = angular.isDefined(selectCtrl.selected[optionHashKey]);

      scope.$apply(function() {
        if (selectCtrl.isMultiple) {
          if (isSelected) {
            selectCtrl.deselect(optionHashKey);
          } else {
            selectCtrl.select(optionHashKey, optionCtrl.value);
          }
        } else {
          if (!isSelected) {
            selectCtrl.deselect(Object.keys(selectCtrl.selected)[0]);
            selectCtrl.select(optionHashKey, optionCtrl.value);
          }
        }
        selectCtrl.refreshViewValue();
      });
    }
  }

  function SelectMenuController($scope, $attrs, $element) {
    var self = this;
    self.isMultiple = angular.isDefined($attrs.multiple);
    // selected is an object with keys matching all of the selected options' hashed values
    self.selected = {};
    // options is an object with keys matching every option's hash value,
    // and values matching every option's controller.
    self.options = {};

    $scope.$watchCollection(function() {
      return self.options;
    }, function() {
      self.ngModel.$render();
    });

    var deregisterCollectionWatch;
    var defaultIsEmpty;
    self.setMultiple = function(isMultiple) {
      var ngModel = self.ngModel;
      defaultIsEmpty = defaultIsEmpty || ngModel.$isEmpty;

      self.isMultiple = isMultiple;
      if (deregisterCollectionWatch) deregisterCollectionWatch();

      if (self.isMultiple) {
        // We want to delay the render method so that the directive has a chance to load before
        // rendering, this prevents the control being marked as dirty onload.
        var loaded = false;
        var delayedRender = function(val) {
          if (!loaded) {
            $mdUtil.nextTick(function () {
              renderMultiple(val);
              loaded = true;
            });
          } else {
            renderMultiple(val);
          }
        };
        ngModel.$validators['md-multiple'] = validateArray;
        ngModel.$render = delayedRender;

        // watchCollection on the model because by default ngModel only watches the model's
        // reference. This allows the developer to also push and pop from their array.
        $scope.$watchCollection(self.modelBinding, function(value) {
          if (validateArray(value)) delayedRender(value);
        });

        ngModel.$isEmpty = function(value) {
          return !value || value.length === 0;
        };
      } else {
        delete ngModel.$validators['md-multiple'];
        ngModel.$render = renderSingular;
      }

      function validateArray(modelValue, viewValue) {
        // If a value is truthy but not an array, reject it.
        // If value is undefined/falsy, accept that it's an empty array.
        return angular.isArray(modelValue || viewValue || []);
      }
    };

    var searchStr = '';
    var clearSearchTimeout, optNodes, optText;
    var CLEAR_SEARCH_AFTER = 300;

    self.optNodeForKeyboardSearch = function(e) {
      clearSearchTimeout && clearTimeout(clearSearchTimeout);
      clearSearchTimeout = setTimeout(function() {
        clearSearchTimeout = undefined;
        searchStr = '';
        optText = undefined;
        optNodes = undefined;
      }, CLEAR_SEARCH_AFTER);

      searchStr += e.key;
      var search = new RegExp('^' + searchStr, 'i');
      if (!optNodes) {
        optNodes = $element.find('md-option');
        optText = new Array(optNodes.length);
        angular.forEach(optNodes, function(el, i) {
          optText[i] = el.textContent.trim();
        });
      }
      for (var i = 0; i < optText.length; ++i) {
        if (search.test(optText[i])) {
          return optNodes[i];
        }
      }
    };

    self.init = function(ngModel, binding) {
      self.ngModel = ngModel;
      self.modelBinding = binding;

      // Setup a more robust version of isEmpty to ensure value is a valid option
      self.ngModel.$isEmpty = function($viewValue) {
        // We have to transform the viewValue into the hashKey, because otherwise the
        // OptionCtrl may not exist. Developers may have specified a trackBy function.
        return !self.options[self.hashGetter($viewValue)];
      };

      // Allow users to provide `ng-model="foo" ng-model-options="{trackBy: 'foo.id'}"` so
      // that we can properly compare objects set on the model to the available options
      var trackByOption = $mdUtil.getModelOption(ngModel, 'trackBy');

      if (trackByOption) {
        var trackByLocals = {};
        var trackByParsed = $parse(trackByOption);
        self.hashGetter = function(value, valueScope) {
          trackByLocals.$value = value;
          return trackByParsed(valueScope || $scope, trackByLocals);
        };
        // If the user doesn't provide a trackBy, we automatically generate an id for every
        // value passed in
      } else {
        self.hashGetter = function getHashValue(value) {
          if (angular.isObject(value)) {
            return 'object_' + (value.$$mdSelectId || (value.$$mdSelectId = ++selectNextId));
          }
          return value;
        };
      }
      self.setMultiple(self.isMultiple);
    };

    self.selectedLabels = function(opts) {
      opts = opts || {};
      var mode = opts.mode || 'html';
      var selectedOptionEls = $mdUtil.nodesToArray($element[0].querySelectorAll('md-option[selected]'));
      if (selectedOptionEls.length) {
        var mapFn;

        if (mode == 'html') {
          // Map the given element to its innerHTML string. If the element has a child ripple
          // container remove it from the HTML string, before returning the string.
          mapFn = function(el) {
            // If we do not have a `value` or `ng-value`, assume it is an empty option which clears the select
            if (el.hasAttribute('md-option-empty')) {
              return '';
            }

            var html = el.innerHTML;

            // Remove the ripple container from the selected option, copying it would cause a CSP violation.
            var rippleContainer = el.querySelector('.md-ripple-container');
            if (rippleContainer) {
              html = html.replace(rippleContainer.outerHTML, '');
            }

            // Remove the checkbox container, because it will cause the label to wrap inside of the placeholder.
            // It should be not displayed inside of the label element.
            var checkboxContainer = el.querySelector('.md-container');
            if (checkboxContainer) {
              html = html.replace(checkboxContainer.outerHTML, '');
            }

            return html;
          };
        } else if (mode == 'aria') {
          mapFn = function(el) { return el.hasAttribute('aria-label') ? el.getAttribute('aria-label') : el.textContent; };
        }

        // Ensure there are no duplicates; see https://github.com/angular/material/issues/9442
        return $mdUtil.uniq(selectedOptionEls.map(mapFn)).join(', ');
      } else {
        return '';
      }
    };

    self.select = function(hashKey, hashedValue) {
      var option = self.options[hashKey];
      option && option.setSelected(true);
      self.selected[hashKey] = hashedValue;
    };
    self.deselect = function(hashKey) {
      var option = self.options[hashKey];
      option && option.setSelected(false);
      delete self.selected[hashKey];
    };

    self.addOption = function(hashKey, optionCtrl) {
      if (angular.isDefined(self.options[hashKey])) {
        throw new Error('Duplicate md-option values are not allowed in a select. ' +
          'Duplicate value "' + optionCtrl.value + '" found.');
      }

      self.options[hashKey] = optionCtrl;

      // If this option's value was already in our ngModel, go ahead and select it.
      if (angular.isDefined(self.selected[hashKey])) {
        self.select(hashKey, optionCtrl.value);

        // When the current $modelValue of the ngModel Controller is using the same hash as
        // the current option, which will be added, then we can be sure, that the validation
        // of the option has occurred before the option was added properly.
        // This means, that we have to manually trigger a new validation of the current option.
        if (angular.isDefined(self.ngModel.$$rawModelValue) &&
            self.hashGetter(self.ngModel.$$rawModelValue) === hashKey) {
          self.ngModel.$validate();
        }

        self.refreshViewValue();
      }
    };
    self.removeOption = function(hashKey) {
      delete self.options[hashKey];
      // Don't deselect an option when it's removed - the user's ngModel should be allowed
      // to have values that do not match a currently available option.
    };

    self.refreshViewValue = function() {
      var values = [];
      var option;
      for (var hashKey in self.selected) {
        // If this hashKey has an associated option, push that option's value to the model.
        if ((option = self.options[hashKey])) {
          values.push(option.value);
        } else {
          // Otherwise, the given hashKey has no associated option, and we got it
          // from an ngModel value at an earlier time. Push the unhashed value of
          // this hashKey to the model.
          // This allows the developer to put a value in the model that doesn't yet have
          // an associated option.
          values.push(self.selected[hashKey]);
        }
      }
      var usingTrackBy = $mdUtil.getModelOption(self.ngModel, 'trackBy');

      var newVal = self.isMultiple ? values : values[0];
      var prevVal = self.ngModel.$modelValue;

      if (usingTrackBy ? !angular.equals(prevVal, newVal) : (prevVal + '') !== newVal) {
        self.ngModel.$setViewValue(newVal);
        self.ngModel.$render();
      }
    };

    function renderMultiple() {
      var newSelectedValues = self.ngModel.$modelValue || self.ngModel.$viewValue || [];
      if (!angular.isArray(newSelectedValues)) return;

      var oldSelected = Object.keys(self.selected);

      var newSelectedHashes = newSelectedValues.map(self.hashGetter);
      var deselected = oldSelected.filter(function(hash) {
        return newSelectedHashes.indexOf(hash) === -1;
      });

      deselected.forEach(self.deselect);
      newSelectedHashes.forEach(function(hashKey, i) {
        self.select(hashKey, newSelectedValues[i]);
      });
    }

    function renderSingular() {
      var value = self.ngModel.$viewValue || self.ngModel.$modelValue;
      Object.keys(self.selected).forEach(self.deselect);
      self.select(self.hashGetter(value), value);
    }
  }

}

/**
 * @ngdoc directive
 * @name mdOption
 * @restrict E
 * @module material.components.select
 *
 * @description Displays an option in a <a ng-href="/api/directive/mdSelect">md-select</a> box's
 * dropdown menu. Options can be grouped using
 * <a ng-href="/api/directive/mdOptgroup">md-optgroup</a> element directives.
 *
 * ### Option Params
 *
 * When applied, `md-option-empty` will mark the option as "empty" allowing the option to clear the
 * select and put it back in it's default state. You may supply this attribute on any option you
 * wish, however, it is automatically applied to an option whose `value` or `ng-value` are not
 * defined.
 *
 * **Automatically Applied**
 *
 *  - `<md-option>`
 *  - `<md-option value>`
 *  - `<md-option value="">`
 *  - `<md-option ng-value>`
 *  - `<md-option ng-value="">`
 *
 * **NOT Automatically Applied**
 *
 *  - `<md-option ng-value="1">`
 *  - `<md-option ng-value="''">`
 *  - `<md-option ng-value="undefined">`
 *  - `<md-option value="undefined">` (this evaluates to the string `"undefined"`)
 *  - <code ng-non-bindable>&lt;md-option ng-value="{{someValueThatMightBeUndefined}}"&gt;</code>
 *
 * **Note:** A value of `undefined` ***is considered a valid value*** (and does not auto-apply this
 * attribute) since you may wish this to be your "Not Available" or "None" option.
 *
 * **Note:** Using the
 * <a ng-href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/option#Attributes">value</a>
 * attribute from the `<option>` element (as opposed to the `<md-option>` element's
 * <a ng-href="https://docs.angularjs.org/api/ng/directive/ngValue">ng-value</a>) always evaluates
 * to a `string`. This means that `value="null"` will cause a check against `myValue != "null"`
 * rather than `!myValue` or `myValue != null`.
 * Importantly, this also applies to `number` values. `value="1"` will not match up with an
 * `ng-model` like `$scope.selectedValue = 1`. Use `ng-value="1"` in this case and other cases where
 * you have values that are not strings.
 *
 * **Note:** Please see our <a ng-href="/api/directive/mdSelect#selects-and-object-equality">docs on
 * using objects with `md-select`</a> for additional guidance on using the `trackBy` option with
 * `ng-model-options`.
 *
 * @param {expression=} ng-value Binds the given expression to the value of the option.
 * @param {string=} value Attribute to set the value of the option.
 * @param {expression=} ng-repeat <a ng-href="https://docs.angularjs.org/api/ng/directive/ngRepeat">
 *  AngularJS directive</a> that instantiates a template once per item from a collection.
 * @param {boolean=} md-option-empty If the attribute exists, mark the option as "empty" allowing
 * the option to clear the select and put it back in it's default state. You may supply this
 * attribute on any option you wish, however, it is automatically applied to an option whose `value`
 * or `ng-value` are not defined.
 * @param {number=} tabindex The `tabindex` of the option. Defaults to `0`.
 *
 * @usage
 * <hljs lang="html">
 * <md-select ng-model="currentState" placeholder="Select a state">
 *   <md-option ng-value="AL">Alabama</md-option>
 *   <md-option ng-value="AK">Alaska</md-option>
 *   <md-option ng-value="FL">Florida</md-option>
 * </md-select>
 * </hljs>
 *
 * With `ng-repeat`:
 * <hljs lang="html">
 * <md-select ng-model="currentState" placeholder="Select a state">
 *   <md-option ng-value="state" ng-repeat="state in states">{{ state }}</md-option>
 * </md-select>
 * </hljs>
 */
function OptionDirective($mdButtonInkRipple, $mdUtil, $mdTheming) {

  OptionController['$inject'] = ["$element"];
  return {
    restrict: 'E',
    require: ['mdOption', '^^mdSelectMenu'],
    controller: OptionController,
    compile: compile
  };

  function compile(element, attr) {
    // Manual transclusion to avoid the extra inner <span> that ng-transclude generates
    element.append(angular.element('<div class="md-text">').append(element.contents()));

    element.attr('tabindex', attr.tabindex || '0');

    if (!hasDefinedValue(attr)) {
      element.attr('md-option-empty', '');
    }

    return postLink;
  }

  function hasDefinedValue(attr) {
    var value = attr.value;
    var ngValue = attr.ngValue;

    return value || ngValue;
  }

  function postLink(scope, element, attr, ctrls) {
    var optionCtrl = ctrls[0];
    var selectCtrl = ctrls[1];

    $mdTheming(element);

    if (selectCtrl.isMultiple) {
      element.addClass('md-checkbox-enabled');
      element.prepend(CHECKBOX_SELECTION_INDICATOR.clone());
    }

    if (angular.isDefined(attr.ngValue)) {
      scope.$watch(attr.ngValue, setOptionValue);
    } else if (angular.isDefined(attr.value)) {
      setOptionValue(attr.value);
    } else {
      scope.$watch(function() {
        return element.text().trim();
      }, setOptionValue);
    }

    attr.$observe('disabled', function(disabled) {
      if (disabled) {
        element.attr('tabindex', '-1');
      } else {
        element.attr('tabindex', '0');
      }
    });

    scope.$$postDigest(function() {
      attr.$observe('selected', function(selected) {
        if (!angular.isDefined(selected)) return;
        if (typeof selected == 'string') selected = true;
        if (selected) {
          if (!selectCtrl.isMultiple) {
            selectCtrl.deselect(Object.keys(selectCtrl.selected)[0]);
          }
          selectCtrl.select(optionCtrl.hashKey, optionCtrl.value);
        } else {
          selectCtrl.deselect(optionCtrl.hashKey);
        }
        selectCtrl.refreshViewValue();
      });
    });

    $mdButtonInkRipple.attach(scope, element);
    configureAria();

    function setOptionValue(newValue, oldValue, prevAttempt) {
      if (!selectCtrl.hashGetter) {
        if (!prevAttempt) {
          scope.$$postDigest(function() {
            setOptionValue(newValue, oldValue, true);
          });
        }
        return;
      }
      var oldHashKey = selectCtrl.hashGetter(oldValue, scope);
      var newHashKey = selectCtrl.hashGetter(newValue, scope);

      optionCtrl.hashKey = newHashKey;
      optionCtrl.value = newValue;

      selectCtrl.removeOption(oldHashKey, optionCtrl);
      selectCtrl.addOption(newHashKey, optionCtrl);
    }

    scope.$on('$destroy', function() {
      selectCtrl.removeOption(optionCtrl.hashKey, optionCtrl);
    });

    function configureAria() {
      var ariaAttrs = {
        'role': 'option',
        'aria-selected': 'false'
      };

      if (!element[0].hasAttribute('id')) {
        ariaAttrs.id = 'select_option_' + $mdUtil.nextUid();
      }
      element.attr(ariaAttrs);
    }
  }

  function OptionController($element) {
    this.selected = false;
    this.setSelected = function(isSelected) {
      if (isSelected && !this.selected) {
        $element.attr({
          'selected': 'selected',
          'aria-selected': 'true'
        });
      } else if (!isSelected && this.selected) {
        $element.removeAttr('selected');
        $element.attr('aria-selected', 'false');
      }
      this.selected = isSelected;
    };
  }

}

/**
 * @ngdoc directive
 * @name mdOptgroup
 * @restrict E
 * @module material.components.select
 *
 * @description Displays a label separating groups of
 * <a ng-href="/api/directive/mdOption">md-option</a> element directives in a
 * <a ng-href="/api/directive/mdSelect">md-select</a> box's dropdown menu.
 *
 * **Note:** When using `md-select-header` element directives within a `md-select`, the labels that
 * would normally be added to the <a ng-href="/api/directive/mdOptgroup">md-optgroup</a> directives
 * are omitted, allowing the `md-select-header` to represent the option group label
 * (and possibly more).
 *
 * @usage
 * With label attributes
 * <hljs lang="html">
 * <md-select ng-model="currentState" placeholder="Select a state">
 *   <md-optgroup label="Southern">
 *     <md-option ng-value="AL">Alabama</md-option>
 *     <md-option ng-value="FL">Florida</md-option>
 *   </md-optgroup>
 *   <md-optgroup label="Northern">
 *     <md-option ng-value="AK">Alaska</md-option>
 *     <md-option ng-value="MA">Massachusetts</md-option>
 *   </md-optgroup>
 * </md-select>
 * </hljs>
 *
 * With label elements
 * <hljs lang="html">
 * <md-select ng-model="currentState" placeholder="Select a state">
 *   <md-optgroup>
 *     <label>Southern</label>
 *     <md-option ng-value="AL">Alabama</md-option>
 *     <md-option ng-value="FL">Florida</md-option>
 *   </md-optgroup>
 *   <md-optgroup>
 *     <label>Northern</label>
 *     <md-option ng-value="AK">Alaska</md-option>
 *     <md-option ng-value="MA">Massachusetts</md-option>
 *   </md-optgroup>
 * </md-select>
 * </hljs>
 *
 * @param {string=} label The option group's label.
 */
function OptgroupDirective() {
  return {
    restrict: 'E',
    compile: compile
  };
  function compile(el, attrs) {
    // If we have a select header element, we don't want to add the normal label
    // header.
    if (!hasSelectHeader()) {
      setupLabelElement();
    }

    function hasSelectHeader() {
      return el.parent().find('md-select-header').length;
    }

    function setupLabelElement() {
      var labelElement = el.find('label');
      if (!labelElement.length) {
        labelElement = angular.element('<label>');
        el.prepend(labelElement);
      }
      labelElement.addClass('md-container-ignore');
      labelElement.attr('aria-hidden', 'true');
      if (attrs.label) labelElement.text(attrs.label);
    }
  }
}

function SelectHeaderDirective() {
  return {
    restrict: 'E',
  };
}

function SelectProvider($$interimElementProvider) {
  selectDefaultOptions['$inject'] = ["$mdSelect", "$mdConstant", "$mdUtil", "$window", "$q", "$$rAF", "$animateCss", "$animate", "$document"];
  return $$interimElementProvider('$mdSelect')
    .setDefaults({
      methods: ['target'],
      options: selectDefaultOptions
    });

  /* ngInject */
  function selectDefaultOptions($mdSelect, $mdConstant, $mdUtil, $window, $q, $$rAF, $animateCss, $animate, $document) {
    var ERROR_TARGET_EXPECTED = "$mdSelect.show() expected a target element in options.target but got '{0}'!";
    var animator = $mdUtil.dom.animator;
    var keyCodes = $mdConstant.KEY_CODE;

    return {
      parent: 'body',
      themable: true,
      onShow: onShow,
      onRemove: onRemove,
      hasBackdrop: true,
      disableParentScroll: true
    };

    /**
     * Interim-element onRemove logic....
     */
    function onRemove(scope, element, opts) {
      var animationRunner = null;
      var destroyListener = scope.$on('$destroy', function() {
        // Listen for the case where the element was destroyed while there was an
        // ongoing close animation. If this happens, we need to end the animation
        // manually.
        animationRunner.end();
      });

      opts = opts || { };
      opts.cleanupInteraction();
      opts.cleanupResizing();
      opts.hideBackdrop();

      // For navigation $destroy events, do a quick, non-animated removal,
      // but for normal closes (from clicks, etc) animate the removal
      return (opts.$destroy === true) ? cleanElement() : animateRemoval().then(cleanElement);

      /**
       * For normal closes (eg clicks), animate the removal.
       * For forced closes (like $destroy events from navigation),
       * skip the animations
       */
      function animateRemoval() {
        animationRunner = $animateCss(element, {addClass: 'md-leave'});
        return animationRunner.start();
      }

      /**
       * Restore the element to a closed state
       */
      function cleanElement() {
        destroyListener();

        element
          .removeClass('md-active')
          .attr('aria-hidden', 'true')
          .css({
            'display': 'none',
            'top': '',
            'right': '',
            'bottom': '',
            'left': '',
            'font-size': '',
            'min-width': ''
          });
        element.parent().find('md-select-value').removeAttr('aria-hidden');

        announceClosed(opts);

        if (!opts.$destroy && opts.restoreFocus) {
          opts.target.focus();
        }
      }

    }

    /**
     * Interim-element onShow logic....
     */
    function onShow(scope, element, opts) {

      watchAsyncLoad();
      sanitizeAndConfigure(scope, opts);

      opts.hideBackdrop = showBackdrop(scope, element, opts);

      return showDropDown(scope, element, opts)
        .then(function(response) {
          element.attr('aria-hidden', 'false');
          opts.alreadyOpen = true;
          opts.cleanupInteraction = activateInteraction();
          opts.cleanupResizing = activateResizing();
          autoFocus(opts.focusedNode);

          return response;
        }, opts.hideBackdrop);

      // ************************************
      // Closure Functions
      // ************************************

      /**
       *  Attach the select DOM element(s) and animate to the correct positions
       *  and scalings...
       */
      function showDropDown(scope, element, opts) {
        if (opts.parent !== element.parent()) {
          element.parent().attr('aria-owns', element.attr('id'));
        }
        element.parent().find('md-select-value').attr('aria-hidden', 'true');

        opts.parent.append(element);

        return $q(function(resolve, reject) {

          try {

            $animateCss(element, {removeClass: 'md-leave', duration: 0})
              .start()
              .then(positionAndFocusMenu)
              .then(resolve);

          } catch (e) {
            reject(e);
          }

        });
      }

      /**
       * Initialize container and dropDown menu positions/scale, then animate
       * to show.
       */
      function positionAndFocusMenu() {
        return $q(function(resolve) {
          if (opts.isRemoved) return $q.reject(false);

          var info = calculateMenuPositions(scope, element, opts);

          info.container.element.css(animator.toCss(info.container.styles));
          info.dropDown.element.css(animator.toCss(info.dropDown.styles));

          $$rAF(function() {
            element.addClass('md-active');
            info.dropDown.element.css(animator.toCss({transform: ''}));
            autoFocus(opts.focusedNode);

            resolve();
          });

        });
      }

      /**
       * Show modal backdrop element...
       */
      function showBackdrop(scope, element, options) {

        // If we are not within a dialog...
        if (options.disableParentScroll && !$mdUtil.getClosest(options.target, 'MD-DIALOG')) {
          // !! DO this before creating the backdrop; since disableScrollAround()
          //    configures the scroll offset; which is used by mdBackDrop postLink()
          options.restoreScroll = $mdUtil.disableScrollAround(options.element, options.parent);
        } else {
          options.disableParentScroll = false;
        }

        if (options.hasBackdrop) {
          // Override duration to immediately show invisible backdrop
          options.backdrop = $mdUtil.createBackdrop(scope, "md-select-backdrop md-click-catcher");
          $animate.enter(options.backdrop, $document[0].body, null, {duration: 0});
        }

        /**
         * Hide modal backdrop element...
         */
        return function hideBackdrop() {
          if (options.backdrop) options.backdrop.remove();
          if (options.disableParentScroll) options.restoreScroll();

          delete options.restoreScroll;
        };
      }

      /**
       *
       */
      function autoFocus(focusedNode) {
        if (focusedNode && !focusedNode.hasAttribute('disabled')) {
          focusedNode.focus();
        }
      }

      /**
       * Check for valid opts and set some sane defaults
       */
      function sanitizeAndConfigure(scope, options) {
        var selectEl = element.find('md-select-menu');

        if (!options.target) {
          throw new Error($mdUtil.supplant(ERROR_TARGET_EXPECTED, [options.target]));
        }

        angular.extend(options, {
          isRemoved: false,
          target: angular.element(options.target), // make sure it's not a naked DOM node
          parent: angular.element(options.parent),
          selectEl: selectEl,
          contentEl: element.find('md-content'),
          optionNodes: selectEl[0].getElementsByTagName('md-option')
        });
      }

      /**
       * Configure various resize listeners for screen changes
       */
      function activateResizing() {
        var debouncedOnResize = (function(scope, target, options) {

          return function() {
            if (options.isRemoved) return;

            var updates = calculateMenuPositions(scope, target, options);
            var container = updates.container;
            var dropDown = updates.dropDown;

            container.element.css(animator.toCss(container.styles));
            dropDown.element.css(animator.toCss(dropDown.styles));
          };

        })(scope, element, opts);

        var window = angular.element($window);
        window.on('resize', debouncedOnResize);
        window.on('orientationchange', debouncedOnResize);

        // Publish deactivation closure...
        return function deactivateResizing() {

          // Disable resizing handlers
          window.off('resize', debouncedOnResize);
          window.off('orientationchange', debouncedOnResize);
        };
      }

      /**
       *  If asynchronously loading, watch and update internal
       *  '$$loadingAsyncDone' flag
       */
      function watchAsyncLoad() {
        if (opts.loadingAsync && !opts.isRemoved) {
          scope.$$loadingAsyncDone = false;

          $q.when(opts.loadingAsync)
            .then(function() {
              scope.$$loadingAsyncDone = true;
              delete opts.loadingAsync;
            }).then(function() {
              $$rAF(positionAndFocusMenu);
            });
        }
      }

      /**
       *
       */
      function activateInteraction() {
        if (opts.isRemoved) return;

        var dropDown = opts.selectEl;
        var selectCtrl = dropDown.controller('mdSelectMenu') || {};

        element.addClass('md-clickable');

        // Close on backdrop click
        opts.backdrop && opts.backdrop.on('click', onBackdropClick);

        // Escape to close
        // Cycling of options, and closing on enter
        dropDown.on('keydown', onMenuKeyDown);
        dropDown.on('click', checkCloseMenu);

        return function cleanupInteraction() {
          opts.backdrop && opts.backdrop.off('click', onBackdropClick);
          dropDown.off('keydown', onMenuKeyDown);
          dropDown.off('click', checkCloseMenu);

          element.removeClass('md-clickable');
          opts.isRemoved = true;
        };

        // ************************************
        // Closure Functions
        // ************************************

        function onBackdropClick(e) {
          e.preventDefault();
          e.stopPropagation();
          opts.restoreFocus = false;
          $mdUtil.nextTick($mdSelect.hide, true);
        }

        function onMenuKeyDown(ev) {
          ev.preventDefault();
          ev.stopPropagation();

          switch (ev.keyCode) {
            case keyCodes.UP_ARROW:
              return focusPrevOption();
            case keyCodes.DOWN_ARROW:
              return focusNextOption();
            case keyCodes.SPACE:
            case keyCodes.ENTER:
              var option = $mdUtil.getClosest(ev.target, 'md-option');
              if (option) {
                dropDown.triggerHandler({
                  type: 'click',
                  target: option
                });
                ev.preventDefault();
              }
              checkCloseMenu(ev);
              break;
            case keyCodes.TAB:
            case keyCodes.ESCAPE:
              ev.stopPropagation();
              ev.preventDefault();
              opts.restoreFocus = true;
              $mdUtil.nextTick($mdSelect.hide, true);
              break;
            default:
              if (shouldHandleKey(ev, $mdConstant)) {
                var optNode = dropDown.controller('mdSelectMenu').optNodeForKeyboardSearch(ev);
                opts.focusedNode = optNode || opts.focusedNode;
                optNode && optNode.focus();
              }
          }
        }

        function focusOption(direction) {
          var optionsArray = $mdUtil.nodesToArray(opts.optionNodes);
          var index = optionsArray.indexOf(opts.focusedNode);

          var newOption;

          do {
            if (index === -1) {
              // We lost the previously focused element, reset to first option
              index = 0;
            } else if (direction === 'next' && index < optionsArray.length - 1) {
              index++;
            } else if (direction === 'prev' && index > 0) {
              index--;
            }
            newOption = optionsArray[index];
            if (newOption.hasAttribute('disabled')) newOption = undefined;
          } while (!newOption && index < optionsArray.length - 1 && index > 0);

          newOption && newOption.focus();
          opts.focusedNode = newOption;
        }

        function focusNextOption() {
          focusOption('next');
        }

        function focusPrevOption() {
          focusOption('prev');
        }

        function checkCloseMenu(ev) {
          if (ev && (ev.type == 'click') && (ev.currentTarget != dropDown[0])) return;
          if (mouseOnScrollbar()) return;

          var option = $mdUtil.getClosest(ev.target, 'md-option');
          if (option && option.hasAttribute && !option.hasAttribute('disabled')) {
            ev.preventDefault();
            ev.stopPropagation();
            if (!selectCtrl.isMultiple) {
              opts.restoreFocus = true;

              $mdUtil.nextTick(function () {
                $mdSelect.hide(selectCtrl.ngModel.$viewValue);
              }, true);
            }
          }
          /**
           * check if the mouseup event was on a scrollbar
           */
          function mouseOnScrollbar() {
            var clickOnScrollbar = false;
            if (ev && (ev.currentTarget.children.length > 0)) {
              var child = ev.currentTarget.children[0];
              var hasScrollbar = child.scrollHeight > child.clientHeight;
              if (hasScrollbar && child.children.length > 0) {
                var relPosX = ev.pageX - ev.currentTarget.getBoundingClientRect().left;
                if (relPosX > child.querySelector('md-option').offsetWidth)
                  clickOnScrollbar = true;
              }
            }
            return clickOnScrollbar;
          }
        }
      }

    }

    /**
     * To notify listeners that the Select menu has closed,
     * trigger the [optional] user-defined expression
     */
    function announceClosed(opts) {
      var mdSelect = opts.selectCtrl;
      if (mdSelect) {
        var menuController = opts.selectEl.controller('mdSelectMenu');
        mdSelect.setLabelText(menuController ? menuController.selectedLabels() : '');
        mdSelect.triggerClose();
      }
    }


    /**
     * Calculate the
     */
    function calculateMenuPositions(scope, element, opts) {
      var
        containerNode = element[0],
        targetNode = opts.target[0].children[0], // target the label
        parentNode = $document[0].body,
        selectNode = opts.selectEl[0],
        contentNode = opts.contentEl[0],
        parentRect = parentNode.getBoundingClientRect(),
        targetRect = targetNode.getBoundingClientRect(),
        shouldOpenAroundTarget = false,
        bounds = {
          left: parentRect.left + SELECT_EDGE_MARGIN,
          top: SELECT_EDGE_MARGIN,
          bottom: parentRect.height - SELECT_EDGE_MARGIN,
          right: parentRect.width - SELECT_EDGE_MARGIN - ($mdUtil.floatingScrollbars() ? 16 : 0)
        },
        spaceAvailable = {
          top: targetRect.top - bounds.top,
          left: targetRect.left - bounds.left,
          right: bounds.right - (targetRect.left + targetRect.width),
          bottom: bounds.bottom - (targetRect.top + targetRect.height)
        },
        maxWidth = parentRect.width - SELECT_EDGE_MARGIN * 2,
        selectedNode = selectNode.querySelector('md-option[selected]'),
        optionNodes = selectNode.getElementsByTagName('md-option'),
        optgroupNodes = selectNode.getElementsByTagName('md-optgroup'),
        isScrollable = calculateScrollable(element, contentNode),
        centeredNode;

      var loading = isPromiseLike(opts.loadingAsync);
      if (!loading) {
        // If a selected node, center around that
        if (selectedNode) {
          centeredNode = selectedNode;
          // If there are option groups, center around the first option group
        } else if (optgroupNodes.length) {
          centeredNode = optgroupNodes[0];
          // Otherwise - if we are not loading async - center around the first optionNode
        } else if (optionNodes.length) {
          centeredNode = optionNodes[0];
          // In case there are no options, center on whatever's in there... (eg progress indicator)
        } else {
          centeredNode = contentNode.firstElementChild || contentNode;
        }
      } else {
        // If loading, center on progress indicator
        centeredNode = contentNode.firstElementChild || contentNode;
      }

      if (contentNode.offsetWidth > maxWidth) {
        contentNode.style['max-width'] = maxWidth + 'px';
      } else {
        contentNode.style.maxWidth = null;
      }
      if (shouldOpenAroundTarget) {
        contentNode.style['min-width'] = targetRect.width + 'px';
      }

      // Remove padding before we compute the position of the menu
      if (isScrollable) {
        selectNode.classList.add('md-overflow');
      }

      var focusedNode = centeredNode;
      if ((focusedNode.tagName || '').toUpperCase() === 'MD-OPTGROUP') {
        focusedNode = optionNodes[0] || contentNode.firstElementChild || contentNode;
        centeredNode = focusedNode;
      }
      // Cache for autoFocus()
      opts.focusedNode = focusedNode;

      // Get the selectMenuRect *after* max-width is possibly set above
      containerNode.style.display = 'block';
      var selectMenuRect = selectNode.getBoundingClientRect();
      var centeredRect = getOffsetRect(centeredNode);

      if (centeredNode) {
        var centeredStyle = $window.getComputedStyle(centeredNode);
        centeredRect.paddingLeft = parseInt(centeredStyle.paddingLeft, 10) || 0;
        centeredRect.paddingRight = parseInt(centeredStyle.paddingRight, 10) || 0;
      }

      if (isScrollable) {
        var scrollBuffer = contentNode.offsetHeight / 2;
        contentNode.scrollTop = centeredRect.top + centeredRect.height / 2 - scrollBuffer;

        if (spaceAvailable.top < scrollBuffer) {
          contentNode.scrollTop = Math.min(
            centeredRect.top,
            contentNode.scrollTop + scrollBuffer - spaceAvailable.top
          );
        } else if (spaceAvailable.bottom < scrollBuffer) {
          contentNode.scrollTop = Math.max(
            centeredRect.top + centeredRect.height - selectMenuRect.height,
            contentNode.scrollTop - scrollBuffer + spaceAvailable.bottom
          );
        }
      }

      var left, top, transformOrigin, minWidth, fontSize;
      if (shouldOpenAroundTarget) {
        left = targetRect.left;
        top = targetRect.top + targetRect.height;
        transformOrigin = '50% 0';
        if (top + selectMenuRect.height > bounds.bottom) {
          top = targetRect.top - selectMenuRect.height;
          transformOrigin = '50% 100%';
        }
      } else {
        left = (targetRect.left + centeredRect.left - centeredRect.paddingLeft) + 2;
        top = Math.floor(targetRect.top + targetRect.height / 2 - centeredRect.height / 2 -
            centeredRect.top + contentNode.scrollTop) + 2;

        transformOrigin = (centeredRect.left + targetRect.width / 2) + 'px ' +
          (centeredRect.top + centeredRect.height / 2 - contentNode.scrollTop) + 'px 0px';

        minWidth = Math.min(targetRect.width + centeredRect.paddingLeft + centeredRect.paddingRight, maxWidth);

        fontSize = window.getComputedStyle(targetNode)['font-size'];
      }

      // Keep left and top within the window
      var containerRect = containerNode.getBoundingClientRect();
      var scaleX = Math.round(100 * Math.min(targetRect.width / selectMenuRect.width, 1.0)) / 100;
      var scaleY = Math.round(100 * Math.min(targetRect.height / selectMenuRect.height, 1.0)) / 100;

      return {
        container: {
          element: angular.element(containerNode),
          styles: {
            left: Math.floor(clamp(bounds.left, left, bounds.right - containerRect.width)),
            top: Math.floor(clamp(bounds.top, top, bounds.bottom - containerRect.height)),
            'min-width': minWidth,
            'font-size': fontSize
          }
        },
        dropDown: {
          element: angular.element(selectNode),
          styles: {
            transformOrigin: transformOrigin,
            transform: !opts.alreadyOpen ? $mdUtil.supplant('scale({0},{1})', [scaleX, scaleY]) : ""
          }
        }
      };

    }

  }

  function isPromiseLike(obj) {
    return obj && angular.isFunction(obj.then);
  }

  function clamp(min, n, max) {
    return Math.max(min, Math.min(n, max));
  }

  function getOffsetRect(node) {
    return node ? {
      left: node.offsetLeft,
      top: node.offsetTop,
      width: node.offsetWidth,
      height: node.offsetHeight
    } : {left: 0, top: 0, width: 0, height: 0};
  }

  function calculateScrollable(element, contentNode) {
    var isScrollable = false;

    try {
      var oldDisplay = element[0].style.display;

      // Set the element's display to block so that this calculation is correct
      element[0].style.display = 'block';

      isScrollable = contentNode.scrollHeight > contentNode.offsetHeight;

      // Reset it back afterwards
      element[0].style.display = oldDisplay;
    } finally {
      // Nothing to do
    }
    return isScrollable;
  }

}

function shouldHandleKey(ev, $mdConstant) {
  var char = String.fromCharCode(ev.keyCode);
  var isNonUsefulKey = (ev.keyCode <= 31);

  return (char && char.length && !isNonUsefulKey &&
    !$mdConstant.isMetaKey(ev) && !$mdConstant.isFnLockKey(ev) && !$mdConstant.hasModifierKey(ev));
}

ngmaterial.components.select = angular.module("material.components.select");