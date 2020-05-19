(function() {

  'use strict';

  function canvasController($scope, Mouseoverfactory, Nodedraggingfactory, FlowchartCanvasFactory, Modelfactory, Edgedraggingfactory, Edgedrawingservice, Rectangleselectfactory) {

    $scope.dragAnimation = angular.isDefined($scope.dragAnimation) ? $scope.dragAnimation : 'repaint';

    $scope.userCallbacks = $scope.userCallbacks || {};
    $scope.automaticResize = $scope.automaticResize || false;
    angular.forEach($scope.userCallbacks, function(callback, key) {
      if (!angular.isFunction(callback) && key !== 'nodeCallbacks') {
        throw new Error('All callbacks should be functions.');
      }
    });

    $scope.arrowDefId = 'arrow-' + Math.random();

    $scope.canvasservice = FlowchartCanvasFactory();

    $scope.modelservice = Modelfactory($scope.model, $scope.selectedObjects, $scope.userCallbacks.dropNode, $scope.userCallbacks.createEdge, $scope.userCallbacks.edgeAdded || angular.noop, $scope.userCallbacks.nodeRemoved || angular.noop,  $scope.userCallbacks.edgeRemoved || angular.noop);

    $scope.nodeDragging = {};
    var nodedraggingservice = Nodedraggingfactory($scope.modelservice, $scope.nodeDragging, $scope.$apply.bind($scope), $scope.automaticResize, $scope.dragAnimation);

    $scope.edgeDragging = {};
    var edgedraggingservice = Edgedraggingfactory($scope.modelservice, $scope.model, $scope.edgeDragging, $scope.userCallbacks.isValidEdge || null, $scope.$apply.bind($scope), $scope.dragAnimation, $scope.edgeStyle);

    $scope.mouseOver = {};
    var mouseoverservice = Mouseoverfactory($scope.mouseOver, $scope.$apply.bind($scope));

    $scope.rectangleselectservice = Rectangleselectfactory($scope.modelservice, $scope.$apply.bind($scope));

    $scope.edgeMouseEnter = mouseoverservice.edgeMouseEnter;
    $scope.edgeMouseLeave = mouseoverservice.edgeMouseLeave;

    //$scope.canvasClick = function(e) {
    //  $scope.modelservice.deselectAll();
    //};

    $scope.drop = function(event) {
      if(event.preventDefault) {
        event.preventDefault();
      }
      if(event.stopPropagation) {
        event.stopPropagation();
      }

      nodedraggingservice.drop(event);
      $scope.canvasservice._notifyDrop(event);

      $scope.$evalAsync();
    };

    $scope.dragover = function(event) {
      nodedraggingservice.dragover(event);
      edgedraggingservice.dragover(event);
      $scope.canvasservice._notifyDragover(event);
    };

    $scope.mousedown = function(event) {
      $scope.rectangleselectservice.mousedown(event);
    };

    $scope.mousemove = function(event) {
      $scope.rectangleselectservice.mousemove(event);
    };

    $scope.mouseup = function(event) {
      $scope.rectangleselectservice.mouseup(event);
    };

    $scope.edgeMouseDown = function(event, edge) {
      event.stopPropagation();
    };

    $scope.edgeClick = function(event, edge) {
      $scope.modelservice.edges.handleEdgeMouseClick(edge, event.ctrlKey);
      // Don't let the chart handle the mouse down.
      event.stopPropagation();
      event.preventDefault();
    };

    $scope.edgeRemove = function(event, edge) {
      $scope.modelservice.edges.delete(edge);
      event.stopPropagation();
      event.preventDefault();
    };

    $scope.edgeDoubleClick = $scope.userCallbacks.edgeDoubleClick || angular.noop;
    $scope.edgeMouseOver = $scope.userCallbacks.edgeMouseOver || angular.noop;
    $scope.edgeEdit = $scope.userCallbacks.edgeEdit || angular.noop;

    $scope.userNodeCallbacks = $scope.userCallbacks.nodeCallbacks;
    $scope.callbacks = {
      nodeDragstart: nodedraggingservice.dragstart,
      nodeDragend: nodedraggingservice.dragend,
      edgeDragstart: edgedraggingservice.dragstart,
      edgeDragend: edgedraggingservice.dragend,
      edgeDrop: edgedraggingservice.drop,
      edgeDragoverConnector: edgedraggingservice.dragoverConnector,
      edgeDragoverMagnet: edgedraggingservice.dragoverMagnet,
      edgeDragleaveMagnet: edgedraggingservice.dragleaveMagnet,
      nodeMouseOver: mouseoverservice.nodeMouseOver,
      nodeMouseOut: mouseoverservice.nodeMouseOut,
      connectorMouseEnter: mouseoverservice.connectorMouseEnter,
      connectorMouseLeave: mouseoverservice.connectorMouseLeave,
      nodeClicked: function(node) {
        return function(event) {
          $scope.modelservice.nodes.handleClicked(node, event.ctrlKey);
          $scope.$apply();

          // Don't let the chart handle the mouse down.
          event.stopPropagation();
          event.preventDefault();
        }
      }
    };

    $scope.getEdgeDAttribute = Edgedrawingservice.getEdgeDAttribute;
    $scope.getEdgeCenter = Edgedrawingservice.getEdgeCenter;

  }

  angular
    .module('flowchart')
    .controller('canvasController', canvasController);

}());


