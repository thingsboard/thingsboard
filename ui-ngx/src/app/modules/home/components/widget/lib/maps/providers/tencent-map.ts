
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapOptions } from '../map-models';

export class TencentMap extends LeafletMap {
    constructor($container, options: MapOptions) {
        super($container, options);
        const txUrl = 'http://rt{s}.map.gtimg.com/realtimerender?z={z}&x={x}&y={y}&type=vector&style=0';
        const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
        const txLayer = L.tileLayer(txUrl, { subdomains: '0123', tms: true }).addTo(map);
        txLayer.addTo(map);
        super.setMap(map);
        super.initSettings(options);
    }
}