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
