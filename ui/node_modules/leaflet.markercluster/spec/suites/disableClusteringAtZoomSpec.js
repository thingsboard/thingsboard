describe('disableClusteringAtZoom option', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var div, map, group, clock;

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
		group.clearLayers();
		map.removeLayer(group);
		map.remove();
		div.remove();
		clock.restore();
		
		div, map, group, clock = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('unclusters at zoom level equal or higher', function () {

		var maxZoom = 15;

		group = new L.MarkerClusterGroup({
			disableClusteringAtZoom: maxZoom
		});

		group.addLayers([
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5])
		]);
		map.addLayer(group);

		expect(group._maxZoom).to.equal(maxZoom - 1);

		expect(map._panes.markerPane.childNodes.length).to.equal(1); // 1 cluster.

		map.setZoom(14);
		clock.tick(1000);
		expect(map._panes.markerPane.childNodes.length).to.equal(1); // 1 cluster.

		map.setZoom(15);
		clock.tick(1000);
		expect(map._panes.markerPane.childNodes.length).to.equal(2); // 2 markers.
	});
});
