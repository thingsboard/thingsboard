(function() {

  'use strict';

  function CanvasFactory($rootScope) {

    return function innerCanvasFactory() {

      var canvasService = {
      };

      canvasService.setCanvasHtmlElement = function(element) {
        canvasService.canvasHtmlElement = element;
      };

      canvasService.getCanvasHtmlElement = function() {
        return canvasService.canvasHtmlElement;
      };

      canvasService.dragover = function(scope, callback) {
        var handler = $rootScope.$on('notifying-dragover-event', callback);
        scope.$on('$destroy', handler);
      };

      canvasService._notifyDragover = function(event) {
        $rootScope.$emit('notifying-dragover-event', event);
      };

      canvasService.drop = function(scope, callback) {
        var handler = $rootScope.$on('notifying-drop-event', callback);
        scope.$on('$destroy', handler);
      };

      canvasService._notifyDrop = function(event) {
        $rootScope.$emit('notifying-drop-event', event);
      };

      return canvasService;
    };


  }

  angular.module('flowchart')
      .service('FlowchartCanvasFactory', CanvasFactory);

}());
