import * as L from 'leaflet';

import 'leaflet-providers';
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import 'leaflet.markercluster/dist/leaflet.markercluster'

import { MapOptions, MarkerSettings } from './map-models';
import { Marker } from './markers';
import { Observable, of } from 'rxjs';

export default class LeafletMap {

    markers = [];
    tooltips = [];
    map: L.Map;
    map$: Observable<L.Map>;
    options: MapOptions;
    isMarketCluster;


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
        this.map$ = of(this.map);
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

    ////Markers


    createMarker() {
        let setings: MarkerSettings = {

        }
        this.markers.push(new Marker(this.map$, { lat: 0, lng: 0 }, setings))
    }

    updateMarker() {

    }

    deleteMarker() {

    }


}