describe('addLayer adding a single marker', function () {
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
	it('appears when added to the group before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		map.addLayer(group);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('appears when added to the group after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('appears (using animations) when added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup({ animateAddingMarkers: true });

		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('does not appear when too far away when added before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([3.5, 1.5]);

		group.addLayer(marker);
		map.addLayer(group);

		expect(marker._icon).to.be(undefined);
	});

	it('does not appear when too far away when added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([3.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		expect(marker._icon).to.be(undefined);
	});

	it('passes control to addLayers when marker is a Layer Group', function () {

		group = new L.MarkerClusterGroup();

		var marker1 = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var layerGroup = new L.LayerGroup([marker1, marker2]);

		map.addLayer(group);
		group.addLayer(layerGroup);

		expect(group._topClusterLevel.getChildCount()).to.equal(2);

		expect(marker1._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);
	});
});
