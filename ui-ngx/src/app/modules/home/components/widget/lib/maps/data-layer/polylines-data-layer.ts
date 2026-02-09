///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  defaultBasePolylinesDataLayerSettings,
  isCutPolygon,
  isJSON,
  MapDataLayerType,
  PolylinesDataLayerSettings,
  TbMapDatasource,
  TbPolylineCoordinates,
  TbPolylineData,
  TbPolylineRawCoordinates
} from '@shared/models/widget/maps/map.models';
import L from 'leaflet';
import { DataKey, FormattedData } from '@shared/models/widget.models';
import { ShapeStyleInfo, TbShapesDataLayer } from '@home/components/widget/lib/maps/data-layer/shapes-data-layer';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { Observable } from 'rxjs';
import { isNotEmptyStr, isString } from '@core/utils';
import {
  TbLatestDataLayerItem,
  UnplacedMapDataItem
} from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';
import { map } from 'rxjs/operators';

class TbPolylineDataLayerItem extends TbLatestDataLayerItem<PolylinesDataLayerSettings, TbPolylineDataLayer> {

  private polylineContainer: L.FeatureGroup;
  private polyline: L.Polyline;
  private polylineStyleInfo: ShapeStyleInfo;
  private editing = false;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: PolylinesDataLayerSettings,
              protected dataLayer: TbPolylineDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  public isEditing() {
    return this.editing;
  }

  public updateBubblingMouseEvents() {
    this.polyline.options.bubblingMouseEvents = !this.dataLayer.isEditMode();
  }

  public remove() {
    super.remove();
    if (this.polylineStyleInfo?.patternId) {
      this.dataLayer.getMap().unUseShapePattern(this.polylineStyleInfo.patternId);
    }
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    const polyData = this.dataLayer.extractPolylineCoordinates(data);
    const polyConstructor = L.polyline;
    this.polyline = polyConstructor(polyData as (TbPolylineRawCoordinates & L.LatLngTuple[]), {
      noClip: true,
      snapIgnore: !this.dataLayer.isSnappable(),
      bubblingMouseEvents: !this.dataLayer.isEditMode()
    });

    this.dataLayer.getStrokeStyle(data, dsData, this.polylineStyleInfo?.patternId).subscribe((styleInfo) => {
      this.polylineStyleInfo = styleInfo;
      if (this.polyline) {
        this.polyline.setStyle(this.polylineStyleInfo.style);
      }
    });

    this.polylineContainer = L.featureGroup();
    this.polyline.addTo(this.polylineContainer);

    this.updateLabel(data, dsData);
    return this.polylineContainer;
  }

  protected unbindLabel() {
    this.polylineContainer.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.polylineContainer.bindTooltip(content, {className: 'tb-polyline-label', permanent: true, direction: 'center'})
      .openTooltip(this.polylineContainer.getBounds().getCenter());
  }

