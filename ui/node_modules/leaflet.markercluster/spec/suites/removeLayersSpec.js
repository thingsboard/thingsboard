describe('removeLayers', function () {
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
			group.clearLayers();
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
	it('removes all the layer given to it', function () {

		group = new L.MarkerClusterGroup();

		var markers = [
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5])
		];

		map.addLayer(group);

		group.addLayers(markers);

		group.removeLayers(markers);

		expect(group.hasLayer(markers[0])).to.be(false);
		expect(group.hasLayer(markers[1])).to.be(false);
		expect(group.hasLayer(markers[2])).to.be(false);

		expect(group.getLayers().length).to.be(0);
	});

	it('removes all the layer given to it even though they move', function () {

		group = new L.MarkerClusterGroup();

		var markers = [
			new L.Marker([10, 10]),
			new L.Marker([20, 20]),
			new L.Marker([30, 30])
		];
		var len = markers.length;
		map.addLayer(group);

		group.addLayers(markers);

		markers.forEach(function (marker) {
			marker.setLatLng([1.5, 1.5]);
			group.removeLayer(marker);
			expect(group.getLayers().length).to.be(len - 1);
			group.addLayer(marker);
			expect(group.getLayers().length).to.be(len);
		});

		expect(group.getLayers().length).to.be(len);
	});

	it('removes all the layer given to it even if the group is not on the map', function () {

		group = new L.MarkerClusterGroup();

		var markers = [
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5])
		];

		map.addLayer(group);
		group.addLayers(markers);
		map.removeLayer(group);
		group.removeLayers(markers);
		map.addLayer(group);

		expect(group.hasLayer(markers[0])).to.be(false);
		expect(group.hasLayer(markers[1])).to.be(false);
		expect(group.hasLayer(markers[2])).to.be(false);

		expect(group.getLayers().length).to.be(0);
	});

	it('doesnt break if we are spiderfied', function () {

		group = new L.MarkerClusterGroup();

		var markers = [
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5]),
			new L.Marker([1.5, 1.5])
		];

		map.addLayer(group);

		group.addLayers(markers);

		markers[0].__parent.spiderfy();

		// We must wait for the spiderfy animation to timeout
		clock.tick(200);

		group.removeLayers(markers);

		expect(group.hasLayer(markers[0])).to.be(false);
		expect(group.hasLayer(markers[1])).to.be(false);
		expect(group.hasLayer(markers[2])).to.be(false);

		expect(group.getLayers().length).to.be(0);

		group.on('spiderfied', function() {
			expect(group._spiderfied).to.be(null);
		});
	});

	it('handles nested Layer Groups', function () {

		group = new L.MarkerClusterGroup();

		var marker1 = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var marker3 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);

		group.addLayers([marker1, marker2, marker3]);

		expect(group.hasLayer(marker1)).to.be(true);
		expect(group.hasLayer(marker2)).to.be(true);
		expect(group.hasLayer(marker3)).to.be(true);

		group.removeLayers([
			marker1,
			new L.LayerGroup([
				marker2, new L.LayerGroup([
					marker3
				])
			])
		]);

		expect(group.hasLayer(marker1)).to.be(false);
		expect(group.hasLayer(marker2)).to.be(false);
		expect(group.hasLayer(marker3)).to.be(false);

		expect(group.getLayers().length).to.be(0);
	});

    it('chunked loading zoom out', function () {
        //See #743 for more details
        var markers = [];

        group = new L.MarkerClusterGroup({
            chunkedLoading: true, chunkProgress: function () {
                //Before this provoked an "undefined" exception
                map.zoomOut();
                group.removeLayers(markers);
            }
        });

        for (var i = 1; i < 1000; i++) {
            markers.push(new L.Marker([1.0 + (.0001 * i), 1.0 + (.0001 * i)]));
        }

        map.addLayer(group);

        group.addLayers(markers);
    });
});
