(function() {

  'use strict';

  function fcConnector(flowchartConstants) {
    return {
      restrict: 'A',
      link: function(scope, element) {
        if (scope.modelservice.isEditable()) {
          element.attr('draggable', 'true');

          element.on('dragover', scope.fcCallbacks.edgeDragoverConnector);
          element.on('drop', scope.fcCallbacks.edgeDrop(scope.connector));
          element.on('dragend', scope.fcCallbacks.edgeDragend);
          element.on('dragstart', scope.fcCallbacks.edgeDragstart(scope.connector));
          element.on('mouseenter', scope.fcCallbacks.connectorMouseEnter(scope.connector));
          element.on('mouseleave', scope.fcCallbacks.connectorMouseLeave(scope.connector));
          scope.$watch('mouseOverConnector', function(value) {
            if (value === scope.connector) {
              element.addClass(flowchartConstants.hoverClass);
            } else {
              element.removeClass(flowchartConstants.hoverClass);
            }
          });
        }
        element.addClass(flowchartConstants.connectorClass);

        scope.modelservice.connectors.setHtmlElement(scope.connector.id, element[0]);
      }
    };
  }

  angular
    .module('flowchart')
    .directive('fcConnector', fcConnector);

}());
