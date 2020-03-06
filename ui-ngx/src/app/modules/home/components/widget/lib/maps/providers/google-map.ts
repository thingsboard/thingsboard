
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapOptions } from '../map-models';
import 'leaflet.gridlayer.googlemutant';

var googleLoaded = false;


export class GoogleMap extends LeafletMap {
    constructor($container, options: MapOptions) {

        super($container, options);
        this.loadGoogle(() => {
            const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
            var roads = (L.gridLayer as any).googleMutant({
                type: options?.gmDefaultMapType || 'roadmap'	// valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
            }).addTo(map);
            super.setMap(map);
        }, options.credentials.apiKey);
        super.initSettings(options);
    }

    private loadGoogle(callback, apiKey = 'AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q') {
        if (googleLoaded) {
            callback()
        }
        else {
            googleLoaded = true;
            let script = document.createElement('script');
            script.onload = () => {
                callback();
            }
            script.onerror = () => {
                googleLoaded = false;
            }
            script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}`;
            document.getElementsByTagName('head')[0].appendChild(script);
        }
    }
}