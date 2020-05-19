describe('removeLayer', function () {
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
	it('removes a layer that was added to it', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);

		group.addLayer(marker);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);

		group.removeLayer(marker);

		expect(marker._icon).to.be(null);
	});

	it('doesnt remove a layer not added to it', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);

		map.addLayer(marker);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);

		group.removeLayer(marker);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('removes a layer that was added to it (before being on the map) that is shown in a cluster', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		group.removeLayer(marker);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('removes a layer that was added to it (after being on the map) that is shown in a cluster', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		group.removeLayer(marker);

		expect(marker._icon).to.be(null);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('removes a layer that was added to it (before being on the map) that is individually', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1, 1.5]);
		var marker2 = new L.Marker([3, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);

		group.removeLayer(marker);

		expect(marker._icon).to.be(null);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('removes a layer (with animation) that was added to it (after being on the map) that is shown in a cluster', function () {

		group = new L.MarkerClusterGroup({ animateAddingMarkers: true });

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		//Run the the animation
		clock.tick(1000);

		expect(marker._icon).to.be(null);
		expect(marker2._icon).to.be(null);

		group.removeLayer(marker);

		//Run the the animation
		clock.tick(1000);

		expect(marker._icon).to.be(null);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('removes the layers that are in the given LayerGroup', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayers([marker, marker2]);

		var layer = L.layerGroup();
		layer.addLayer(marker2);
		group.removeLayer(layer);

		expect(marker._icon).to.not.be(undefined);
		expect(marker2._icon).to.be(undefined);
	});

	it('removes the layers that are in the given LayerGroup when not on the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);

		var layer = L.layerGroup();
		layer.addLayer(marker2);
		group.removeLayer(layer);

		expect(group.hasLayer(marker)).to.be(true);
		expect(group.hasLayer(marker2)).to.be(false);
	});

	it('passes control to removeLayers when marker is a Layer Group', function () {

		group = new L.MarkerClusterGroup();

		var marker1 = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker1, marker2]);

		var layer = L.layerGroup();
		layer.addLayer(marker2);
		group.removeLayer(new L.LayerGroup([layer]));

		expect(group.hasLayer(marker1)).to.be(true);
		expect(group.hasLayer(marker2)).to.be(false);
	});
});
