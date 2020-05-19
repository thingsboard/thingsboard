describe('refreshClusters', function () {
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

	function getClusterAtZoom(marker, zoom) {
		var parent = marker.__parent;

		while (parent && parent._zoom !== zoom) {
			parent = parent.__parent;
		}

		return parent;
	}

	function setMapView() {
		// Now look at the markers to force cluster icons drawing.
		// Corresponds to zoom level 8 for the above div dimensions.
		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	}

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('flags all non-visible parent clusters of a given marker', function () {

		group = L.markerClusterGroup().addTo(map);

		var marker1 = L.marker([1.5, 1.5]).addTo(group),
		    marker2 = L.marker([1.5, 1.5]).addTo(group); // Needed to force a cluster.

		setMapView();

		var marker1cluster10 = getClusterAtZoom(marker1, 10),
		    marker1cluster2 = getClusterAtZoom(marker1, 2),
		    marker1cluster5 = getClusterAtZoom(marker1, 5);

		// First go to some zoom levels so that Leaflet initializes clusters icons.
		expect(marker1cluster10._iconNeedsUpdate).to.be.ok();
		map.setZoom(10, {animate: false});
		expect(marker1cluster10._iconNeedsUpdate).to.not.be.ok();

		expect(marker1cluster2._iconNeedsUpdate).to.be.ok();
		map.setZoom(2, {animate: false});
		expect(marker1cluster2._iconNeedsUpdate).to.not.be.ok();

		// Finish on an intermediate zoom level.
		expect(marker1cluster5._iconNeedsUpdate).to.be.ok();
		map.setZoom(5, {animate: false});
		expect(marker1cluster5._iconNeedsUpdate).to.not.be.ok();

		// Run any animation.
		clock.tick(1000);

		// Then request clusters refresh.
		// No need to actually modify the marker.
		group.refreshClusters(marker1);

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster10._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster2._iconNeedsUpdate).to.be.ok();

		// Also check that visible clusters are "un-flagged" since they should be re-drawn.
		expect(marker1cluster5._iconNeedsUpdate).to.not.be.ok();

	});

	it('re-draws visible clusters', function () {

		group = L.markerClusterGroup({
			iconCreateFunction: function (cluster) {
				var markers = cluster.getAllChildMarkers();

				for(var i in markers) {
					if (markers[i].changed) {
						return new L.DivIcon({
							className: "changed"
						});
					}
				}
				return new L.DivIcon({
					className: "original"
				});
			}
		}).addTo(map);

		var marker1 = L.marker([1.5, 1.5]).addTo(group),
		    marker2 = L.marker([1.5, 1.5]).addTo(group); // Needed to force a cluster.

		setMapView();

		var marker1cluster9 = getClusterAtZoom(marker1, 9);

		// First go to some zoom levels so that Leaflet initializes clusters icons.
		expect(marker1cluster9._iconNeedsUpdate).to.be.ok();
		map.setZoom(9, {animate: false});
		expect(marker1cluster9._iconNeedsUpdate).to.not.be.ok();

		expect(marker1cluster9._icon.className).to.contain("original");
		expect(marker1cluster9._icon.className).to.not.contain("changed");

		// Run any animation.
		clock.tick(1000);

		// Alter the marker.
		marker1.changed = true;

		// Then request clusters refresh.
		group.refreshClusters(marker1);

		// Now check that visible clusters icon is re-drawn.
		expect(marker1cluster9._icon.className).to.contain("changed");
		expect(marker1cluster9._icon.className).to.not.contain("original");

	});

	// Shared code for the 2 below tests
	function iconCreateFunction(cluster) {
		var markers = cluster.getAllChildMarkers();

		for(var i in markers) {
			if (markers[i].changed) {
				return new L.DivIcon({
					className: "changed"
				});
			}
		}
		return new L.DivIcon({
			className: "original"
		});
	}

	it('re-draws markers in singleMarkerMode', function () {

		group = L.markerClusterGroup({
			singleMarkerMode: true,
			iconCreateFunction: iconCreateFunction
		}).addTo(map);

		var marker1 = L.marker([1.5, 1.5]).addTo(group);

		setMapView();

		expect(marker1._icon.className).to.contain("original");

		// Alter the marker.
		marker1.changed = true;

		// Then request clusters refresh.
		group.refreshClusters(marker1);

		expect(marker1._icon.className).to.contain("changed");
		expect(marker1._icon.className).to.not.contain("original");

	});

	it('does not modify markers that do not belong to the current group (in singleMarkerMode)', function () {

		group = L.markerClusterGroup({
			singleMarkerMode: true,
			iconCreateFunction: iconCreateFunction
		}).addTo(map);

		var marker1 = L.marker([1.5, 1.5]).addTo(group),
			marker2 = L.marker([1.5, 1.5], {
				icon: iconCreateFunction({
					getAllChildMarkers: function () {
						return marker2;
					}
				})
			}).addTo(map);

		setMapView();

		expect(marker1._icon.className).to.contain("original");
		expect(marker2._icon.className).to.contain("original");

		// Alter the markers.
		marker1.changed = true;
		marker2.changed = true;

		// Then request clusters refresh.
		group.refreshClusters([marker1, marker2]);

		expect(marker1._icon.className).to.contain("changed");
		expect(marker1._icon.className).to.not.contain("original");

		expect(marker2._icon.className).to.contain("original");
		expect(marker2._icon.className).to.not.contain("changed");

	});


	// Shared code for below tests.
	var marker1 = L.marker([1.5, 1.5]),
	    marker2 = L.marker([1.5, 1.5]), // Needed to force a cluster.
	    marker3 = L.marker([1.1, 1.1]),
	    marker4 = L.marker([1.1, 1.1]), // Needed to force a cluster.
	    marker5 = L.marker([1.9, 1.9]),
	    marker6 = L.marker([1.9, 1.9]), // Needed to force a cluster.
	    marker1cluster8,
	    marker1cluster3,
	    marker1cluster5,
	    marker3cluster8,
	    marker3cluster3,
	    marker3cluster5,
	    marker5cluster8,
	    marker5cluster3,
	    marker5cluster5;

	function init3clusterBranches() {

		group = L.markerClusterGroup({
			maxClusterRadius: 2 // Make sure we keep distinct clusters.
		}).addTo(map);

		// Populate Marker Cluster Group.
		marker1.addTo(group);
		marker2.addTo(group);
		marker3.addTo(group);
		marker4.addTo(group);
		marker5.addTo(group);
		marker6.addTo(group);

		setMapView();

		marker1cluster8 = getClusterAtZoom(marker1, 8);
		marker1cluster3 = getClusterAtZoom(marker1, 3);
		marker1cluster5 = getClusterAtZoom(marker1, 5);
		marker3cluster8 = getClusterAtZoom(marker3, 8);
		marker3cluster3 = getClusterAtZoom(marker3, 3);
		marker3cluster5 = getClusterAtZoom(marker3, 5);
		marker5cluster8 = getClusterAtZoom(marker5, 8);
		marker5cluster3 = getClusterAtZoom(marker5, 3);
		marker5cluster5 = getClusterAtZoom(marker5, 5);

		// Make sure we have 3 distinct clusters up to zoom level Z (let's choose Z = 3)
		expect(marker1cluster3._childCount).to.equal(2);
		expect(marker3cluster3._childCount).to.equal(2);
		expect(marker5cluster3._childCount).to.equal(2);

		// First go to some zoom levels so that Leaflet initializes clusters icons.
		expect(marker1cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker5cluster8._iconNeedsUpdate).to.not.be.ok();

		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.be.ok();
		map.setZoom(3, {animate: false});
		expect(marker1cluster3._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.not.be.ok();

		// Finish on an intermediate zoom level.
		expect(marker1cluster5._iconNeedsUpdate).to.be.ok();
		expect(marker3cluster5._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster5._iconNeedsUpdate).to.be.ok();
		map.setZoom(5, {animate: false});
		expect(marker1cluster5._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster5._iconNeedsUpdate).to.not.be.ok();
		expect(marker5cluster5._iconNeedsUpdate).to.not.be.ok();

		// Run any animation.
		clock.tick(1000);

		// Ready to refresh clusters with method of choice and assess result.
	}

	it('does not flag clusters of other markers', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the marker.
		group.refreshClusters(marker1);

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		// Finally check that non-involved clusters are not "dirty".
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.not.be.ok();

	});

	it('processes itself when no argument is passed', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the marker.
		group.refreshClusters();

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		expect(marker3cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.be.ok();

	});

	it('accepts an array of markers', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the markers.
		group.refreshClusters([marker1, marker5]);
		// Clusters of marker3 and 4 shall not be flagged.

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.be.ok();

		// Clusters of marker3 and 4 shall not be flagged.
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();

	});

	it('accepts a mapping of markers', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the markers.
		group.refreshClusters({
			id1: marker1,
			id2: marker5
		}); // Clusters of marker3 and 4 shall not be flagged.

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.be.ok();

		// Clusters of marker3 and 4 shall not be flagged.
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();

	});

	it('accepts an L.LayerGroup', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the markers.
		var layerGroup = new L.LayerGroup([marker1, marker5]);
		group.refreshClusters(layerGroup);
		// Clusters of marker3 and 4 shall not be flagged.

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.be.ok();

		// Clusters of marker3 and 4 shall not be flagged.
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();

	});

	it('accepts an L.MarkerCluster', function () {

		init3clusterBranches();

		// Then request clusters refresh.
		// No need to actually modify the markers.
		group.refreshClusters(marker1cluster8);
		// Clusters of marker3, 4, 5 and 6 shall not be flagged.

		// Now check that non-visible clusters are flagged as "dirty".
		expect(marker1cluster8._iconNeedsUpdate).to.be.ok();
		expect(marker1cluster3._iconNeedsUpdate).to.be.ok();

		// Clusters of marker3 and 4 shall not be flagged.
		expect(marker3cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker3cluster3._iconNeedsUpdate).to.not.be.ok();

		expect(marker5cluster8._iconNeedsUpdate).to.not.be.ok();
		expect(marker5cluster3._iconNeedsUpdate).to.not.be.ok();

	});
});
