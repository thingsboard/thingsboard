describe('L#noConflict', function() {
	it('restores the previous L value and returns Leaflet namespace', function(){

		expect(L.version).to.be.ok();
	});
});
