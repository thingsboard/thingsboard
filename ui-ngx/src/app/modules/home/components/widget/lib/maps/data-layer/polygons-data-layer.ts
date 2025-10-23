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

import {
  defaultBasePolygonsDataLayerSettings,
  isCutPolygon, isJSON, MapDataLayerType,
  PolygonsDataLayerSettings,
  TbMapDatasource, TbPolyData, TbPolygonCoordinates, TbPolygonRawCoordinates
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

class TbPolygonDataLayerItem extends TbLatestDataLayerItem<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

  private polygonContainer: L.FeatureGroup;
  private polygon: L.Polygon;
  private polygonStyleInfo: ShapeStyleInfo;
  private editing = false;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: PolygonsDataLayerSettings,
              protected dataLayer: TbPolygonsDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  public isEditing() {
    return this.editing;
  }

  public updateBubblingMouseEvents() {
    this.polygon.options.bubblingMouseEvents = !this.dataLayer.isEditMode();
  }

  public remove() {
    super.remove();
    if (this.polygonStyleInfo?.patternId) {
      this.dataLayer.getMap().unUseShapePattern(this.polygonStyleInfo.patternId);
    }
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    const polyConstructor = isCutPolygon(polyData) || polyData.length !== 2 ? L.polygon : L.rectangle;
    this.polygon = polyConstructor(polyData as (TbPolygonRawCoordinates & L.LatLngTuple[]), {
      noClip: true,
      snapIgnore: !this.dataLayer.isSnappable(),
      bubblingMouseEvents: !this.dataLayer.isEditMode()
    });

    this.dataLayer.getShapeStyle(data, dsData, this.polygonStyleInfo?.patternId).subscribe((styleInfo) => {
      this.polygonStyleInfo = styleInfo;
      if (this.polygon) {
        this.polygon.setStyle(this.polygonStyleInfo.style);
      }
    });

    this.polygonContainer = L.featureGroup();
    this.polygon.addTo(this.polygonContainer);

    this.updateLabel(data, dsData);
    return this.polygonContainer;
  }

  protected unbindLabel() {
    this.polygonContainer.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.polygonContainer.bindTooltip(content, {className: 'tb-polygon-label', permanent: true, direction: 'center'})
      .openTooltip(this.polygonContainer.getBounds().getCenter());
  }

  protected doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getShapeStyle(data, dsData, this.polygonStyleInfo?.patternId).subscribe((styleInfo) => {
      this.polygonStyleInfo = styleInfo;
      this.updatePolygonShape(data);
      this.updateTooltip(data, dsData);
      this.updateLabel(data, dsData);
      if (!this.editing || !this.dataLayer.getMap().getMap().pm.globalCutModeEnabled()) {
        this.polygon.setStyle(this.polygonStyleInfo.style);
      }
    });
  }

  protected doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): void {
    this.updatePolygonShape(data);
  }

  protected addItemClass(clazz: string): void {
    if ((this.polygon as any)._path) {
      L.DomUtil.addClass((this.polygon as any)._path, clazz);
    }
  }

  protected removeItemClass(clazz: string): void {
    if ((this.polygon as any)._path) {
      L.DomUtil.removeClass((this.polygon as any)._path, clazz);
    }
  }

  protected enableDrag(): void {
    this.polygon.pm.setOptions({
      snappable: this.dataLayer.isSnappable()
    });
    this.polygon.pm.enableLayerDrag();
    this.polygon.on('pm:dragstart', () => {
      this.editing = true;
    });
    this.polygon.on('pm:drag', () => {
      if (this.tooltip?.isOpen()) {
        this.tooltip.setLatLng(this.polygon.getBounds().getCenter());
      }
    });
    this.polygon.on('pm:dragend', () => {
      this.savePolygonCoordinates();
      this.editing = false;
    });
  }

  protected disableDrag(): void {
    this.polygon.pm.disableLayerDrag();
    this.polygon.off('pm:dragstart');
    this.polygon.off('pm:dragend');
  }

  protected onSelected(): L.TB.ToolbarButtonOptions[] {
    const buttons:  L.TB.ToolbarButtonOptions[] = [];
    if (this.dataLayer.isEditEnabled()) {
      this.enablePolygonEditMode();
      buttons.push(
        {
          id: 'cut',
          title: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.cut'),
          iconClass: 'tb-cut',
          click: (e, button) => {
            const map = this.dataLayer.getMap().getMap();
            if (!map.pm.globalCutModeEnabled()) {
              this.disablePolygonRotateMode();
              this.disablePolygonEditMode();
              this.enablePolygonCutMode(button);
            } else {
              this.disablePolygonCutMode(button);
              this.enablePolygonEditMode();
            }
          }
        },
        {
          id: 'rotate',
          title: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.rotate'),
          iconClass: 'tb-rotate',
          click: (e, button) => {
            if (!this.polygon.pm.rotateEnabled()) {
              this.disablePolygonCutMode();
              this.disablePolygonEditMode();
              this.enablePolygonRotateMode(button);
            } else {
              this.disablePolygonRotateMode(button);
              this.enablePolygonEditMode();
            }
          }
        }
      );
    }
    return buttons;
  }

  protected onDeselected(): void {
    if (this.dataLayer.isEditEnabled()) {
      this.disablePolygonEditMode();
      this.disablePolygonCutMode();
      this.disablePolygonRotateMode();
    }
  }

  protected canDeselect(cancel = false): boolean {
    const map = this.dataLayer.getMap().getMap();
    if (map.pm.globalCutModeEnabled()) {
      if (cancel) {
        this.disablePolygonCutMode();
      }
      return false;
    } else if (this.polygon.pm.rotateEnabled()) {
      if (cancel) {
        this.disablePolygonRotateMode();
      }
      return false;
    } else if (this.editing) {
      return false;
    }
    return true;
  }

  protected removeDataItemTitle(): string {
    return this.dataLayer.getCtx().translate.instant('widgets.maps.data-layer.polygon.remove-polygon-for', {entityName: this.data.entityName});
  }

  protected removeDataItem(): Observable<any> {
    return this.dataLayer.savePolygonCoordinates(this.data, null);
  }

  private enablePolygonEditMode() {
    this.polygon.on('pm:markerdragstart', () => this.editing = true);
    this.polygon.on('pm:markerdragend', () => setTimeout(() => {
      this.editing = false;
    }) );
    this.polygon.on('pm:edit', () => this.savePolygonCoordinates());
    this.polygon.pm.enable();
    const map = this.dataLayer.getMap();
    map.getEditToolbar().getButton('remove')?.setDisabled(false);
  }

  private disablePolygonEditMode() {
    this.polygon.pm.disable();
    this.polygon.off('pm:markerdragstart');
    this.polygon.off('pm:markerdragend');
    this.polygon.off('pm:edit');
    const map = this.dataLayer.getMap();
    map.getEditToolbar().getButton('remove')?.setDisabled(true);
  }

  private enablePolygonCutMode(cutButton?: L.TB.ToolbarButton) {
    this.polygonContainer.closePopup();
    this.editing = true;
    this.polygon.options.bubblingMouseEvents = true;
    this.polygon.setStyle({...this.polygonStyleInfo.style, dashArray: '5 5', weight: 3,
      color: '#3388ff', opacity: 1, fillColor: '#3388ff', fillOpacity: 0.2});
    this.addItemClass('tb-cut-mode');
    this.polygon.once('pm:cut', (e) => {
      if (e.layer instanceof L.Polygon) {
        if (this.polygon instanceof L.Rectangle) {
          this.polygonContainer.removeLayer(this.polygon);
          this.polygon = L.polygon(e.layer.getLatLngs(), {
            ...this.polygonStyleInfo.style,
            snapIgnore: !this.dataLayer.isSnappable(),
            bubblingMouseEvents: !this.dataLayer.isEditMode()
          });
          this.polygon.addTo(this.polygonContainer);
        } else {
          this.polygon.setLatLngs(e.layer.getLatLngs());
        }
      }
      // @ts-ignore
      e.layer._pmTempLayer = true;
      e.layer.remove();
      this.polygonContainer.removeLayer(this.polygon);
      // @ts-ignore
      this.polygon._pmTempLayer = false;
      this.polygon.addTo(this.polygonContainer);
      this.updateSelectedState();
      cutButton?.setActive(false);
      this.savePolygonCoordinates()
    });
    const map = this.dataLayer.getMap().getMap();
    map.pm.setLang('en', {
      tooltips: {
        firstVertex: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.polygon-place-first-point-cut-hint'),
        continueLine: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.continue-polygon-cut-hint'),
        finishPoly: this.getDataLayer().getCtx().translate.instant('widgets.maps.data-layer.polygon.finish-polygon-cut-hint')
      }
    }, 'en');
    map.pm.enableGlobalCutMode({
      // @ts-ignore
      layersToCut: [this.polygon]
    });
    // @ts-ignore
    L.DomUtil.addClass(map.pm.Draw.Cut._hintMarker.getTooltip()._container, 'tb-place-item-label');
    cutButton?.setActive(true);
    map.once('pm:globalcutmodetoggled', (e) => {
      if (!e.enabled) {
        this.disablePolygonCutMode(cutButton);
        this.enablePolygonEditMode();
      }
    });
  }

  private disablePolygonCutMode(cutButton?: L.TB.ToolbarButton) {
    this.editing = false;
    this.polygon.options.bubblingMouseEvents = !this.dataLayer.isEditMode();
    this.polygon.setStyle({...this.polygonStyleInfo.style, dashArray: null});
    this.removeItemClass('tb-cut-mode');
    this.polygon.off('pm:cut');
    const map = this.dataLayer.getMap().getMap();
    map.pm.disableGlobalCutMode();
    cutButton?.setActive(false);
  }

  private enablePolygonRotateMode(rotateButton?: L.TB.ToolbarButton) {
    this.polygonContainer.closePopup();
    this.editing = true;
    this.polygon.on('pm:rotateend', () => {
      this.savePolygonCoordinates();
    });
    this.polygon.pm.enableRotate();
    rotateButton?.setActive(true);
    this.polygon.on('pm:rotatedisable', () => {
      this.disablePolygonRotateMode(rotateButton);
      this.enablePolygonEditMode();
    });
  }

  private disablePolygonRotateMode(rotateButton?: L.TB.ToolbarButton) {
    this.editing = false;
    this.polygon.pm.disableRotate();
    this.polygon.off('pm:rotateend');
    this.polygon.off('pm:rotatedisable');
    rotateButton?.setActive(false);
  }

  private savePolygonCoordinates() {
    let coordinates: TbPolygonCoordinates = this.polygon.getLatLngs();
    if (coordinates.length === 1) {
      coordinates = coordinates[0] as TbPolygonCoordinates;
    }
    if (this.polygon instanceof L.Rectangle && !isCutPolygon(coordinates)) {
      const bounds = this.polygon.getBounds();
      const boundsArray = [bounds.getNorthWest(), bounds.getNorthEast(), bounds.getSouthWest(), bounds.getSouthEast()];
      if (coordinates.every(point => boundsArray.find(boundPoint => boundPoint.equals(point as L.LatLng)) !== undefined)) {
        coordinates = [bounds.getNorthWest(), bounds.getSouthEast()];
      }
    }
    this.dataLayer.savePolygonCoordinates(this.data, coordinates).subscribe();
  }

  private updatePolygonShape(data: FormattedData<TbMapDatasource>) {
    if (this.editing) {
      return;
    }
    const polyData = this.dataLayer.extractPolygonCoordinates(data) as TbPolyData;
    if (isCutPolygon(polyData) || polyData.length !== 2) {
      if (this.polygon instanceof L.Rectangle) {
        this.polygonContainer.removeLayer(this.polygon);
        this.polygon = L.polygon(polyData, {
          ...this.polygonStyleInfo.style,
          snapIgnore: !this.dataLayer.isSnappable(),
          bubblingMouseEvents: !this.dataLayer.isEditMode(),
          noClip: true
        });
        this.polygon.addTo(this.polygonContainer);
        this.editModeUpdated();
      } else {
        this.polygon.setLatLngs(polyData);
      }
    } else if (polyData.length === 2) {
      const bounds = new L.LatLngBounds(polyData as L.LatLngTuple[]);
      (this.polygon as L.Rectangle).setBounds(bounds);
    }
  }

}

