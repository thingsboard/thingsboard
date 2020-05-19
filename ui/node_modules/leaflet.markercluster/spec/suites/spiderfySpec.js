describe('spiderfy', function () {
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
	it('Spiderfies 2 Markers', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('Spiderfies 2 CircleMarkers', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.CircleMarker([1.5, 1.5]);
		var marker2 = new L.CircleMarker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		// Leaflet 1.0.0 now uses an intermediate L.Renderer.
		// marker > _path > _rootGroup (g) > _container (svg) > pane (div)
		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
		expect(marker2._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
	});

	it('Spiderfies 2 Circles', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Circle([1.5, 1.5], 10);
		var marker2 = new L.Circle([1.5, 1.5], 10);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		marker.__parent.spiderfy();

		expect(marker._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
		expect(marker2._path.parentNode.parentNode.parentNode).to.be(map.getPane('overlayPane'));
	});

	it('Spiderfies at current zoom if all child markers are at the exact same position', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		// Get the appropriate cluster.
		var cluster = marker.__parent,
		    zoom = map.getZoom();

		while (cluster._zoom !== zoom) {
			cluster = cluster.__parent;
		}

		expect(zoom).to.be.lessThan(10);

		cluster.fireEvent('click', null, true);

		clock.tick(1000);

		expect(map.getZoom()).to.equal(zoom);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);

	});

	it('Spiderfies at current zoom if all child markers are still within a single cluster at map maxZoom', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.50001]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		expect(marker.__parent._zoom).to.equal(18);

		// Get the appropriate cluster.
		var cluster = marker.__parent,
		    zoom = map.getZoom();

		while (cluster._zoom !== zoom) {
			cluster = cluster.__parent;
		}

		expect(zoom).to.be.lessThan(10);

		cluster.fireEvent('click', null, true);

		clock.tick(1000);

		expect(map.getZoom()).to.equal(zoom);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);

	});

	it('removes all markers and spider legs when group is removed from map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.__parent.spiderfy();

		expect(map._panes.markerPane.childNodes.length).to.be(3); // The 2 markers + semi-transparent cluster.
		expect(map.getPane('overlayPane').firstChild.firstChild.childNodes.length).to.be(2); // The 2 spider legs.

	});

	it('adds then removes class "leaflet-cluster-anim" from mapPane on spiderfy', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.__parent.spiderfy();

		expect(map._panes.mapPane.className).to.contain('leaflet-cluster-anim');

		clock.tick(1000);

		expect(map._panes.mapPane.className).to.not.contain('leaflet-cluster-anim');

	});

	it('adds then removes class "leaflet-cluster-anim" from mapPane on unspiderfy', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		marker.__parent.unspiderfy();

		expect(map._panes.mapPane.className).to.contain('leaflet-cluster-anim');

		clock.tick(1000);

		expect(map._panes.mapPane.className).to.not.contain('leaflet-cluster-anim');

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

		marker.__parent.unspiderfy();

		clock.tick(1000);

	});

	it('does not leave class "leaflet-cluster-anim" on mapPane when group is removed while spiderfied', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.__parent.spiderfy();

		clock.tick(1000);

		map.removeLayer(group);

		expect(map._panes.mapPane.className).to.not.contain('leaflet-cluster-anim');

	});

	describe('zoomend event listener', function () {

		it('unspiderfies correctly', function () {

			group = new L.MarkerClusterGroup();

			var marker = new L.Circle([1.5, 1.5], 10);
			var marker2 = new L.Circle([1.5, 1.5], 10);

			group.addLayer(marker);
			group.addLayer(marker2);
			map.addLayer(group);

			marker.__parent.spiderfy();

			expect(group._spiderfied).to.not.be(null);

			map.fire('zoomend');

			//We should unspiderfy with no animation, so this should be null
			expect(group._spiderfied).to.be(null);
		});

	});

	describe('spiderfied event listener', function () {
		it('Spiderfies 2 Markers', function (done) {

			group = new L.MarkerClusterGroup();
			var marker = new L.Marker([1.5, 1.5]);
			var marker2 = new L.Marker([1.5, 1.5]);

			group.addLayer(marker);
			group.addLayer(marker2);
			map.addLayer(group);

			// Add event listener
			group.on('spiderfied', function (event) {
				expect(event.target).to.be(group);
				expect(event.cluster).to.be.a(L.Marker);
				expect(event.markers[1]).to.be(marker);
				expect(event.markers[0]).to.be(marker2);

				done();
			});

			marker.__parent.spiderfy();

			clock.tick(200);
		});

		it('Spiderfies 2 Circles', function (done) {

			group = new L.MarkerClusterGroup();
			var marker = new L.Circle([1.5, 1.5], 10);
			var marker2 = new L.Circle([1.5, 1.5], 10);

			group.addLayer(marker);
			group.addLayer(marker2);
			map.addLayer(group);

			// Add event listener
			group.on('spiderfied', function (event) {
				expect(event.target).to.be(group);
				expect(event.cluster).to.be.a(L.Marker);
				expect(event.markers[1]).to.be(marker);
				expect(event.markers[0]).to.be(marker2);

				done();
			});

			marker.__parent.spiderfy();

			clock.tick(200);
		});
	});
});
