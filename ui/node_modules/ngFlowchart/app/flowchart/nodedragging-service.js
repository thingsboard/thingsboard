(function() {

  'use strict';

  function Nodedraggingfactory(flowchartConstants) {

    var nodeDropScope = {};
    nodeDropScope.dropElement = null;

    return function(modelservice, nodeDraggingScope, applyFunction, automaticResize, dragAnimation) {

      nodeDraggingScope.shadowDragStarted = false;
      nodeDraggingScope.dropElement = null;

      var dragOffsets = [];
      var draggedElements = [];
      nodeDraggingScope.draggedNodes = [];
      nodeDraggingScope.shadowElements = [];

      var destinationHtmlElements = [];
      var oldDisplayStyles = [];

      function getCoordinate(coordinate, max) {
        coordinate = Math.max(coordinate, 0);
        coordinate = Math.min(coordinate, max);
        return coordinate;
      }
      function getXCoordinate(x) {
        return getCoordinate(x, modelservice.getCanvasHtmlElement().offsetWidth);
      }
      function getYCoordinate(y) {
        return getCoordinate(y, modelservice.getCanvasHtmlElement().offsetHeight);
      }
      function resizeCanvas(draggedNode, nodeElement) {
        if (automaticResize && !modelservice.isDropSource()) {
          var canvasElement = modelservice.getCanvasHtmlElement();
          if (canvasElement.offsetWidth < draggedNode.x + nodeElement.offsetWidth + flowchartConstants.canvasResizeThreshold) {
            canvasElement.style.width = canvasElement.offsetWidth + flowchartConstants.canvasResizeStep + 'px';
          }
          if (canvasElement.offsetHeight < draggedNode.y + nodeElement.offsetHeight + flowchartConstants.canvasResizeThreshold) {
            canvasElement.style.height = canvasElement.offsetHeight + flowchartConstants.canvasResizeStep + 'px';
          }
        }
      }
      return {
        dragstart: function(node) {
          return function(event) {
            if (node.readonly) {
              return;
            }
            dragOffsets.length = 0;
            draggedElements.length = 0;
            nodeDraggingScope.draggedNodes.length = 0;
            nodeDraggingScope.shadowElements.length = 0;
            destinationHtmlElements.length = 0;
            oldDisplayStyles.length = 0;

            var elements = [];
            var nodes = [];
            if (modelservice.nodes.isSelected(node)) {
              var selectedNodes = modelservice.nodes.getSelectedNodes();
              for (var i=0;i<selectedNodes.length;i++) {
                var selectedNode = selectedNodes[i];
                var element = angular.element(modelservice.nodes.getHtmlElement(selectedNode.id));
                elements.push(element);
                nodes.push(selectedNode);
              }
            } else {
              elements.push(angular.element(event.target));
              nodes.push(node);
            }
            var offsetsX = [];
            var offsetsY = [];
            for (var i=0;i<elements.length;i++) {
              var element = elements[i];
              offsetsX.push(parseInt(element.css('left')) - event.clientX);
              offsetsY.push(parseInt(element.css('top')) - event.clientY);
            }
            if (modelservice.isDropSource()) {
              if (nodeDropScope.dropElement) {
                nodeDropScope.dropElement.parentNode.removeChild(nodeDropScope.dropElement);
                nodeDropScope.dropElement = null;
              }
              nodeDropScope.dropElement = elements[0][0].cloneNode(true);

              var offset = angular.element(modelservice.getCanvasHtmlElement()).offset();

              nodeDropScope.dropElement.offsetInfo = {
                offsetX: Math.round(offsetsX[0] + offset.left),
                offsetY: Math.round(offsetsY[0] + offset.top)
              };
              nodeDropScope.dropElement.style.position = 'absolute';
              nodeDropScope.dropElement.style.pointerEvents = 'none';
              nodeDropScope.dropElement.style.zIndex = '9999';

              document.body.appendChild(nodeDropScope.dropElement);

              var dropNodeInfo = {
                node: node,
                dropTargetId: modelservice.getDropTargetId(),
                offsetX: Math.round(offsetsX[0] + offset.left),
                offsetY: Math.round(offsetsY[0] + offset.top)
              };
              event.originalEvent.dataTransfer.setData('text', angular.toJson(dropNodeInfo));

              if (event.originalEvent.dataTransfer.setDragImage) {
                //var invisibleDiv = angular.element('<div></div>')[0]; // This divs stays invisible, because it is not in the dom.
                //event.originalEvent.dataTransfer.setDragImage(invisibleDiv, 0, 0);
                event.originalEvent.dataTransfer.setDragImage(modelservice.getDragImage(), 0, 0);
              } else {
                destinationHtmlElements.push(event.target);
                oldDisplayStyles.push(event.target.style.display);
                event.target.style.display = 'none';
                nodeDraggingScope.shadowDragStarted = true;
              }
              return;
            }
            //modelservice.deselectAll();
            //modelservice.nodes.select(node);
            nodeDraggingScope.draggedNodes = nodes;
            for (var i=0;i<elements.length;i++) {
              draggedElements.push(elements[i][0]);
              dragOffsets.push(
                {
                  x: offsetsX[i],
                  y: offsetsY[i]
                }
              );
            }

            if (dragAnimation == flowchartConstants.dragAnimationShadow) {
              for (var i=0;i<draggedElements.length;i++) {
                var dragOffset = dragOffsets[i];
                var draggedNode = nodeDraggingScope.draggedNodes[i];
                var shadowElement = angular.element('<div style="position: absolute; opacity: 0.7; top: '+ getYCoordinate(dragOffset.y + event.clientY) +'px; left: '+ getXCoordinate(dragOffset.x + event.clientX) +'px; "><div class="innerNode"><p style="padding: 0 15px;">'+ draggedNode.name +'</p> </div></div>');
                var targetInnerNode = angular.element(draggedElements[i]).children()[0];
                shadowElement.children()[0].style.backgroundColor = targetInnerNode.style.backgroundColor;
                nodeDraggingScope.shadowElements.push(shadowElement);
                modelservice.getCanvasHtmlElement().appendChild(nodeDraggingScope.shadowElements[i][0]);
              }
            }

            event.originalEvent.dataTransfer.setData('text', 'Just to support firefox');
            if (event.originalEvent.dataTransfer.setDragImage) {
              //var invisibleDiv = angular.element('<div></div>')[0]; // This divs stays invisible, because it is not in the dom.
              //event.originalEvent.dataTransfer.setDragImage(invisibleDiv, 0, 0);
              event.originalEvent.dataTransfer.setDragImage(modelservice.getDragImage(), 0, 0);
            } else {
              for (var i=0;i<draggedElements.length;i++) {
                destinationHtmlElements.push(draggedElements[i]);
                oldDisplayStyles.push(destinationHtmlElements[i].style.display);
                destinationHtmlElements[i].style.display = 'none'; // Internetexplorer does not support setDragImage, but it takes an screenshot, from the draggedelement and uses it as dragimage.
              }
              // Since angular redraws the element in the next dragover call, display: none never gets visible to the user.
              if (dragAnimation == flowchartConstants.dragAnimationShadow) {
                // IE Drag Fix
                nodeDraggingScope.shadowDragStarted = true;
              }
            }
          };
        },

        drop: function(event) {
          if (modelservice.isDropSource()) {
            event.preventDefault();
            return false;
          }
          var dropNode = null;
          var infoText = event.originalEvent.dataTransfer.getData('text');
          if (infoText) {
            var dropNodeInfo = null;
            try {
                dropNodeInfo = angular.fromJson(infoText);
            } catch (e) {}
            if (dropNodeInfo && dropNodeInfo.dropTargetId) {
              if (modelservice.getCanvasHtmlElement().id &&
                modelservice.getCanvasHtmlElement().id == dropNodeInfo.dropTargetId) {
                dropNode = dropNodeInfo.node;
                var offset = angular.element(modelservice.getCanvasHtmlElement()).offset();
                var x = event.clientX - offset.left;
                var y = event.clientY - offset.top;
                dropNode.x = Math.round(getXCoordinate(dropNodeInfo.offsetX + x));
                dropNode.y = Math.round(getYCoordinate(dropNodeInfo.offsetY + y));
              }
             }
          }
          if (dropNode) {
              modelservice.dropNode(event, dropNode);
              event.preventDefault();
              return false;
          } else if (nodeDraggingScope.draggedNodes.length) {
            return applyFunction(function() {
              for (var i=0;i<nodeDraggingScope.draggedNodes.length;i++) {
                var draggedNode = nodeDraggingScope.draggedNodes[i];
                var dragOffset = dragOffsets[i];
                draggedNode.x = Math.round(getXCoordinate(dragOffset.x + event.clientX));
                draggedNode.y = Math.round(getYCoordinate(dragOffset.y + event.clientY));
              }
              event.preventDefault();
              return false;
            })
          }
        },
        dragover: function(event) {
          if (nodeDropScope.dropElement) {
              var offsetInfo = nodeDropScope.dropElement.offsetInfo;
              nodeDropScope.dropElement.style.left = (offsetInfo.offsetX + event.clientX) + 'px';
              nodeDropScope.dropElement.style.top = (offsetInfo.offsetY + event.clientY) + 'px';
              if(nodeDraggingScope.shadowDragStarted) {
                applyFunction(function() {
                  destinationHtmlElements[0].style.display = oldDisplayStyles[0];
                  nodeDraggingScope.shadowDragStarted = false;
                });
              }
              event.preventDefault();
              return;
          }
          if (modelservice.isDropSource()) {
            event.preventDefault();
            return;
          }
          if (!nodeDraggingScope.draggedNodes.length) {
            event.preventDefault();
            return;
          }
          if (dragAnimation == flowchartConstants.dragAnimationRepaint) {
            if (nodeDraggingScope.draggedNodes.length) {
              return applyFunction(function() {
                for (var i=0;i<nodeDraggingScope.draggedNodes.length;i++) {
                  var draggedNode = nodeDraggingScope.draggedNodes[i];
                  var dragOffset = dragOffsets[i];
                  draggedNode.x = getXCoordinate(dragOffset.x + event.clientX);
                  draggedNode.y = getYCoordinate(dragOffset.y + event.clientY);
                  resizeCanvas(draggedNode, draggedElements[i]);
                }
                event.preventDefault();
                return false;
              });
            }
          } else if (dragAnimation == flowchartConstants.dragAnimationShadow) {
            if (nodeDraggingScope.draggedNodes.length) {
              if(nodeDraggingScope.shadowDragStarted) {
                applyFunction(function() {
                  for (var i=0;i<nodeDraggingScope.draggedNodes.length;i++) {
                    destinationHtmlElements[i].style.display = oldDisplayStyles[i];
                  }
                  nodeDraggingScope.shadowDragStarted = false;
                });
              }
              for (var i=0;i<nodeDraggingScope.draggedNodes.length;i++) {
                var draggedNode = nodeDraggingScope.draggedNodes[i];
                var dragOffset = dragOffsets[i];
                nodeDraggingScope.shadowElements[i].css('left', getXCoordinate(dragOffset.x + event.clientX) + 'px');
                nodeDraggingScope.shadowElements[i].css('top', getYCoordinate(dragOffset.y + event.clientY) + 'px');
                resizeCanvas(draggedNode, draggedElements[i]);
              }
              event.preventDefault();
            }
          }
        },

        dragend: function(event) {
          applyFunction(function() {
            if (nodeDropScope.dropElement) {
              nodeDropScope.dropElement.parentNode.removeChild(nodeDropScope.dropElement);
              nodeDropScope.dropElement = null;
            }
            if (modelservice.isDropSource()) {
              return;
            }
            if (nodeDraggingScope.shadowElements.length) {
              for (var i=0;i<nodeDraggingScope.draggedNodes.length;i++) {
                var draggedNode = nodeDraggingScope.draggedNodes[i];
                var shadowElement = nodeDraggingScope.shadowElements[i];
                draggedNode.x = parseInt(shadowElement.css('left').replace('px', ''));
                draggedNode.y = parseInt(shadowElement.css('top').replace('px', ''));
                modelservice.getCanvasHtmlElement().removeChild(shadowElement[0]);
              }
              nodeDraggingScope.shadowElements.length = 0;
            }

            if (nodeDraggingScope.draggedNodes.length) {
              nodeDraggingScope.draggedNodes.length = 0;
              draggedElements.length = 0;
              dragOffsets.length = 0;
            }
          });
        }
      };
    };
  }

  angular
    .module('flowchart')
    .factory('Nodedraggingfactory', Nodedraggingfactory);

}());
