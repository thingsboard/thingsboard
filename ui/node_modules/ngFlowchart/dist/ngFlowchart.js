if (!Function.prototype.bind) {
  Function.prototype.bind = function (oThis) {
    if (typeof this !== "function") {
      // closest thing possible to the ECMAScript 5 internal IsCallable function
      throw new TypeError("Function.prototype.bind - what is trying to be bound is not callable");
    }

    var aArgs = Array.prototype.slice.call(arguments, 1), 
        fToBind = this, 
        fNOP = function () {},
        fBound = function () {
          return fToBind.apply(this instanceof fNOP && oThis
                                 ? this
                                 : oThis,
                               aArgs.concat(Array.prototype.slice.call(arguments)));
        };

    fNOP.prototype = this.prototype;
    fBound.prototype = new fNOP();

    return fBound;
  };
}
(function() {

  'use strict';

  angular
    .module('flowchart', ['flowchart-templates']);

}());

(function() {

  'use strict';

  /**
   *
   * @returns {Function}
   * @constructor
   */
  function Topsortservice() {
    /**
     * @returns An array of node ids as string. ['idOfFirstNode', 'idOfSecondNode', ...]. Tbis is not exactly the best way to return ids, but until now there is no need for a better return.
     */
    return function(graph) {

      // Build adjacent list with incoming and outgoing edges.
      var adjacentList = {};
      angular.forEach(graph.nodes, function(node) {
        adjacentList[node.id] = {incoming: 0, outgoing: []};
      });
      angular.forEach(graph.edges, function(edge) {
        var sourceNode = graph.nodes.filter(function(node) {
          return node.connectors.some(function(connector) {
            return connector.id === edge.source;
          })
        })[0];
        var destinationNode = graph.nodes.filter(function(node) {
          return node.connectors.some(function(connector) {
            return connector.id === edge.destination;
          })
        })[0];

        adjacentList[sourceNode.id].outgoing.push(destinationNode.id);
        adjacentList[destinationNode.id].incoming++;
      });

      var orderedNodes = [];
      var sourceNodes = [];
      angular.forEach(adjacentList, function(edges, node) {
        if (edges.incoming === 0) {
          sourceNodes.push(node);
        }
      });
      while (sourceNodes.length !== 0) {
        var sourceNode = sourceNodes.pop();
        for (var i = 0; i < adjacentList[sourceNode].outgoing.length; i++) {
          var destinationNode = adjacentList[sourceNode].outgoing[i];
          adjacentList[destinationNode].incoming--;
          if (adjacentList[destinationNode].incoming === 0) {
            sourceNodes.push('' + destinationNode);
          }
          adjacentList[sourceNode].outgoing.splice(i, 1);
          i--;
        }
        orderedNodes.push(sourceNode);
      }

      var hasEdges = false;
      angular.forEach(adjacentList, function(edges) {
        if (edges.incoming !== 0) {
          hasEdges = true;
        }
      });
      if (hasEdges) {
        return null;
      } else {
        return orderedNodes;
      }

    }
  }

  angular.module('flowchart')
    .factory('Topsortservice', Topsortservice);
})();

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
  Nodedraggingfactory.$inject = ["flowchartConstants"];

  angular
    .module('flowchart')
    .factory('Nodedraggingfactory', Nodedraggingfactory);

}());

(function() {

  'use strict';

  angular
    .module('flowchart')
    .provider('NodeTemplatePath', NodeTemplatePath);

  function NodeTemplatePath() {
    var templatePath = "flowchart/node.html";

    this.setTemplatePath = setTemplatePath;
    this.$get = NodeTemplatePath;

    function setTemplatePath(path) {
      templatePath = path;
    }

    function NodeTemplatePath() {
      return templatePath;
    }
  }

}());

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
  fcNode.$inject = ["flowchartConstants", "NodeTemplatePath"];

  angular.module('flowchart').directive('fcNode', fcNode);
}());

(function() {

  'use strict';

  function Mouseoverfactory() {
    return function(mouseoverscope, applyFunction) {
      var mouseoverservice = {};

      mouseoverscope.connector = null;
      mouseoverscope.edge = null;
      mouseoverscope.node = null;

      mouseoverservice.nodeMouseOver = function(node) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.node = node;
          });
        };
      };

      mouseoverservice.nodeMouseOut = function(node) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.node = null;
          });
        };
      };

      mouseoverservice.connectorMouseEnter = function(connector) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.connector = connector;
          });
        };
      };

      mouseoverservice.connectorMouseLeave = function(connector) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.connector = null
          });
        };
      };

      mouseoverservice.edgeMouseEnter = function(event, edge) {
        mouseoverscope.edge = edge;
      };

      mouseoverservice.edgeMouseLeave = function(event, egde) {
        mouseoverscope.edge = null;
      };

      return mouseoverservice;
    };
  }

  angular.module('flowchart')
    .factory('Mouseoverfactory', Mouseoverfactory);
}());


