describe('unspiderfy', function () {
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
		if (group instanceof L.MarkerClusterGroup) {
			group.removeLayers(group.getLayers());
			map.removeLayer(group);
		}
		map.remove();
		div.remove();

		clock.restore();

		div = map = group = clock = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('Unspiderfies 2 Markers', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		group.unspiderfy();

		clock.tick(1000);

		expect(map.hasLayer(marker)).to.be(false);
		expect(map.hasLayer(marker2)).to.be(false);
	});

	it('Unspiderfies 2 CircleMarkers', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.CircleMarker([1.5, 1.5]);
		var marker2 = new L.CircleMarker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		group.unspiderfy();

		clock.tick(1000);

		expect(map.hasLayer(marker)).to.be(false);
		expect(map.hasLayer(marker2)).to.be(false);
	});

	it('Unspiderfies 2 Circles', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 10);
		var marker2 = new L.Circle([1.5, 1.5], 10);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		group.unspiderfy();

		clock.tick(1000);

		expect(map.hasLayer(marker)).to.be(false);
		expect(map.hasLayer(marker2)).to.be(false);
	});

	it('fires unspiderfied event on unspiderfy', function (done) {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		// Add event listener
		group.on('unspiderfied', function (event) {
			expect(event.target).to.be(group);
			expect(event.cluster).to.be.a(L.Marker);
			expect(event.markers[1]).to.be(marker);
			expect(event.markers[0]).to.be(marker2);

			done();
		});

		group.unspiderfy();

		clock.tick(1000);

	});

});