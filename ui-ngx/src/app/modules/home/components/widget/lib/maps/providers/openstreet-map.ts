
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapOptions } from '../map-models';

export class OpenStreetMap extends LeafletMap {
    constructor($container, options: MapOptions) {
        super($container, options);
        const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
        var tileLayer = (L.tileLayer as any).provider("OpenStreetMap.Mapnik");
        tileLayer.addTo(map);
        super.setMap(map);
        super.initSettings(options);
    }
}