(function() {

  'use strict';

  function Modelvalidation(Topsortservice, flowchartConstants) {

    function ModelvalidationError(message) {
      this.message = message;
    }
    ModelvalidationError.prototype = new Error;
    ModelvalidationError.prototype.name = 'ModelvalidationError';
    ModelvalidationError.prototype.constructor = ModelvalidationError;
    this.ModelvalidationError = ModelvalidationError;

    this.validateModel = function(model) {
      this.validateNodes(model.nodes);
      this._validateEdges(model.edges, model.nodes);
      return model;
    };

    this.validateNodes = function(nodes) {
      var that = this;

      var ids = [];
      angular.forEach(nodes, function(node) {
        that.validateNode(node);
        if (ids.indexOf(node.id) !== -1) {
          throw new ModelvalidationError('Id not unique.');
        }
        ids.push(node.id);
      });

      var connectorIds = [];
      angular.forEach(nodes, function(node) {
        angular.forEach(node.connectors, function(connector) {
          if (connectorIds.indexOf(connector.id) !== -1) {
            throw new ModelvalidationError('Id not unique.');
          }
          connectorIds.push(connector.id);
        });
      });
      return nodes;
    };

    this.validateNode = function(node) {
      var that = this;
      if (node.id === undefined) {
        throw new ModelvalidationError('Id not valid.');
      }
      if (typeof node.name !== 'string') {
        throw new ModelvalidationError('Name not valid.');
      }
      if (typeof node.x !== 'number' || node.x < 0 || Math.round(node.x) !== node.x) {
        throw new ModelvalidationError('Coordinates not valid.')
      }
      if (typeof node.y !== 'number' || node.y < 0 || Math.round(node.y) !== node.y) {
        throw new ModelvalidationError('Coordinates not valid.')
      }
      if (!Array.isArray(node.connectors)) {
        throw new ModelvalidationError('Connectors not valid.');
      }
      angular.forEach(node.connectors, function(connector) {
        that.validateConnector(connector);
      });
      return node;
    };

    this._validateEdges = function(edges, nodes) {
      var that = this;

      angular.forEach(edges, function(edge) {
        that._validateEdge(edge, nodes);
      });
      angular.forEach(edges, function(edge1, index1) {
        angular.forEach(edges, function(edge2, index2) {
          if (index1 !== index2) {
            if ((edge1.source === edge2.source && edge1.destination === edge2.destination) || (edge1.source === edge2.destination && edge1.destination === edge2.source)) {
              throw new ModelvalidationError('Duplicated edge.')
            }
          }
        });
      });

      if (Topsortservice({nodes: nodes, edges: edges}) === null) {
        throw new ModelvalidationError('Graph has a circle.');
      }

      return edges;
    };

    this.validateEdges = function(edges, nodes) {
      this.validateNodes(nodes);
      return this._validateEdges(edges, nodes);
    };

    this._validateEdge = function(edge, nodes) {
      if (edge.source === undefined) {
        throw new ModelvalidationError('Source not valid.');
      }
      if (edge.destination === undefined) {
        throw new ModelvalidationError('Destination not valid.');
      }

      if (edge.source === edge.destination) {
        throw new ModelvalidationError('Edge with same source and destination connectors.');
      }
      var sourceNode = nodes.filter(function(node) {return node.connectors.some(function(connector) {return connector.id === edge.source})})[0];
      if (sourceNode === undefined) {
        throw new ModelvalidationError('Source not valid.');
      }
      var destinationNode = nodes.filter(function(node) {return node.connectors.some(function(connector) {return connector.id === edge.destination})})[0];
      if (destinationNode === undefined) {
        throw new ModelvalidationError('Destination not valid.');
      }
      if (sourceNode === destinationNode) {
        throw new ModelvalidationError('Edge with same source and destination nodes.');
      }
      return edge;
    };

    this.validateEdge = function(edge, nodes) {
      this.validateNodes(nodes);
      return this._validateEdge(edge, nodes);
    };

    this.validateConnector = function(connector) {
      if (connector.id === undefined) {
        throw new ModelvalidationError('Id not valid.');
      }
      if (connector.type === undefined || connector.type === null || typeof connector.type !== 'string') {
        throw new ModelvalidationError('Type not valid.');
      }
      return connector;
    };

  }
  Modelvalidation.$inject = ["Topsortservice", "flowchartConstants"];

  angular.module('flowchart')
    .service('Modelvalidation', Modelvalidation);

}());

