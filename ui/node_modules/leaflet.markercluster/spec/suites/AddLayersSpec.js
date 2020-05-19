describe('addLayers adding multiple markers', function () {
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
	it('creates a cluster when 2 overlapping markers are added before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);
	});

	it('creates a cluster when 2 overlapping markers are added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayers([marker, marker2]);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);
	});

	it('creates a cluster and marker when 2 overlapping markers and one non-overlapping are added before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var marker3 = new L.Marker([3.0, 1.5]);

		group.addLayers([marker, marker2, marker3]);
		map.addLayer(group);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);
		expect(marker3._icon.parentNode).to.be(map._panes.markerPane);

		expect(map._panes.markerPane.childNodes.length).to.be(2);
	});

	it('creates a cluster and marker when 2 overlapping markers and one non-overlapping are added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var marker3 = new L.Marker([3.0, 1.5]);

		map.addLayer(group);
		group.addLayers([marker, marker2, marker3]);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);
		expect(marker3._icon.parentNode).to.be(map._panes.markerPane);

		expect(map._panes.markerPane.childNodes.length).to.be(2);
	});

	it('handles nested Layer Groups', function () {

		group = new L.MarkerClusterGroup();

		var marker1 = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var marker3 = new L.Marker([3.0, 1.5]);
		var layerGroup = new L.LayerGroup([marker1, new L.LayerGroup([marker2])]);

		map.addLayer(group);
		group.addLayers([layerGroup, marker3]);

		expect(marker1._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);
		expect(marker3._icon.parentNode).to.be(map._panes.markerPane);

		expect(map._panes.markerPane.childNodes.length).to.be(2);
	});
});
