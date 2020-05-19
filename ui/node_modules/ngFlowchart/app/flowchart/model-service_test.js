describe('Test the modelservice', function() {
  var flowchartConstants;

  beforeEach(function() {
    module('flowchart', function($provide) {
      $provide.factory('Modelvalidation', function() {return jasmine.createSpyObj('Modelvalidation', ['validateModel', 'validateNodes', 'validateNode', 'validateEdges', 'validateEdge', 'validateConnector'])});
    });
    module('flowchart');
  });

  function getNewModelservice(namespace) {
    namespace.model = angular.copy(namespace.MODEL);
    namespace.selectedObjects = angular.copy(namespace.SELECTEDOBJECTS);
    namespace.modelservice = namespace.Modelfactory(namespace.model, namespace.selectedObjects, namespace.addedEdgeCallback || angular.noop);
  }

  beforeEach(inject(function(Modelfactory, Modelvalidation,  _flowchartConstants_) {
    flowchartConstants = _flowchartConstants_;
    this.Modelvalidation = Modelvalidation;
    this.Modelfactory = Modelfactory;
    this.MODEL = {
      nodes: [
        {
          id: 1, name: 'testNode', x: 0, y: 0,
          connectors: [{id: 1, type: flowchartConstants.rightConnectorType},
            {id: 4, type: flowchartConstants.rightConnectorType}]
        },
        {
          id: 2, name: 'testNode', x: 100, y: 100,
          connectors: [{id: 2, type: flowchartConstants.rightConnectorType},
            {id: 3, type: flowchartConstants.leftConnectorType}]
        },
        {
          id: 3, name: 'testNode3', x: 200, y: 200, connectors: []
        }
      ],
      edges: [{source: 1, destination: 2},
        {source: 1, destination: 3}]
    };
    this.SELECTEDOBJECTS = [];
    getNewModelservice(this);
  }));

  it('selectAll should select everything, but just once', function() {
    var that = this;

    this.modelservice.selectAll();
    expect(this.selectedObjects.length).toEqual(this.MODEL.nodes.length + this.MODEL.edges.length);
    angular.forEach(this.MODEL.nodes, function(node) {
      expect(that.selectedObjects).toContain(jasmine.objectContaining(node));
    });
    angular.forEach(this.MODEL.edges, function(edge) {
      expect(that.selectedObjects).toContain(jasmine.objectContaining(edge));
    });
  });

  it('deselectAll should deselect everything', function() {
    this.modelservice.nodes.select(this.model.nodes[0]);
    expect(this.selectedObjects.length).toEqual(1);
    this.modelservice.deselectAll();
    expect(this.selectedObjects.length).toEqual(0);

    this.modelservice.selectAll();
    expect(this.selectedObjects.length).toEqual(this.MODEL.nodes.length + this.MODEL.edges.length);
    this.modelservice.deselectAll();
    expect(this.selectedObjects.length).toEqual(0);
  });


  it('deleteSelected should delete selected elements, should not remove other things', function() {
    this.modelservice.deleteSelected();
    expect(this.model).toEqual(this.MODEL);

    this.modelservice.edges.select(this.model.edges[0]);
    this.modelservice.deleteSelected();

    expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 1);
    expect(this.model.edges[0]).toEqual(this.MODEL.edges[1]);
    expect(this.model.nodes).toEqual(this.MODEL.nodes);

    this.modelservice.nodes.select(this.model.nodes[0]);
    this.modelservice.deleteSelected();

    expect(this.model.nodes.length).toEqual(this.MODEL.nodes.length - 1);
    expect(this.model.nodes[0]).toEqual(this.MODEL.nodes[1]);
    expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 2);

  });

  it('should store the canvashtml element', function() {
    expect(this.modelservice.getCanvasHtmlElement()).toBeNull();

    var object = {};
    this.modelservice.setCanvasHtmlElement(object);
    expect(this.modelservice.getCanvasHtmlElement()).toBe(object);
  });

  describe('testing modelservice.connectors', function() {
    it('should calculate the coords relative to the canvas', function() {
      var connectorId = this.model.nodes[0].connectors[0].id;
      // Returns x = 0, y = 0 if one element is not set.
      expect(this.modelservice.connectors.getCoord(connectorId)).toEqual({x: 0, y: 0});

      var canvasHtmlElementMock = jasmine.createSpyObj('canvas element', ['getBoundingClientRect']);
      var connectorHtmlElementMock = jasmine.createSpyObj('canvas element', ['getBoundingClientRect']);
      canvasHtmlElementMock.getBoundingClientRect.and.returnValue({top: 100, left: 100});
      connectorHtmlElementMock.getBoundingClientRect.and.returnValue({top: 150, left: 150});
      connectorHtmlElementMock.offsetWidth = 50;
      connectorHtmlElementMock.offsetHeight = 50;

      this.modelservice.setCanvasHtmlElement(canvasHtmlElementMock);
      expect(this.modelservice.connectors.getCoord(connectorId)).toEqual({x: 0, y: 0});

      this.modelservice.connectors.setHtmlElement(connectorId, connectorHtmlElementMock);
      expect(this.modelservice.connectors.getCoord(connectorId)).toEqual({x: 50, y: 50});
      expect(this.modelservice.connectors.getCenteredCoord(connectorId)).toEqual({x: 75, y: 75});
    });
  });

  describe('testing modelservice.nodes', function() {
    it('_addNode', function() {
      var that = this;
      var newNode = {id: 3, name: 'testnode', x: 0, y: 0, connectors: []};

      this.Modelvalidation.validateNodes.and.throwError(new Error('Test'));
      expect(function() {that.modelservice.nodes._addNode(angular.copy(newNode));}).toThrowError('Test');
      expect(this.model.nodes).not.toContain(newNode);
      this.Modelvalidation.validateNodes.and.stub();

      this.modelservice.nodes._addNode(angular.copy(newNode));
      expect(this.model.nodes).toContain(jasmine.objectContaining(newNode));
    });

    it('_addConnector', function() {
      var that = this;
      var newConnector = {id: 10, type: flowchartConstants.leftConnectorType};

      this.Modelvalidation.validateNode.and.throwError(new Error('Test'));
      expect(function() {that.modelservice.nodes._addConnector(that.model.nodes[0], angular.copy(newConnector));}).toThrowError('Test');
      expect(this.model.nodes[0].connectors).not.toContain(newConnector);

      this.Modelvalidation.validateNode.and.stub();
      this.modelservice.nodes._addConnector(this.model.nodes[0], angular.copy(newConnector));
      expect(this.model.nodes[0].connectors).toContain(jasmine.objectContaining(newConnector));
    });

    it('when the node is clicked, it should be selected and all others deselected. If controll was pressed the node selectionstatus should be toggled and the other selected should not change', function() {
      this.modelservice.nodes.select(this.model.nodes[1]);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[1]));
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.nodes.handleClicked(this.model.nodes[0], false);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[0]));
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.nodes.select(this.model.nodes[1]);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[0]));
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[1]));
      expect(this.selectedObjects.length).toEqual(2);

      this.modelservice.nodes.handleClicked(this.model.nodes[0], true);
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(false);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[1]));
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.nodes.handleClicked(this.model.nodes[0], true);
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(true);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[0]));
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.nodes[1]));
      expect(this.selectedObjects.length).toEqual(2);
    });

    it('delete should deselect and delete a node', function() {
      expect(this.modelservice.nodes.delete).toThrow(); // The same as delete(undefined)

      this.modelservice.nodes.delete(this.model.nodes[0]);
      expect(this.model.nodes.length).toEqual(this.MODEL.nodes.length - 1);
      expect(this.model.nodes[0]).toEqual(this.MODEL.nodes[1]);

      this.modelservice.nodes.select(this.model.nodes[0]);
      expect(this.selectedObjects.length).toEqual(1);
      this.modelservice.nodes.delete(this.model.nodes[0]);
      expect(this.model.nodes.length).toEqual(this.MODEL.nodes.length - 2);
      expect(this.selectedObjects.length).toEqual(0);
    });

    it('delete should delete all connected edges', function() {
      this.modelservice.nodes.delete(this.model.nodes[2]); // No edges
      expect(this.model.edges).toEqual(this.MODEL.edges);
      expect(this.model.nodes.length).toEqual(this.MODEL.nodes.length - 1);
      expect(this.model.nodes.length).not.toContain(jasmine.objectContaining(this.MODEL.nodes[2]));


      this.modelservice.nodes.delete(this.model.nodes[0]); // Deleting node with out edges.
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 2);

      getNewModelservice(this);
      this.modelservice.nodes.delete(this.model.nodes[1]); // Deleting node with inedges
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 2);
    });

    it('should filter connector by types', function() {
      expect(this.modelservice.nodes.getConnectorsByType(this.model.nodes[1], flowchartConstants.rightConnectorType)).toEqual([this.MODEL.nodes[1].connectors[0]]);
      expect(this.modelservice.nodes.getConnectorsByType(this.model.nodes[0], flowchartConstants.leftConnectorType)).toEqual([]);
    });

    it('getSelectedNodes should return selected nodes', function() {
      expect(this.modelservice.nodes.getSelectedNodes()).toEqual([]);

      this.modelservice.nodes.select(this.model.nodes[0]);
      var selectedNodes = this.modelservice.nodes.getSelectedNodes();
      expect(selectedNodes.length).toEqual(1);
      expect(selectedNodes).toContain(jasmine.objectContaining(this.MODEL.nodes[0]));

      this.modelservice.nodes.select(this.model.nodes[1]);
      selectedNodes = this.modelservice.nodes.getSelectedNodes();
      expect(selectedNodes.length).toEqual(2);
      expect(selectedNodes).toContain(jasmine.objectContaining(this.MODEL.nodes[0]));
      expect(selectedNodes).toContain(jasmine.objectContaining(this.MODEL.nodes[1]));
    });

    it('should select, deselect and toggle selection of nodes', function() {
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(false);

      this.modelservice.nodes.select(this.model.nodes[0]);
      this.modelservice.nodes.select(this.model.nodes[0]); // Double call, nothing should happen.
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(true);
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.nodes.deselect(this.model.nodes[0]);
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(false);
      expect(this.selectedObjects.length).toEqual(0);

      this.modelservice.nodes.toggleSelected(this.model.nodes[0]);
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(true);
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.nodes.toggleSelected(this.model.nodes[0]);
      expect(this.modelservice.nodes.isSelected(this.model.nodes[0])).toBe(false);
      expect(this.selectedObjects.length).toEqual(0);
    });
  });

  describe('testing modelservice.edges', function() {
    it('delete should delete an edge', function() {
      this.modelservice.edges.delete(this.model.edges[0]);
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 1);
      expect(this.model.edges[0]).toEqual(this.MODEL.edges[1]);

      this.modelservice.edges.select(this.model.edges[0]);
      expect(this.selectedObjects.length).toEqual(1);
      this.modelservice.edges.delete(this.model.edges[0]);
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length - 2);
      expect(this.selectedObjects.length).toEqual(0);
    });

    it('getSelectedEdges should return selected nodes', function() {
      expect(this.modelservice.edges.getSelectedEdges()).toEqual([]);

      this.modelservice.edges.select(this.model.edges[0]);
      expect(this.modelservice.edges.getSelectedEdges()).toEqual([this.MODEL.edges[0]]);

      this.modelservice.edges.select(this.model.edges[1]);
      expect(this.modelservice.edges.getSelectedEdges()).toEqual(this.MODEL.edges);
    });

    it('should select, deselect and toggle selection of nodes', function() {
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(false);

      this.modelservice.edges.select(this.model.edges[0]);
      this.modelservice.edges.select(this.model.edges[0]); // Double call everything should be fine.
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(true);
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.edges.deselect(this.model.edges[0]);
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(false);
      expect(this.selectedObjects.length).toEqual(0);


      this.modelservice.edges.toggleSelected(this.model.edges[0]);
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(true);
      expect(this.selectedObjects.length).toEqual(1);


      this.modelservice.edges.toggleSelected(this.model.edges[0]);
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(false);
      expect(this.selectedObjects.length).toEqual(0);
    });

    it('when the edge is clicked, it should be selected and all others deselected. If controll was pressed the selectionstatus should be toggled and the other selected should not change', function() {
      this.modelservice.edges.select(this.model.edges[1]);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.edges[1]));
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.edges.handleEdgeMouseClick(this.model.edges[0], false);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.edges[0]));
      expect(this.selectedObjects.length).toEqual(1);

      this.modelservice.edges.select(this.model.edges[1]);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.edges[0]));
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.edges[1]));
      expect(this.selectedObjects.length).toEqual(2);

      this.modelservice.edges.handleEdgeMouseClick(this.model.edges[0], true);
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(false);
      expect(this.selectedObjects).toContain(jasmine.objectContaining(this.MODEL.edges[1]));

      this.modelservice.edges.handleEdgeMouseClick(this.model.edges[0], true);
      expect(this.modelservice.edges.isSelected(this.model.edges[0])).toBe(true);
      expect(this.selectedObjects.length).toEqual(2);
    });

    it('_addEdge', function() {
      var that = this;

      this.Modelvalidation.validateEdges.and.throwError(new Error('Test'));
      expect(function() {that.modelservice.edges._addEdge(that.model.nodes[0].connectors[1], that.model.nodes[1].connectors[0])}).toThrowError('Test');
      this.Modelvalidation.validateEdges.and.stub();


      this.modelservice.edges._addEdge(this.model.nodes[0].connectors[1], this.model.nodes[1].connectors[0]);
      expect(this.model.edges).toContain(jasmine.objectContaining({
        source: this.MODEL.nodes[0].connectors[0].id,
        destination: this.MODEL.nodes[1].connectors[0].id
      }));
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length + 1);

      this.addedEdgeCallback = jasmine.createSpy('addedEdgeCallback');
      getNewModelservice(this);
      this.modelservice
    });

    it('_addEdge with callback', function() {
      this.addedEdgeCallback = jasmine.createSpy('addedEdgeCallback');
      getNewModelservice(this);

      this.modelservice.edges._addEdge(this.model.nodes[0].connectors[1], this.model.nodes[1].connectors[0]);
      expect(this.model.edges).toContain(jasmine.objectContaining({
        source: this.MODEL.nodes[0].connectors[0].id,
        destination: this.MODEL.nodes[1].connectors[0].id
      }));
      expect(this.model.edges.length).toEqual(this.MODEL.edges.length + 1);
      expect(this.addedEdgeCallback).toHaveBeenCalled();
    });

    describe('tests with an empty model', function() {
      beforeEach(function() {
        this.MODEL = {
          nodes: [],
          edges: []
        };
        getNewModelservice(this);
      });

      it('model should be empty', function() {
        expect(this.model.nodes.length).toEqual(0);
        expect(this.model.edges.length).toEqual(0);
      });

      it('selectAll should select nothing', function() {
        this.modelservice.selectAll();
        expect(this.selectedObjects.length).toEqual(0);
      });

      it('deselectAll should deselect nothing', function() {
        this.modelservice.selectAll();
        this.modelservice.deselectAll();
        expect(this.selectedObjects.length).toEqual(0);
      });

    });

  });
});
