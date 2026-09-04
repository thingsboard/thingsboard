// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { mergeDeep } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultMapDataSourceSettings,
  MapDataSourceSettings,
  mapDataSourceValid,
  mapDataSourceValidator
} from '@shared/models/widget/maps/map.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
    selector: 'tb-map-data-sources',
    templateUrl: './map-data-sources.component.html',
    styleUrls: ['./map-data-sources.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapDataSourcesComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MapDataSourcesComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MapDataSourcesComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  dataSourcesFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataSourcesFormGroup = this.fb.group({
      dataSources: [this.fb.array([]), []]
    });
    this.dataSourcesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let dataSources: MapDataSourceSettings[] = this.dataSourcesFormGroup.get('dataSources').value;
        if (dataSources) {
          dataSources = dataSources.filter(dataSource => mapDataSourceValid(dataSource));
        }
        this.propagateChange(dataSources);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataSourcesFormGroup.disable({emitEvent: false});
    } else {
      this.dataSourcesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapDataSourceSettings[] | undefined): void {
    const dataSources: MapDataSourceSettings[] = value || [];
    this.dataSourcesFormGroup.setControl('dataSources', this.prepareDataSourcesFormArray(dataSources), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.dataSourcesFormGroup.valid;
    return valid ? null : {
      dataSources: {
        valid: false,
      },
    };
  }

  dataSourcesFormArray(): UntypedFormArray {
    return this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray;
  }

  trackByDataSource(index: number, dataSourceControl: AbstractControl): any {
    return dataSourceControl;
  }

  removeDataSource(index: number) {
    (this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray).removeAt(index);
  }

  addDataSource() {
    const dataSource = mergeDeep<MapDataSourceSettings>({} as MapDataSourceSettings,
      defaultMapDataSourceSettings);
    const dataSourcesArray = this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray;
    const dataSourceControl = this.fb.control(dataSource, [mapDataSourceValidator]);
    dataSourcesArray.push(dataSourceControl);
  }

  private prepareDataSourcesFormArray(dataSources: MapDataSourceSettings[]): UntypedFormArray {
    const dataSourcesControls: Array<AbstractControl> = [];
    dataSources.forEach((dataSource) => {
      dataSourcesControls.push(this.fb.control(dataSource, [mapDataSourceValidator]));
    });
    return this.fb.array(dataSourcesControls);
  }
}