export class TbPolygonsDataLayer extends TbShapesDataLayer<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

  constructor(protected map: TbMap<any>,
              inputSettings: PolygonsDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return 'polygons';
  }

  public placeItem(item: UnplacedMapDataItem, layer: L.Layer): void {
    if (layer instanceof L.Polygon) {
      let coordinates: TbPolygonCoordinates;
      if (layer instanceof L.Rectangle) {
        const bounds = layer.getBounds();
        coordinates = [bounds.getNorthWest(), bounds.getSouthEast()];
      } else {
        coordinates = layer.getLatLngs();
        if (coordinates.length === 1) {
          coordinates = coordinates[0] as TbPolygonCoordinates;
        }
      }
      this.savePolygonCoordinates(item.entity, coordinates).subscribe(
        (converted) => {
          item.entity[this.settings.polygonKey.label] = JSON.stringify(converted);
          this.createItemFromUnplaced(item);
        }
      );
    } else {
      console.warn('Unable to place item, layer is not a polygon.');
    }
  }

  public extractPolygonCoordinates(data: FormattedData<TbMapDatasource>): TbPolygonRawCoordinates {
    let rawPolyData = data[this.settings.polygonKey.label];
    if (isString(rawPolyData)) {
      rawPolyData = JSON.parse(rawPolyData);
    }
    return this.map.polygonDataToCoordinates(rawPolyData);
  }

  public savePolygonCoordinates(data: FormattedData<TbMapDatasource>, coordinates: TbPolygonCoordinates): Observable<TbPolygonRawCoordinates> {
    const converted = coordinates ? this.map.coordinatesToPolygonData(coordinates) : null;
    const polygonData = [
      {
        dataKey: this.settings.polygonKey,
        value: converted
      }
    ];
    return this.map.saveItemData(data.$datasource, polygonData, this.settings.edit?.attributeScope).pipe(
      map(() => converted)
    );
  }

  protected getDataKeys(): DataKey[] {
    return [this.settings.polygonKey];
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<PolygonsDataLayerSettings> {
    return defaultBasePolygonsDataLayerSettings(map.type());
  }

  protected doSetup(): Observable<any> {
    return super.doSetup();
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return layerData && ((isNotEmptyStr(layerData[this.settings.polygonKey.label]) && !isJSON(layerData[this.settings.polygonKey.label])
      || Array.isArray(layerData[this.settings.polygonKey.label])));
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbPolygonDataLayerItem {
    return new TbPolygonDataLayerItem(data, dsData, this.settings, this);
  }

}
