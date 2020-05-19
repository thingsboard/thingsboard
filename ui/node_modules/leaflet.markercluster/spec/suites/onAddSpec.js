describe('onAdd', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var map, div;

	beforeEach(function () {
		div = document.createElement('div');
		div.style.width = '200px';
		div.style.height = '200px';
		document.body.appendChild(div);

		map = L.map(div, { trackResize: false });

		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	});

	afterEach(function () {
		map.remove();
		document.body.removeChild(div);

		map = div = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('throws an error if maxZoom is not specified', function () {

		var group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);

		var ex = null;
		try {
			map.addLayer(group);
		} catch (e) {
			ex = e;
		}

		expect(ex).to.not.be(null);
	});

	it('successfully handles removing and re-adding a layer while not on the map', function () {
		map.options.maxZoom = 18;
		var group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		map.removeLayer(group);
		group.removeLayer(marker);
		group.addLayer(marker);

		map.addLayer(group);

		expect(map.hasLayer(group)).to.be(true);
		expect(group.hasLayer(marker)).to.be(true);
	});
});