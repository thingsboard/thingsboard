describe('things behave correctly with negative minZoom', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var div, map, group;

	beforeEach(function () {
		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);
	
		map = L.map(div, { minZoom: -3, maxZoom: 18, trackResize: false });
	
		map.setView(L.latLng(0, 0), -3);
	});

	afterEach(function () {
		if (group instanceof L.MarkerClusterGroup) {
			group.clearLayers();
			map.removeLayer(group);
		}

		map.remove();
		div.remove();

		div = map = group = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('shows a single marker added to the group before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		map.addLayer(group);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('shows a single marker added to the group after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();

		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		expect(marker._icon).to.not.be(undefined);
		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
	});

	it('creates a cluster when 2 overlapping markers are added before the group is added to the map', function () {

		group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		map.addLayer(group);

		expect(marker._icon).to.be(undefined);
		expect(marker2._icon).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);
	});
	it('creates a cluster when 2 overlapping markers are added after the group is added to the map', function () {

		group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);
		group.addLayer(marker2);

		expect(marker._icon).to.be(null); //Null as was added and then removed
		expect(marker2._icon).to.be(undefined);

		expect(map._panes.markerPane.childNodes.length).to.be(1);
	});
});
