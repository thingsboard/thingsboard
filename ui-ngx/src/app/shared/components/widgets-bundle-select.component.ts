///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { isDefined } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
  selector: 'tb-widgets-bundle-select',
  templateUrl: './widgets-bundle-select.component.html',
  styleUrls: ['./widgets-bundle-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetsBundleSelectComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class WidgetsBundleSelectComponent implements ControlValueAccessor, OnInit, OnChanges {

  @Input()
  bundlesScope: 'system' | 'tenant';

  @Input()
  selectFirstBundle: boolean;

  @Input()
  selectBundleAlias: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  widgetsBundles: Array<WidgetsBundle>;

  widgetsBundle: WidgetsBundle | null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private widgetService: WidgetService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.widgetsBundles$ = this.getWidgetsBundles().pipe(
      tap((widgetsBundles) => {
        this.widgetsBundles = widgetsBundles;
        if (this.selectFirstBundle) {
          if (widgetsBundles.length > 0) {
            if (this.widgetsBundle !== widgetsBundles[0]) {
              this.widgetsBundle = widgetsBundles[0];
              this.updateView();
            } else if (isDefined(this.selectBundleAlias)) {
              this.selectWidgetsBundleByAlias(this.selectBundleAlias);
            }
          }
        }
      }),
      share()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'selectBundleAlias') {
          this.selectWidgetsBundleByAlias(this.selectBundleAlias);
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: WidgetsBundle | null): void {
    this.widgetsBundle = value;
  }

  widgetsBundleChanged() {
    this.updateView();
  }

  isSystem(item: WidgetsBundle) {
    return item && item.tenantId.id === NULL_UUID;
  }

  private selectWidgetsBundleByAlias(alias: string) {
    if (this.widgetsBundles && alias) {
      const found = this.widgetsBundles.find((widgetsBundle) => widgetsBundle.alias === alias);
      if (found && this.widgetsBundle !== found) {
        this.widgetsBundle = found;
        this.updateView();
      }
    }
  }

  private updateView() {
    this.propagateChange(this.widgetsBundle);
  }

  private getWidgetsBundles(): Observable<Array<WidgetsBundle>> {
    let widgetsBundlesObservable: Observable<Array<WidgetsBundle>>;
    if (this.bundlesScope) {
      if (this.bundlesScope === 'system') {
        widgetsBundlesObservable = this.widgetService.getSystemWidgetsBundles();
      } else if (this.bundlesScope === 'tenant') {
        widgetsBundlesObservable = this.widgetService.getTenantWidgetsBundles();
      }
    } else {
      widgetsBundlesObservable = this.widgetService.getAllWidgetsBundles();
    }
    return widgetsBundlesObservable;
  }

}