  protected doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getStrokeStyle(data, dsData, this.polylineStyleInfo?.patternId).subscribe((styleInfo) => {
      this.polylineStyleInfo = styleInfo;
      this.updatePolylineShape(data);
      this.updateTooltip(data, dsData);
      this.updateLabel(data, dsData);

      if (!this.editing || !this.dataLayer.getMap().getMap().pm.globalCutModeEnabled()) {
        this.polyline.setStyle(this.polylineStyleInfo.style);
      }
    });
  }

  protected doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): void {
    this.updatePolylineShape(data);
  }

  protected addItemClass(clazz: string): void {
    if ((this.polyline as any)._path) {
      L.DomUtil.addClass((this.polyline as any)._path, clazz);
    }
  }

  protected removeItemClass(clazz: string): void {
    if ((this.polyline as any)._path) {
      L.DomUtil.removeClass((this.polyline as any)._path, clazz);
    }
  }

  protected enableDrag(): void {
    this.polyline.pm.setOptions({
      snappable: this.dataLayer.isSnappable()
    });
    this.polyline.pm.enableLayerDrag();
    this.polyline.on('pm:dragstart', () => {
      this.editing = true;
    });
    this.polyline.on('pm:drag', () => {
      if (this.tooltip?.isOpen()) {
        this.tooltip.setLatLng(this.polyline.getBounds().getCenter());
      }
    });
    this.polyline.on('pm:dragend', () => {
      this.savePolylineCoordinates();
      this.editing = false;
    });
  }

  protected disableDrag(): void {
    this.polyline.pm.disableLayerDrag();
    this.polyline.off('pm:dragstart');
    this.polyline.off('pm:dragend');
  }

  protected onSelected(): L.TB.ToolbarButtonOptions[] {
    const buttons: L.TB.ToolbarButtonOptions[] = [];
    if (this.dataLayer.isEditEnabled()) {
      this.enablePolylineEditMode();
      buttons.push(
        {
          id: 'cut',
          title: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.cut'),
          iconClass: 'tb-cut',
          click: (e, button) => {
            const map = this.dataLayer.getMap().getMap();
            if (!map.pm.globalCutModeEnabled()) {
              this.disablePolylineRotateMode();
              this.disablePolylineEditMode();
              this.enablePolylineCutMode(button);
            } else {
              this.disablePolylineCutMode(button);
              this.enablePolylineEditMode();
            }
          }
        },
        {
          id: 'rotate',
          title: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polyline.rotate'),
          iconClass: 'tb-rotate',
          click: (e, button) => {
            if (!this.polyline.pm.rotateEnabled()) {
              this.disablePolylineCutMode();
              this.disablePolylineEditMode();
              this.enablePolylineRotateMode(button);
            } else {
              this.disablePolylineRotateMode(button);
              this.enablePolylineEditMode();
            }
          }
        }
      );
    }
    return buttons;
  }

  protected onDeselected(): void {
    if (this.dataLayer.isEditEnabled()) {
      this.disablePolylineEditMode();
      this.disablePolylineRotateMode();
    }
  }

  protected canDeselect(cancel = false): boolean {
    const map = this.dataLayer.getMap().getMap();
    if (map.pm.globalCutModeEnabled()) {
      if (cancel) {
        this.disablePolylineCutMode();
      }
      return false;
    } else if (this.polyline.pm.rotateEnabled()) {
      if (cancel) {
        this.disablePolylineRotateMode();
      }
      return false;
    } else if (this.editing) {
      return false;
    }
    return true;
  }

  protected removeDataItemTitle(): string {
    return this.dataLayer.getCtx().translate.instant('widgets.maps.data-layer.polyline.remove-polyline-for', {entityName: this.data.entityName});
  }

  protected removeDataItem(): Observable<any> {
    return this.dataLayer.savePolylineCoordinates(this.data, null);
  }

  private enablePolylineEditMode() {
    this.polyline.on('pm:markerdragstart', () => this.editing = true);
    this.polyline.on('pm:markerdragend', () => setTimeout(() => {
      this.editing = false;
    }));
    this.polyline.on('pm:edit', () => this.savePolylineCoordinates());
    this.polyline.pm.enable();
    const map = this.dataLayer.getMap();
    map.getEditToolbar().getButton('remove')?.setDisabled(false);
  }

  private disablePolylineEditMode() {
    this.polyline.pm.disable();
    this.polyline.off('pm:markerdragstart');
    this.polyline.off('pm:markerdragend');
    this.polyline.off('pm:edit');
    const map = this.dataLayer.getMap();
    map.getEditToolbar().getButton('remove')?.setDisabled(true);
  }

  private enablePolylineCutMode(cutButton?: L.TB.ToolbarButton) {
    this.polylineContainer.closePopup();
    this.editing = true;
    this.polyline.options.bubblingMouseEvents = true;
    this.polyline.setStyle({
      ...this.polylineStyleInfo.style, dashArray: '5 5', weight: 3,
      color: '#3388ff', opacity: 1, fillColor: '#3388ff', fillOpacity: 0.2
    });
    this.addItemClass('tb-cut-mode');
    this.polyline.once('pm:cut', (e) => {
      if (e.layer instanceof L.Polyline) {
        this.polyline = L.polyline(e.layer.getLatLngs() as L.LatLngExpression[] | L.LatLngExpression[][] , {
          ...this.polylineStyleInfo.style,
          snapIgnore: !this.dataLayer.isSnappable(),
          bubblingMouseEvents: !this.dataLayer.isEditMode()
        });
        this.polyline.addTo(this.polylineContainer);
      } else if (e.layer instanceof L.LayerGroup) {
        const parts: L.LatLngExpression[][] = [];

        if (e.layer && typeof e.layer.getLayers === 'function') {
          const segments: L.Polyline[] = e.layer.getLayers() as L.Polyline[];
          segments.forEach(segment => {
            parts.push(segment.getLatLngs() as L.LatLngExpression[]);
          });
        } else if (e.layer instanceof L.Polyline) {
          parts.push(e.layer.getLatLngs() as L.LatLngExpression[]);
        }

        if (parts.length > 0) {
          this.polyline = L.polyline(parts, {
            ...this.polylineStyleInfo.style,
            snapIgnore: !this.dataLayer.isSnappable(),
            bubblingMouseEvents: !this.dataLayer.isEditMode()
          });
          this.polyline.addTo(this.polylineContainer);
        }
      }
      // @ts-ignore
      e.layer._pmTempLayer = true;
      e.layer.remove();
      this.polylineContainer.removeLayer(this.polyline);
      // @ts-ignore
      this.polyline._pmTempLayer = false;
      this.polyline.addTo(this.polylineContainer);
      this.updateSelectedState();
      cutButton?.setActive(false);
      this.savePolylineCoordinates();
    });
    const map = this.dataLayer.getMap().getMap();
    map.pm.setLang('en', {
      tooltips: {
        firstVertex: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polyline.polyline-place-first-point-cut-hint'),
        finishLine: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polyline.finish-polyline-cut-hint')
      }
    }, 'en');
    map.pm.enableGlobalCutMode({
      // @ts-ignore
      layersToCut: [this.polyline]
    });
    // @ts-ignore
    L.DomUtil.addClass(map.pm.Draw.Cut._hintMarker.getTooltip()._container, 'tb-place-item-label');
    cutButton?.setActive(true);
    map.once('pm:globalcutmodetoggled', (e) => {
      if (!e.enabled) {
        this.disablePolylineCutMode(cutButton);
        this.enablePolylineEditMode();
      }
    });
  }

  private disablePolylineCutMode(cutButton?: L.TB.ToolbarButton) {
    this.editing = false;
    this.polyline.options.bubblingMouseEvents = !this.dataLayer.isEditMode();
    this.polyline.setStyle({...this.polylineStyleInfo.style, dashArray: null});
    this.removeItemClass('tb-cut-mode');
    this.polyline.off('pm:cut');
    const map = this.dataLayer.getMap().getMap();
    map.pm.disableGlobalCutMode();
    cutButton?.setActive(false);
  }

  private enablePolylineRotateMode(rotateButton?: L.TB.ToolbarButton) {
    this.polylineContainer.closePopup();
    this.editing = true;
    this.polyline.on('pm:rotateend', () => {
      this.savePolylineCoordinates();
    });
    this.polyline.pm.enableRotate();
    rotateButton?.setActive(true);
    this.polyline.on('pm:rotatedisable', () => {
      this.disablePolylineRotateMode(rotateButton);
      this.enablePolylineEditMode();
    });
  }

  private disablePolylineRotateMode(rotateButton?: L.TB.ToolbarButton) {
    this.editing = false;
    this.polyline.pm.disableRotate();
    this.polyline.off('pm:rotateend');
    this.polyline.off('pm:rotatedisable');
    rotateButton?.setActive(false);
  }

  private savePolylineCoordinates() {
    let coordinates: TbPolylineCoordinates = this.polyline.getLatLngs();
    if (coordinates.length === 1) {
      coordinates = coordinates[0] as TbPolylineCoordinates;
    }
    if (this.polyline instanceof L.Rectangle && !isCutPolygon(coordinates)) {
      const bounds = this.polyline.getBounds();
      const boundsArray = [bounds.getNorthWest(), bounds.getNorthEast(), bounds.getSouthWest(), bounds.getSouthEast()];
      if (coordinates.every(point => boundsArray.find(boundPoint => boundPoint.equals(point as L.LatLng)) !== undefined)) {
        coordinates = [bounds.getNorthWest(), bounds.getSouthEast()];
      }
    }
    this.dataLayer.savePolylineCoordinates(this.data, coordinates).subscribe();
  }

  private updatePolylineShape(data: FormattedData<TbMapDatasource>) {
    if (this.editing) {
      return;
    }

    const polyData = this.dataLayer.extractPolylineCoordinates(data) as TbPolylineData;
    if (this.polyline instanceof L.Polyline && !(this.polyline instanceof L.Rectangle)) {
      this.polyline.setLatLngs(polyData as L.LatLngExpression[] | L.LatLngExpression[][]);
    } else if (this.polyline instanceof L.Rectangle && polyData.length === 2) {
      const bounds = new L.LatLngBounds(polyData as L.LatLngTuple[]);
      this.polyline.setBounds(bounds);
    } else {
      this.polylineContainer.removeLayer(this.polyline);
      if (polyData.length === 2 && isCutPolygon(polyData)) {
        this.polyline = L.polyline(polyData as L.LatLngExpression[] | L.LatLngExpression[][], {
          ...this.polylineStyleInfo.style,
          snapIgnore: !this.dataLayer.isSnappable(),
          bubblingMouseEvents: !this.dataLayer.isEditMode(),
          noClip: true
        });
      }

      this.polyline.addTo(this.polylineContainer);
      this.editModeUpdated();
    }
  }
}

