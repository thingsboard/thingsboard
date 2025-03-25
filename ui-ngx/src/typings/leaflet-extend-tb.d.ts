///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { Map as MapLibreGLMap, MapOptions as MapLibreGLMapOptions } from 'maplibre-gl';
import { TbMapDatasource } from '@shared/models/widget/maps/map.models';
import { MatIconRegistry } from '@angular/material/icon';

// redeclare module, maintains compatibility with @types/leaflet
declare module 'leaflet' {
  interface MarkerOptions {
    tbMarkerData?: FormattedData<TbMapDatasource>;
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
      addPane(pane: JQuery<HTMLElement>, button: JQuery<HTMLElement>): this;
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

    interface DataLayer {
      toggleGroup(group: string): boolean;
    }

    interface GroupData {
      title: string;
      group: string;
      enabled: boolean;
      dataLayers: DataLayer[];
    }

    interface GroupsControlOptions extends SidebarPaneControlOptions {
      groups: GroupData[];
    }

    class GroupsControl extends SidebarPaneControl<GroupsControlOptions> {
      constructor(options: GroupsControlOptions);
    }

    interface TopToolbarButtonOptions {
      icon: string;
      color?: string;
      title: string;
    }

    class TopToolbarButton {
      constructor(options: TopToolbarButtonOptions, iconRegistry: MatIconRegistry);
      onClick(onClick: (e: MouseEvent, button: TopToolbarButton) => void): void;
      setActive(active: boolean): void;
      isActive(): boolean;
      setDisabled(disabled: boolean): void;
      isDisabled(): boolean;
    }

    interface TopToolbarControlOptions {
      mapElement: JQuery<HTMLElement>;
      iconRegistry: MatIconRegistry;
    }

    class TopToolbarControl {
      constructor(options: TopToolbarControlOptions);
      toolbarButton(options: TopToolbarButtonOptions): TopToolbarButton;
      setDisabled(disabled: boolean): void;
    }

    interface ToolbarButtonOptions {
      id: string;
      title: string;
      click: (e: MouseEvent, button: ToolbarButton) => void;
      iconClass: string;
      showText?: boolean;
    }

    class ToolbarButton {
      constructor(options: ToolbarButtonOptions);
      setActive(active: boolean): void;
      isActive(): boolean;
      setDisabled(disabled: boolean): void;
      isDisabled(): boolean;
    }

    class ToolbarControl extends Control<ControlOptions> {
      constructor(options: ControlOptions);
      toolbarButton(options: ToolbarButtonOptions): ToolbarButton;
    }

    interface BottomToolbarControlOptions {
      mapElement: JQuery<HTMLElement>;
      closeTitle: string;
      onClose: () => boolean;
    }

    class BottomToolbarControl {
      constructor(options: BottomToolbarControlOptions);
      getButton(id: string): ToolbarButton | undefined;
      open(buttons: ToolbarButtonOptions[], showCloseButton?: boolean): void;
      close(): void;
      container: HTMLElement;
    }

    function sidebar(options: SidebarControlOptions): SidebarControl;

    function sidebarPane<O extends SidebarPaneControlOptions>(options: O): SidebarPaneControl<O>;

    function layers(options: LayersControlOptions): LayersControl;

    function groups(options: GroupsControlOptions): GroupsControl;

    function topToolbar(options: TopToolbarControlOptions): TopToolbarControl;

    function toolbar(options: ControlOptions): ToolbarControl;

    function bottomToolbar(options: BottomToolbarControlOptions): BottomToolbarControl;

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

    namespace MapLibreGL {

      interface LeafletMapLibreGLMapOptions extends L.InteractiveLayerOptions, Omit<MapLibreGLMapOptions, "container"> {
        updateInterval?: number;
        padding?: number;
        className?: string;
      }

      class MapLibreGLLayer extends L.Layer {
        constructor(options: LeafletMapLibreGLMapOptions);
        getMapLibreGLMap(): MapLibreGLMap
        getCanvas(): HTMLCanvasElement
        getSize(): L.Point
        getBounds(): L.LatLngBounds
        getContainer(): HTMLDivElement
        getPaneName(): string
      }

      function mapLibreGLLayer(options: LeafletMapLibreGLMapOptions): MapLibreGLLayer;
    }
  }
}
