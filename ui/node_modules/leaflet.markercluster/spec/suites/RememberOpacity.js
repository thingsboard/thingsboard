describe('Remember opacity', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var map, div, clock, markers, group;

	var markerDefs = [
		{latLng: [ 0, 0], opts: {opacity: 0.9}},
		{latLng: [ 0, 1], opts: {opacity: 0.5}},
		{latLng: [ 0,-1], opts: {opacity: 0.5}},
		{latLng: [ 1, 0], opts: {opacity: 0.5}},
		{latLng: [-1, 0], opts: {opacity: 0.5}},
		{latLng: [ 1, 1], opts: {opacity: 0.2}},
		{latLng: [ 1,-1], opts: {opacity: 0.2}},
		{latLng: [-1, 1], opts: {opacity: 0.2}},
		{latLng: [-1,-1], opts: {opacity: 0.2}}
	];

	var bounds = L.latLngBounds( L.latLng( -1.1, -1.1),
	                             L.latLng(  1.1,  1.1) );

	beforeEach(function () {
		clock = sinon.useFakeTimers();

		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);

		map = L.map(div, { maxZoom: 18, trackResize: false });

		markers = [];
		for (var i = 0; i < markerDefs.length; i++) {
			markers.push( L.marker(markerDefs[i].latLng, markerDefs[i].opts ) );
		}
	});

	afterEach(function () {
		group.clearLayers();
		map.removeLayer(group);
		map.remove();
		document.body.removeChild(div);
		clock.restore();

		clock = div = map = markers = group = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('clusters semitransparent markers into an opaque one', function () {
		map.setView(new L.LatLng(0,0), 1);

		group = new L.MarkerClusterGroup({
			maxClusterRadius: 20
		});
		group.addLayers(markers);
		map.addLayer(group);

		var visibleClusters = group._featureGroup.getLayers();
		expect(visibleClusters.length).to.be(1);
		expect(visibleClusters[0].options.opacity).to.be(1);
	});


	it('unclusters an opaque marker into semitransparent ones', function () {
		map.setView(new L.LatLng(0,0), 1);
		var visibleClusters;

		group = new L.MarkerClusterGroup({
			maxClusterRadius: 20
		});
		group.addLayers(markers);
		map.addLayer(group);

		map.fitBounds(bounds);
		clock.tick(1000);

		visibleClusters = group._featureGroup.getLayers();
		expect(visibleClusters.length).to.be(9);
		for (var i=0; i<9; i++) {
			expect(visibleClusters[i].options.opacity).to.be.within(0.2,0.9);
		}

		// It shall also work after zooming in/out a second time.
		map.setView(new L.LatLng(0,0), 1);
		clock.tick(1000);

		map.fitBounds(bounds);
		clock.tick(1000);

		visibleClusters = group._featureGroup.getLayers();
		expect(visibleClusters.length).to.be(9);
		for (var i=0; i<9; i++) {
			expect(visibleClusters[i].options.opacity).to.be.within(0.2,0.9);
		}
	});


	it('has no problems zooming in and out several times', function () {
		var visibleClusters;

		group = new L.MarkerClusterGroup({
			maxClusterRadius: 20
		});
		group.addLayers(markers);
		map.addLayer(group);

		// Zoom in and out a couple times
		for (var i=0; i<10; i++) {
			map.fitBounds(bounds);
			clock.tick(1000);

			visibleClusters = group._featureGroup.getLayers();
			expect(visibleClusters.length).to.be(9);
			for (var i=0; i<9; i++) {
				expect(visibleClusters[i].options.opacity).to.be.within(0.2,0.9);
			}

			map.setView(new L.LatLng(0,0), 1);
			clock.tick(1000);

			visibleClusters = group._featureGroup.getLayers();
			expect(visibleClusters.length).to.be(1);
			expect(visibleClusters[0].options.opacity).to.be(1);
		}

	});

	it('retains the opacity of each individual marker', function () {
		map.setView(new L.LatLng(0,0), 1);

		var visibleClusters;
		group = new L.MarkerClusterGroup({
			maxClusterRadius: 20
		});
		group.addLayers(markers);
		map.addLayer(group);


		// Zoom in and out a couple times
		for (var i=0; i<5; i++) {
			map.fitBounds(bounds);
			clock.tick(1000);

			map.setView(new L.LatLng(0,0), 1);
			clock.tick(1000);
		}

		for (var i=0; i<markerDefs.length; i++) {

// 			console.log(markerDefs[i].latLng, markerDefs[i].opts.opacity);

			map.setView(L.latLng(markerDefs[i].latLng), 18);
			clock.tick(1000);
			visibleClusters = group._featureGroup.getLayers();
			expect(visibleClusters.length).to.be(1);
			expect(visibleClusters[0].options.opacity).to.be(markerDefs[i].opts.opacity);
		}
	});

});