(function() {

  'use strict';

  function Modelfactory($q, Modelvalidation) {

    return function innerModelfactory(model, selectedObjects, dropNode, createEdge, edgeAddedCallback, nodeRemovedCallback, edgeRemovedCallback) {
      Modelvalidation.validateModel(model);
      var modelservice = {
        selectedObjects: selectedObjects
      };

      modelservice.connectorsHtmlElements = {};
      modelservice.nodesHtmlElements = {};
      modelservice.canvasHtmlElement = null;
      modelservice.dragImage = null;
      modelservice.svgHtmlElement = null;

      modelservice.dropNode = dropNode || angular.noop;
      modelservice.createEdge = createEdge || function() { return $q.when({ label: "label" })};
      modelservice.edgeAddedCallback = edgeAddedCallback || angular.noop;
      modelservice.nodeRemovedCallback = nodeRemovedCallback || angular.noop;
      modelservice.edgeRemovedCallback = edgeRemovedCallback || angular.noop;

      function selectObject(object) {
        if (modelservice.isEditable()) {
          if (modelservice.selectedObjects.indexOf(object) === -1) {
            modelservice.selectedObjects.push(object);
          }
        }
      }

      function deselectObject(object) {
        if (modelservice.isEditable()) {
          var index = modelservice.selectedObjects.indexOf(object);
          if (index === -1) {
            throw new Error('Tried to deselect an unselected object');
          }
          modelservice.selectedObjects.splice(index, 1);
        }
      }

      function toggleSelectedObject(object) {
        if (isSelectedObject(object)) {
          deselectObject(object);
        } else {
          selectObject(object);
        }
      }

      function isSelectedObject(object) {
        return modelservice.selectedObjects.indexOf(object) !== -1;
      }

      function isEditObject(object) {
        return modelservice.selectedObjects.length === 1 &&
          modelservice.selectedObjects.indexOf(object) !== -1;
      }

      modelservice.connectors = {

        getConnector: function(connectorId) {
          for(var i=0; i<model.nodes.length; i++) {
            for(var j=0; j<model.nodes[i].connectors.length; j++) {
              if(model.nodes[i].connectors[j].id == connectorId) {
                return model.nodes[i].connectors[j];
              }
            }
          }
        },

        setHtmlElement: function(connectorId, element) {
          modelservice.connectorsHtmlElements[connectorId] = element;
        },

        getHtmlElement: function(connectorId) {
          return modelservice.connectorsHtmlElements[connectorId];
        },

        _getCoords: function(connectorId, centered) {
          var element = this.getHtmlElement(connectorId);
          var canvas = modelservice.getCanvasHtmlElement();
          if (element === null || element === undefined || canvas === null) {
            return {x: 0, y: 0};
          }
          var connectorElementBox = element.getBoundingClientRect();
          var canvasElementBox = canvas.getBoundingClientRect();


          var coords = {
            x: connectorElementBox.left - canvasElementBox.left,
            y: connectorElementBox.top - canvasElementBox.top
          };
          if (centered) {
            coords = {
              x: Math.round(coords.x + element.offsetWidth / 2),
              y: Math.round(coords.y + element.offsetHeight / 2)
            };
          }
          return coords;
        },

        getCoord: function(connectorId) {
          return this._getCoords(connectorId, false);
        },

        getCenteredCoord: function(connectorId) {
          return this._getCoords(connectorId, true);
        }

      };

      modelservice.nodes = {
        getConnectorsByType: function(node, type) {
          return node.connectors.filter(function(connector) {
            return connector.type === type
          });
        },

        select: selectObject,
        deselect: deselectObject,
        toggleSelected: toggleSelectedObject,
        isSelected: isSelectedObject,
        isEdit: isEditObject,

        _addConnector: function(node, connector) {
          node.connectors.push(connector);
          try {
            Modelvalidation.validateNode(node);
          } catch (error) {
            node.connectors.splice(node.connectors.indexOf(connector), 1);
            throw error;
          }
        },

        delete: function(node) {

          if (this.isSelected(node)) {
            this.deselect(node);
          }
          var index = model.nodes.indexOf(node);
          if (index === -1) {
            if (node === undefined) {
              throw new Error('Passed undefined');
            }
            throw new Error('Tried to delete not existing node')
          }

          var connectorIds = this.getConnectorIds(node);
          for (var i = 0; i < model.edges.length; i++) {
            var edge = model.edges[i];
            if (connectorIds.indexOf(edge.source) !== -1 || connectorIds.indexOf(edge.destination) !== -1) {
              modelservice.edges.delete(edge);
              i--;
            }
          }
          model.nodes.splice(index, 1);
          modelservice.nodeRemovedCallback(node);
        },

        getSelectedNodes: function() {
          return model.nodes.filter(function(node) {
            return modelservice.nodes.isSelected(node)
          });
        },

        handleClicked: function(node, ctrlKey) {
          if (ctrlKey) {
            modelservice.nodes.toggleSelected(node);
          } else {
            modelservice.deselectAll();
            modelservice.nodes.select(node);
          }
        },

        setHtmlElement: function(nodeId, element) {
          modelservice.nodesHtmlElements[nodeId] = element;
        },

        getHtmlElement: function(nodeId) {
          return modelservice.nodesHtmlElements[nodeId];
        },

        _addNode: function(node) {
          try {
            model.nodes.push(node);
            Modelvalidation.validateNodes(model.nodes);
          } catch(error) {
            model.nodes.splice(model.nodes.indexOf(node), 1);
            throw error;
          }
        },

        getConnectorIds: function(node) {
          return node.connectors.map(function(connector) {
            return connector.id
          });
        },

        getNodeByConnectorId: function(connectorId) {
          for (var i = 0; i < model.nodes.length; i++) {
            var node = model.nodes[i];
            var connectorIds = this.getConnectorIds(node);
            if (connectorIds.indexOf(connectorId) > -1) {
              return node;
            }
          }
          return null;
        }

      };

      modelservice.edges = {

        sourceCoord: function(edge) {
          return modelservice.connectors.getCenteredCoord(edge.source, edge.source);
        },

        destCoord: function(edge) {
          return modelservice.connectors.getCenteredCoord(edge.destination);
        },

        select: selectObject,
        deselect: deselectObject,
        toggleSelected: toggleSelectedObject,
        isSelected: isSelectedObject,
        isEdit: isEditObject,

        delete: function
          (edge) {
          var index = model.edges.indexOf(edge);
          if (index === -1) {
            throw new Error('Tried to delete not existing edge')
          }
          if (this.isSelected(edge)) {
            this.deselect(edge)
          }
          model.edges.splice(index, 1);
          modelservice.edgeRemovedCallback(edge);
        },

        getSelectedEdges: function() {
          return model.edges.filter(function(edge) {
            return modelservice.edges.isSelected(edge)
          });
        },

        handleEdgeMouseClick: function(edge, ctrlKey) {
          if (ctrlKey) {
            modelservice.edges.toggleSelected(edge);
          } else {
            modelservice.deselectAll();
            modelservice.edges.select(edge);
          }
        },

        putEdge: function(edge) {
          model.edges.push(edge);
        },

        _addEdge: function(event, sourceConnector, destConnector, label) {
          Modelvalidation.validateConnector(sourceConnector);
          Modelvalidation.validateConnector(destConnector);
          var edge = {};
          edge.source = sourceConnector.id;
          edge.destination = destConnector.id;
          edge.label = label;
          Modelvalidation.validateEdges(model.edges.concat([edge]), model.nodes);
          modelservice.createEdge(event, edge).then(
            function (edge) {
              model.edges.push(edge);
              modelservice.edgeAddedCallback(edge);
            }
          );
        }
      };

      function inRectBox(x, y, rectBox) {
        return x >= rectBox.left && x <= rectBox.right &&
            y >= rectBox.top && y <= rectBox.bottom;
      }

      modelservice.getItemInfoAtPoint = function(x,y) {
        return {
            node: modelservice.getNodeAtPoint(x,y),
            edge: modelservice.getEdgeAtPoint(x,y)
        };
      };

      modelservice.getNodeAtPoint = function(x,y) {
        for (var i=0;i<model.nodes.length;i++) {
          var node = model.nodes[i];
          var element = modelservice.nodes.getHtmlElement(node.id);
          var nodeElementBox = element.getBoundingClientRect();
          if (x >= nodeElementBox.left && x <= nodeElementBox.right
                && y >= nodeElementBox.top && y <= nodeElementBox.bottom) {
            return node;
          }
        }
        return null;
      };

      modelservice.getEdgeAtPoint = function(x,y) {
        var element = document.elementFromPoint(x, y);
        var id = element.id;
        var edgeIndex = -1;
        if (id) {
          if (id.startsWith("fc-edge-path-")) {
            edgeIndex = Number(id.substring("fc-edge-path-".length));
          } else if (id.startsWith("fc-edge-label-")) {
            edgeIndex = Number(id.substring("fc-edge-label-".length));
          }
        }
        if (edgeIndex > -1) {
          return model.edges[edgeIndex];
        }
        return null;
      };

      modelservice.selectAllInRect = function(rectBox) {
        angular.forEach(model.nodes, function(value) {
          var element = modelservice.nodes.getHtmlElement(value.id);
          var nodeElementBox = element.getBoundingClientRect();
          if (!value.readonly) {
            var x = nodeElementBox.left + nodeElementBox.width/2;
            var y = nodeElementBox.top + nodeElementBox.height/2;
            if (inRectBox(x, y, rectBox)) {
              modelservice.nodes.select(value);
            } else {
              if (modelservice.nodes.isSelected(value)) {
                modelservice.nodes.deselect(value);
              }
            }
          }
        });
        angular.forEach(model.edges, function(value) {
          var start = modelservice.edges.sourceCoord(value);
          var end = modelservice.edges.destCoord(value);
          var x = (start.x + end.x)/2 + rectBox.parentOffset.left;
          var y = (start.y + end.y)/2 + rectBox.parentOffset.top;
          if (inRectBox(x, y, rectBox)) {
            modelservice.edges.select(value);
          } else {
            if (modelservice.edges.isSelected(value)) {
              modelservice.edges.deselect(value);
            }
          }
        });
      };

      modelservice.selectAll = function() {
        angular.forEach(model.nodes, function(value) {
          if (!value.readonly) {
            modelservice.nodes.select(value);
          }
        });
        angular.forEach(model.edges, function(value) {
          modelservice.edges.select(value);
        });
      };

      modelservice.deselectAll = function() {
        modelservice.selectedObjects.splice(0, modelservice.selectedObjects.length);
      };

      modelservice.deleteSelected = function() {
        var edgesToDelete = modelservice.edges.getSelectedEdges();
        angular.forEach(edgesToDelete, function(edge) {
          modelservice.edges.delete(edge);
        });
        var nodesToDelete = modelservice.nodes.getSelectedNodes();
        angular.forEach(nodesToDelete, function(node) {
          modelservice.nodes.delete(node);
        });
      };

      modelservice.setDropTargetId = function(dropTargetId) {
        modelservice.dropTargetId = dropTargetId;
      };

      modelservice.getDropTargetId = function() {
        return modelservice.dropTargetId;
      };

      modelservice.isDropSource = function() {
        return modelservice.dropTargetId;
      };

      modelservice.isEditable = function() {
        return !modelservice.dropTargetId;
      };

      modelservice.setCanvasHtmlElement = function(element) {
        modelservice.canvasHtmlElement = element;
      };

      modelservice.getCanvasHtmlElement = function() {
        return modelservice.canvasHtmlElement;
      };

      modelservice.getDragImage = function() {
        if (!modelservice.dragImage) {
          modelservice.dragImage = new Image();
          modelservice.dragImage.src = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
          modelservice.dragImage.style.visibility = 'hidden';
        }
        return modelservice.dragImage;
      };

      modelservice.setSvgHtmlElement = function(element) {
        modelservice.svgHtmlElement = element;
      };

      modelservice.getSvgHtmlElement = function() {
        return modelservice.svgHtmlElement;
      };

      modelservice.registerCallbacks = function (edgeAddedCallback, nodeRemovedCallback, edgeRemovedCallback) {
        modelservice.edgeAddedCallback = edgeAddedCallback;
        modelservice.nodeRemovedCallback = nodeRemovedCallback;
        modelservice.edgeRemovedCallback = edgeRemovedCallback;
      };

      return modelservice;
    }

  }
  Modelfactory.$inject = ["$q", "Modelvalidation"];

  angular.module('flowchart')
    .service('Modelfactory', Modelfactory);

})();

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
  fcMagnet.$inject = ["flowchartConstants"];

  angular.module('flowchart')
    .directive('fcMagnet', fcMagnet);
}());