export class TbPolylineDataLayer extends TbShapesDataLayer<PolylinesDataLayerSettings, TbPolylineDataLayer> {

  constructor(protected map: TbMap<any>,
              inputSettings: PolylinesDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return 'polylines';
  }

  public placeItem(item: UnplacedMapDataItem, layer: L.Layer): void {
    if (layer instanceof L.Polyline) {
      let coordinates: TbPolylineCoordinates;
      coordinates = layer.getLatLngs();
      if (coordinates.length === 1) {
        coordinates = coordinates[0] as TbPolylineCoordinates;
      }
      this.savePolylineCoordinates(item.entity, coordinates).subscribe(
        (converted) => {
          item.entity[this.settings.polylineKey.label] = JSON.stringify(converted);
          this.createItemFromUnplaced(item);
        }
      );
    } else {
      console.warn('Unable to place item, layer is not a polyline.');
    }
  }

  public extractPolylineCoordinates(data: FormattedData<TbMapDatasource>): TbPolylineRawCoordinates {
    let rawPolyData = data[this.settings.polylineKey.label];
    if (isString(rawPolyData)) {
      rawPolyData = JSON.parse(rawPolyData);
    }
    return this.map.polylineDataToCoordinates(rawPolyData);
  }

  public savePolylineCoordinates(data: FormattedData<TbMapDatasource>, coordinates: TbPolylineCoordinates): Observable<TbPolylineRawCoordinates> {
    const converted = coordinates ? this.map.coordinatesToPolylineData(coordinates) : null;
    const polylineData = [
      {
        dataKey: this.settings.polylineKey,
        value: converted
      }
    ];
    return this.map.saveItemData(data.$datasource, polylineData, this.settings.edit?.attributeScope).pipe(
      map(() => converted)
    );
  }

  protected getDataKeys(): DataKey[] {
    return [this.settings.polylineKey];
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<PolylinesDataLayerSettings> {
    return defaultBasePolylinesDataLayerSettings(map.type());
  }

  protected doSetup(): Observable<any> {
    return super.doSetup();
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return layerData && ((isNotEmptyStr(layerData[this.settings.polylineKey.label]) && !isJSON(layerData[this.settings.polylineKey.label])
      || Array.isArray(layerData[this.settings.polylineKey.label])));
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbPolylineDataLayerItem {
    return new TbPolylineDataLayerItem(data, dsData, this.settings, this);
  }
}
