///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  BaseMapSettings,
  DEFAULT_ZOOM_LEVEL,
  defaultGeoMapSettings,
  defaultImageMapSettings,
  GeoMapSettings,
  ImageMapSettings,
  MapSetting,
  MapType,
  MapZoomAction,
  parseCenterPosition
} from '@home/components/widget/lib/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { mergeDeep } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L from 'leaflet';
import { forkJoin, Observable, of } from 'rxjs';
import { TbMapLayer } from '@home/components/widget/lib/maps/map-layer';
import { map } from 'rxjs/operators';

export abstract class TbMap<S extends BaseMapSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapSetting>,
                      mapElement: HTMLElement): TbMap<MapSetting> {
    switch (inputSettings.mapType) {
      case MapType.geoMap:
        return new TbGeoMap(ctx, inputSettings, mapElement);
      case MapType.image:
        return new TbImageMap(ctx, inputSettings, mapElement);
    }
  }

  protected settings: S;
  protected map: L.Map;

  protected defaultCenterPosition: [number, number];
  protected bounds: L.LatLngBounds;
  protected layerControl: L.Control.Layers;

  protected mapElement: HTMLElement;
  protected sidebarElement: HTMLElement;

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<S>,
                        protected containerElement: HTMLElement) {
    this.settings = mergeDeep({} as S, this.defaultSettings(), this.inputSettings as S);
    $(containerElement).empty();
    $(containerElement).addClass('tb-map-layout');
    if (this.settings.controlsPosition.endsWith('left')) {
      $(containerElement).addClass('tb-sidebar-left');
    } else {
      $(containerElement).addClass('tb-sidebar-right');
    }
    const mapElement = $('<div class="tb-map"></div>');
    const sidebarElement = $('<div class="tb-map-sidebar"></div>');
    $(containerElement).append(mapElement);
    $(containerElement).append(sidebarElement);

    this.mapElement = mapElement[0];
    this.sidebarElement = sidebarElement[0];

    this.defaultCenterPosition = parseCenterPosition(this.settings.defaultCenterPosition);

    this.layerControl = L.control.layers({}, {}, {position: this.settings.controlsPosition, collapsed: true});
    this.createMap().subscribe((map) => {
      this.map = map;
      this.initMap();
    });
    L.TB = {
      sidebar: (s) => { return null;}
    };
  }

  private initMap() {
    this.map.zoomControl.setPosition(this.settings.controlsPosition);
    this.layerControl.addTo(this.map);
    this.map.on('move', () => {
      this.ctx.updatePopoverPositions();
    });
    this.map.on('zoomstart', () => {
      this.ctx.setPopoversHidden(true);
    });
    this.map.on('zoomend', () => {
      this.ctx.setPopoversHidden(false);
      this.ctx.updatePopoverPositions();
      setTimeout(() => {
        this.ctx.updatePopoverPositions();
      });
    });
    if (this.settings.useDefaultCenterPosition) {
      this.map.panTo(this.defaultCenterPosition);
      this.bounds = this.map.getBounds();
    } else {
      this.bounds = new L.LatLngBounds(null, null);
    }
  }

  protected abstract defaultSettings(): S;

  protected abstract createMap(): Observable<L.Map>;

  public destroy() {
    if (this.map) {
      this.map.remove();
    }
  }

}

class TbGeoMap extends TbMap<GeoMapSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<GeoMapSettings>,
              protected containerElement: HTMLElement) {
    super(ctx, inputSettings, containerElement);
  }

  protected defaultSettings(): GeoMapSettings {
    return defaultGeoMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const theMap = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons)
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return this.loadLayers().pipe(
      map((layers) => {
        if (layers.length) {
          const layer = layers[0];
          layer.layer.addTo(theMap);
          if (layers.length > 1) {
            layers.forEach(l => {
              this.layerControl.addBaseLayer(l.layer, l.title);
            });
          }
        }
        return theMap;
      })
    );
  }

  private loadLayers(): Observable<{title: string, layer: L.Layer}[]> {
    const layers = this.settings.layers.map(settings => TbMapLayer.fromSettings(this.ctx, settings));
    return forkJoin(layers.map(layer => layer.loadLayer()));
  }


}

class TbImageMap extends TbMap<ImageMapSettings> {

  private maxZoom = 4;

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<ImageMapSettings>,
              protected mapElement: HTMLElement) {
    super(ctx, inputSettings, mapElement);
  }

  protected defaultSettings(): ImageMapSettings {
    return defaultImageMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const map = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons),
      minZoom: 1,
      maxZoom: this.maxZoom,
      zoom: 1,
      crs: L.CRS.Simple,
      attributionControl: false
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return of(map);
  }
}