(function() {

  'use strict';

  var constants = {
    htmlPrefix: 'fc',
    leftConnectorType: 'leftConnector',
    rightConnectorType: 'rightConnector',
    curvedStyle: 'curved',
    lineStyle: 'line',
    dragAnimationRepaint: 'repaint',
    dragAnimationShadow: 'shadow'
  };
  constants.canvasClass = constants.htmlPrefix + '-canvas';
  constants.selectedClass = constants.htmlPrefix + '-selected';
  constants.editClass = constants.htmlPrefix + '-edit';
  constants.activeClass = constants.htmlPrefix + '-active';
  constants.hoverClass = constants.htmlPrefix + '-hover';
  constants.draggingClass = constants.htmlPrefix + '-dragging';
  constants.edgeClass = constants.htmlPrefix + '-edge';
  constants.edgeLabelClass = constants.htmlPrefix + '-edge-label';
  constants.connectorClass = constants.htmlPrefix + '-connector';
  constants.magnetClass = constants.htmlPrefix + '-magnet';
  constants.nodeClass = constants.htmlPrefix + '-node';
  constants.nodeOverlayClass = constants.htmlPrefix + '-node-overlay';
  constants.leftConnectorClass = constants.htmlPrefix + '-' + constants.leftConnectorType + 's';
  constants.rightConnectorClass = constants.htmlPrefix + '-' + constants.rightConnectorType + 's';
  constants.canvasResizeThreshold = 200;
  constants.canvasResizeStep = 200;

  angular
    .module('flowchart')
    .constant('flowchartConstants', constants);

}());

