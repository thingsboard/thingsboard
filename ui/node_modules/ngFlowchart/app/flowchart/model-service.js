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

  angular.module('flowchart')
    .service('Modelfactory', Modelfactory);

})();
