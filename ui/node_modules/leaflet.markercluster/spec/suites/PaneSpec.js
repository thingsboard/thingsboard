describe('Map pane selection', function() {
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
	
		// Create map pane
		map.createPane('testPane');
		
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

		div = map = group = null;
	});

    /////////////////////////////
    // TESTS
    /////////////////////////////
    it('recognizes and applies option', function() {
        group = new L.MarkerClusterGroup({clusterPane: 'testPane'});

        var marker = new L.Marker([1.5, 1.5]);
        var marker2 = new L.Marker([1.5, 1.5]);

        group.addLayers([marker, marker2]);
        map.addLayer(group);

        expect(map._panes.testPane.childNodes.length).to.be(1);
    });

    it('defaults to default marker pane', function() {
        group = new L.MarkerClusterGroup();

        var marker = new L.Marker([1.5, 1.5]);
        var marker2 = new L.Marker([1.5, 1.5]);

        group.addLayers([marker, marker2]);
        map.addLayer(group);

        expect(map._panes[L.Marker.prototype.options.pane].childNodes.length).to.be(1);
    });
});