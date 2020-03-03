import L from 'leaflet';

export class Polyline {

    leafletPoly: L.Polyline;

    constructor(private map: L.Map, locations, settings) {
        this.leafletPoly = L.polyline(locations,
            {
                color: settings.color,
                opacity: settings.strokeOpacity,
                weight: settings.strokeWeight
            }
        ).addTo(this.map);
    }

    updatePolylineColor(settings, color) {
        var style = {
            color: color,
            opacity: settings.strokeOpacity,
            weight: settings.strokeWeight
        };
        this.leafletPoly.setStyle(style);
    }

    removePolyline() {
        this.map.removeLayer(this.leafletPoly);
    }

    getPolylineLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolylineLatLngs(latLngs) {
        this.leafletPoly.setLatLngs(latLngs);
    }
}