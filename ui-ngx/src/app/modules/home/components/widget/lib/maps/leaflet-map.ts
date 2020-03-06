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
    bounds: L.LatLngBounds;


    constructor($container: HTMLElement, options: MapOptions) {
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
        if (this.options.useDefaultCenterPosition) {
            this.map.panTo(this.options.defaultCenterPosition);
            this.bounds = map.getBounds();
        }
        else this.bounds = new L.LatLngBounds(null, null)
        this.map$.next(this.map);
    }

    getContainer() {
        return this.map;
    }

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

    createTooltip(marker, dsIndex, settings, markerArgs) {
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
                this.updateMarker(data.aliasName, data, markersData, this.options as MarkerSettings)
            }
            else {
                this.createMarker(data.aliasName, data, markersData, this.options as MarkerSettings);
            }
        });
    }

    private createMarker(key, data, dataSources, settings: MarkerSettings) {
        this.ready$.subscribe(() => {
            const newMarker = new Marker(this.map, this.convertPosition(data), settings, data, dataSources);
            this.map.fitBounds(this.bounds.extend(newMarker.leafletMarker.getLatLng()));
            this.markers.set(key, newMarker);
        });
    }

    private updateMarker(key, data, dataSources, settings: MarkerSettings) {
        const marker: Marker = this.markers.get(key);
        let location = this.convertPosition(data)
        if (!location.equals(marker.location)) {
            marker.updateMarkerPosition(location);
        }
        marker.setDataSources(data, dataSources);
        marker.updateMarkerIcon(settings);
    }

    private deleteMarker() {

    }

    //polyline

    updatePolylines(polyData: Array<Array<any>>) {
        polyData.forEach(data => {
            if (data.length) {
                let dataSource = polyData.map(arr => arr[0]);
                if (this.poly) {
                    this.updatePolyline(data, dataSource, this.options);
                }
                else {
                    this.createPolyline(data, dataSource, this.options);
                }
            }
        })
    }

    createPolyline(data, dataSources, settings) {
        this.ready$.subscribe(() => {
            this.poly = new Polyline(this.map, data.map(data => this.convertPosition(data)), data, dataSources, settings);
            const bounds = this.bounds.extend(this.poly.leafletPoly.getBounds());
            if (bounds.isValid()) {
                this.map.fitBounds(bounds);
                this.bounds = bounds;
            }
        });
    }

    updatePolyline(data, dataSources, settings) {
        this.ready$.subscribe(() => {
            this.poly.updatePolyline(settings, data, dataSources);
            const bounds = this.bounds.extend(this.poly.leafletPoly.getBounds());
            if (bounds.isValid()) {
                this.map.fitBounds(bounds);
                this.bounds = bounds;
            }
        });
    }
}