(function() {

  'use strict';

  function Edgedrawingservice(flowchartConstants) {
    function computeEdgeTangentOffset(pt1, pt2) {
        return (pt2.y - pt1.y) / 2;
    }

    function computeEdgeSourceTangent(pt1, pt2) {
      return {
        x: pt1.x,
        y: pt1.y + computeEdgeTangentOffset(pt1, pt2)
      };
    }

    function computeEdgeDestinationTangent(pt1, pt2) {
      return {
        x: pt2.x,
        y: pt2.y - computeEdgeTangentOffset(pt1, pt2)
      };
    }

    this.getEdgeDAttribute = function(pt1, pt2, style) {
      var dAddribute = 'M ' + pt1.x + ', ' + pt1.y + ' ';
      if (style === flowchartConstants.curvedStyle) {
        var sourceTangent = computeEdgeSourceTangent(pt1, pt2);
        var destinationTangent = computeEdgeDestinationTangent(pt1, pt2);
        dAddribute += 'C ' + sourceTangent.x + ', ' + sourceTangent.y + ' ' + (destinationTangent.x-50) + ', ' + destinationTangent.y + ' ' + pt2.x + ', ' + pt2.y;
      } else {
        dAddribute += 'L ' + pt2.x + ', ' + pt2.y;
      }
      return dAddribute;
    };

    this.getEdgeCenter = function(pt1, pt2) {
      return {
          x: (pt1.x + pt2.x)/2,
          y: (pt1.y + pt2.y)/2
      };
    }
  }
  Edgedrawingservice.$inject = ["flowchartConstants"];

  angular
    .module('flowchart')
    .service('Edgedrawingservice', Edgedrawingservice);

}());

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
  Edgedraggingfactory.$inject = ["Modelvalidation", "flowchartConstants", "Edgedrawingservice"];

  angular.module('flowchart')
    .factory('Edgedraggingfactory', Edgedraggingfactory);

}());

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
  fcConnector.$inject = ["flowchartConstants"];

  angular
    .module('flowchart')
    .directive('fcConnector', fcConnector);

}());

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
  CanvasFactory.$inject = ["$rootScope"];

  angular.module('flowchart')
      .service('FlowchartCanvasFactory', CanvasFactory);

}());

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
  fcCanvas.$inject = ["flowchartConstants"];

  angular
    .module('flowchart')
    .directive('fcCanvas', fcCanvas);

}());


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
  canvasController.$inject = ["$scope", "Mouseoverfactory", "Nodedraggingfactory", "FlowchartCanvasFactory", "Modelfactory", "Edgedraggingfactory", "Edgedrawingservice", "Rectangleselectfactory"];

  angular
    .module('flowchart')
    .controller('canvasController', canvasController);

}());



