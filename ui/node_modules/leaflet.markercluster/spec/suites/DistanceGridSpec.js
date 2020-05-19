describe('distance grid', function () {
	it('addObject', function () {
		var grid = new L.DistanceGrid(100),
		    obj = {};

		expect(grid.addObject(obj, { x: 0, y: 0 })).to.eql(undefined);
		expect(grid.removeObject(obj, { x: 0, y: 0 })).to.eql(true);
	});

	it('eachObject', function (done) {
		var grid = new L.DistanceGrid(100),
		    obj = {};

		expect(grid.addObject(obj, { x: 0, y: 0 })).to.eql(undefined);

		grid.eachObject(function(o) {
			expect(o).to.eql(obj);
			done();
		});
	});

	it('getNearObject', function () {
		var grid = new L.DistanceGrid(100),
			obj = {};

		grid.addObject(obj, { x: 0, y: 0 });

		expect(grid.getNearObject({ x: 50, y: 50 })).to.equal(obj);
		expect(grid.getNearObject({ x: 100, y: 0 })).to.equal(obj);
	});

	it('getNearObject with cellSize 0', function () {
		var grid = new L.DistanceGrid(0),
			obj = {};

		grid.addObject(obj, { x: 0, y: 0 });

		expect(grid.getNearObject({ x: 50, y: 50 })).to.equal(null);
		expect(grid.getNearObject({ x: 0, y: 0 })).to.equal(obj);
	});
});
