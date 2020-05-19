/**
 * vAccordion - AngularJS multi-level accordion component
 * @version v1.6.0
 * @link http://lukaszwatroba.github.io/v-accordion
 * @author Łukasz Wątroba <l@lukaszwatroba.com>
 * @license MIT License, http://www.opensource.org/licenses/MIT
 */

(function (angular) {
'use strict';

// Config
angular.module('vAccordion.config', [])
  .constant('accordionConfig', {
    states: {
      expanded: 'is-expanded'
    },
    expandAnimationDuration: 0.5
  })
  .animation('.is-expanded', [ '$animateCss', 'accordionConfig', function ($animateCss, accordionConfig) {
    return {
      addClass: function (element, className, done) {
        var paneContent = angular.element(element[0].querySelector('v-pane-content')),
            paneInner = angular.element(paneContent[0].querySelector('div'));

        var height = paneInner[0].offsetHeight;

        var expandAnimation = $animateCss(paneContent, {
          easing: 'ease',
          from: { maxHeight: '0px' },
          to: { maxHeight: height + 'px' },
          duration: accordionConfig.expandAnimationDuration
        });

        expandAnimation.start().done(function () {
          paneContent.css('max-height', 'none');
          done();
        });

        return function (isCancelled) {
          if (isCancelled) {
            paneContent.css('max-height', 'none');
          }
        };
      },
      removeClass: function (element, className, done) {
        var paneContent = angular.element(element[0].querySelector('v-pane-content')),
            paneInner = angular.element(paneContent[0].querySelector('div'));

        var height = paneInner[0].offsetHeight;

        var collapseAnimation = $animateCss(paneContent, {
          easing: 'ease',
          from: { maxHeight: height + 'px' },
          to: { maxHeight: '0px' },
          duration: accordionConfig.expandAnimationDuration
        });

        collapseAnimation.start().done(done);

        return function (isCancelled) {
          if (isCancelled) {
            paneContent.css('max-height', '0px');
          }
        };
      }
    };
  } ]);


// Modules
angular.module('vAccordion.directives', []);
angular.module('vAccordion',
  [
    'vAccordion.config',
    'vAccordion.directives'
  ]);



// vAccordion directive
angular.module('vAccordion.directives')
  .directive('vAccordion', vAccordionDirective);


function vAccordionDirective ($timeout) {
  return {
    restrict: 'E',
    transclude: true,
    controller: vAccordionController,
    scope: {
      control: '=?',
      expandCb: '&?onexpand',
      collapseCb: '&?oncollapse',
      id: '@?'
    },
    link: {
      pre: function (scope, iElement, iAttrs) {
        scope.allowMultiple = (angular.isDefined(iAttrs.multiple) && (iAttrs.multiple === '' || iAttrs.multiple === 'true'));
      },
      post: function (scope, iElement, iAttrs, ctrl, transclude) {
        transclude(scope.$parent.$new(), function (clone, transclusionScope) {
          transclusionScope.$accordion = scope.internalControl;
          if (scope.id) { transclusionScope.$accordion.id = scope.id; }
          iElement.append(clone);
        });

        iAttrs.$set('role', 'tablist');

        if (scope.allowMultiple) {
          iAttrs.$set('aria-multiselectable', 'true');
        }

        if (angular.isDefined(scope.control)) {
          checkCustomControlAPIMethods();

          var mergedControl = angular.extend({}, scope.internalControl, scope.control);
          scope.control = scope.internalControl = mergedControl;
        }
        else {
          scope.control = scope.internalControl;
        }

        function checkCustomControlAPIMethods () {
          var protectedApiMethods = ['toggle', 'expand', 'collapse', 'expandAll', 'collapseAll', 'hasExpandedPane'];

          angular.forEach(protectedApiMethods, function (iteratedMethodName) {
            if (scope.control[iteratedMethodName]) {
              throw new Error('The `' + iteratedMethodName + '` method can not be overwritten');
            }
          });
        }

        $timeout(function () {
          var eventName = (angular.isDefined(ctrl.getAccordionId())) ? ctrl.getAccordionId() + ':onReady' : 'vAccordion:onReady';
          scope.$emit(eventName);
        }, 0);
      }
    }
  };
}
vAccordionDirective.$inject = ['$timeout'];


// vAccordion directive controller
function vAccordionController ($scope) {
  var ctrl = this;
  var isDisabled = false;

  $scope.panes = [];

	$scope.expandCb = (angular.isFunction($scope.expandCb)) ? $scope.expandCb : angular.noop;
	$scope.collapseCb = (angular.isFunction($scope.collapseCb)) ? $scope.collapseCb : angular.noop;

  ctrl.hasExpandedPane = function hasExpandedPane () {
    var bool = false;

    for (var i = 0, length = $scope.panes.length; i < length; i++) {
      var iteratedPane = $scope.panes[i];

      if (iteratedPane.isExpanded) {
        bool = true;
        break;
      }
    }

    return bool;
  };

  ctrl.getPaneByIndex = function getPaneByIndex (index) {
    var thePane;

    angular.forEach($scope.panes, function (iteratedPane) {
      if (iteratedPane.$parent && angular.isDefined(iteratedPane.$parent.$index) && iteratedPane.$parent.$index === index) {
        thePane = iteratedPane;
      }
    });

    return (thePane) ? thePane : $scope.panes[index];
  };

  ctrl.getPaneIndex = function getPaneIndex (pane) {
    var theIndex;

    angular.forEach($scope.panes, function (iteratedPane) {
      if (iteratedPane.$parent && angular.isDefined(iteratedPane.$parent.$index) && iteratedPane === pane) {
        theIndex = iteratedPane.$parent.$index;
      }
    });

    return (angular.isDefined(theIndex)) ? theIndex : $scope.panes.indexOf(pane);
  };

  ctrl.getPaneById = function getPaneById (id) {
    var thePane;

    angular.forEach($scope.panes, function (iteratedPane) {
      if (iteratedPane.id && iteratedPane.id === id) {
        thePane = iteratedPane;
      }
    });

    return thePane;
  };

  ctrl.getPaneId = function getPaneId (pane) {
    return pane.id;
  };

  ctrl.getAccordionId = function getAccordionId () {
    return $scope.id;
  };


  ctrl.disable = function disable () {
    isDisabled = true;
  };

  ctrl.enable = function enable () {
    isDisabled = false;
  };

  ctrl.addPane = function addPane (paneToAdd) {
    if (!$scope.allowMultiple) {
      if (ctrl.hasExpandedPane() && paneToAdd.isExpanded) {
        throw new Error('The `multiple` attribute can\'t be found');
      }
    }

    $scope.panes.push(paneToAdd);

    if (paneToAdd.isExpanded) {
      $scope.expandCb({ index: ctrl.getPaneIndex(paneToAdd), id: paneToAdd.id, pane: paneToAdd });
    }
  };

  ctrl.focusNext = function focusNext () {
    var length = $scope.panes.length;

    for (var i = 0; i < length; i++) {
      var iteratedPane = $scope.panes[i];

      if (iteratedPane.isFocused) {
        var paneToFocusIndex = i + 1;

        if (paneToFocusIndex > $scope.panes.length - 1) {
          paneToFocusIndex = 0;
        }

        var paneToFocus = $scope.panes[paneToFocusIndex];
            paneToFocus.paneElement.find('v-pane-header')[0].focus();

        break;
      }
    }
  };

  ctrl.focusPrevious = function focusPrevious () {
    var length = $scope.panes.length;

    for (var i = 0; i < length; i++) {
      var iteratedPane = $scope.panes[i];

      if (iteratedPane.isFocused) {
        var paneToFocusIndex = i - 1;

        if (paneToFocusIndex < 0) {
          paneToFocusIndex = $scope.panes.length - 1;
        }

        var paneToFocus = $scope.panes[paneToFocusIndex];
            paneToFocus.paneElement.find('v-pane-header')[0].focus();

        break;
      }
    }
  };

  ctrl.toggle = function toggle (paneToToggle) {
    if (isDisabled || !paneToToggle) { return; }

    if (!$scope.allowMultiple) {
      ctrl.collapseAll(paneToToggle);
    }

    paneToToggle.isExpanded = !paneToToggle.isExpanded;

    if (paneToToggle.isExpanded) {
      $scope.expandCb({ index: ctrl.getPaneIndex(paneToToggle), id: paneToToggle.id, pane: paneToToggle });
    } else {
      $scope.collapseCb({ index: ctrl.getPaneIndex(paneToToggle), id: paneToToggle.id, pane: paneToToggle });
    }
  };

  ctrl.expand = function expand (paneToExpand) {
    if (isDisabled || !paneToExpand) { return; }

    if (!$scope.allowMultiple) {
      ctrl.collapseAll(paneToExpand);
    }

    if (!paneToExpand.isExpanded) {
      paneToExpand.isExpanded = true;
      $scope.expandCb({ index: ctrl.getPaneIndex(paneToExpand), id: paneToExpand.id, pane: paneToExpand });
    }
  };

  ctrl.collapse = function collapse (paneToCollapse) {
    if (isDisabled || !paneToCollapse) { return; }

    if (paneToCollapse.isExpanded) {
      paneToCollapse.isExpanded = false;
      $scope.collapseCb({ index: ctrl.getPaneIndex(paneToCollapse), id: paneToCollapse.id, pane: paneToCollapse });
    }
  };

  ctrl.expandAll = function expandAll () {
    if (isDisabled) { return; }

    if ($scope.allowMultiple) {
      angular.forEach($scope.panes, function (iteratedPane) {
        ctrl.expand(iteratedPane);
      });
    } else {
      throw new Error('The `multiple` attribute can\'t be found');
    }
  };

  ctrl.collapseAll = function collapseAll (exceptionalPane) {
    if (isDisabled) { return; }

    angular.forEach($scope.panes, function (iteratedPane) {
      if (iteratedPane !== exceptionalPane) {
        ctrl.collapse(iteratedPane);
      }
    });
  };

  // API
  $scope.internalControl = {
    toggle: function toggle (indexOrId) {
      if (angular.isString(indexOrId)) {
        ctrl.toggle( ctrl.getPaneById(indexOrId) );
      } else {
        ctrl.toggle( ctrl.getPaneByIndex(indexOrId) );
      }
    },
    expand: function expand (indexOrId) {
      if (angular.isString(indexOrId)) {
        ctrl.expand( ctrl.getPaneById(indexOrId) );
      } else {
        ctrl.expand( ctrl.getPaneByIndex(indexOrId) );
      }
    },
    collapse: function collapse (indexOrId) {
      if (angular.isString(indexOrId)) {
        ctrl.collapse( ctrl.getPaneById(indexOrId) );
      } else {
        ctrl.collapse( ctrl.getPaneByIndex(indexOrId) );
      }
    },
    expandAll: ctrl.expandAll,
    collapseAll: ctrl.collapseAll,
    hasExpandedPane: ctrl.hasExpandedPane
  };
}
vAccordionController.$inject = ['$scope'];



// vPane directive
angular.module('vAccordion.directives')
  .directive('vPane', vPaneDirective);


function vPaneDirective ($timeout, $animate, accordionConfig) {
  return {
    restrict: 'E',
    require: '^vAccordion',
    transclude: true,
    controller: vPaneController,
    scope: {
      isExpanded: '=?expanded',
      isDisabled: '=?ngDisabled',
      id: '@?'
    },
    link: function (scope, iElement, iAttrs, accordionCtrl, transclude) {
      transclude(scope.$parent.$new(), function (clone, transclusionScope) {
        transclusionScope.$pane = scope.internalControl;
        if (scope.id) { transclusionScope.$pane.id = scope.id; }
        iElement.append(clone);
      });

      if (!angular.isDefined(scope.isExpanded)) {
        scope.isExpanded = (angular.isDefined(iAttrs.expanded) && (iAttrs.expanded === ''));
      }

      if (angular.isDefined(iAttrs.disabled)) {
        scope.isDisabled = true;
      }

      var states = accordionConfig.states;

      var paneHeader = iElement.find('v-pane-header'),
          paneContent = iElement.find('v-pane-content'),
          paneInner = paneContent.find('div');

      var accordionId = accordionCtrl.getAccordionId();

      if (!paneHeader[0]) {
        throw new Error('The `v-pane-header` directive can\'t be found');
      }

      if (!paneContent[0]) {
        throw new Error('The `v-pane-content` directive can\'t be found');
      }

      scope.paneElement = iElement;
      scope.paneContentElement = paneContent;
      scope.paneInnerElement = paneInner;

      scope.accordionCtrl = accordionCtrl;

      accordionCtrl.addPane(scope);

      function emitEvent (eventName) {
        eventName = (angular.isDefined(accordionId)) ? accordionId + ':' + eventName : 'vAccordion:' + eventName;
        scope.$emit(eventName);
      }

      function expand () {
        accordionCtrl.disable();

        paneContent.attr('aria-hidden', 'false');

        paneHeader.attr({
          'aria-selected': 'true',
          'aria-expanded': 'true'
        });

        emitEvent('onExpand');

        $animate
          .addClass(iElement, states.expanded)
          .then(function () {
            accordionCtrl.enable();
            emitEvent('onExpandAnimationEnd');
          });
      }

      function collapse () {
        accordionCtrl.disable();

        paneContent.attr('aria-hidden', 'true');

        paneHeader.attr({
          'aria-selected': 'false',
          'aria-expanded': 'false'
        });

        emitEvent('onCollapse');

        $animate
          .removeClass(iElement, states.expanded)
          .then(function () {
            accordionCtrl.enable();
            emitEvent('onCollapseAnimationEnd');
          });
      }

      scope.$evalAsync(function () {
        if (scope.isExpanded) {
          iElement.addClass(states.expanded);
          paneContent
            .css('max-height', 'none')
            .attr('aria-hidden', 'false');

          paneHeader.attr({
            'aria-selected': 'true',
            'aria-expanded': 'true'
          });
        } else {
          paneContent
            .css('max-height', '0px')
            .attr('aria-hidden', 'true');

          paneHeader.attr({
            'aria-selected': 'false',
            'aria-expanded': 'false'
          });
        }
      });

      scope.$watch('isExpanded', function (newValue, oldValue) {
        if (newValue === oldValue) { return true; }
        if (newValue) { expand(); }
        else { collapse(); }
      });
    }
  };
}
vPaneDirective.$inject = ['$timeout', '$animate', 'accordionConfig'];


// vPane directive controller
function vPaneController ($scope) {
  var ctrl = this;

  ctrl.isExpanded = function isExpanded () {
    return $scope.isExpanded;
  };

  ctrl.toggle = function toggle () {
    if (!$scope.isAnimating && !$scope.isDisabled) {
      $scope.accordionCtrl.toggle($scope);
    }
  };

  ctrl.expand = function expand () {
    if (!$scope.isAnimating && !$scope.isDisabled) {
      $scope.accordionCtrl.expand($scope);
    }
  };

  ctrl.collapse = function collapse () {
    if (!$scope.isAnimating && !$scope.isDisabled) {
      $scope.accordionCtrl.collapse($scope);
    }
  };

  ctrl.focusPane = function focusPane () {
    $scope.isFocused = true;
  };

  ctrl.blurPane = function blurPane () {
    $scope.isFocused = false;
  };

  $scope.internalControl = {
    toggle: ctrl.toggle,
    expand: ctrl.expand,
    collapse: ctrl.collapse,
    isExpanded: ctrl.isExpanded
  };
}
vPaneController.$inject = ['$scope'];



// vPaneContent directive
angular.module('vAccordion.directives')
  .directive('vPaneContent', vPaneContentDirective);


function vPaneContentDirective () {
  return {
    restrict: 'E',
    require: '^vPane',
    transclude: true,
    template: '<div ng-transclude></div>',
    scope: {},
    link: function (scope, iElement, iAttrs) {
      iAttrs.$set('role', 'tabpanel');
      iAttrs.$set('aria-hidden', 'true');
    }
  };
}



// vPaneHeader directive
angular.module('vAccordion.directives')
  .directive('vPaneHeader', vPaneHeaderDirective);


function vPaneHeaderDirective () {
  return {
    restrict: 'E',
    require: ['^vPane', '^vAccordion'],
    transclude: true,
    template: '<div ng-transclude></div>',
    scope: {},
    link: function (scope, iElement, iAttrs, ctrls) {
      iAttrs.$set('role', 'tab');
      iAttrs.$set('tabindex', '0');

      var paneCtrl = ctrls[0],
          accordionCtrl = ctrls[1];

      var isInactive = angular.isDefined(iAttrs.inactive);

      function onClick () {
        if (isInactive) { return false; }
        scope.$apply(function () { paneCtrl.toggle(); });
      }

      function onKeydown (event) {
        if (event.keyCode === 32  || event.keyCode === 13) {
          scope.$apply(function () { paneCtrl.toggle(); });
          event.preventDefault();
        } else if (event.keyCode === 39 || event.keyCode === 40) {
          scope.$apply(function () { accordionCtrl.focusNext(); });
          event.preventDefault();
        } else if (event.keyCode === 37 || event.keyCode === 38) {
          scope.$apply(function () { accordionCtrl.focusPrevious(); });
          event.preventDefault();
        }
      }

      function onFocus () {
        paneCtrl.focusPane();
      }

      function onBlur () {
        paneCtrl.blurPane();
      }

      iElement[0].onfocus = onFocus;
      iElement[0].onblur = onBlur;
      iElement.bind('click', onClick);
      iElement.bind('keydown', onKeydown);

      scope.$on('$destroy', function () {
        iElement.unbind('click', onClick);
        iElement.unbind('keydown', onKeydown);
        iElement[0].onfocus = null;
        iElement[0].onblur = null;
      });
    }
  };
}

})(angular);