(function(module) {
try {
  module = angular.module('flowchart-templates');
} catch (e) {
  module = angular.module('flowchart-templates', []);
}
module.run(['$templateCache', function($templateCache) {
  $templateCache.put('flowchart/canvas.html',
    '<div ng-click="canvasClick($event)">\n' +
    '  <svg>\n' +
    '    <defs>\n' +
    '      <marker class="fc-arrow-marker" ng-attr-id="{{arrowDefId}}" markerWidth="5" markerHeight="5" viewBox="-6 -6 12 12" refX="10" refY="0" markerUnits="strokeWidth" orient="auto">\n' +
    '        <polygon points="-2,0 -5,5 5,0 -5,-5" stroke="gray" fill="gray" stroke-width="1px"/>\n' +
    '      </marker>\n' +
    '      <marker class="fc-arrow-marker-selected" ng-attr-id="{{arrowDefId}}-selected" markerWidth="5" markerHeight="5" viewBox="-6 -6 12 12" refX="10" refY="0" markerUnits="strokeWidth" orient="auto">\n' +
    '        <polygon points="-2,0 -5,5 5,0 -5,-5" stroke="red" fill="red" stroke-width="1px"/>\n' +
    '      </marker>\n' +
    '    </defs>\n' +
    '    <g ng-repeat="edge in model.edges">\n' +
    '      <path\n' +
    '        ng-attr-id="{{\'fc-edge-path-\'+$index}}"\n' +
    '        ng-mousedown="edgeMouseDown($event, edge)"\n' +
    '        ng-click="edgeClick($event, edge)"\n' +
    '        ng-dblclick="edgeDoubleClick($event, edge)"\n' +
    '        ng-mouseover="edgeMouseOver($event, edge)"\n' +
    '        ng-mouseenter="edgeMouseEnter($event, edge)"\n' +
    '        ng-mouseleave="edgeMouseLeave($event, edge)"\n' +
    '        ng-attr-class="{{(modelservice.edges.isSelected(edge) && flowchartConstants.selectedClass + \' \' + flowchartConstants.edgeClass) || edge == mouseOver.edge && flowchartConstants.hoverClass + \' \' + flowchartConstants.edgeClass || edge.active && flowchartConstants.activeClass + \' \' + flowchartConstants.edgeClass || flowchartConstants.edgeClass}}"\n' +
    '        ng-attr-d="{{getEdgeDAttribute(modelservice.edges.sourceCoord(edge), modelservice.edges.destCoord(edge), edgeStyle)}}"\n' +
    '        ng-attr-marker-end="url(#{{modelservice.edges.isSelected(edge) ? arrowDefId+\'-selected\' : arrowDefId}})"></path>\n' +
    '    </g>\n' +
    '    <g ng-if="dragAnimation == flowchartConstants.dragAnimationRepaint && edgeDragging.isDragging">\n' +
    '\n' +
    '      <path class="{{ flowchartConstants.edgeClass }} {{ flowchartConstants.draggingClass }}"\n' +
    '            ng-attr-d="{{getEdgeDAttribute(edgeDragging.dragPoint1, edgeDragging.dragPoint2, edgeStyle)}}"></path>\n' +
    '      <circle class="edge-endpoint" r="4" ng-attr-cx="{{edgeDragging.dragPoint2.x}}"\n' +
    '              ng-attr-cy="{{edgeDragging.dragPoint2.y}}"></circle>\n' +
    '\n' +
    '    </g>\n' +
    '    <g ng-if="dragAnimation == flowchartConstants.dragAnimationShadow" class="shadow-svg-class {{ flowchartConstants.edgeClass }} {{ flowchartConstants.draggingClass }}" style="display:none">\n' +
    '      <path d=""></path>\n' +
    '      <circle class="edge-endpoint" r="4"></circle>\n' +
    '    </g>\n' +
    '  </svg>\n' +
    '  <fc-node selected="modelservice.nodes.isSelected(node)"\n' +
    '           edit="modelservice.nodes.isEdit(node)"\n' +
    '           under-mouse="node === mouseOver.node"\n' +
    '           node="node"\n' +
    '           mouse-over-connector="mouseOver.connector"\n' +
    '           modelservice="modelservice"\n' +
    '           dragged-node="nodeDragging.draggedNode"\n' +
    '           callbacks="callbacks"\n' +
    '           user-node-callbacks="userNodeCallbacks"\n' +
    '           ng-repeat="node in model.nodes"></fc-node>\n' +
    '  <div ng-if="dragAnimation == flowchartConstants.dragAnimationRepaint && edgeDragging.isDragging"\n' +
    '    ng-attr-class="{{\'fc-noselect \' + flowchartConstants.edgeLabelClass}}"\n' +
    '    ng-style="{ top: (getEdgeCenter(edgeDragging.dragPoint1, edgeDragging.dragPoint2).y)+\'px\',\n' +
    '                left: (getEdgeCenter(edgeDragging.dragPoint1, edgeDragging.dragPoint2).x)+\'px\'}">\n' +
    '    <div class="fc-edge-label-text">\n' +
    '      <span ng-attr-id="{{\'fc-edge-label-dragging\'}}" ng-if="edgeDragging.dragLabel">{{edgeDragging.dragLabel}}</span>\n' +
    '    </div>\n' +
    '  </div>\n' +
    '  <div ng-mousedown="edgeMouseDown($event, edge)"\n' +
    '       ng-click="edgeClick($event, edge)"\n' +
    '       ng-dblclick="edgeDoubleClick($event, edge)"\n' +
    '       ng-mouseover="edgeMouseOver($event, edge)"\n' +
    '       ng-mouseenter="edgeMouseEnter($event, edge)"\n' +
    '       ng-mouseleave="edgeMouseLeave($event, edge)"\n' +
    '       ng-attr-class="{{\'fc-noselect \' + ((modelservice.edges.isEdit(edge) && flowchartConstants.editClass + \' \' + flowchartConstants.edgeLabelClass) || (modelservice.edges.isSelected(edge) && flowchartConstants.selectedClass + \' \' + flowchartConstants.edgeLabelClass) || edge == mouseOver.edge && flowchartConstants.hoverClass + \' \' + flowchartConstants.edgeLabelClass || edge.active && flowchartConstants.activeClass + flowchartConstants.edgeLabelClass || flowchartConstants.edgeLabelClass)}}"\n' +
    '       ng-style="{ top: (getEdgeCenter(modelservice.edges.sourceCoord(edge), modelservice.edges.destCoord(edge)).y)+\'px\',\n' +
    '                   left: (getEdgeCenter(modelservice.edges.sourceCoord(edge), modelservice.edges.destCoord(edge)).x)+\'px\'}"\n' +
    '       ng-repeat="edge in model.edges">\n' +
    '    <div class="fc-edge-label-text">\n' +
    '      <div ng-if="modelservice.isEditable()" class="fc-noselect fc-nodeedit" ng-click="edgeEdit($event, edge)">\n' +
    '        <i class="fa fa-pencil" aria-hidden="true"></i>\n' +
    '      </div>\n' +
    '      <div ng-if="modelservice.isEditable()" class="fc-noselect fc-nodedelete" ng-click="edgeRemove($event, edge)">\n' +
    '        &times;\n' +
    '      </div>\n' +
    '      <span ng-attr-id="{{\'fc-edge-label-\'+$index}}" ng-if="edge.label">{{edge.label}}</span>\n' +
    '    </div>\n' +
    '  </div>\n' +
    '  <div id="select-rectangle" class="fc-select-rectangle" hidden></div>\n' +
    '</div>\n' +
    '');
}]);
})();

