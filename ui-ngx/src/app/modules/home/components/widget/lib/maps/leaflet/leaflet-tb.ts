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

import L, { Coords, TB, TileLayerOptions } from 'leaflet';
import { guid } from '@core/utils';

class SidebarControl extends L.Control<TB.SidebarControlOptions> {

  private readonly sidebar: JQuery<HTMLElement>;

  private current = $();
  private currentButton = $();

  private map: L.Map;

  constructor(options: TB.SidebarControlOptions) {
    super(options);
    this.sidebar = $('<div class="tb-map-sidebar"></div>');
    this.options.container.append(this.sidebar);
    const position = options?.position || 'topleft';
    if (['topleft', 'bottomleft'].includes(position)) {
      this.options.container.addClass('tb-sidebar-left');
    } else {
      this.options.container.addClass('tb-sidebar-right');
    }
  }

  addPane(pane: JQuery<HTMLElement>): this {
    pane.hide().appendTo(this.sidebar);
    return this;
  }

  togglePane(pane: JQuery<HTMLElement>, button: JQuery<HTMLElement>) {
    const paneWidth = this.options?.paneWidth || 220;
    const position = this.options?.position || 'topleft';

    this.current.hide().trigger('hide');
    this.currentButton.removeClass('active');

    if (this.current === pane) {
      if (['topleft', 'bottomleft'].includes(position)) {
        this.map.panBy([-paneWidth, 0], { animate: false });
      }
      this.sidebar.hide();
      this.current = this.currentButton = $();
    } else {
      this.sidebar.show();
      this.current = pane;
      this.currentButton = button || $();
      if (['topleft', 'bottomleft'].includes(position)) {
        this.map.panBy([paneWidth, 0], { animate: false });
      }
    }
    this.map.invalidateSize({ pan: false, animate: false });
    this.current.show().trigger('show');
    this.currentButton.addClass('active');
  }

  addTo(map: L.Map): this {
    this.map = map;
    return this;
  }
}

class SidebarPaneControl<O extends TB.SidebarPaneControlOptions> extends L.Control<O> {

  private button: JQuery<HTMLElement>;
  private $ui: JQuery<HTMLElement>;

  constructor(options: O) {
    super(options);
  }

  onAdd(map: L.Map): HTMLElement {
    const $container = $("<div>")
    .attr('class', 'leaflet-bar');

    this.button = $("<a>")
    .attr('class', 'tb-control-button')
    .attr('href', '#')
    .html('<div class="' + this.options.uiClass + '"></div>')
    .on('click', (e) => {
      this.toggle(e);
    });
    if (this.options.buttonTitle) {
      this.button.attr('title', this.options.buttonTitle);
    }
    this.button.appendTo($container);

    this.$ui = $('<div>')
        .attr('class', this.options.uiClass);

    $('<div class="tb-layers-title-container">')
    .appendTo(this.$ui)
    .append($('<div class="tb-layers-title">')
    .text(this.options.paneTitle))
    .append($('<div>')
    .append($('<button type="button" class="tb-button-close mdc-icon-button mat-mdc-icon-button">' +
      '<span class="mat-mdc-button-persistent-ripple mdc-icon-button__ripple"></span>' +
      '<span class="material-icons">close</span>' +
      '</button>')
    .attr('aria-label', 'Close')
    .bind('click', (e) => {
      this.toggle(e);
    })));

    this.options.sidebar.addPane(this.$ui);

    this.onAddPane(map, this.button, this.$ui, (e) => {
      this.toggle(e);
    });

    return $container[0];
  }

  public onAddPane(map: L.Map, button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, toggle: (e: JQuery.MouseEventBase) => void) {}

  private toggle(e: JQuery.MouseEventBase) {
    e.stopPropagation();
    e.preventDefault();
    if (!this.button.hasClass("disabled")) {
      this.options.sidebar.togglePane(this.$ui, this.button);
    }
  }
}

class LayersControl extends SidebarPaneControl<TB.LayersControlOptions> {
  constructor(options: TB.LayersControlOptions) {
    super(options);
  }

