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
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { isDefined } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatDialog } from '@angular/material/dialog';
import {
  WidgetsBundleDialogComponent,
  WidgetsBundleDialogData
} from '@home/pages/widget/widgets-bundle-dialog.component';

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
  @coerceBoolean()
  selectFirstBundle: boolean;

  @Input()
  selectBundleAlias: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  excludeBundleIds: Array<string>;

  @Input()
  @coerceBoolean()
  createNew: boolean;

  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  widgetsBundles: Array<WidgetsBundle>;

  widgetsBundle: WidgetsBundle | null;

  onTouched = () => {};
  private propagateChange: (value: any) => void = () => {};

  constructor(private store: Store<AppState>,
              private widgetService: WidgetService,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  ngOnInit() {
    this.widgetsBundles$ = this.getWidgetsBundles().pipe(
      map((widgetsBundles) => {
        const authState = getCurrentAuthState(this.store);
        if (!authState.edgesSupportEnabled) {
          widgetsBundles = widgetsBundles.filter(widgetsBundle => widgetsBundle.alias !== 'edge_widgets');
        }
        return widgetsBundles;
      }),
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
    } else if (this.widgetsBundle) {
      this.widgetsBundle = null;
      this.updateView();
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
    if (this.excludeBundleIds && this.excludeBundleIds.length) {
      widgetsBundlesObservable = widgetsBundlesObservable.pipe(
        map((widgetBundles) =>
          widgetBundles.filter(w => !this.excludeBundleIds.includes(w.id.id)))
      );
    }
    return widgetsBundlesObservable;
  }

  compareById(f1: WidgetsBundle, f2: WidgetsBundle): boolean {
    return f1 && f2 && f1.id.id === f2.id.id;
  }

  openWidgetsBundleDialog($event) {
    $event.preventDefault();
    const widgetsBundle: WidgetsBundle = {
      title: '',
      image: '',
      description: '',
      scada: true,
      order: null
    };
    this.dialog.open<WidgetsBundleDialogComponent, WidgetsBundleDialogData,
      WidgetsBundle>(WidgetsBundleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        widgetsBundle
      }
    }).afterClosed().subscribe(
      (savedWidgetBundle) => {
        if (savedWidgetBundle) {
          this.widgetsBundles$ = of([...this.widgetsBundles, savedWidgetBundle]);
          this.widgetsBundle = savedWidgetBundle;
          this.widgetsBundleChanged();
        }
      }
    );
  }

}
