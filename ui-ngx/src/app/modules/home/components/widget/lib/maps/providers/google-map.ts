///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///


import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { UnitedMapSettings } from '../map-models';
import 'leaflet.gridlayer.googlemutant';

let googleLoaded = false;


export class GoogleMap extends LeafletMap {
    constructor($container, options: UnitedMapSettings) {

        super($container, options);
        this.loadGoogle(() => {
            const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
            (L.gridLayer as any).googleMutant({
                type: options?.gmDefaultMapType || 'roadmap'
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
            const script = document.createElement('script');
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
