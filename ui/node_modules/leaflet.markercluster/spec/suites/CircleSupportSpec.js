describe('support for Circle elements', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var clock, div, map, group;

	beforeEach(function () {
		clock = sinon.useFakeTimers();

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
		clock.restore();

		clock = div = map = group = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('appears when added to the group before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 200);

		group.addLayer(marker);
		map.addLayer(group);

		// Leaflet 1.0.0 now uses an intermediate L.Renderer.
		// marker > _path > _rootGroup (g) > _container (svg) > pane (div)
		expect(marker._path.parentNode).to.not.be(undefined);
		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));

		clock.tick(1000);
	});

	it('appears when added to the group after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 200);

		group.addLayer(marker);
		map.addLayer(group);

		expect(marker._path.parentNode).to.not.be(undefined);
		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));

		clock.tick(1000);
	});

	it('appears animated when added to the group after the group is added to the map', function () {

		group = new L.MarkerClusterGroup({ animateAddingMarkers: true });

		var marker = new L.Circle([1.5, 1.5], 200);
		var marker2 = new L.Circle([1.5, 1.5], 200);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
		expect(marker2._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));

		clock.tick(1000);
	});

	it('creates a cluster when 2 overlapping markers are added before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 200);
		var marker2 = new L.Circle([1.5, 1.5], 200);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		expect(marker._path).to.be(undefined);
		expect(marker2._path).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);

		clock.tick(1000);
	});

	it('creates a cluster when 2 overlapping markers are added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 200);
		var marker2 = new L.Circle([1.5, 1.5], 200);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		expect(marker._path.parentNode).to.be(null); //Removed then re-added, so null
		expect(marker2._path).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);

		clock.tick(1000);
	});

	it('disappears when removed from the group', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 200);

		group.addLayer(marker);
		map.addLayer(group);

		expect(marker._path.parentNode).to.not.be(undefined);
		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));

		group.removeLayer(marker);

		expect(marker._path.parentNode).to.be(null);

		clock.tick(1000);
	});
});