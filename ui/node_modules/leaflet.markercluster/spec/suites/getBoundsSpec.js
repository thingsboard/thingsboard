describe('getBounds', function() {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var map, div;

	beforeEach(function() {
		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);

		map = L.map(div, { maxZoom: 18, trackResize: false });

		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	});
	afterEach(function() {
		map.remove();
		document.body.removeChild(div);

		map = div = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	describe('polygon layer', function() {
		it('returns the correct bounds before adding to the map', function() {
			var group = new L.MarkerClusterGroup();
			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);

			expect(group.getBounds().equals(polygon.getBounds())).to.be(true);
		});

		it('returns the correct bounds after adding to the map after adding polygon', function() {
			var group = new L.MarkerClusterGroup();
			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			map.addLayer(group);

			expect(group.getBounds().equals(polygon.getBounds())).to.be(true);
		});

		it('returns the correct bounds after adding to the map before adding polygon', function() {
			var group = new L.MarkerClusterGroup();
			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			map.addLayer(group);
			group.addLayer(polygon);

			expect(group.getBounds().equals(polygon.getBounds())).to.be(true);
		});
	});

	describe('marker layers', function () {
		it('returns the correct bounds before adding to the map', function () {
			var group = new L.MarkerClusterGroup();
			var marker = new L.Marker([1.5, 1.5]);
			var marker2 = new L.Marker([1.0, 5.0]);
			var marker3 = new L.Marker([6.0, 2.0]);

			group.addLayers([marker, marker2, marker3]);

			expect(group.getBounds().equals(L.latLngBounds([1.0, 5.0], [6.0, 1.5]))).to.be(true);
		});

		it('returns the correct bounds after adding to the map after adding markers', function () {
			var group = new L.MarkerClusterGroup();
			var marker = new L.Marker([1.5, 1.5]);
			var marker2 = new L.Marker([1.0, 5.0]);
			var marker3 = new L.Marker([6.0, 2.0]);

			group.addLayers([marker, marker2, marker3]);
			map.addLayer(group);

			expect(group.getBounds().equals(L.latLngBounds([1.0, 5.0], [6.0, 1.5]))).to.be(true);
		});

		it('returns the correct bounds after adding to the map before adding markers', function () {
			var group = new L.MarkerClusterGroup();
			var marker = new L.Marker([1.5, 1.5]);
			var marker2 = new L.Marker([1.0, 5.0]);
			var marker3 = new L.Marker([6.0, 2.0]);

			map.addLayer(group);
			group.addLayers([marker, marker2, marker3]);

			expect(group.getBounds().equals(L.latLngBounds([1.0, 5.0], [6.0, 1.5]))).to.be(true);
		});
	});

	describe('marker and polygon layers', function() {
		it('returns the correct bounds before adding to the map', function() {
			var group = new L.MarkerClusterGroup();
			var marker = new L.Marker([6.0, 3.0]);
			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayers([marker, polygon]);

			expect(group.getBounds().equals(L.latLngBounds([1.5, 1.5], [6.0, 3.0]))).to.be(true);
		});

		it('returns the correct bounds after adding to the map', function () {
			var group = new L.MarkerClusterGroup();
			var marker = new L.Marker([6.0, 3.0]);
			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			map.addLayer(group);
			group.addLayers([marker, polygon]);

			expect(group.getBounds().equals(L.latLngBounds([1.5, 1.5], [6.0, 3.0]))).to.be(true);
		});
	});

	describe('blank layer', function () {
		it('returns a blank bounds', function () {
			var group = new L.MarkerClusterGroup();

			expect(group.getBounds().isValid()).to.be(false);
		});
	});
});