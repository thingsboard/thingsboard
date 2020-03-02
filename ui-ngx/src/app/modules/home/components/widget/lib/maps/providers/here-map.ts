import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapOptions } from '../map-models';

export class HEREMap extends LeafletMap {
    constructor($container, options: MapOptions) {
        console.log("HEREMap -> constructor -> options", options)
        const defaultCredentials =
        {
            app_id: "AhM6TzD9ThyK78CT3ptx",
            app_code: "p6NPiITB3Vv0GMUFnkLOOg"
        }
        super($container, options);
        const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
        var tileLayer = (L.tileLayer as any).provider(options.mapProvider || "HERE.normalDay", options.credentials || defaultCredentials);
        tileLayer.addTo(map);
        super.setMap(map);
        super.initSettings(options);
    }
}