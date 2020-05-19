describe('The topsort service', function() {

  beforeEach(module('flowchart'));

  beforeEach(inject(function(Topsortservice) {
    this.Topsortservice = Topsortservice;
  }));

  it('should find direct circles', function() {
    var that = this;
    var circularModel = {
      nodes: [
        {id: 1, name: '', x: 0, y: 0, connectors: [{id: 1, type: ''}, {id: 2, type: ''}]},
        {id: 2, name: '', x: 0, y: 0, connectors: [{id: 3, type: ''}]}
      ],
      edges: [
        {source: 1, destination: 3},
        {source: 3, destination: 2}
      ]
    };
    expect(that.Topsortservice(circularModel)).toBeNull();
  });

  it('should find indirect circles', function() {
    var that = this;

    var circularModel = {
      nodes: [
        {id: 1, name: '', x: 0, y: 0, connectors: [{id: 1, type: ''}, {id: 2, type: ''}]},
        {id: 2, name: '', x: 0, y: 0, connectors: [{id: 3, type: ''}]},
        {id: 3, name: '', x: 0, y: 0, connectors: [{id: 4, type: ''}]}
      ],
      edges: [
        {source: 1, destination: 3},
        {source: 3, destination: 4},
        {source: 4, destination: 1}
      ]
    };

    expect(that.Topsortservice(circularModel)).toBeNull();
  });

  it('should find circles in graph with source node', function() {
    var that = this;

    var circularModel = {
      nodes: [
        {id: 1, name: '', x: 0, y: 0, connectors: [{id: 1, type: ''}, {id: 2, type: ''}]},
        {id: 2, name: '', x: 0, y: 0, connectors: [{id: 3, type: ''}]},
        {id: 3, name: '', x: 0, y: 0, connectors: [{id: 4, type: ''}]},
        {id: 4, name: '', x: 0, y: 0, connectors: [{id: 5, type: ''}]},
        {id: 5, name: '', x: 0, y: 0, connectors: [{id: 6, type: ''}]},
        {id: 6, name: '', x: 0, y: 0, connectors: [{id: 7, type: ''}]}
      ],
      edges: [
        {source: 1, destination: 3},
        {source: 3, destination: 4},
        {source: 4, destination: 1},
        {source: 5, destination: 1},
        {source: 6, destination: 5},
        {source: 7, destination: 1}
      ]
    };

    expect(that.Topsortservice(circularModel)).toBe(null);
  });

  it('should work on empty graphs', function() {
    expect(this.Topsortservice({nodes: [], edges: []})).toEqual([]);
  });

  it('should work on unconnected graphs', function() {
    var noEdgesModel = {
      nodes: [{id: 1, name: '', x: 0, y: 0, connectors: [{id: 1, type: ''}]},
        {id: 2, name: '', x: 0, y: 0, connectors: [{id: 2, type: ''}]}
      ],
      edges: []
    };
    var orderedNodes = this.Topsortservice(noEdgesModel);
    expect(orderedNodes).toContain('1');
    expect(orderedNodes).toContain('2');
  });

  it('should sort easy graphes correct', function() {
    var noEdgesModel = {
      nodes: [{id: 1, name: '', x: 0, y: 0, connectors: [{id: 1, type: ''}]},
        {id: 2, name: '', x: 0, y: 0, connectors: [{id: 2, type: ''}]}
      ],
      edges: [{source: 1, destination: 2}]
    };
    var orderedNodes = this.Topsortservice(noEdgesModel);
    expect(orderedNodes).toEqual(['1', '2']);
  });

});
