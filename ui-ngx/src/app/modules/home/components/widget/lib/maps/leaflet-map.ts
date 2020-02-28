import * as L from 'leaflet';

import 'leaflet-providers';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import 'leaflet.markercluster/dist/leaflet.markercluster'

import { MapOptions } from './map-models';

export default class LeafletMap {

    markers = [];
    tooltips = [];
    map: L.Map;
    isMarketCluster;


    constructor($container, options: MapOptions) {
        this.isMarketCluster = options.markerClusteringSetting?.isMarketCluster;
        // defaultCenterPosition = options.defaultCenterPosition || [0, 0];
        this.map = L.map($container).setView(options.defaultCenterPosition, options.defaultZoomLevel || 8);

        if (options.disableScrollZooming) {
            this.map.scrollWheelZoom.disable();
        }
        let credetials = {
            app_id: "AhM6TzD9ThyK78CT3ptx",
            app_code: "p6NPiITB3Vv0GMUFnkLOOg"
        }
        var tileLayer = (L.tileLayer as any).provider("OpenStreetMap.Mapnik", credetials);
        tileLayer.addTo(this.map);

        /*  if (this.isMarketCluster) {
              this.markersCluster = L.markerClusterGroup(options.markerClusteringSetting);
              this.map.addLayer(this.markersCluster);
          }*/

        if (options.initCallback) {
            setTimeout(options.initCallback, 0); //eslint-disable-line
        }

    }

    inited() {
        return !!this.map;
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

    getTooltips() {
        return this.tooltips;//rewrite
    }

    getCenter() {
        return this.map.getCenter();
    }


}