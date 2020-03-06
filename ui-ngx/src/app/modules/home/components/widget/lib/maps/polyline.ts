import L from 'leaflet';
import { safeExecute } from '@app/core/utils';

export class Polyline {

    leafletPoly: L.Polyline;
    dataSources;
    data;

    constructor(private map: L.Map, locations, data, dataSources, settings) {
        this.dataSources = dataSources;
        this.data = data;
        this.leafletPoly = L.polyline(locations,
            this.getPolyStyle(settings, data, dataSources)
        ).addTo(this.map);
    }

    updatePolyline(settings, data, dataSources) {
        this.leafletPoly.setStyle(this.getPolyStyle(settings, data, dataSources));

    }

    getPolyStyle(settings, data, dataSources): L.PolylineOptions {
        return {
            color: settings.useColorFunction ? safeExecute(settings.colorFunction, [data, dataSources, data[0]?.dsIndex]) : settings.color,
            opacity: settings.strokeOpacity,
            weight: settings.strokeWeight
        }
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