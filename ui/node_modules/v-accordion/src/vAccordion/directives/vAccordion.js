

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
