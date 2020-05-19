describe('adding non point data works', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var div, map, group;

	beforeEach(function () {
		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);
	
		map = L.map(div, { maxZoom: 18, trackResize: false });
	
		// Corresponds to zoom level 8 for the above div dimensions.
		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	});

	afterEach(function () {
		if (group instanceof L.MarkerClusterGroup) {
			group.clearLayers();
			map.removeLayer(group);
		}

		map.remove();
		div.remove()

		div = map = group = null;
	});


	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('Allows adding a polygon before via addLayer', function () {

		group = new L.MarkerClusterGroup();

		var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0,2.0], [1.5, 2.0]]);

		group.addLayer(polygon);
		map.addLayer(group);

		// Leaflet 1.0.0 now uses an intermediate L.Renderer.
		// polygon > _path > _rootGroup (g) > _container (svg) > pane (div)
		expect(polygon._path).to.not.be(undefined);
		expect(polygon._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));

		expect(group.hasLayer(polygon));
	});

	it('Allows adding a polygon before via addLayers([])', function () {

		group = new L.MarkerClusterGroup();

		var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

		group.addLayers([polygon]);
		map.addLayer(group);

		expect(polygon._path).to.not.be(undefined);
		expect(polygon._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
	});

	it('Removes polygons from map when removed', function () {

		group = new L.MarkerClusterGroup();

		var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

		group.addLayer(polygon);
		map.addLayer(group);
		map.removeLayer(group);

		expect(polygon._path.parentNode).to.be(null);
	});

	describe('hasLayer', function () {

		it('returns false when not added', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			expect(group.hasLayer(polygon)).to.be(false);

			map.addLayer(group);

			expect(group.hasLayer(polygon)).to.be(false);

			map.addLayer(polygon);

			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('returns true before adding to map', function() {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayers([polygon]);

			expect(group.hasLayer(polygon)).to.be(true);
		});

		it('returns true after adding to map after adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			map.addLayer(group);

			expect(group.hasLayer(polygon)).to.be(true);
		});

		it('returns true after adding to map before adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			map.addLayer(group);
			group.addLayer(polygon);

			expect(group.hasLayer(polygon)).to.be(true);
		});

	});

	describe('removeLayer', function() {

		it('removes before adding to map', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes before adding to map', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes after adding to map after adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			map.addLayer(group);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes after adding to map before adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			map.addLayer(group);
			group.addLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(false);
		});

	});

	describe('removeLayers', function () {

		it('removes before adding to map', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes before adding to map', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes after adding to map after adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.addLayer(polygon);
			map.addLayer(group);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(false);
		});

		it('removes after adding to map before adding polygon', function () {
			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			map.addLayer(group);
			group.addLayer(polygon);
			expect(group.hasLayer(polygon)).to.be(true);

			group.removeLayers([polygon]);
			expect(group.hasLayer(polygon)).to.be(false);
		});

	});
});