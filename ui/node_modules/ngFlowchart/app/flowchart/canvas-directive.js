(function() {

  'use strict';

  function fcCanvas(flowchartConstants) {
    return {
      restrict: 'E',
      templateUrl: "flowchart/canvas.html",
      replace: true,
      scope: {
        model: "=",
        selectedObjects: "=",
        edgeStyle: '@',
        userCallbacks: '=?callbacks',
        automaticResize: '=?',
        dragAnimation: '=?',
        nodeWidth: '=?',
        nodeHeight: '=?',
        dropTargetId: '=?',
        control: '=?'
      },
      controller: 'canvasController',
      link: function(scope, element) {
        function adjustCanvasSize(fit) {
          if (scope.model) {
            var maxX = 0;
            var maxY = 0;
            angular.forEach(scope.model.nodes, function (node, key) {
              maxX = Math.max(node.x + scope.nodeWidth, maxX);
              maxY = Math.max(node.y + scope.nodeHeight, maxY);
            });
            var width, height;
            if (fit) {
              width = maxX;
              height = maxY;
            } else {
              width = Math.max(maxX, element.prop('offsetWidth'));
              height = Math.max(maxY, element.prop('offsetHeight'));
            }
            element.css('width', width + 'px');
            element.css('height', height + 'px');
          }
        }
        if (!scope.dropTargetId && scope.edgeStyle !== flowchartConstants.curvedStyle && scope.edgeStyle !== flowchartConstants.lineStyle) {
          throw new Error('edgeStyle not supported.');
        }
        scope.nodeHeight = scope.nodeHeight || 200;
        scope.nodeWidth = scope.nodeWidth || 200;
        scope.dragAnimation = scope.dragAnimation || 'repaint';

        scope.flowchartConstants = flowchartConstants;
        element.addClass(flowchartConstants.canvasClass);
        element.on('dragover', scope.dragover);
        element.on('drop', scope.drop);
        element.on('mousedown', scope.mousedown);
        element.on('mousemove', scope.mousemove);
        element.on('mouseup', scope.mouseup);

        scope.$watch('model', adjustCanvasSize);

        scope.internalControl = scope.control || {};
        scope.internalControl.adjustCanvasSize = adjustCanvasSize;
        scope.internalControl.modelservice = scope.modelservice;

        scope.canvasservice.setCanvasHtmlElement(element[0]);
        scope.modelservice.setCanvasHtmlElement(element[0]);
        scope.modelservice.setSvgHtmlElement(element[0].querySelector('svg'));
        scope.rectangleselectservice.setRectangleSelectHtmlElement(element[0].querySelector('#select-rectangle'));
        if (scope.dropTargetId) {
          scope.modelservice.setDropTargetId(scope.dropTargetId);
        }
      }
    };
  }

  angular
    .module('flowchart')
    .directive('fcCanvas', fcCanvas);

}());

