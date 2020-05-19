(function() {

  'use strict';

  function fcNode(flowchartConstants, NodeTemplatePath) {
    return {
      restrict: 'E',
      templateUrl: function() {
        return NodeTemplatePath;
      },
      replace: true,
      scope: {
        fcCallbacks: '=callbacks',
        callbacks: '=userNodeCallbacks',
        node: '=',
        selected: '=',
        edit: '=',
        underMouse: '=',
        mouseOverConnector: '=',
        modelservice: '=',
        draggedNode: '='
      },
      link: function(scope, element) {
        scope.flowchartConstants = flowchartConstants;
        element.on('mousedown', function(e){e.stopPropagation();});
        if (!scope.node.readonly) {
          element.attr('draggable', 'true');
          element.on('dragstart', scope.fcCallbacks.nodeDragstart(scope.node));
          element.on('dragend', scope.fcCallbacks.nodeDragend);
          element.on('click', scope.fcCallbacks.nodeClicked(scope.node));
          element.on('mouseover', scope.fcCallbacks.nodeMouseOver(scope.node));
          element.on('mouseout', scope.fcCallbacks.nodeMouseOut(scope.node));
        }

        element.addClass(flowchartConstants.nodeClass);

        function myToggleClass(clazz, set) {
          if (set) {
            element.addClass(clazz);
          } else {
            element.removeClass(clazz);
          }
        }

        scope.$watch('selected', function(value) {
          myToggleClass(flowchartConstants.selectedClass, value);
        });
        scope.$watch('edit', function(value) {
          myToggleClass(flowchartConstants.editClass, value);
        });
        scope.$watch('underMouse', function(value) {
          myToggleClass(flowchartConstants.hoverClass, value);
        });
        scope.$watch('draggedNode', function(value) {
          myToggleClass(flowchartConstants.draggingClass, value===scope.node);
        });

        scope.modelservice.nodes.setHtmlElement(scope.node.id, element[0]);
      }
    };
  }

  angular.module('flowchart').directive('fcNode', fcNode);
}());
