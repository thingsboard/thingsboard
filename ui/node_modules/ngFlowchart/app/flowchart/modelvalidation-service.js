
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

  angular.module('flowchart')
    .service('Modelvalidation', Modelvalidation);

}());
