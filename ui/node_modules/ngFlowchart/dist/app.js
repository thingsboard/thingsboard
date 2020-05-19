angular.module('app', ['flowchart'])
  .factory('prompt', function () {
    return prompt;
  })
  .config(function (NodeTemplatePathProvider) {
    NodeTemplatePathProvider.setTemplatePath("flowchart/node.html");
  })

  .controller('AppCtrl', function AppCtrl($scope, $q, prompt, Modelfactory, flowchartConstants) {

    var deleteKeyCode = 46;
    var ctrlKeyCode = 17;
    var aKeyCode = 65;
    var escKeyCode = 27;
    var nextNodeID = 10;
    var nextConnectorID = 20;
    var ctrlDown = false;

    var nodeTypesModel = {
      nodes: [],
      edges: []
    };

    for (var i=0;i<10;i++) {
      var node = {
        name: "type"+i,
        id: (i+1),
        x: 50,
        y: 100*(i+1),
        connectors: [
          {
            type: flowchartConstants.leftConnectorType,
            id: i*2+1
          },
          {
            type: flowchartConstants.rightConnectorType,
            id: i*2+2
          }
        ]
      };
      nodeTypesModel.nodes.push(node);
    }

    var model = {
      nodes: [
        {
          name: "ngFlowchart",
          readonly: true,
          id: 2,
          x: 400,
          y: 100,
          color: '#000',
          borderColor: '#000',
          connectors: [
            {
              type: flowchartConstants.leftConnectorType,
              id: 1
            },
            {
              type: flowchartConstants.rightConnectorType,
              id: 2
            }
          ]
        },
        {
          name: "Implemented with AngularJS",
          id: 3,
          x: 700,
          y: 100,
          color: '#F15B26',
          connectors: [
            {
              type: flowchartConstants.leftConnectorType,
              id: 3
            },
            {
              type: flowchartConstants.rightConnectorType,
              id: 4
            }
          ]
        },
        {
          name: "Easy Integration",
          id: 4,
          x: 1100,
          y: 100,
          color: '#000',
          borderColor: '#000',
          connectors: [
            {
              type: flowchartConstants.leftConnectorType,
              id: 5
            },
            {
              type: flowchartConstants.rightConnectorType,
              id: 6
            }
          ]
        },
        {
          name: "Customizable templates",
          id: 5,
          x: 1400,
          y: 100,
          color: '#000',
          borderColor: '#000',
          connectors: [
            {
              type: flowchartConstants.leftConnectorType,
              id: 7
            },
            {
              type: flowchartConstants.rightConnectorType,
              id: 8
            }
          ]
        }
      ],
    edges: [
      {
        source: 2,
        destination: 3,
        label: 'label1'
      },
      {
        source: 4,
        destination: 5,
        label: 'label2'
      },
      {
        source: 6,
        destination: 7,
        label: 'label3'
      }
    ]
  };

$scope.flowchartselected = [];
$scope.nodeTypesFlowchartselected = [];

$scope.canvasControl = {};

var modelservice = Modelfactory(model, $scope.flowchartselected);

$scope.model = model;
$scope.nodeTypesModel = nodeTypesModel;
$scope.modelservice = modelservice;

$scope.keyDown = function (evt) {
  if (evt.keyCode === ctrlKeyCode) {
    ctrlDown = true;
    evt.stopPropagation();
    evt.preventDefault();
  }
};

$scope.keyUp = function (evt) {

  if (evt.keyCode === deleteKeyCode) {
    modelservice.deleteSelected();
  }

  if (evt.keyCode == aKeyCode && ctrlDown) {
    modelservice.selectAll();
  }

  if (evt.keyCode == escKeyCode) {
    modelservice.deselectAll();
  }

  if (evt.keyCode === ctrlKeyCode) {
    ctrlDown = false;
    evt.stopPropagation();
    evt.preventDefault();
  }
};

$scope.addNewNode = function () {
  var nodeName = prompt("Enter a node name:", "New node");
  if (!nodeName) {
    return;
  }

  var newNode = {
    name: nodeName,
    id: nextNodeID++,
    x: 200,
    y: 100,
    color: '#F15B26',
    connectors: [
      {
        id: nextConnectorID++,
        type: flowchartConstants.leftConnectorType
      },
      {
        id: nextConnectorID++,
        type: flowchartConstants.rightConnectorType
      }
    ]
  };

  model.nodes.push(newNode);
};

$scope.activateWorkflow = function() {
  angular.forEach($scope.model.edges, function(edge) {
    edge.active = !edge.active;
  });
};

$scope.addNewInputConnector = function () {
  var connectorName = prompt("Enter a connector name:", "New connector");
  if (!connectorName) {
    return;
  }

  var selectedNodes = modelservice.nodes.getSelectedNodes($scope.model);
  for (var i = 0; i < selectedNodes.length; ++i) {
    var node = selectedNodes[i];
    node.connectors.push({id: nextConnectorID++, type: flowchartConstants.leftConnectorType});
  }
};

$scope.addNewOutputConnector = function () {
  var connectorName = prompt("Enter a connector name:", "New connector");
  if (!connectorName) {
    return;
  }

  var selectedNodes = modelservice.nodes.getSelectedNodes($scope.model);
  for (var i = 0; i < selectedNodes.length; ++i) {
    var node = selectedNodes[i];
    node.connectors.push({id: nextConnectorID++, type: flowchartConstants.rightConnectorType});
  }
};

$scope.deleteSelected = function () {
  modelservice.deleteSelected();
};

/*angular.element('#main-container').bind('contextmenu', function($event) {
  var x = $event.clientX;
  var y = $event.clientY;
  console.log(`${x}, ${y}`);
  if ($scope.canvasControl.modelservice) {
    var item = $scope.canvasControl.modelservice.getItemInfoAtPoint(x, y);
    console.log(item);
  }
});*/

$scope.callbacks = {
  edgeDoubleClick: function () {
    console.log('Edge double clicked.');
  },
  edgeEdit: function(event, edge) {
    var label = prompt("Enter a link label:", edge.label);
    if (label) {
      edge.label = label;
    }
  },
  edgeMouseOver: function () {
    console.log('mouserover')
  },
  isValidEdge: function (source, destination) {
    return source.type === flowchartConstants.rightConnectorType && destination.type === flowchartConstants.leftConnectorType;
  },
  createEdge: function (event, edge) {
    var deferred = $q.defer();
    if (!edge.label) {
      var label = prompt("Enter a link label:", "New label");
      edge.label = label;
    }
    deferred.resolve(edge);
    return deferred.promise;
  },
  dropNode: function (event, node) {
    var name = prompt("Enter a node name:", node.name);
    if (name) {
      node.name = name;
      node.id = nextNodeID++;
      node.connectors = [
        {
          id: nextConnectorID++,
          type: flowchartConstants.leftConnectorType
        },
        {
          id: nextConnectorID++,
          type: flowchartConstants.rightConnectorType
        }
      ]
      model.nodes.push(node);
    }
  },
  edgeAdded: function (edge) {
    console.log("edge added");
    console.log(edge);
  },
  nodeRemoved: function (node) {
    console.log("node removed");
    console.log(node);
  },
  edgeRemoved: function (edge) {
    console.log("edge removed");
    console.log(edge);
  },
  nodeCallbacks: {
    'doubleClick': function (event) {
      console.log('Node was doubleclicked.')
    },
    'click': function (event) {
      console.log('Node was clicked.')
    },
    'nodeEdit': function (event, node) {
      var name = prompt("Enter a node name:", node.name);
      if (name) {
        node.name = name;
      }
    }
  }
};
modelservice.registerCallbacks($scope.callbacks.edgeAdded, $scope.callbacks.nodeRemoved, $scope.callbacks.edgeRemoved);

})
;