(function(module) {
try {
  module = angular.module('flowchart-templates');
} catch (e) {
  module = angular.module('flowchart-templates', []);
}
module.run(['$templateCache', function($templateCache) {
  $templateCache.put('flowchart/node.html',
    '<div\n' +
    '  id="{{node.id}}"\n' +
    '  ng-attr-style="position: absolute; top: {{ node.y }}px; left: {{ node.x }}px;"\n' +
    '  ng-dblclick="callbacks.doubleClick($event)">\n' +
    '  <div class="{{flowchartConstants.nodeOverlayClass}}"></div>\n' +
    '  <div class="innerNode">\n' +
    '    <p>{{ node.name }}</p>\n' +
    '\n' +
    '    <div class="{{flowchartConstants.leftConnectorClass}}">\n' +
    '      <div fc-magnet\n' +
    '           ng-repeat="connector in modelservice.nodes.getConnectorsByType(node, flowchartConstants.leftConnectorType)">\n' +
    '        <div fc-connector></div>\n' +
    '      </div>\n' +
    '    </div>\n' +
    '    <div class="{{flowchartConstants.rightConnectorClass}}">\n' +
    '      <div fc-magnet\n' +
    '           ng-repeat="connector in modelservice.nodes.getConnectorsByType(node, flowchartConstants.rightConnectorType)">\n' +
    '        <div fc-connector></div>\n' +
    '      </div>\n' +
    '    </div>\n' +
    '  </div>\n' +
    '  <div ng-if="modelservice.isEditable() && !node.readonly" class="fc-nodeedit" ng-click="callbacks.nodeEdit($event, node)">\n' +
    '    <i class="fa fa-pencil" aria-hidden="true"></i>\n' +
    '  </div>\n' +
    '  <div ng-if="modelservice.isEditable() && !node.readonly" class="fc-nodedelete" ng-click="modelservice.nodes.delete(node)">\n' +
    '    &times;\n' +
    '  </div>\n' +
    '</div>\n' +
    '');
}]);
})();

(function(module) {
try {
  module = angular.module('flowchart-templates');
} catch (e) {
  module = angular.module('flowchart-templates', []);
}
module.run(['$templateCache', function($templateCache) {
  $templateCache.put('flowchart/onedatanode.html',
    '<div\n' +
    '  id="{{node.id}}"\n' +
    '  ng-attr-style="position: absolute; top: {{ node.y }}px; left: {{ node.x }}px; background: {{ node.color }}; border-color: {{node.borderColor}}">\n' +
    '  <p>{{ node.name }}</p>\n' +
    '\n' +
    '  <div class="{{flowchartConstants.leftConnectorClass}}">\n' +
    '    <div fc-connector\n' +
    '         ng-repeat="connector in modelservice.nodes.getConnectorsByType(node, flowchartConstants.leftConnectorType)"></div>\n' +
    '  </div>\n' +
    '  <div class="{{flowchartConstants.rightConnectorClass}}">\n' +
    '    <div fc-connector\n' +
    '         ng-repeat="connector in modelservice.nodes.getConnectorsByType(node, flowchartConstants.rightConnectorType)"></div>\n' +
    '  </div>\n' +
    '</div>\n' +
    '');
}]);
})();
