describe('events', function() {
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
			group.removeLayers(group.getLayers());
			map.removeLayer(group);
		}

		map.remove();
		div.remove();

		div = map = group = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('is fired for a single child marker', function () {
		var callback = sinon.spy();

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		group.on('click', callback);
		group.addLayer(marker);
		map.addLayer(group);

		// In Leaflet 1.0.0, event propagation must be explicitly set by 3rd argument.
		marker.fire('click', null, true);

		expect(callback.called).to.be(true);
	});

	it('is fired for a child polygon', function () {
		var callback = sinon.spy();

		group = new L.MarkerClusterGroup();

		var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

		group.on('click', callback);
		group.addLayer(polygon);
		map.addLayer(group);

		polygon.fire('click', null, true);

		expect(callback.called).to.be(true);
	});

	it('is fired for a cluster click', function () {
		var callback = sinon.spy();

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.on('clusterclick', callback);
		group.addLayers([marker, marker2]);
		map.addLayer(group);

		var cluster = group.getVisibleParent(marker);
		expect(cluster instanceof L.MarkerCluster).to.be(true);

		cluster.fire('click', null, true);

		expect(callback.called).to.be(true);
	});

	describe('after being added, removed, re-added from the map', function() {

		it('still fires events for nonpoint data', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);

			group.on('click', callback);
			group.addLayer(polygon);
			map.addLayer(group);
			map.removeLayer(group);
			map.addLayer(group);

			polygon.fire('click', null, true);

			expect(callback.called).to.be(true);
		});

		it('still fires events for point data', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();

			var marker = new L.Marker([1.5, 1.5]);

			group.on('click', callback);
			group.addLayer(marker);
			map.addLayer(group);
			map.removeLayer(group);
			map.addLayer(group);

			marker.fire('click', null, true);

			expect(callback.called).to.be(true);
		});

		it('still fires cluster events', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();

			var marker = new L.Marker([1.5, 1.5]);
			var marker2 = new L.Marker([1.5, 1.5]);

			group.on('clusterclick', callback);
			group.addLayers([marker, marker2]);
			map.addLayer(group);

			map.removeLayer(group);
			map.addLayer(group);

			var cluster = group.getVisibleParent(marker);
			expect(cluster instanceof L.MarkerCluster).to.be(true);

			cluster.fire('click', null, true);

			expect(callback.called).to.be(true);
		});

		it('does not break map events', function () {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();

			map.on('zoomend', callback);
			map.addLayer(group);

			map.removeLayer(group);
			map.addLayer(group);

			map.fire('zoomend');

			expect(callback.called).to.be(true);
		});

		//layeradd
		it('fires layeradd when markers are added while not on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layeradd', callback);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayer(marker);

			expect(callback.callCount).to.be(1);
		});

		it('fires layeradd when vectors are added while not on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layeradd', callback);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayer(polygon);

			expect(callback.callCount).to.be(1);
		});
		
		it('fires layeradd when markers are added while on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layeradd', callback);
			map.addLayer(group);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayer(marker);

			expect(callback.callCount).to.be(1);
		});

		it('fires layeradd when vectors are added while on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layeradd', callback);
			map.addLayer(group);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayer(polygon);

			expect(callback.callCount).to.be(1);
		});
		
		it('fires layeradd when markers are added using addLayers while on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layeradd', callback);
			map.addLayer(group);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayers([marker]);

			expect(callback.callCount).to.be(1);
		});

		it('fires layeradd when vectors are added using addLayers while on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layeradd', callback);
			map.addLayer(group);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayers([polygon]);

			expect(callback.callCount).to.be(1);
		});

		//layerremove
		it('fires layerremove when a marker is removed while not on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layerremove', callback);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayer(marker);
			group.removeLayer(marker);

			expect(callback.callCount).to.be(1);
		});

		it('fires layerremove when a vector is removed while not on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layerremove', callback);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayer(polygon);
			group.removeLayer(polygon);

			expect(callback.callCount).to.be(1);
		});
		
		it('fires layerremove when a marker is removed while on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layerremove', callback);
			map.addLayer(group);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayer(marker);
			group.removeLayer(marker);

			expect(callback.callCount).to.be(1);
		});

		it('fires layerremove when a vector is removed while on the map', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup();
			group.on('layerremove', callback);
			map.addLayer(group);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayer(polygon);
			group.removeLayer(polygon);

			expect(callback.callCount).to.be(1);
		});
		
		it('fires layerremove when a marker is removed using removeLayers while on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layerremove', callback);
			map.addLayer(group);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayers([marker]);
			group.removeLayers([marker]);

			expect(callback.callCount).to.be(1);
		});

		it('fires layerremove when a vector is removed using removeLayers while on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layerremove', callback);
			map.addLayer(group);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayers([polygon]);
			group.removeLayers([polygon]);

			expect(callback.callCount).to.be(1);
		});
		
		it('fires layerremove when a marker is removed using removeLayers while not on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layerremove', callback);

			var marker = new L.Marker([1.5, 1.5]);
			group.addLayers([marker]);
			group.removeLayers([marker]);

			expect(callback.callCount).to.be(1);
		});

		it('fires layerremove when a vector is removed using removeLayers while not on the map with chunked loading', function() {
			var callback = sinon.spy();

			group = new L.MarkerClusterGroup({ chunkedLoading: true });
			group.on('layerremove', callback);

			var polygon = new L.Polygon([[1.5, 1.5], [2.0, 1.5], [2.0, 2.0], [1.5, 2.0]]);
			group.addLayers([polygon]);
			group.removeLayers([polygon]);

			expect(callback.callCount).to.be(1);
		});
	});

	/*
	//No normal events can be fired by a clustered marker, so probably don't need this.
	it('is fired for a clustered child marker', function() {
		var callback = sinon.spy();

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.on('click', callback);
		group.addLayers([marker, marker2]);
		map.addLayer(group);

		marker.fire('click');

		expect(callback.called).to.be(true);
	});
	*/
});