describe('animate option', function () {
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
	
		// Corresponds to zoom level 8 for the above div dimensions.
		map.fitBounds(new L.LatLngBounds([
			[1, 1],
			[2, 2]
		]));
	});

	afterEach(function () {
		if (group instanceof L.MarkerClusterGroup) {
			group.removeLayers(group.getLayers());
			map.removeLayer(group);
		}

		map.remove();
		div.remove();

		div = map = group = null;
	});

	/////////////////////////////
	// TESTS
	/////////////////////////////
	it('hooks animated methods version by default', function () {

		// Need to add to map so that we have the top cluster level created.
		group = L.markerClusterGroup().addTo(map);

		var withAnimation = L.MarkerClusterGroup.prototype._withAnimation;

		// MCG animated methods.
		expect(group._animationStart).to.be(withAnimation._animationStart);
		expect(group._animationZoomIn).to.be(withAnimation._animationZoomIn);
		expect(group._animationZoomOut).to.be(withAnimation._animationZoomOut);
		expect(group._animationAddLayer).to.be(withAnimation._animationAddLayer);

		// MarkerCluster spiderfy animated methods
		var cluster = group._topClusterLevel;

		withAnimation = L.MarkerCluster.prototype;

		expect(cluster._animationSpiderfy).to.be(withAnimation._animationSpiderfy);
		expect(cluster._animationUnspiderfy).to.be(withAnimation._animationUnspiderfy);

	});

	it('hooks non-animated methods version when set to false', function () {

		// Need to add to map so that we have the top cluster level created.
		group = L.markerClusterGroup({animate: false}).addTo(map);

		var noAnimation = L.MarkerClusterGroup.prototype._noAnimation;

		// MCG non-animated methods.
		expect(group._animationStart).to.be(noAnimation._animationStart);
		expect(group._animationZoomIn).to.be(noAnimation._animationZoomIn);
		expect(group._animationZoomOut).to.be(noAnimation._animationZoomOut);
		expect(group._animationAddLayer).to.be(noAnimation._animationAddLayer);

		// MarkerCluster spiderfy non-animated methods
		var cluster = group._topClusterLevel;

		noAnimation = L.MarkerClusterNonAnimated.prototype;

		expect(cluster._animationSpiderfy).to.be(noAnimation._animationSpiderfy);
		expect(cluster._animationUnspiderfy).to.be(noAnimation._animationUnspiderfy);

	});

	it('always hooks non-animated methods version when L.DomUtil.TRANSITION is false', function () {
		// Fool Leaflet, make it think the browser does not support transitions.
		var realDomUtil = L.DomUtil;
		var fakeDomUtil = {};
		for (k in realDomUtil) {
			fakeDomUtil[k] = realDomUtil[k];
		}
		fakeDomUtil.TRANSITION = false;
		L.DomUtil = fakeDomUtil;

		try {
			// Need to add to map so that we have the top cluster level created.
			group = L.markerClusterGroup({animate: true}).addTo(map);

			var noAnimation = L.MarkerClusterGroup.prototype._noAnimation;

			// MCG non-animated methods.
			expect(group._animationStart).to.be(noAnimation._animationStart);
			expect(group._animationZoomIn).to.be(noAnimation._animationZoomIn);
			expect(group._animationZoomOut).to.be(noAnimation._animationZoomOut);
			expect(group._animationAddLayer).to.be(noAnimation._animationAddLayer);

			// MarkerCluster spiderfy non-animated methods
			var cluster = group._topClusterLevel;

			noAnimation = L.MarkerClusterNonAnimated.prototype;

			expect(cluster._animationSpiderfy).to.be(noAnimation._animationSpiderfy);
			expect(cluster._animationUnspiderfy).to.be(noAnimation._animationUnspiderfy);
		} finally {
			//Undo the DomUtil replacement hack
			L.DomUtil = realDomUtil;
		}
	});
});
