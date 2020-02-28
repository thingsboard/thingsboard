import L from 'leaflet';
import { createTooltip } from './maps-utils';

export class Polygon {

    map: L.Map;
    leafletPoly: L.Polygon;

    tooltip;

    constructor(latLangs, settings, location, onClickListener) {
        this.leafletPoly = L.polygon(latLangs, {
            fill: true,
            fillColor: settings.polygonColor,
            color: settings.polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        }).addTo(this.map);

        if (settings.displayTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, location.dsIndex, settings);
        }
        if (onClickListener) {
            this.leafletPoly.on('click', onClickListener);
        }
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings, color) {
        let style = {
            fill: true,
            fillColor: color,
            color: color,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        };
        this.leafletPoly.setStyle(style);
    }

    getPolygonLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolygonLatLngs(latLngs) {
        this.leafletPoly.setLatLngs(latLngs);
        this.leafletPoly.redraw();
    }
}