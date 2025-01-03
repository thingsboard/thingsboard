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

import { FormattedData } from '@shared/models/widget.models';
import L from 'leaflet';
import { TileLayerOptions } from 'leaflet';

// redeclare module, maintains compatibility with @types/leaflet
declare module 'leaflet' {
  interface MarkerOptions {
    tbMarkerData?: FormattedData;
  }

  interface TileLayer {
    _url: string;
    _getSubdomain(tilePoint: L.Coords): string;
    _globalTileRange: L.Bounds;
  }

  namespace TB {

    interface SidebarControlOptions extends ControlOptions {
      container: JQuery<HTMLElement>;
      paneWidth?: number;
    }

    class SidebarControl extends Control<SidebarControlOptions> {
      constructor(options: SidebarControlOptions);
      addPane(pane: JQuery<HTMLElement>): this;
      togglePane(pane: JQuery<HTMLElement>, button: JQuery<HTMLElement>): void;
    }

    interface SidebarPaneControlOptions extends ControlOptions {
      sidebar: SidebarControl;
      uiClass: string;
      buttonTitle?: string;
      paneTitle: string;
    }

    class SidebarPaneControl<O extends SidebarPaneControlOptions> extends Control<O> {
      constructor(options: O);
      onAddPane(map: Map, button: JQuery<HTMLElement>, $ui: JQuery<HTMLElement>, toggle: (e: JQuery.MouseEventBase) => void);
    }

    interface LayerData {
      title: string;
      attributionPrefix?: string;
      layer: Layer;
      mini: Layer;
    }

    interface LayersControlOptions extends SidebarPaneControlOptions {
      layers: LayerData[];
    }

    class LayersControl extends SidebarPaneControl<LayersControlOptions> {
      constructor(options: LayersControlOptions);
    }

    function sidebar(options: SidebarControlOptions): SidebarControl;

    function sidebarPane<O extends SidebarPaneControlOptions>(options: O): SidebarPaneControl<O>;

    function layers(options: LayersControlOptions): LayersControl;

    namespace TileLayer {

      interface ChinaProvidersData {
        [provider: string]: {
          [type: string]: string;
          Subdomains: string;
        };
      }

      class ChinaProvider extends L.TileLayer {
        constructor(type: string, options?: TileLayerOptions);
      }
    }

    namespace tileLayer {
      function chinaProvider(type: string, options?: TileLayerOptions): TileLayer.ChinaProvider;
    }
  }
}
