(function() {

  'use strict';

  function fcMagnet(flowchartConstants) {
    return {
      restrict: 'AE',
      link: function(scope, element) {
        element.addClass(flowchartConstants.magnetClass);

        element.on('dragover', scope.fcCallbacks.edgeDragoverMagnet(scope.connector));
        element.on('dragleave', scope.fcCallbacks.edgeDragleaveMagnet);
        element.on('drop', scope.fcCallbacks.edgeDrop(scope.connector));
        element.on('dragend', scope.fcCallbacks.edgeDragend);
      }
    }
  }

  angular.module('flowchart')
    .directive('fcMagnet', fcMagnet);
}());
