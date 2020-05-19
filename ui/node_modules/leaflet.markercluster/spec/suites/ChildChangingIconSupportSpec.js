describe('support child markers changing icon', function () {
	/////////////////////////////
	// SETUP FOR EACH TEST
	/////////////////////////////
	var map, div, clock;

	beforeEach(function () {
		clock = sinon.useFakeTimers();

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
		clock.restore();

		map = div = clock = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('child markers end up with the right icon after becoming unclustered', function () {

		var group = new L.MarkerClusterGroup();
		var marker = new L.Marker([1.5, 1.5], { icon: new L.DivIcon({html: 'Inner1Text' }) });
		var marker2 = new L.Marker([1.5, 1.5]);

		map.addLayer(group);
		group.addLayer(marker);

		expect(marker._icon.parentNode).to.be(map._panes.markerPane);
		expect(marker._icon.innerHTML).to.contain('Inner1Text');

		group.addLayer(marker2);

		expect(marker._icon).to.be(null); //Have been removed from the map

		marker.setIcon(new L.DivIcon({ html: 'Inner2Text' })); //Change the icon

		group.removeLayer(marker2); //Remove the other marker, so we'll become unclustered

		expect(marker._icon.innerHTML).to.contain('Inner2Text');
	});
});