import * as L from 'leaflet';

import 'leaflet-providers';
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import 'leaflet.markercluster/dist/leaflet.markercluster'

import { MapOptions, MarkerSettings } from './map-models';
import { Marker } from './markers';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Polyline } from './polyline';

export default abstract class LeafletMap {

    markers: Map<string, Marker> = new Map();
    tooltips = [];
    poly: Polyline;
    map: L.Map;
    map$: BehaviorSubject<L.Map> = new BehaviorSubject(null);
    ready$: Observable<L.Map> = this.map$.pipe(filter(map => !!map));
    options: MapOptions;
    isMarketCluster;


    constructor($container: HTMLElement, options: MapOptions) {
        console.log("LeafletMap -> constructor -> options", options)
        this.options = options;
    }

    public initSettings(options: MapOptions) {
        const { initCallback,
            defaultZoomLevel,
            dontFitMapBounds,
            disableScrollZooming,
            minZoomLevel,
            mapProvider,
            credentials,
            defaultCenterPosition,
            markerClusteringSetting }: MapOptions = options;
        if (disableScrollZooming) {
            this.map.scrollWheelZoom.disable();
        }
        if (initCallback) {
            setTimeout(options.initCallback, 0);
        }
    }

    inited() {
        return !!this.map;
    }

    public setMap(map: L.Map) {
        this.map = map;
        this.map$.next(this.map);
    }

    getContainer() {
        return /* this.isMarketCluster ? this.markersCluster :*/ this.map;
    }
    /*
        fitBounds(bounds, useDefaultZoom) {
            if (bounds.isValid()) {
                if ((this.dontFitMapBounds || useDefaultZoom) && this.defaultZoomLevel) {
                    this.map.setZoom(this.defaultZoomLevel, { animate: false });
                    this.map.panTo(bounds.getCenter(), { animate: false });
                } else {
                    this.map.once('zoomend', () => {
                        if (!this.defaultZoomLevel && this.map.getZoom() > this.minZoomLevel) {
                            this.map.setZoom(this.minZoomLevel, { animate: false });
                        }
                    });
                    this.map.fitBounds(bounds, { padding: [50, 50], animate: false });
                }
            }
        }*/

    createLatLng(lat, lng) {
        return L.latLng(lat, lng);
    }

    createBounds() {
        return this.map.getBounds();
    }

    extendBounds(bounds, polyline) {
        if (polyline && polyline.getLatLngs() && polyline.getBounds()) {
            bounds.extend(polyline.getBounds());
        }
    }

    invalidateSize() {
        this.map.invalidateSize(true);
    }

    createTool0tip(marker, dsIndex, settings, markerArgs) {
        var popup = L.popup();
        popup.setContent('');
        marker.bindPopup(popup, { autoClose: settings.autocloseTooltip, closeOnClick: false });
        if (settings.displayTooltipAction == 'hover') {
            marker.off('click');
            marker.on('mouseover', function () {
                this.openPopup();
            });
            marker.on('mouseout', function () {
                this.closePopup();
            });
        }
        return {
            markerArgs: markerArgs,
            popup: popup,
            locationSettings: settings,
            dsIndex: dsIndex
        }
    }

    onResize() {

    }

    getTooltips() {
        return this.tooltips;//rewrite
    }

    getCenter() {
        return this.map.getCenter();
    }

    convertPosition(expression: any): L.LatLng {
        return L.latLng(expression[this.options.latKeyName], expression[this.options.lngKeyName]) as L.LatLng;
    }

    ////Markers
    updateMarkers(markersData) {
        markersData.forEach(data => {
            if (this.markers.get(data.aliasName)) {
                this.updateMarker(data.aliasName, this.convertPosition(data), this.options as MarkerSettings)
            }
            else {
                this.createMarker(data.aliasName, this.convertPosition(data), this.options as MarkerSettings);
            }
        });
    }

    private createMarker(key, location, settings: MarkerSettings) {
        this.ready$.subscribe(() => {
            let defaultSettings: MarkerSettings = {
                color: '#FD2785'
            }
            this.markers.set(key, new Marker(this.map, location, { ...defaultSettings, ...settings }))
        });
    }

    private updateMarker(key, location: L.LatLng, settings: MarkerSettings) {
        const marker: Marker = this.markers.get(key);
        if (!location.equals(marker.location)) {
            marker.updateMarkerPosition(location);
        }
        //other implements later

    }

    private deleteMarker() {

    }

    //polyline

    updatePolylines(polyData) {
        if (this.poly) {

        }

        else {
            this.map$
            this.createPolyline(polyData.map(data => this.convertPosition(data)), this.options);
        }

        /*  markersData.forEach(data => {
              if (this.markers.get(data.aliasName)) {
                  this.updateMarker(data.aliasName, this.convertPosition(data), this.options as MarkerSettings)
              }
              else {
                  this.createMarker(data.aliasName, this.convertPosition(data), this.options as MarkerSettings);
              }
          });*/
    }

    createPolyline(locations, settings) {
        this.ready$.subscribe(() =>
            this.poly = new Polyline(this.map, locations, settings)
        )
    }
}