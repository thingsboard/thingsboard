describe('The modelvalidation', function() {

  beforeEach(function() {
    var that = this;
    this.Topsortservice = jasmine.createSpy('Topsortservice');
    module('flowchart', function($provide) {
      $provide.factory('Topsortservice', function() {
        return that.Topsortservice;
      });
    });
  });

  beforeEach(inject(function(Modelvalidation, flowchartConstants) {
    this.Modelvalidation = Modelvalidation;
    this.validNode = {id: 1, name: 'name', x: 10000, y: 1, connectors: []};
    this.validConnector = {id: 1, type: flowchartConstants.rightConnectorType};

    this.validNodes = [this.validNode,
      {id: 2, name: 'name', x: 12, y: 2131, connectors: [this.validConnector, {id: -49, type: ''}]},
      {id: 10000, name: '', x: 0, y: 0, connectors: [{id: -5, type: 'customType'}]}];

    this.validEdge = {source: 1, destination: -5};

    this.validEges = [this.validEdge, {source: -49, destination: -5}];

    this.validModel = {nodes: this.validNodes, edges: this.validEges};
  }));

  it('should not change the model', function() {
    var connector = angular.copy(this.validConnector);
    var node = angular.copy(this.validNode);
    var nodes = angular.copy(this.validNodes);
    var edge = angular.copy(this.validEdge);
    var edges = angular.copy(this.validEdges);
    var model = angular.copy(this.validModel);

    this.Modelvalidation.validateConnector(connector);
    expect(connector).toEqual(this.validConnector);

    this.Modelvalidation.validateNode(node);
    expect(node).toEqual(this.validNode);

    this.Modelvalidation.validateNodes(nodes);
    expect(nodes).toEqual(this.validNodes);

    this.Modelvalidation.validateEdge(edge, nodes);
    expect(edge).toEqual(this.validEdge);
    expect(nodes).toEqual(this.validNodes);

    this.Modelvalidation.validateEdges(edges, nodes);
    expect(edges).toEqual(this.validEdges);
    expect(nodes).toEqual(this.validNodes);

    var node = angular.copy(this.validNode);
    this.Modelvalidation.validateModel(model);
    expect(model).toEqual(this.validModel);
  });

  describe('The nodesvalidation', function() {
    it('should assure that all ids are unique', function() {
      var that = this;

      var nodes = angular.copy(this.validNodes);
      nodes[0].id = nodes[1].id;
      expect(function() {
        that.Modelvalidation.validateNodes(nodes)
      }).toThrowError('Id not unique.');

      nodes = angular.copy(this.validNodes);
      expect(this.Modelvalidation.validateNodes(angular.copy(nodes))).toEqual(nodes);
    });

    it('should assure that all connector ids are unique', function() {
      var that = this;

      var nodes = angular.copy(this.validNodes);
      nodes[1].connectors[0].id = nodes[2].connectors[0].id;
      expect(function() {
        that.Modelvalidation.validateNodes(nodes)
      }).toThrowError('Id not unique.');

      nodes = angular.copy(this.validNodes);
      angular.forEach(nodes, function(node) {
        node.connectors = [];
      });
      expect(this.Modelvalidation.validateNodes(angular.copy(nodes))).toEqual(nodes);
    });

    it('should work with empty array', function() {
      expect(this.Modelvalidation.validateNodes([])).toEqual([]);
    });

    it('should work invalid node', function() {
      var that = this;

      var nodes = [angular.copy(this.validNode)];
      delete nodes[0].name;
      expect(function() {
        that.Modelvalidation.validateNodes(nodes)
      }).toThrowError('Name not valid.');

      nodes = angular.copy(this.validNodes);
      delete nodes[1].connectors[0].id;
      expect(function() {
        that.Modelvalidation.validateNodes(nodes)
      }).toThrowError('Id not valid.');
    });
  });

  describe('The nodevalidation', function() {

    it('should detect if id, x, y, name and connectors are defined.', function() {
      var that = this;
      var node = {};
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow();

      node = angular.copy(this.validNode);
      delete node.connectors;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Connectors not valid.'));

      node = angular.copy(this.validNode);
      delete node.y;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      delete node.x;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      delete node.id;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Id not valid.'));

      node = angular.copy(this.validNode);
      delete node.name;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Name not valid.'));
    });

    it('should detect if x, y are natural numbers', function() {
      var that = this;

      var node = angular.copy(this.validNode);
      node.x = -1;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      node.x = '1';
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      node.x = true;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      node.x = 1.1;
      node = {id: 1, name: '', x: 1.1, y: 1, connectors: []};
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Coordinates not valid.'));

      node = angular.copy(this.validNode);
      node.x = 10000;
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);

      node = angular.copy(this.validNode);
      node.x = 0;
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);

    });

    it('should detect if name is string', function() {
      var that = this;

      var node = angular.copy(this.validNode);
      node.name = true;
      expect(function() {
        that.Modelvalidation.validateNode(node)
      }).toThrow(new Error('Name not valid.'));

      node = angular.copy(this.validNode);
      node.name = '';
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);

      node = angular.copy(this.validNode);
      node.name = 'name';
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);
    });

    it('should assure that connectors is an array', function() {
      var that = this;

      var node = angular.copy(this.validNode);
      node.connectors = '';
      expect(function() {
        that.Modelvalidation.validateNode(angular.copy(node))
      }).toThrow(new Error('Connectors not valid.'));

      node = angular.copy(this.validNode);
      node.connectors = [];
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);

      node = angular.copy(this.validNode);
      node.connectors = [this.validConnector];
      expect(this.Modelvalidation.validateNode(angular.copy(node))).toEqual(node);
    });
  });

  describe('The connectorvalidation', function() {
    it('should assure that id and type are defined', function() {
      var that = this;

      var connector = angular.copy(this.validConnector);
      delete connector.id;
      expect(function() {
        that.Modelvalidation.validateConnector(connector)
      }).toThrowError('Id not valid.');

      connector = angular.copy(this.validConnector);
      delete connector.type;
      expect(function() {
        that.Modelvalidation.validateConnector(connector)
      }).toThrowError('Type not valid.');

      connector = angular.copy(this.validConnector);
      expect(that.Modelvalidation.validateConnector(connector)).toEqual(this.validConnector);
    });

    it('should assure that type is a string', function() {
      var that = this;

      var connector = angular.copy(this.validConnector);
      connector.type = null;
      expect(function() {
        that.Modelvalidation.validateConnector(connector)
      }).toThrowError('Type not valid.');

      connector = angular.copy(this.validConnector);
      connector.type = 1;
      expect(function() {
        that.Modelvalidation.validateConnector(connector)
      }).toThrowError('Type not valid.');

      connector = angular.copy(this.validConnector);
      connector.type = true;
      expect(function() {
        that.Modelvalidation.validateConnector(connector)
      }).toThrowError('Type not valid.');

      connector = angular.copy(this.validConnector);
      connector.type = '';
      expect(that.Modelvalidation.validateConnector(angular.copy(connector))).toEqual(connector);
    });

    describe('The edgesvalidation', function() {
      it('should assure that there are not two edges with same source and destination', function() {
        var that = this;

        var model = angular.copy(this.validModel);
        model.edges[0] = model.edges[1];
        expect(function() {
          that.Modelvalidation.validateEdges(model.edges, model.nodes)
        }).toThrowError('Duplicated edge.');

        model = angular.copy(this.validModel);
        model.edges[0].source = model.edges[1].destination;
        model.edges[0].destination = model.edges[1].source;
        expect(function() {
          that.Modelvalidation.validateEdges(model.edges, model.nodes)
        }).toThrowError('Duplicated edge.');
      });

      it('should assure that there are no circular edges', function() {
        this.Modelvalidation.validateEdges([], []);
        expect(this.Topsortservice).toHaveBeenCalled();
      });

      it('should exist a positive test', function() {
        expect(this.Modelvalidation.validateEdges(angular.copy(this.validModel.edges), angular.copy(this.validModel.nodes))).toEqual(this.validModel.edges);
        expect(this.Modelvalidation.validateEdges([], [])).toEqual([]);
      })
    });

    describe('The edgevalidation', function() {
      it('should assure that source and destination are set', function() {
        var that = this;

        var edge = angular.copy(this.validEdge);
        delete edge.source;
        expect(function() {
          that.Modelvalidation.validateEdge(edge, angular.copy(that.validModel.nodes))
        }).toThrowError('Source not valid.');

        edge = angular.copy(this.validEdge);
        delete edge.destination;
        expect(function() {
          that.Modelvalidation.validateEdge(edge, angular.copy(that.validModel.nodes))
        }).toThrowError('Destination not valid.');
      });

      it('should guarantee that source and destination are valid connector ids', function() {
        var that = this;

        var model = angular.copy(this.validModel);
        model.edges.push({source: -1000, destination: model.nodes[1].connectors[0].id});
        expect(function() {
          that.Modelvalidation.validateEdge(model.edges[model.edges.length - 1], model.nodes)
        }).toThrowError('Source not valid.');

        model = angular.copy(this.validModel);
        model.edges.push({source: model.nodes[1].connectors[0].id, destination: -1000});
        expect(function() {
          that.Modelvalidation.validateEdge(model.edges[model.edges.length - 1], model.nodes)
        }).toThrowError('Destination not valid.');

        model = angular.copy(this.validModel);
        expect(this.Modelvalidation.validateEdge(model.edges[0], model.nodes)).toEqual(this.validModel.edges[0]);
      });

      it('should guarantee that source and destination are not equal and not on the same node', function() {
        var that = this;

        var model = angular.copy(this.validModel);
        model.edges.push({source: model.nodes[1].connectors[0].id, destination: model.nodes[1].connectors[1].id});
        expect(function() {
          that.Modelvalidation.validateEdge(model.edges[model.edges.length - 1], model.nodes)
        }).toThrowError('Edge with same source and destination nodes.');

        model = angular.copy(this.validModel);
        model.edges.push({source: model.nodes[1].connectors[0].id, destination: model.nodes[1].connectors[0].id});
        expect(function() {
          that.Modelvalidation.validateEdge(model.edges[model.edges.length - 1], model.nodes)
        }).toThrowError('Edge with same source and destination connectors.');
      });

      it('should validate the nodes before using them', function() {
        var that = this;

        var model = angular.copy(this.validModel);
        delete model.nodes[0].id;
        expect(function() {
          that.Modelvalidation.validateEdge(model.edges[0], model.nodes)
        }).toThrowError('Id not valid.');
      });
    });

  });
});