  public onAddPane(map: L.Map, button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, toggle: (e: JQuery.MouseEventBase) => void) {
    const paneId = guid();
    const layers = this.options.layers;
    const baseSection = $("<div>")
    .attr('class', 'tb-layers-container')
    .appendTo($ui);

    layers.forEach((layerData, i) => {
      const id = `map-ui-layer-${paneId}-${i}`;
      const buttonContainer = $('<div class="tb-layer-card">')
      .appendTo(baseSection);
      const mapContainer = $('<div class="tb-layer-map">')
      .appendTo(buttonContainer);
      const input = $('<input type="radio" class="tb-layer-button" name="layer">')
      .prop('id', id)
      .prop('checked', map.hasLayer(layerData.layer))
      .appendTo(buttonContainer);

      const item = $('<label class="tb-layer-label">')
      .prop('for', id)
      .append($('<span>').append(layerData.title))
      .appendTo(buttonContainer);

      map.whenReady(() => {

        const miniMap = L.map(mapContainer[0], { attributionControl: false, zoomControl: false, keyboard: false })
        .addLayer(layerData.mini);

        miniMap.dragging.disable();
        miniMap.touchZoom.disable();
        miniMap.doubleClickZoom.disable();
        miniMap.scrollWheelZoom.disable();

        const moved = () => {
          miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 2, 0));
        };

        const shown = () => {
          miniMap.invalidateSize();
          miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 2, 0), { animate: false });
          map.on('moveend', moved);
        };

        $ui.on('show', shown);
        $ui.on('hide', () => {
          map.off('moveend', moved);
        })
      });


      input.on('click', (e: JQuery.MouseEventBase) => {
        e.stopPropagation();
        layers.forEach((other) => {
          if (other.layer === layerData.layer) {
            map.addLayer(other.layer);
            map.attributionControl.setPrefix(other.attributionPrefix);
          } else {
            map.removeLayer(other.layer);
          }
        });
        map.fire('baselayerchange', { layer: layerData.layer });
      });

      item.on('dblclick', (e) => {
        toggle(e);
      });

      map.on('layeradd layerremove', function () {
        input.prop('checked', map.hasLayer(layerData.layer));
      });
    });
  }
}

const sidebar = (options: TB.SidebarControlOptions): SidebarControl => {
  return new SidebarControl(options);
}

const sidebarPane = <O extends TB.SidebarPaneControlOptions>(options: O): SidebarPaneControl<O> => {
  return new SidebarPaneControl(options);
}

const layers = (options: TB.LayersControlOptions): LayersControl => {
  return new LayersControl(options);
}

class ChinaProvider extends L.TileLayer {

  static chinaProviders: L.TB.TileLayer.ChinaProvidersData = {
    Tencent: {
      Normal: "//rt{s}.map.gtimg.com/tile?z={z}&x={x}&y={-y}&type=vector&styleid=3",
      Satellite: "//p{s}.map.gtimg.com/sateTiles/{z}/{sx}/{sy}/{x}_{-y}.jpg",
      Terrain: "//p{s}.map.gtimg.com/demTiles/{z}/{sx}/{sy}/{x}_{-y}.jpg",
      Subdomains: '0123',
    }
  };

  constructor(type: string, options?: TileLayerOptions) {
    options = options || {};

    const parts = type.split('.');
    const providerName = parts[0];
    const mapName = parts[1];

    const url = ChinaProvider.chinaProviders[providerName][mapName];
    options.subdomains = ChinaProvider.chinaProviders[providerName].Subdomains;

    super(url, options);
  }

  getTileUrl(coords: Coords): string {
    const data = {
      s: this._getSubdomain(coords),
      x: coords.x,
      y: coords.y,
      z: this._getZoomForUrl(),
      sx: null,
      sy: null
    };
    if (this._map && !this._map.options.crs.infinite) {
      const invertedY = this._globalTileRange.max.y - coords.y;
      if (this.options.tms) {
        data['y'] = invertedY;
      }
      data['-y'] = invertedY;
    }
    data.sx = data.x >> 4;
    data.sy = (( 1 << data.z) - data.y) >> 4;
    return L.Util.template(this._url, L.Util.extend(data, this.options));
  }
}

const chinaProvider = (type: string, options?: TileLayerOptions): ChinaProvider => {
  return new ChinaProvider(type, options);
}

L.TB = L.TB || {
  SidebarControl,
  SidebarPaneControl,
  LayersControl,
  sidebar,
  sidebarPane,
  layers,
  TileLayer: {
    ChinaProvider
  },
  tileLayer: {
    chinaProvider
  }
}
