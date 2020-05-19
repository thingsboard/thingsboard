describe('getVisibleParent', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var group, map, div;

	beforeEach(function () {
		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);

		map = L.map(div, { maxZoom: 18, trackResize: false });

		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	});

	afterEach(function () {
		group.clearLayers();
		map.removeLayer(group);
		map.remove();
		document.body.removeChild(div);

		group = map = div = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('gets the marker if the marker is visible', function () {
		group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		map.addLayer(group);

		var vp = group.getVisibleParent(marker);

		expect(vp).to.be(marker);
	});

	it('gets the visible cluster if it is clustered', function () {
		group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		var vp = group.getVisibleParent(marker);

		expect(vp).to.be.a(L.MarkerCluster);
		expect(vp._icon).to.not.be(null);
		expect(vp._icon).to.not.be(undefined);
	});

	it('returns null if the marker and parents are all not visible', function () {
		group = new L.MarkerClusterGroup();
		var marker = new L.Marker([5.5, 1.5]);
		var marker2 = new L.Marker([5.5, 1.5]);

		group.addLayers([marker, marker2]);
		map.addLayer(group);

		var vp = group.getVisibleParent(marker);

		expect(vp).to.be(null);
	});
});