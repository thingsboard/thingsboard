describe('onRemove', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var map, div;

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
		map.remove();
		document.body.removeChild(div);

		map = div = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('removes the shown coverage polygon', function () {

		var group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5]);
		var marker2 = new L.Marker([1.5, 1.5]);
		var marker3 = new L.Marker([1.5, 1.5]);

		group.addLayer(marker);
		group.addLayer(marker2);
		group.addLayer(marker3);

		map.addLayer(group);

		group._showCoverage({ layer: group._topClusterLevel });

		expect(group._shownPolygon).to.not.be(null);

		map.removeLayer(group);

		expect(group._shownPolygon).to.be(null);
	});
});