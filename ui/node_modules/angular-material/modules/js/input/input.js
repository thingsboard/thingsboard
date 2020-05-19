/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
(function( window, angular, undefined ){
"use strict";

/**
 * @ngdoc module
 * @name material.components.input
 */
mdInputContainerDirective['$inject'] = ["$mdTheming", "$parse", "$$rAF"];
inputTextareaDirective['$inject'] = ["$mdUtil", "$window", "$mdAria", "$timeout", "$mdGesture"];
mdMaxlengthDirective['$inject'] = ["$animate", "$mdUtil"];
placeholderDirective['$inject'] = ["$compile"];
ngMessageDirective['$inject'] = ["$mdUtil"];
mdSelectOnFocusDirective['$inject'] = ["$document", "$timeout"];
mdInputInvalidMessagesAnimation['$inject'] = ["$$AnimateRunner", "$animateCss", "$mdUtil"];
ngMessagesAnimation['$inject'] = ["$$AnimateRunner", "$animateCss", "$mdUtil"];
ngMessageAnimation['$inject'] = ["$$AnimateRunner", "$animateCss", "$mdUtil", "$log"];
var inputModule = angular.module('material.components.input', [
    'material.core'
  ])
  .directive('mdInputContainer', mdInputContainerDirective)
  .directive('label', labelDirective)
  .directive('input', inputTextareaDirective)
  .directive('textarea', inputTextareaDirective)
  .directive('mdMaxlength', mdMaxlengthDirective)
  .directive('placeholder', placeholderDirective)
  .directive('ngMessages', ngMessagesDirective)
  .directive('ngMessage', ngMessageDirective)
  .directive('ngMessageExp', ngMessageDirective)
  .directive('mdSelectOnFocus', mdSelectOnFocusDirective)

  .animation('.md-input-invalid', mdInputInvalidMessagesAnimation)
  .animation('.md-input-messages-animation', ngMessagesAnimation)
  .animation('.md-input-message-animation', ngMessageAnimation);

// If we are running inside of tests; expose some extra services so that we can test them
if (window._mdMocksIncluded) {
  inputModule.service('$$mdInput', function() {
    return {
      // special accessor to internals... useful for testing
      messages: {
        getElement  : getMessagesElement
      }
    };
  })

  // Register a service for each animation so that we can easily inject them into unit tests
  .service('mdInputInvalidAnimation', mdInputInvalidMessagesAnimation)
  .service('mdInputMessagesAnimation', ngMessagesAnimation)
  .service('mdInputMessageAnimation', ngMessageAnimation);
}

/**
 * @ngdoc directive
 * @name mdInputContainer
 * @module material.components.input
 *
 * @restrict E
 *
 * @description
 * `<md-input-container>` is the parent of any input or textarea element. It can also optionally
 * wrap `<md-select>` elements so that they will be formatted for use in a form.
 *
 * Input and textarea elements will not behave properly unless the md-input-container parent is
 * provided.
 *
 * A single `<md-input-container>` should contain only one `<input>` or `<md-select>` element,
 * otherwise it will throw an error.
 *
 * <b>Exception:</b> Hidden inputs (`<input type="hidden" />`) are ignored and will not throw an
 * error, so you may combine these with other inputs.
 *
 * <b>Note:</b> When using `ngMessages` with your input element, make sure the message and container
 * elements are *block* elements, otherwise animations applied to the messages will not look as
 * intended. Either use a `div` and apply the `ng-message` and `ng-messages` classes respectively,
 * or use the `md-block` class on your element.
 *
 * @param {expression=} md-is-error When the given expression evaluates to `true`, the input
 *   container will go into the error state. Defaults to erroring if the input has been touched and
 *   is invalid.
 * @param {boolean=} md-no-float When present, `placeholder` attributes on the input will not be
 *   converted to floating labels.
 *
 * @usage
 * <hljs lang="html">
 * <md-input-container>
 *   <label>Username</label>
 *   <input type="text" ng-model="user.name">
 * </md-input-container>
 *
 * <md-input-container>
 *   <label>Description</label>
 *   <textarea ng-model="user.description"></textarea>
 * </md-input-container>
 *
 * <md-input-container>
 *   <md-select ng-model="user.state" placeholder="State of Residence">
 *     <md-option ng-value="state" ng-repeat="state in states">{{ state }}</md-option>
 *   </md-select>
 * </md-input-container>
 * </hljs>
 *
 * <h3>When disabling floating labels</h3>
 * <hljs lang="html">
 *
 * <md-input-container md-no-float>
 *   <input type="text" placeholder="Non-Floating Label">
 * </md-input-container>
 * </hljs>
 */
function mdInputContainerDirective($mdTheming, $parse, $$rAF) {

  ContainerCtrl['$inject'] = ["$scope", "$element", "$attrs", "$animate"];
  var INPUT_TAGS = ['INPUT', 'TEXTAREA', 'SELECT', 'MD-SELECT'];

  var LEFT_SELECTORS = INPUT_TAGS.reduce(function(selectors, isel) {
    return selectors.concat(['md-icon ~ ' + isel, '.md-icon ~ ' + isel]);
  }, []).join(",");

  var RIGHT_SELECTORS = INPUT_TAGS.reduce(function(selectors, isel) {
    return selectors.concat([isel + ' ~ md-icon', isel + ' ~ .md-icon']);
  }, []).join(",");

  return {
    restrict: 'E',
    compile: compile,
    controller: ContainerCtrl
  };

  function compile(tElement) {
    // Check for both a left & right icon
    var hasLeftIcon = tElement[0].querySelector(LEFT_SELECTORS);
    var hasRightIcon = tElement[0].querySelector(RIGHT_SELECTORS);

    return function postLink(scope, element) {
      $mdTheming(element);

      if (hasLeftIcon || hasRightIcon) {
        // When accessing the element's contents synchronously, they may not be defined yet because
        // of the use of ng-if. If we wait one frame, then the element should be there if the ng-if
        // resolves to true.
        $$rAF(function() {
          // Handle the case where the md-icon element is initially hidden via ng-if from #9529.
          // We don't want to preserve the space for the icon in the case of ng-if, like we do for
          // ng-show.
          // Note that we can't use the same selectors from above because the elements are no longer
          // siblings for textareas at this point due to the insertion of the md-resize-wrapper.
          var iconNotRemoved = element[0].querySelector('md-icon') ||
            element[0].querySelector('.md-icon');
          if (hasLeftIcon && iconNotRemoved) {
            element.addClass('md-icon-left');
          }
          if (hasRightIcon && iconNotRemoved) {
            element.addClass('md-icon-right');
          }
        });
      }
    };
  }

  function ContainerCtrl($scope, $element, $attrs, $animate) {
    var self = this;

    self.isErrorGetter = $attrs.mdIsError && $parse($attrs.mdIsError);

    self.delegateClick = function() {
      self.input.focus();
    };
    self.element = $element;
    self.setFocused = function(isFocused) {
      $element.toggleClass('md-input-focused', !!isFocused);
    };
    self.setHasValue = function(hasValue) {
      $element.toggleClass('md-input-has-value', !!hasValue);
    };
    self.setHasPlaceholder = function(hasPlaceholder) {
      $element.toggleClass('md-input-has-placeholder', !!hasPlaceholder);
    };
    self.setInvalid = function(isInvalid) {
      if (isInvalid) {
        $animate.addClass($element, 'md-input-invalid');
      } else {
        $animate.removeClass($element, 'md-input-invalid');
      }
    };
    $scope.$watch(function() {
      return self.label && self.input;
    }, function(hasLabelAndInput) {
      if (hasLabelAndInput && !self.label.attr('for')) {
        self.label.attr('for', self.input.attr('id'));
      }
    });
  }
}

function labelDirective() {
  return {
    restrict: 'E',
    require: '^?mdInputContainer',
    link: function(scope, element, attr, containerCtrl) {
      if (!containerCtrl || attr.mdNoFloat || element.hasClass('md-container-ignore')) return;

      containerCtrl.label = element;
      scope.$on('$destroy', function() {
        containerCtrl.label = null;
      });
    }
  };
}

/**
 * @ngdoc directive
 * @name mdInput
 * @restrict E
 * @module material.components.input
 *
 * @description
 * You can use any `<input>` or `<textarea>` element as a child of an `<md-input-container>`. This
 * allows you to build complex forms for data entry.
 *
 * When the input is required and uses a floating label, then the label will automatically contain
 * an asterisk (`*`).<br/>
 * This behavior can be disabled by using the `md-no-asterisk` attribute.
 *
 * @param {number=} md-maxlength The maximum number of characters allowed in this input. If this is
 *   specified, a character counter will be shown underneath the input.<br/><br/>
 *   The purpose of **`md-maxlength`** is exactly to show the max length counter text. If you don't
 *   want the counter text and only need "plain" validation, you can use the "simple" `ng-maxlength`
 *   or maxlength attributes.<br/><br/>
 * @param {boolean=} ng-trim If set to false, the input text will be not trimmed automatically.
 *     Defaults to true.
 * @param {string=} aria-label Aria-label is required when no label is present.  A warning message
 *   will be logged in the console if not present.
 * @param {string=} placeholder An alternative approach to using aria-label when the label is not
 *   PRESENT. The placeholder text is copied to the aria-label attribute.
 * @param {boolean=} md-no-autogrow When present, textareas will not grow automatically.
 * @param {boolean=} md-no-asterisk When present, an asterisk will not be appended to the inputs
 *   floating label.
 * @param {boolean=} md-no-resize Disables the textarea resize handle.
 * @param {number=} max-rows The maximum amount of rows for a textarea.
 * @param {boolean=} md-detect-hidden When present, textareas will be sized properly when they are
 *   revealed after being hidden. This is off by default for performance reasons because it
 *   guarantees a reflow every digest cycle.
 *
 * @usage
 * <hljs lang="html">
 * <md-input-container>
 *   <label>Color</label>
 *   <input type="text" ng-model="color" required md-maxlength="10">
 * </md-input-container>
 * </hljs>
 *
 * <h3>With Errors</h3>
 *
 * `md-input-container` also supports errors using the standard `ng-messages` directives and
 * animates the messages when they become visible using from the `ngEnter`/`ngLeave` events or
 * the `ngShow`/`ngHide` events.
 *
 * By default, the messages will be hidden until the input is in an error state. This is based off
 * of the `md-is-error` expression of the `md-input-container`. This gives the user a chance to
 * fill out the form before the errors become visible.
 *
 * <hljs lang="html">
 * <form name="colorForm">
 *   <md-input-container>
 *     <label>Favorite Color</label>
 *     <input name="favoriteColor" ng-model="favoriteColor" required>
 *     <div ng-messages="colorForm.favoriteColor.$error">
 *       <div ng-message="required">This is required!</div>
 *     </div>
 *   </md-input-container>
 * </form>
 * </hljs>
 *
 * We automatically disable this auto-hiding functionality if you provide any of the following
 * visibility directives on the `ng-messages` container:
 *
 *  - `ng-if`
 *  - `ng-show`/`ng-hide`
 *  - `ng-switch-when`/`ng-switch-default`
 *
 * You can also disable this functionality manually by adding the `md-auto-hide="false"` expression
 * to the `ng-messages` container. This may be helpful if you always want to see the error messages
 * or if you are building your own visibility directive.
 *
 * _<b>Note:</b> The `md-auto-hide` attribute is a static string that is  only checked upon
 * initialization of the `ng-messages` directive to see if it equals the string `false`._
 *
 * <hljs lang="html">
 * <form name="userForm">
 *   <md-input-container>
 *     <label>Last Name</label>
 *     <input name="lastName" ng-model="lastName" required md-maxlength="10" minlength="4">
 *     <div ng-messages="userForm.lastName.$error" ng-show="userForm.lastName.$dirty">
 *       <div ng-message="required">This is required!</div>
 *       <div ng-message="md-maxlength">That's too long!</div>
 *       <div ng-message="minlength">That's too short!</div>
 *     </div>
 *   </md-input-container>
 *   <md-input-container>
 *     <label>Biography</label>
 *     <textarea name="bio" ng-model="biography" required md-maxlength="150"></textarea>
 *     <div ng-messages="userForm.bio.$error" ng-show="userForm.bio.$dirty">
 *       <div ng-message="required">This is required!</div>
 *       <div ng-message="md-maxlength">That's too long!</div>
 *     </div>
 *   </md-input-container>
 *   <md-input-container>
 *     <input aria-label='title' ng-model='title'>
 *   </md-input-container>
 *   <md-input-container>
 *     <input placeholder='title' ng-model='title'>
 *   </md-input-container>
 * </form>
 * </hljs>
 *
 * <h3>Notes</h3>
 *
 * - Requires [ngMessages](https://docs.angularjs.org/api/ngMessages).
 * - Behaves like the [AngularJS input directive](https://docs.angularjs.org/api/ng/directive/input).
 *
 * The `md-input` and `md-input-container` directives use very specific positioning to achieve the
 * error animation effects. Therefore, it is *not* advised to use the Layout system inside of the
 * `<md-input-container>` tags. Instead, use relative or absolute positioning.
 *
 *
 * <h3>Textarea directive</h3>
 * The `textarea` element within a `md-input-container` has the following specific behavior:
 * - By default the `textarea` grows as the user types. This can be disabled via the `md-no-autogrow`
 * attribute.
 * - If a `textarea` has the `rows` attribute, it will treat the `rows` as the minimum height and will
 * continue growing as the user types. For example a textarea with `rows="3"` will be 3 lines of text
 * high initially. If no rows are specified, the directive defaults to 1.
 * - The textarea's height gets set on initialization, as well as while the user is typing. In certain situations
 * (e.g. while animating) the directive might have been initialized, before the element got it's final height. In
 * those cases, you can trigger a resize manually by broadcasting a `md-resize-textarea` event on the scope.
 * - If you want a `textarea` to stop growing at a certain point, you can specify the `max-rows` attribute.
 * - The textarea's bottom border acts as a handle which users can drag, in order to resize the element vertically.
 * Once the user has resized a `textarea`, the autogrowing functionality becomes disabled. If you don't want a
 * `textarea` to be resizeable by the user, you can add the `md-no-resize` attribute.
 */

function inputTextareaDirective($mdUtil, $window, $mdAria, $timeout, $mdGesture) {
  return {
    restrict: 'E',
    require: ['^?mdInputContainer', '?ngModel', '?^form'],
    link: postLink
  };

  function postLink(scope, element, attr, ctrls) {

    var containerCtrl = ctrls[0];
    var hasNgModel = !!ctrls[1];
    var ngModelCtrl = ctrls[1] || $mdUtil.fakeNgModel();
    var parentForm = ctrls[2];
    var isReadonly = angular.isDefined(attr.readonly);
    var mdNoAsterisk = $mdUtil.parseAttributeBoolean(attr.mdNoAsterisk);
    var tagName = element[0].tagName.toLowerCase();


    if (!containerCtrl) return;
    if (attr.type === 'hidden') {
      element.attr('aria-hidden', 'true');
      return;
    } else if (containerCtrl.input) {
      if (containerCtrl.input[0].contains(element[0])) {
        return;
      } else {
        throw new Error("<md-input-container> can only have *one* <input>, <textarea> or <md-select> child element!");
      }
    }
    containerCtrl.input = element;

    setupAttributeWatchers();

    // Add an error spacer div after our input to provide space for the char counter and any ng-messages
    var errorsSpacer = angular.element('<div class="md-errors-spacer">');
    element.after(errorsSpacer);

    var placeholderText = angular.isString(attr.placeholder) ? attr.placeholder.trim() : '';
    if (!containerCtrl.label && !placeholderText.length) {
      $mdAria.expect(element, 'aria-label');
    }

    element.addClass('md-input');
    if (!element.attr('id')) {
      element.attr('id', 'input_' + $mdUtil.nextUid());
    }

    // This works around a Webkit issue where number inputs, placed in a flexbox, that have
    // a `min` and `max` will collapse to about 1/3 of their proper width. Please check #7349
    // for more info. Also note that we don't override the `step` if the user has specified it,
    // in order to prevent some unexpected behaviour.
    if (tagName === 'input' && attr.type === 'number' && attr.min && attr.max && !attr.step) {
      element.attr('step', 'any');
    } else if (tagName === 'textarea') {
      setupTextarea();
    }

    // If the input doesn't have an ngModel, it may have a static value. For that case,
    // we have to do one initial check to determine if the container should be in the
    // "has a value" state.
    if (!hasNgModel) {
      inputCheckValue();
    }

    var isErrorGetter = containerCtrl.isErrorGetter || function() {
      return ngModelCtrl.$invalid && (ngModelCtrl.$touched || (parentForm && parentForm.$submitted));
    };

    scope.$watch(isErrorGetter, containerCtrl.setInvalid);

    // When the developer uses the ngValue directive for the input, we have to observe the attribute, because
    // AngularJS's ngValue directive is just setting the `value` attribute.
    if (attr.ngValue) {
      attr.$observe('value', inputCheckValue);
    }

    ngModelCtrl.$parsers.push(ngModelPipelineCheckValue);
    ngModelCtrl.$formatters.push(ngModelPipelineCheckValue);

    element.on('input', inputCheckValue);

    if (!isReadonly) {
      element
        .on('focus', function(ev) {
          $mdUtil.nextTick(function() {
            containerCtrl.setFocused(true);
          });
        })
        .on('blur', function(ev) {
          $mdUtil.nextTick(function() {
            containerCtrl.setFocused(false);
            inputCheckValue();
          });
        });
    }

    scope.$on('$destroy', function() {
      containerCtrl.setFocused(false);
      containerCtrl.setHasValue(false);
      containerCtrl.input = null;
    });

    /** Gets run through ngModel's pipeline and set the `has-value` class on the container. */
    function ngModelPipelineCheckValue(arg) {
      containerCtrl.setHasValue(!ngModelCtrl.$isEmpty(arg));
      return arg;
    }

    function setupAttributeWatchers() {
      if (containerCtrl.label) {
        attr.$observe('required', function (value) {
          // We don't need to parse the required value, it's always a boolean because of angular's
          // required directive.
          containerCtrl.label.toggleClass('md-required', value && !mdNoAsterisk);
        });
      }
    }

    function inputCheckValue() {
      // An input's value counts if its length > 0,
      // or if the input's validity state says it has bad input (eg string in a number input)
      containerCtrl.setHasValue(element.val().length > 0 || (element[0].validity || {}).badInput);
    }

    function setupTextarea() {
      var isAutogrowing = !attr.hasOwnProperty('mdNoAutogrow');

      attachResizeHandle();

      if (!isAutogrowing) return;

      // Can't check if height was or not explicity set,
      // so rows attribute will take precedence if present
      var minRows = attr.hasOwnProperty('rows') ? parseInt(attr.rows) : NaN;
      var maxRows = attr.hasOwnProperty('maxRows') ? parseInt(attr.maxRows) : NaN;
      var scopeResizeListener = scope.$on('md-resize-textarea', growTextarea);
      var lineHeight = null;
      var node = element[0];

      // This timeout is necessary, because the browser needs a little bit
      // of time to calculate the `clientHeight` and `scrollHeight`.
      $timeout(function() {
        $mdUtil.nextTick(growTextarea);
      }, 10, false);

      // We could leverage ngModel's $parsers here, however it
      // isn't reliable, because AngularJS trims the input by default,
      // which means that growTextarea won't fire when newlines and
      // spaces are added.
      element.on('input', growTextarea);

      // We should still use the $formatters, because they fire when
      // the value was changed from outside the textarea.
      if (hasNgModel) {
        ngModelCtrl.$formatters.push(formattersListener);
      }

      if (!minRows) {
        element.attr('rows', 1);
      }

      angular.element($window).on('resize', growTextarea);
      scope.$on('$destroy', disableAutogrow);

      function growTextarea() {
        // temporarily disables element's flex so its height 'runs free'
        element
          .attr('rows', 1)
          .css('height', 'auto')
          .addClass('md-no-flex');

        var height = getHeight();

        if (!lineHeight) {
          // offsetHeight includes padding which can throw off our value
          var originalPadding = element[0].style.padding || '';
          lineHeight = element.css('padding', 0).prop('offsetHeight');
          element[0].style.padding = originalPadding;
        }

        if (minRows && lineHeight) {
          height = Math.max(height, lineHeight * minRows);
        }

        if (maxRows && lineHeight) {
          var maxHeight = lineHeight * maxRows;

          if (maxHeight < height) {
            element.attr('md-no-autogrow', '');
            height = maxHeight;
          } else {
            element.removeAttr('md-no-autogrow');
          }
        }

        if (lineHeight) {
          element.attr('rows', Math.round(height / lineHeight));
        }

        element
          .css('height', height + 'px')
          .removeClass('md-no-flex');
      }

      function getHeight() {
        var offsetHeight = node.offsetHeight;
        var line = node.scrollHeight - offsetHeight;
        return offsetHeight + Math.max(line, 0);
      }

      function formattersListener(value) {
        $mdUtil.nextTick(growTextarea);
        return value;
      }

      function disableAutogrow() {
        if (!isAutogrowing) return;

        isAutogrowing = false;
        angular.element($window).off('resize', growTextarea);
        scopeResizeListener && scopeResizeListener();
        element
          .attr('md-no-autogrow', '')
          .off('input', growTextarea);

        if (hasNgModel) {
          var listenerIndex = ngModelCtrl.$formatters.indexOf(formattersListener);

          if (listenerIndex > -1) {
            ngModelCtrl.$formatters.splice(listenerIndex, 1);
          }
        }
      }

      function attachResizeHandle() {
        if (attr.hasOwnProperty('mdNoResize')) return;

        var handle = angular.element('<div class="md-resize-handle"></div>');
        var isDragging = false;
        var dragStart = null;
        var startHeight = 0;
        var container = containerCtrl.element;
        var dragGestureHandler = $mdGesture.register(handle, 'drag', { horizontal: false });


        element.wrap('<div class="md-resize-wrapper">').after(handle);
        handle.on('mousedown', onMouseDown);

        container
          .on('$md.dragstart', onDragStart)
          .on('$md.drag', onDrag)
          .on('$md.dragend', onDragEnd);

        scope.$on('$destroy', function() {
          handle
            .off('mousedown', onMouseDown)
            .remove();

          container
            .off('$md.dragstart', onDragStart)
            .off('$md.drag', onDrag)
            .off('$md.dragend', onDragEnd);

          dragGestureHandler();
          handle = null;
          container = null;
          dragGestureHandler = null;
        });

        function onMouseDown(ev) {
          ev.preventDefault();
          isDragging = true;
          dragStart = ev.clientY;
          startHeight = parseFloat(element.css('height')) || element.prop('offsetHeight');
        }

        function onDragStart(ev) {
          if (!isDragging) return;
          ev.preventDefault();
          disableAutogrow();
          container.addClass('md-input-resized');
        }

        function onDrag(ev) {
          if (!isDragging) return;

          element.css('height', (startHeight + ev.pointer.distanceY) + 'px');
        }

        function onDragEnd(ev) {
          if (!isDragging) return;
          isDragging = false;
          container.removeClass('md-input-resized');
        }
      }

      // Attach a watcher to detect when the textarea gets shown.
      if (attr.hasOwnProperty('mdDetectHidden')) {

        var handleHiddenChange = function() {
          var wasHidden = false;

          return function() {
            var isHidden = node.offsetHeight === 0;

            if (isHidden === false && wasHidden === true) {
              growTextarea();
            }

            wasHidden = isHidden;
          };
        }();

        // Check every digest cycle whether the visibility of the textarea has changed.
        // Queue up to run after the digest cycle is complete.
        scope.$watch(function() {
          $mdUtil.nextTick(handleHiddenChange, false);
          return true;
        });
      }
    }
  }
}

function mdMaxlengthDirective($animate, $mdUtil) {
  return {
    restrict: 'A',
    require: ['ngModel', '^mdInputContainer'],
    link: postLink
  };

  function postLink(scope, element, attr, ctrls) {
    var maxlength = parseInt(attr.mdMaxlength);
    if (isNaN(maxlength)) maxlength = -1;
    var ngModelCtrl = ctrls[0];
    var containerCtrl = ctrls[1];
    var charCountEl, errorsSpacer;
    var ngTrim = angular.isDefined(attr.ngTrim) ? $mdUtil.parseAttributeBoolean(attr.ngTrim) : true;
    var isPasswordInput = attr.type === 'password';

    scope.$watch(attr.mdMaxlength, function(value) {
      maxlength = value;
    });

    ngModelCtrl.$validators['md-maxlength'] = function(modelValue, viewValue) {
      if (!angular.isNumber(maxlength) || maxlength < 0) {
        return true;
      }

      // We always update the char count, when the modelValue has changed.
      // Using the $validators for triggering the update works very well.
      renderCharCount();

      var elementVal = element.val() || viewValue;
      if (elementVal === undefined || elementVal === null) {
        elementVal = '';
      }
      elementVal = ngTrim && !isPasswordInput && angular.isString(elementVal) ? elementVal.trim() : elementVal;
      // Force the value into a string since it may be a number,
      // which does not have a length property.
      return String(elementVal).length <= maxlength;
    };

    /**
     * Override the default NgModelController $isEmpty check to take ng-trim, password inputs,
     * etc. into account.
     * @param value {*} the input's value
     * @returns {boolean} true if the input's value should be considered empty, false otherwise
     */
    ngModelCtrl.$isEmpty = function(value) {
      return calculateInputValueLength(value) === 0;
    };

    // Wait until the next tick to ensure that the input has setup the errors spacer where we will
    // append our counter
    $mdUtil.nextTick(function() {
      errorsSpacer = angular.element(containerCtrl.element[0].querySelector('.md-errors-spacer'));
      charCountEl = angular.element('<div class="md-char-counter">');

      // Append our character counter inside the errors spacer
      errorsSpacer.append(charCountEl);

      attr.$observe('ngTrim', function (value) {
        ngTrim = angular.isDefined(value) ? $mdUtil.parseAttributeBoolean(value) : true;
      });

      scope.$watch(attr.mdMaxlength, function(value) {
        if (angular.isNumber(value) && value > 0) {
          if (!charCountEl.parent().length) {
            $animate.enter(charCountEl, errorsSpacer);
          }
          renderCharCount();
        } else {
          $animate.leave(charCountEl);
        }
      });
    });

    /**
     * Calculate the input value's length after coercing it to a string
     * and trimming it if appropriate.
     * @param value {*} the input's value
     * @returns {number} calculated length of the input's value
     */
    function calculateInputValueLength(value) {
      value = ngTrim && !isPasswordInput && angular.isString(value) ? value.trim() : value;
      if (value === undefined || value === null) {
        value = '';
      }
      return String(value).length;
    }

    function renderCharCount() {
      // If we have not been initialized or appended to the body yet; do not render.
      if (!charCountEl || !charCountEl.parent()) {
        return;
      }
      // Force the value into a string since it may be a number,
      // which does not have a length property.
      charCountEl.text(calculateInputValueLength(element.val()) + ' / ' + maxlength);
    }
  }
}

function placeholderDirective($compile) {
  return {
    restrict: 'A',
    require: '^^?mdInputContainer',
    priority: 200,
    link: {
      // Note that we need to do this in the pre-link, as opposed to the post link, if we want to
      // support data bindings in the placeholder. This is necessary, because we have a case where
      // we transfer the placeholder value to the `<label>` and we remove it from the original `<input>`.
      // If we did this in the post-link, AngularJS would have set up the observers already and would be
      // re-adding the attribute, even though we removed it from the element.
      pre: preLink
    }
  };

  function preLink(scope, element, attr, inputContainer) {
    // If there is no input container, just return
    if (!inputContainer) return;

    var label = inputContainer.element.find('label');
    var noFloat = inputContainer.element.attr('md-no-float');

    // If we have a label, or they specify the md-no-float attribute, just return
    if ((label && label.length) || noFloat === '' || scope.$eval(noFloat)) {
      // Add a placeholder class so we can target it in the CSS
      inputContainer.setHasPlaceholder(true);
      return;
    }

    // md-select handles placeholders on it's own
    if (element[0].nodeName !== 'MD-SELECT') {
      // Move the placeholder expression to the label
      var newLabel = angular.element(
        '<label ng-click="delegateClick()" tabindex="-1" aria-hidden="true">' + attr.placeholder +
        '</label>');

      // Note that we unset it via `attr`, in order to get AngularJS
      // to remove any observers that it might have set up. Otherwise
      // the attribute will be added on the next digest.
      attr.$set('placeholder', null);

      // We need to compile the label manually in case it has any bindings.
      // A gotcha here is that we first add the element to the DOM and we compile
      // it later. This is necessary, because if we compile the element beforehand,
      // it won't be able to find the `mdInputContainer` controller.
      inputContainer.element
        .addClass('md-icon-float')
        .prepend(newLabel);

      $compile(newLabel)(scope);
    }
  }
}

/**
 * @ngdoc directive
 * @name mdSelectOnFocus
 * @module material.components.input
 *
 * @restrict A
 *
 * @description
 * The `md-select-on-focus` directive allows you to automatically select the element's input text on focus.
 *
 * <h3>Notes</h3>
 * - The use of `md-select-on-focus` is restricted to `<input>` and `<textarea>` elements.
 *
 * @usage
 * <h3>Using with an Input</h3>
 * <hljs lang="html">
 *
 * <md-input-container>
 *   <label>Auto Select</label>
 *   <input type="text" md-select-on-focus>
 * </md-input-container>
 * </hljs>
 *
 * <h3>Using with a Textarea</h3>
 * <hljs lang="html">
 *
 * <md-input-container>
 *   <label>Auto Select</label>
 *   <textarea md-select-on-focus>This text will be selected on focus.</textarea>
 * </md-input-container>
 *
 * </hljs>
 */
function mdSelectOnFocusDirective($document, $timeout) {

  return {
    restrict: 'A',
    link: postLink
  };

  function postLink(scope, element, attr) {
    if (element[0].nodeName !== 'INPUT' && element[0].nodeName !== "TEXTAREA") return;

    var preventMouseUp = false;

    element
      .on('focus', onFocus)
      .on('mouseup', onMouseUp);

    scope.$on('$destroy', function() {
      element
        .off('focus', onFocus)
        .off('mouseup', onMouseUp);
    });

    function onFocus() {
      preventMouseUp = true;

      $timeout(function() {

        // Use HTMLInputElement#select to fix firefox select issues.
        // The debounce is here for Edge's sake, otherwise the selection doesn't work.
        // Since focus may already have been lost on the input (and because `select()`
        // will re-focus), make sure the element is still active before applying.
        if ($document[0].activeElement === element[0]) {
          element[0].select();
        }

        // This should be reset from inside the `focus`, because the event might
        // have originated from something different than a click, e.g. a keyboard event.
        preventMouseUp = false;
      }, 1, false);
    }

    // Prevents the default action of the first `mouseup` after a focus.
    // This is necessary, because browsers fire a `mouseup` right after the element
    // has been focused. In some browsers (Firefox in particular) this can clear the
    // selection. There are examples of the problem in issue #7487.
    function onMouseUp(event) {
      if (preventMouseUp) {
        event.preventDefault();
      }
    }
  }
}

var visibilityDirectives = ['ngIf', 'ngShow', 'ngHide', 'ngSwitchWhen', 'ngSwitchDefault'];
function ngMessagesDirective() {
  return {
    restrict: 'EA',
    link: postLink,

    // This is optional because we don't want target *all* ngMessage instances, just those inside of
    // mdInputContainer.
    require: '^^?mdInputContainer'
  };

  function postLink(scope, element, attrs, inputContainer) {
    // If we are not a child of an input container, don't do anything
    if (!inputContainer) return;

    // Add our animation class
    element.toggleClass('md-input-messages-animation', true);

    // Add our md-auto-hide class to automatically hide/show messages when container is invalid
    element.toggleClass('md-auto-hide', true);

    // If we see some known visibility directives, remove the md-auto-hide class
    if (attrs.mdAutoHide == 'false' || hasVisibiltyDirective(attrs)) {
      element.toggleClass('md-auto-hide', false);
    }
  }

  function hasVisibiltyDirective(attrs) {
    return visibilityDirectives.some(function(attr) {
      return attrs[attr];
    });
  }
}

function ngMessageDirective($mdUtil) {
  return {
    restrict: 'EA',
    compile: compile,
    priority: 100
  };

  function compile(tElement) {
    if (!isInsideInputContainer(tElement)) {

      // When the current element is inside of a document fragment, then we need to check for an input-container
      // in the postLink, because the element will be later added to the DOM and is currently just in a temporary
      // fragment, which causes the input-container check to fail.
      if (isInsideFragment()) {
        return function (scope, element) {
          if (isInsideInputContainer(element)) {
            // Inside of the postLink function, a ngMessage directive will be a comment element, because it's
            // currently hidden. To access the shown element, we need to use the element from the compile function.
            initMessageElement(tElement);
          }
        };
      }
    } else {
      initMessageElement(tElement);
    }

    function isInsideFragment() {
      var nextNode = tElement[0];
      while (nextNode = nextNode.parentNode) {
        if (nextNode.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
          return true;
        }
      }
      return false;
    }

    function isInsideInputContainer(element) {
      return !!$mdUtil.getClosest(element, "md-input-container");
    }

    function initMessageElement(element) {
      // Add our animation class
      element.toggleClass('md-input-message-animation', true);
    }
  }
}

var $$AnimateRunner, $animateCss, $mdUtil;

function mdInputInvalidMessagesAnimation($$AnimateRunner, $animateCss, $mdUtil) {
  saveSharedServices($$AnimateRunner, $animateCss, $mdUtil);

  return {
    addClass: function(element, className, done) {
      showInputMessages(element, done);
    }

    // NOTE: We do not need the removeClass method, because the message ng-leave animation will fire
  };
}

function ngMessagesAnimation($$AnimateRunner, $animateCss, $mdUtil) {
  saveSharedServices($$AnimateRunner, $animateCss, $mdUtil);

  return {
    enter: function(element, done) {
      showInputMessages(element, done);
    },

    leave: function(element, done) {
      hideInputMessages(element, done);
    },

    addClass: function(element, className, done) {
      if (className == "ng-hide") {
        hideInputMessages(element, done);
      } else {
        done();
      }
    },

    removeClass: function(element, className, done) {
      if (className == "ng-hide") {
        showInputMessages(element, done);
      } else {
        done();
      }
    }
  };
}

function ngMessageAnimation($$AnimateRunner, $animateCss, $mdUtil, $log) {
  saveSharedServices($$AnimateRunner, $animateCss, $mdUtil, $log);

  return {
    enter: function(element, done) {
      var animator = showMessage(element);

      animator.start().done(done);
    },

    leave: function(element, done) {
      var animator = hideMessage(element);

      animator.start().done(done);
    }
  };
}

function showInputMessages(element, done) {
  var animators = [], animator;
  var messages = getMessagesElement(element);
  var children = messages.children();

  if (messages.length == 0 || children.length == 0) {
    done();
    return;
  }

  angular.forEach(children, function(child) {
    animator = showMessage(angular.element(child));

    animators.push(animator.start());
  });

  $$AnimateRunner.all(animators, done);
}

function hideInputMessages(element, done) {
  var animators = [], animator;
  var messages = getMessagesElement(element);
  var children = messages.children();

  if (messages.length == 0 || children.length == 0) {
    done();
    return;
  }

  angular.forEach(children, function(child) {
    animator = hideMessage(angular.element(child));

    animators.push(animator.start());
  });

  $$AnimateRunner.all(animators, done);
}

function showMessage(element) {
  var height = parseInt(window.getComputedStyle(element[0]).height);
  var topMargin = parseInt(window.getComputedStyle(element[0]).marginTop);

  var messages = getMessagesElement(element);
  var container = getInputElement(element);

  // Check to see if the message is already visible so we can skip
  var alreadyVisible = (topMargin > -height);

  // If we have the md-auto-hide class, the md-input-invalid animation will fire, so we can skip
  if (alreadyVisible || (messages.hasClass('md-auto-hide') && !container.hasClass('md-input-invalid'))) {
    return $animateCss(element, {});
  }

  return $animateCss(element, {
    event: 'enter',
    structural: true,
    from: {"opacity": 0, "margin-top": -height + "px"},
    to: {"opacity": 1, "margin-top": "0"},
    duration: 0.3
  });
}

function hideMessage(element) {
  var height = element[0].offsetHeight;
  var styles = window.getComputedStyle(element[0]);

  // If we are already hidden, just return an empty animation
  if (parseInt(styles.opacity) === 0) {
    return $animateCss(element, {});
  }

  // Otherwise, animate
  return $animateCss(element, {
    event: 'leave',
    structural: true,
    from: {"opacity": 1, "margin-top": 0},
    to: {"opacity": 0, "margin-top": -height + "px"},
    duration: 0.3
  });
}

function getInputElement(element) {
  var inputContainer = element.controller('mdInputContainer');

  return inputContainer.element;
}

function getMessagesElement(element) {
  // If we ARE the messages element, just return ourself
  if (element.hasClass('md-input-messages-animation')) {
    return element;
  }

  // If we are a ng-message element, we need to traverse up the DOM tree
  if (element.hasClass('md-input-message-animation')) {
    return angular.element($mdUtil.getClosest(element, function(node) {
      return node.classList.contains('md-input-messages-animation');
    }));
  }

  // Otherwise, we can traverse down
  return angular.element(element[0].querySelector('.md-input-messages-animation'));
}

function saveSharedServices(_$$AnimateRunner_, _$animateCss_, _$mdUtil_) {
  $$AnimateRunner = _$$AnimateRunner_;
  $animateCss = _$animateCss_;
  $mdUtil = _$mdUtil_;
}

})(window, window.angular);