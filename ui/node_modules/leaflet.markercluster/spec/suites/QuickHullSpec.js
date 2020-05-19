describe('quickhull', function () {
	describe('getDistant', function () {
		it('zero distance', function () {
			var bl = [
				{ lat: 0, lng: 0 },
				{ lat: 0, lng: 10 }
			];
			expect(L.QuickHull.getDistant({ lat: 0, lng: 0 }, bl)).to.eql(0);
		});
		it('non-zero distance', function () {
			var bl = [
				{ lat: 0, lng: 0 },
				{ lat: 0, lng: 10 }
			];
			expect(L.QuickHull.getDistant({ lat: 5, lng: 5 }, bl)).to.eql(-50);
		});
	});

	describe('getConvexHull', function () {
		it('creates a hull', function () {
			expect(L.QuickHull.getConvexHull([	{ lat: 0, lng: 0 },
								{ lat: 10, lng: 0 },
								{ lat: 10, lng: 10 },
								{ lat: 0, lng: 10 },
								{ lat: 5, lng: 5 }
							 ])).to.eql([
							 	{ lat: 0, lng: 10 },
							 	{ lat: 10, lng: 10 },
							 	{ lat: 10, lng: 0 },
							 	{ lat: 0, lng: 0 }
							 ]);
		});
		it('creates a hull for vertically-aligned objects', function () {
			expect(L.QuickHull.getConvexHull([	{ lat: 0, lng: 0 },
								{ lat: 5, lng: 0 },
								{ lat: 10, lng: 0 }
							 ])).to.eql([
							 	{ lat: 0, lng: 0 },
							 	{ lat: 10, lng: 0 }
							 ]);
		});
		it('creates a hull for horizontally-aligned objects', function () {
			expect(L.QuickHull.getConvexHull([	{ lat: 0, lng: 0 },
								{ lat: 0, lng: 5 },
								{ lat: 0, lng: 10 }
							 ])).to.eql([
							 	{ lat: 0, lng: 0 },
							 	{ lat: 0, lng: 10 }
							 ]);
		});
	});
});
