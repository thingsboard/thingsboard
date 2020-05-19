

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
