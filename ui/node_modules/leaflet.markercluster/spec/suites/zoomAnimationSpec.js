describe('zoomAnimation', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var div, map, group, clock, realBrowser;

	beforeEach(function () {
		realBrowser = L.Browser;
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
		document.body.removeChild(div);
		clock.restore();

		div = map = group = clock = null;
	});

	function setBrowserToMobile() {
		var fakeBrowser = {};
		for (k in realBrowser) {
			fakeBrowser[k] = realBrowser[k];
		}
		fakeBrowser.mobile = true;
		L.Browser = fakeBrowser;
	}

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('adds the visible marker to the map when zooming in', function () {
		map.setView(new L.LatLng(-37.36142550190516, 174.254150390625), 7);

		group = new L.MarkerClusterGroup({
			showCoverageOnHover: true,
			maxClusterRadius: 20,
			disableClusteringAtZoom: 15
		});
		var marker = new L.Marker([-37.77852090603777, 175.3103667497635]);
		group.addLayer(marker); //The one we zoom in on
		group.addLayer(new L.Marker([-37.711800591811055, 174.50034790039062])); //Marker that we cluster with at the top zoom level, but not 1 level down
		map.addLayer(group);

		clock.tick(1000);
		map.setView([-37.77852090603777, 175.3103667497635], 15);

		//Run the the animation
		clock.tick(1000);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon).to.not.be(null);
	});

	it('adds the visible marker to the map when jumping around', function () {

		group = new L.MarkerClusterGroup();
		var marker1 = new L.Marker([48.858280181884766, 2.2945759296417236]);
		var marker2 = new L.Marker([16.02359962463379, -61.70280075073242]);
		group.addLayer(marker1); //The one we zoom in on first
		group.addLayer(marker2); //Marker that we cluster with at the top zoom level, but not 1 level down
		map.addLayer(group);

		//show the first
		map.fitBounds(new L.LatLngBounds(new L.LatLng(41.371582, -5.142222), new L.LatLng(51.092804, 9.561556)));

		clock.tick(1000);

		map.fitBounds(new L.LatLngBounds(new L.LatLng(15.830972671508789, -61.807167053222656), new L.LatLng(16.516849517822266, -61.0)));

		//Run the the animation
		clock.tick(1000);

		//Now the second one should be visible on the map
		expect(marker2._icon).to.not.be(undefined);
		expect(marker2._icon).to.not.be(null);
	});

	it('adds the visible markers to the map, but not parent clusters when jumping around', function () {

		group = new L.MarkerClusterGroup();

		var marker1 = new L.Marker([59.9520, 30.3307]),
			marker2 = new L.Marker([59.9516, 30.3308]),
			marker3 = new L.Marker([59.9513, 30.3312]);

		group.addLayer(marker1);
		group.addLayer(marker2);
		group.addLayer(marker3);
		map.addLayer(group);

		//Show none of them
		map.setView([53.0676, 170.6835], 16);

		clock.tick(1000);

		//Zoom so that all the markers will be visible (Same as zoomToShowLayer)
		map.setView(marker1.getLatLng(), 18);

		//Run the the animation
		clock.tick(1000);

		//Now the markers should all be visible, and there should be no visible clusters
		expect(marker1._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker3._icon.parentNode).to.be(map._panes.markerPane);
		expect(map._panes.markerPane.childNodes.length).to.be(3);
	});

	it('removes clicked clusters on the edge of a mobile screen', function () {
		setBrowserToMobile();

		try {
			// Corresponds to zoom level 8 for the above div dimensions.
			map.fitBounds(new L.LatLngBounds([
				[1, 1],
				[2, 2]
			]));

			group = new L.MarkerClusterGroup({
				maxClusterRadius: 80
			}).addTo(map);

			// Add a marker 1 pixel below the initial screen bottom edge.
			var bottomPoint = map.getPixelBounds().max.add([0, 1]),
				bottomLatLng = map.unproject(bottomPoint),
				centerLng = map.getCenter().lng,
				bottomPosition = new L.LatLng(
					bottomLatLng.lat,
					centerLng
				),
				bottomMarker = new L.Marker(bottomPosition).addTo(group),
				initialZoom = map.getZoom();

			expect(bottomMarker._icon).to.be(undefined);

			// Add many markers 79 pixels above the first one, so they cluster with it.
			var newPoint = bottomPoint.add([0, -79]),
				newLatLng = L.latLng(
					map.unproject(newPoint).lat,
					centerLng
				);

			for (var i = 0; i < 10; i += 1) {
				group.addLayer(new L.Marker(newLatLng));
			}

			var parentCluster = bottomMarker.__parent;

			expect(parentCluster._icon.parentNode).to.be(map._panes.markerPane);

			parentCluster.fireEvent('click', null, true);

			//Run the the animation
			clock.tick(1000);

			expect(map.getZoom()).to.equal(initialZoom + 1); // The fitBounds with 200px height should result in zooming 1 level in.

			// Finally make sure that the cluster has been removed from map.
			expect(parentCluster._icon).to.be(null);
			expect(map._panes.markerPane.childNodes.length).to.be(2); // The bottomMarker + cluster for the 10 above markers.
		}
		finally {
			L.Browser = realBrowser;
		}
	});

	describe('zoomToShowLayer', function () {

		it('zoom to single marker inside map view', function () {
			group = new L.MarkerClusterGroup();

			var marker = new L.Marker([59.9520, 30.3307]);

			group.addLayer(marker);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			map.setView(marker.getLatLng(), 10);

			clock.tick(1000);

			var initialCenter = map.getCenter();
			var initialZoom = map.getZoom();

			group.zoomToShowLayer(marker, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Marker should be visible, map center and zoom level should stay the same, callback called once
			expect(marker._icon).to.not.be(undefined);
			expect(marker._icon).to.not.be(null);
			expect(map.getBounds().contains(marker.getLatLng())).to.be.true;
			expect(map.getCenter()).to.eql(initialCenter);
			expect(map.getZoom()).to.equal(initialZoom);
			sinon.assert.calledOnce(zoomCallbackSpy);
		});

		it('pan map to single marker outside map view', function () {
			group = new L.MarkerClusterGroup();

			var marker = new L.Marker([59.9520, 30.3307]);

			group.addLayer(marker);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			//Show none of them
			map.setView([53.0676, 170.6835], 16);

			clock.tick(1000);

			var initialZoom = map.getZoom();

			group.zoomToShowLayer(marker, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Marker should be visible, map center should be equal to marker center,
			//zoom level should stay the same, callback called once
			expect(marker._icon).to.not.be(undefined);
			expect(marker._icon).to.not.be(null);
			expect(map.getBounds().contains(marker.getLatLng())).to.be.true;
			expect(map.getCenter()).to.eql(marker.getLatLng());
			expect(map.getZoom()).to.equal(initialZoom);
			sinon.assert.calledOnce(zoomCallbackSpy);
		});

		it('change view and zoom to marker in cluster inside map view', function () {
			group = new L.MarkerClusterGroup();

			var marker1 = new L.Marker([59.9520, 30.3307]);
			var marker2 = new L.Marker([59.9516, 30.3308]);
			var marker3 = new L.Marker([59.9513, 30.3312]);

			group.addLayer(marker1);
			group.addLayer(marker2);
			group.addLayer(marker3);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			map.setView(marker1.getLatLng(), 16);

			clock.tick(1000);

			group.zoomToShowLayer(marker1, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Now the markers should all be visible, there should be no visible clusters, and callback called once
			expect(marker1._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker3._icon.parentNode).to.be(map._panes.markerPane);
			expect(map._panes.markerPane.childNodes.length).to.be(3);
			expect(map.getBounds().contains(marker1.getLatLng())).to.be.true;
			sinon.assert.calledOnce(zoomCallbackSpy);
		});

		it('change view and zoom to marker in cluster outside map view', function () {
			group = new L.MarkerClusterGroup();

			var marker1 = new L.Marker([59.9520, 30.3307]);
			var marker2 = new L.Marker([59.9516, 30.3308]);
			var marker3 = new L.Marker([59.9513, 30.3312]);

			group.addLayer(marker1);
			group.addLayer(marker2);
			group.addLayer(marker3);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			//Show none of them
			map.setView([53.0676, 170.6835], 16);

			clock.tick(1000);

			group.zoomToShowLayer(marker1, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Now the markers should all be visible, there should be no visible clusters, and callback called once
			expect(marker1._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker3._icon.parentNode).to.be(map._panes.markerPane);
			expect(map._panes.markerPane.childNodes.length).to.be(3);
			expect(map.getBounds().contains(marker1.getLatLng())).to.be.true;
			sinon.assert.calledOnce(zoomCallbackSpy);
		});

		it('spiderfy overlapping markers', function () {
			group = new L.MarkerClusterGroup();

			var marker1 = new L.Marker([59.9520, 30.3307]);
			var marker2 = new L.Marker([59.9520, 30.3307]);
			var marker3 = new L.Marker([59.9520, 30.3307]);

			group.addLayer(marker1);
			group.addLayer(marker2);
			group.addLayer(marker3);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			//Show none of them
			map.setView([53.0676, 170.6835], 16);

			clock.tick(1000);

			group.zoomToShowLayer(marker1, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Now the markers should all be visible, parent cluster should be spiderfied, and callback called once
			expect(marker1._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker2._icon.parentNode).to.be(map._panes.markerPane);
			expect(marker3._icon.parentNode).to.be(map._panes.markerPane);
			expect(map._panes.markerPane.childNodes.length).to.be(4);//3 markers + spiderfied parent cluster
			sinon.assert.calledOnce(zoomCallbackSpy);
		});

		it('zoom or spiderfy markers if they visible on next level of zoom', function () {
			group = new L.MarkerClusterGroup();

			var marker1 = new L.Marker([59.9520, 30.3307]);
			var marker2 = new L.Marker([59.9516, 30.3308]);
			var marker3 = new L.Marker([59.9513, 30.3312]);

			group.addLayer(marker1);
			group.addLayer(marker2);
			group.addLayer(marker3);
			map.addLayer(group);

			var zoomCallbackSpy = sinon.spy();

			//Markers will be visible on zoom 18
			map.setView([59.9520, 30.3307], 17);

			clock.tick(1000);

			group.zoomToShowLayer(marker1, zoomCallbackSpy);

			//Run the the animation
			clock.tick(1000);

			//Now the markers should all be visible (zoomed or spiderfied), and callback called once
			expect(marker1._icon).to.not.be(undefined);
			expect(marker1._icon).to.not.be(null);
			expect(marker2._icon).to.not.be(undefined);
			expect(marker2._icon).to.not.be(null);
			expect(marker3._icon).to.not.be(undefined);
			expect(marker3._icon).to.not.be(null);
			sinon.assert.calledOnce(zoomCallbackSpy);
		});
	});
});
