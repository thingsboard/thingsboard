(function() {

  'use strict';

  function Rectangleselectfactory() {

    return function(modelservice, applyFunction) {

      var rectangleSelectService = {
      };

      var selectRect = {
        x1: 0,
        x2: 0,
        y1: 0,
        y2: 0
      };

      function updateSelectRect() {
        var x3 = Math.min(selectRect.x1,selectRect.x2);
        var x4 = Math.max(selectRect.x1,selectRect.x2);
        var y3 = Math.min(selectRect.y1,selectRect.y2);
        var y4 = Math.max(selectRect.y1,selectRect.y2);
        rectangleSelectService.selectElement.style.left = x3 + 'px';
        rectangleSelectService.selectElement.style.top = y3 + 'px';
        rectangleSelectService.selectElement.style.width = x4 - x3 + 'px';
        rectangleSelectService.selectElement.style.height = y4 - y3 + 'px';
      }

      function selectObjects(rectBox) {
        applyFunction(function() {
          modelservice.selectAllInRect(rectBox);
        });
      }

      rectangleSelectService.setRectangleSelectHtmlElement = function(element) {
        rectangleSelectService.selectElement = element;
      };

      rectangleSelectService.mousedown = function(e) {
        if (modelservice.isEditable() && !e.ctrlKey && !e.metaKey && e.button === 0) {
          rectangleSelectService.selectElement.hidden = 0;
          var offset = angular.element(modelservice.getCanvasHtmlElement()).offset();
          selectRect.x1 = Math.round(e.clientX - offset.left);
          selectRect.y1 = Math.round(e.clientY - offset.top);
          updateSelectRect();
        }
      };
      rectangleSelectService.mousemove = function(e) {
        if (modelservice.isEditable() && !e.ctrlKey && !e.metaKey && e.button === 0) {
          var offset = angular.element(modelservice.getCanvasHtmlElement()).offset();
          selectRect.x2 = Math.round(e.clientX - offset.left);
          selectRect.y2 = Math.round(e.clientY - offset.top);
          updateSelectRect();
        }
      };
      rectangleSelectService.mouseup = function (e) {
        if (modelservice.isEditable() && !e.ctrlKey && !e.metaKey && e.button === 0) {
          var rectBox = rectangleSelectService.selectElement.getBoundingClientRect();
          rectBox.parentOffset = angular.element(modelservice.getCanvasHtmlElement()).offset();
          rectangleSelectService.selectElement.hidden = 1;
          selectObjects(rectBox);
        }
      };

      return rectangleSelectService;
    }

  }

  angular
    .module('flowchart')
    .factory('Rectangleselectfactory', Rectangleselectfactory);

}());
