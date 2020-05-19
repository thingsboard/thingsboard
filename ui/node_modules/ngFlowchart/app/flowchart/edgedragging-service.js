(function() {

  'use strict';

  function Edgedraggingfactory(Modelvalidation, flowchartConstants, Edgedrawingservice) {
    function factory(modelservice, model, edgeDragging, isValidEdgeCallback, applyFunction, dragAnimation, edgeStyle) {
      if (isValidEdgeCallback === null) {
        isValidEdgeCallback = function() {
          return true;
        };
      }

      var edgedraggingService = {};

      var draggedEdgeSource = null;
      var dragOffset = {};

      edgeDragging.isDragging = false;
      edgeDragging.dragPoint1 = null;
      edgeDragging.dragPoint2 = null;
      edgeDragging.shadowDragStarted = false;

      var destinationHtmlElement = null;
      var oldDisplayStyle = "";

      edgedraggingService.dragstart = function(connector) {
        return function(event) {

          if (connector.type == flowchartConstants.leftConnectorType) {
            for (var i = 0; i < model.edges.length; i++) {
              if (model.edges[i].destination == connector.id) {
                var swapConnector = modelservice.connectors.getConnector(model.edges[i].source);
                var dragLabel = model.edges[i].label;
                var prevEdge = model.edges[i];
                applyFunction(function() {
                  modelservice.edges.delete(model.edges[i]);
                });
                break;
              }
            }
          }

          edgeDragging.isDragging = true;

          if (swapConnector != undefined) {
            draggedEdgeSource = swapConnector;
            edgeDragging.dragPoint1 = modelservice.connectors.getCenteredCoord(swapConnector.id);
            edgeDragging.dragLabel = dragLabel;
            edgeDragging.prevEdge = prevEdge;
          } else {
            draggedEdgeSource = connector;
            edgeDragging.dragPoint1 = modelservice.connectors.getCenteredCoord(connector.id);
          }

          var canvas = modelservice.getCanvasHtmlElement();
          if (!canvas) {
            throw new Error('No canvas while edgedraggingService found.');
          }
          dragOffset.x = -canvas.getBoundingClientRect().left;
          dragOffset.y = -canvas.getBoundingClientRect().top;

          edgeDragging.dragPoint2 = {
            x: event.clientX + dragOffset.x,
            y: event.clientY + dragOffset.y
          };

          event.originalEvent.dataTransfer.setData('Text', 'Just to support firefox');
          if (event.originalEvent.dataTransfer.setDragImage) {
            //var invisibleDiv = angular.element('<div></div>')[0]; // This divs stays invisible, because it is not in the dom.
            //event.originalEvent.dataTransfer.setDragImage(invisibleDiv, 0, 0);
            event.originalEvent.dataTransfer.setDragImage(modelservice.getDragImage(), 0, 0);
          } else {
            destinationHtmlElement = event.target;
            oldDisplayStyle = destinationHtmlElement.style.display;
            event.target.style.display = 'none'; // Internetexplorer does not support setDragImage, but it takes an screenshot, from the draggedelement and uses it as dragimage.
            // Since angular redraws the element in the next dragover call, display: none never gets visible to the user.

            if (dragAnimation == flowchartConstants.dragAnimationShadow) {
              // IE Drag Fix
              edgeDragging.shadowDragStarted = true;
            }
          }

          if (dragAnimation == flowchartConstants.dragAnimationShadow) {
            if (edgeDragging.gElement == undefined) {
              //set shadow elements once
              // IE Support
              edgeDragging.gElement = angular.element(document.querySelectorAll('.shadow-svg-class'));
              edgeDragging.pathElement = angular.element(document.querySelectorAll('.shadow-svg-class')).find('path');
              edgeDragging.circleElement = angular.element(document.querySelectorAll('.shadow-svg-class')).find('circle');
            }

            edgeDragging.gElement.css('display', 'block');
            edgeDragging.pathElement.attr('d', Edgedrawingservice.getEdgeDAttribute(edgeDragging.dragPoint1, edgeDragging.dragPoint2, edgeStyle));
            edgeDragging.circleElement.attr('cx', edgeDragging.dragPoint2.x);
            edgeDragging.circleElement.attr('cy', edgeDragging.dragPoint2.y);
          }
          event.stopPropagation();
        };
      };

      edgedraggingService.dragover = function(event) {

        if (edgeDragging.isDragging) {
          if (!edgeDragging.magnetActive && dragAnimation == flowchartConstants.dragAnimationShadow) {
            if (destinationHtmlElement !== null) {
              destinationHtmlElement.style.display = oldDisplayStyle;
            }

            if (edgeDragging.shadowDragStarted) {
              applyFunction(function() {
                edgeDragging.shadowDragStarted = false;
              });
            }

            edgeDragging.dragPoint2 = {
              x: event.clientX + dragOffset.x,
              y: event.clientY + dragOffset.y
            };

            edgeDragging.pathElement.attr('d', Edgedrawingservice.getEdgeDAttribute(edgeDragging.dragPoint1, edgeDragging.dragPoint2, edgeStyle));
            edgeDragging.circleElement.attr('cx', edgeDragging.dragPoint2.x);
            edgeDragging.circleElement.attr('cy', edgeDragging.dragPoint2.y);

          } else if (dragAnimation == flowchartConstants.dragAnimationRepaint) {
            return applyFunction(function () {

              if (destinationHtmlElement !== null) {
                destinationHtmlElement.style.display = oldDisplayStyle;
              }

              edgeDragging.dragPoint2 = {
                x: event.clientX + dragOffset.x,
                y: event.clientY + dragOffset.y
              };
            });
          }
        }
      };

      edgedraggingService.dragoverConnector = function(connector) {
        return function(event) {

          if (edgeDragging.isDragging) {
            edgedraggingService.dragover(event);
            try {
              Modelvalidation.validateEdges(model.edges.concat([{
                source: draggedEdgeSource.id,
                destination: connector.id
              }]), model.nodes);
            } catch (error) {
              if (error instanceof Modelvalidation.ModelvalidationError) {
                return true;
              } else {
                throw error;
              }
            }
            if (isValidEdgeCallback(draggedEdgeSource, connector)) {
              event.preventDefault();
              event.stopPropagation();
              return false;
            }
          }
        };
      };

      edgedraggingService.dragleaveMagnet = function (event) {
          edgeDragging.magnetActive = false;
      };

      edgedraggingService.dragoverMagnet = function(connector) {
        return function(event) {
          if (edgeDragging.isDragging) {
            edgedraggingService.dragover(event);
              try {
              Modelvalidation.validateEdges(model.edges.concat([{
                source: draggedEdgeSource.id,
                destination: connector.id
              }]), model.nodes);
            } catch (error) {
              if (error instanceof Modelvalidation.ModelvalidationError) {
                return true;
              } else {
                throw error;
              }
            }
            if (isValidEdgeCallback(draggedEdgeSource, connector)) {
              if (dragAnimation == flowchartConstants.dragAnimationShadow) {

                edgeDragging.magnetActive = true;

                edgeDragging.dragPoint2 = modelservice.connectors.getCenteredCoord(connector.id);
                edgeDragging.pathElement.attr('d', Edgedrawingservice.getEdgeDAttribute(edgeDragging.dragPoint1, edgeDragging.dragPoint2, edgeStyle));
                edgeDragging.circleElement.attr('cx', edgeDragging.dragPoint2.x);
                edgeDragging.circleElement.attr('cy', edgeDragging.dragPoint2.y);

                event.preventDefault();
                event.stopPropagation();
                return false;

              } else if (dragAnimation == flowchartConstants.dragAnimationRepaint) {
                return applyFunction(function() {
                  edgeDragging.dragPoint2 = modelservice.connectors.getCenteredCoord(connector.id);
                  event.preventDefault();
                  event.stopPropagation();
                  return false;
                });
              }
            }
          }

        }
      };

      edgedraggingService.dragend = function(event) {
        if (edgeDragging.isDragging) {
          edgeDragging.isDragging = false;
          edgeDragging.dragPoint1 = null;
          edgeDragging.dragPoint2 = null;
          edgeDragging.dragLabel = null;
          event.stopPropagation();

          if (dragAnimation == flowchartConstants.dragAnimationShadow) {
            edgeDragging.gElement.css('display', 'none');
          }
          if (edgeDragging.prevEdge) {
            var edge = edgeDragging.prevEdge;
            edgeDragging.prevEdge = null;
            applyFunction(function() {
              modelservice.edges.putEdge(edge);
            });
          }
        }
      };

      edgedraggingService.drop = function(targetConnector) {
        return function(event) {
          if (edgeDragging.isDragging) {
            try {
              Modelvalidation.validateEdges(model.edges.concat([{
                source: draggedEdgeSource.id,
                destination: targetConnector.id
              }]), model.nodes);
            } catch (error) {
              if (error instanceof Modelvalidation.ModelvalidationError) {
                return true;
              } else {
                throw error;
              }
            }

            if (isValidEdgeCallback(draggedEdgeSource, targetConnector)) {
              edgeDragging.prevEdge = null;
              modelservice.edges._addEdge(event, draggedEdgeSource, targetConnector, edgeDragging.dragLabel);
              event.stopPropagation();
              event.preventDefault();
              return false;
            }
          }
        }
      };
      return edgedraggingService;
    }

    return factory;
  }

  angular.module('flowchart')
    .factory('Edgedraggingfactory', Edgedraggingfactory);

}());
