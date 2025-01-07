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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  CirclesDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  MapType,
  MarkersDataLayerSettings,
  PolygonsDataLayerSettings
} from '@home/components/widget/lib/maps/map.models';
import {
  DataKey,
  DataKeyConfigMode,
  DatasourceType,
  datasourceTypeTranslationMap,
  widgetType
} from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/common/map/map-settings.component';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/config/data-key-config-dialog.component';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasSelectCallbacks
} from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';

@Component({
  selector: 'tb-map-data-layer-row',
  templateUrl: './map-data-layer-row.component.html',
  styleUrls: ['./map-data-layer-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapDataLayerRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapDataLayerRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  EntityType = EntityType;

  MapType = MapType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  mapType: MapType = MapType.geoMap;

  @Input()
  dataLayerType: MapDataLayerType = 'markers';

  get functionsOnly(): boolean {
    return this.mapSettingsComponent.functionsOnly;
  }

  get aliasController(): IAliasController {
    return this.mapSettingsComponent.aliasController;
  }

  get dataKeyCallbacks(): DataKeysCallbacks {
    return this.mapSettingsComponent.callbacks;
  }

  public get entityAliasSelectCallbacks(): EntityAliasSelectCallbacks {
    return this.mapSettingsComponent.callbacks;
  }

  @Output()
  dataLayerRemoved = new EventEmitter();

  generateDataKey = this._generateDataKey.bind(this);

  dataLayerFormGroup: UntypedFormGroup;

  modelValue: MapDataLayerSettings;

  editDataLayerText: string;

  removeDataLayerText: string;

  private propagateChange = (_val: any) => {};

  constructor(private mapSettingsComponent: MapSettingsComponent,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    if (this.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }
    this.dataLayerFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]]
    });
    switch (this.dataLayerType) {
      case 'markers':
        this.editDataLayerText = 'widgets.maps.data-layer.marker.marker-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.marker.remove-marker';
        this.dataLayerFormGroup.addControl('xKey', this.fb.control(null, Validators.required));
        this.dataLayerFormGroup.addControl('yKey', this.fb.control(null, Validators.required));
        break;
      case 'polygons':
        this.editDataLayerText = 'widgets.maps.data-layer.polygon.polygon-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.polygon.remove-polygon';
        this.dataLayerFormGroup.addControl('polygonKey', this.fb.control(null, Validators.required));
        break;
      case 'circles':
        this.editDataLayerText = 'widgets.maps.data-layer.circle.circle-configuration';
        this.removeDataLayerText = 'widgets.maps.data-layer.circle.remove-circle';
        this.dataLayerFormGroup.addControl('circleKey', this.fb.control(null, Validators.required));
        break;
    }
    this.dataLayerFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataLayerFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (newDsType: DatasourceType) => this.onDsTypeChanged(newDsType)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataLayerFormGroup.disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MapDataLayerSettings): void {
    this.modelValue = value;
    this.dataLayerFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId
      }, {emitEvent: false}
    );
    switch (this.dataLayerType) {
      case 'markers':
        const markersDataLayer = value as MarkersDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            xKey: markersDataLayer?.xKey,
            yKey: markersDataLayer?.yKey
          }, {emitEvent: false}
        );
        break;
      case 'polygons':
        const polygonsDataLayer = value as PolygonsDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            polygonKey: polygonsDataLayer?.polygonKey
          }, {emitEvent: false}
        );
        break;
      case 'circles':
        const circlesDataLayer = value as CirclesDataLayerSettings;
        this.dataLayerFormGroup.patchValue(
          {
            circleKey: circlesDataLayer?.circleKey
          }, {emitEvent: false}
        );
        break;
    }
    this.updateValidators();
    this.cd.markForCheck();
  }

  editKey(keyType: 'xKey' | 'yKey' | 'polygonKey' | 'circleKey') {
    const targetDataKey: DataKey = this.dataLayerFormGroup.get(keyType).value;
    this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(targetDataKey),
          dataKeyConfigMode: DataKeyConfigMode.general,
          aliasController: this.aliasController,
          widgetType: widgetType.latest,
          deviceId: this.dataLayerFormGroup.get('dsDeviceId').value,
          entityAliasId: this.dataLayerFormGroup.get('dsEntityAliasId').value,
          showPostProcessing: true,
          callbacks: this.mapSettingsComponent.callbacks,
          hideDataKeyColor: true,
          hideDataKeyDecimals: true,
          hideDataKeyUnits: true,
          widget: this.mapSettingsComponent.widget,
          dashboard: null,
          dataKeySettingsForm: null,
          dataKeySettingsDirective: null
        }
      }).afterClosed().subscribe((updatedDataKey) => {
      if (updatedDataKey) {
        this.dataLayerFormGroup.get(keyType).patchValue(updatedDataKey);
      }
    });
  }

  editDataLayer($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      /*const ctx: any = {
        mapLayerSettings: deepClone(this.modelValue)
      };
      const mapLayerSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, MapLayerSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      mapLayerSettingsPanelPopover.tbComponentRef.instance.popover = mapLayerSettingsPanelPopover;
      mapLayerSettingsPanelPopover.tbComponentRef.instance.mapLayerSettingsApplied.subscribe((layer) => {
        mapLayerSettingsPanelPopover.hide();
        this.layerFormGroup.patchValue(
          layer,
          {emitEvent: false});
        this.updateValidators();
        this.updateModel();
      });*/
    }
  }

  private _generateDataKey(key: DataKey): DataKey {
    key = this.dataKeyCallbacks.generateDataKey(key.name, key.type, null, false,
      null);
    return key;
  }

  private onDsTypeChanged(newDsType: DatasourceType) {
    let updateModel = false;
    switch (this.dataLayerType) {
      case 'markers':
        const xKey: DataKey = this.dataLayerFormGroup.get('xKey').value;
        if (this.updateDataKeyToNewDsType(xKey, newDsType)) {
          this.dataLayerFormGroup.get('xKey').patchValue(xKey, {emitEvent: false});
          updateModel = true;
        }
        const yKey: DataKey = this.dataLayerFormGroup.get('yKey').value;
        if (this.updateDataKeyToNewDsType(yKey, newDsType)) {
          this.dataLayerFormGroup.get('yKey').patchValue(yKey, {emitEvent: false});
          updateModel = true;
        }
        break;
      case 'polygons':
        const polygonKey: DataKey = this.dataLayerFormGroup.get('polygonKey').value;
        if (this.updateDataKeyToNewDsType(polygonKey, newDsType)) {
          this.dataLayerFormGroup.get('polygonKey').patchValue(polygonKey, {emitEvent: false});
          updateModel = true;
        }
        break;
      case 'circles':
        const circleKey: DataKey = this.dataLayerFormGroup.get('circleKey').value;
        if (this.updateDataKeyToNewDsType(circleKey, newDsType)) {
          this.dataLayerFormGroup.get('circleKey').patchValue(circleKey, {emitEvent: false});
          updateModel = true;
        }
        break;
    }
    this.updateValidators();
    if (updateModel) {
      this.updateModel();
    }
  }

  private updateDataKeyToNewDsType(dataKey: DataKey, newDsType: DatasourceType): boolean {
    if (newDsType === DatasourceType.function) {
      if (dataKey.type !== DataKeyType.function) {
        dataKey.type = DataKeyType.function;
        return true;
      }
    } else {
      if (dataKey.type === DataKeyType.function) {
        dataKey.type = DataKeyType.attribute;
        return true;
      }
    }
    return false;
  }

  private updateValidators() {
    const dsType: DatasourceType = this.dataLayerFormGroup.get('dsType').value;
    if (dsType === DatasourceType.function) {
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else if (dsType === DatasourceType.device) {
      this.dataLayerFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataLayerFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataLayerFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataLayerFormGroup.value};
    this.propagateChange(this.modelValue);
  }

  protected readonly datasourceType = DatasourceType;
}
