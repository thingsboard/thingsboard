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
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { WidgetTypeInfo } from '@shared/models/widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetService } from '@core/http/widget.service';

@Component({
    selector: 'tb-widget-type-autocomplete',
    templateUrl: './widget-type-autocomplete.component.html',
    styleUrls: ['./widget-type-autocomplete.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => WidgetTypeAutocompleteComponent),
            multi: true
        }],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WidgetTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  private dirty = false;

  selectWidgetTypeFormGroup: UntypedFormGroup;

  modelValue: WidgetTypeInfo | null;

  @Input()
  label = this.translate.instant('widget.widget');

  @Input()
  placeholder: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  excludeWidgetTypeIds: Array<string>;

  @ViewChild('widgetTypeInput', {static: true}) widgetTypeInput: ElementRef;

  filteredWidgetTypes: Observable<Array<WidgetTypeInfo>>;

  searchText = '';

  private propagateChange = (_v: any) => { };

  constructor(public translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    this.selectWidgetTypeFormGroup = this.fb.group({
      widgetType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredWidgetTypes = this.selectWidgetTypeFormGroup.get('widgetType').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: WidgetTypeInfo;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchWidgetTypes(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectWidgetTypeFormGroup.disable({emitEvent: false});
    } else {
      this.selectWidgetTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: WidgetTypeInfo | string | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        this.widgetService.getWidgetTypeInfoById(value).subscribe(
          (widgetType) => {
            this.modelValue = widgetType;
            this.selectWidgetTypeFormGroup.get('widgetType').patchValue(widgetType, {emitEvent: false});
          }
        );
      } else {
        this.modelValue = value;
        this.selectWidgetTypeFormGroup.get('widgetType').patchValue(value, {emitEvent: false});
      }
    } else {
      this.modelValue = null;
      this.selectWidgetTypeFormGroup.get('widgetType').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  updateView(value: WidgetTypeInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayWidgetTypeFn(widgetType?: WidgetTypeInfo): string | undefined {
    return widgetType ? widgetType.name : undefined;
  }

  fetchWidgetTypes(searchText?: string): Observable<Array<WidgetTypeInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getWidgetTypes(pageLink);
  }

  getWidgetTypes(pageLink: PageLink,
                 result: Array<WidgetTypeInfo> = []): Observable<Array<WidgetTypeInfo>> {
    return this.widgetService.getWidgetTypes(pageLink, true).pipe(
      catchError(() => of(emptyPageData<WidgetTypeInfo>())),
      switchMap((data) => {
        if (this.excludeWidgetTypeIds?.length) {
          const filtered = data.data.filter(w => !this.excludeWidgetTypeIds.includes(w.id.id));
          result = result.concat(filtered);
        } else {
          result = data.data;
        }
        if (result.length >= pageLink.pageSize || !this.excludeWidgetTypeIds?.length || !data.hasNext) {
          return of(result);
        } else {
          return this.getWidgetTypes(pageLink.nextPageLink(), result);
        }
      })
    );
  }

  onFocus() {
    if (this.dirty) {
      this.selectWidgetTypeFormGroup.get('widgetType').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectWidgetTypeFormGroup.get('widgetType').patchValue('');
    setTimeout(() => {
      this.widgetTypeInput.nativeElement.blur();
      this.widgetTypeInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

}
