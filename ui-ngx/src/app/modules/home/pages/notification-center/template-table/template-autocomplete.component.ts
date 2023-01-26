///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import {
  NotificationDeliveryMethodTranslateMap,
  NotificationTemplate,
  NotificationType
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-template-autocomplete',
  templateUrl: './template-autocomplete.component.html',
  styleUrls: ['./template-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TemplateAutocompleteComponent),
    multi: true
  }]
})
export class TemplateAutocompleteComponent implements ControlValueAccessor, OnInit {

  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;
  selectTemplateFormGroup: FormGroup;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

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

  @Input()
  notificationTypes: NotificationType;

  @ViewChild('templateInput', {static: true}) templateInput: ElementRef;

  filteredTemplate: Observable<Array<NotificationTemplate>>;

  searchText = '';

  private modelValue: EntityId | null;
  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private notificationService: NotificationService,
              private fb: FormBuilder) {
    this.selectTemplateFormGroup = this.fb.group({
      templateName: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    if (this.required) {
      this.selectTemplateFormGroup.get('templateName').addValidators(Validators.required);
      this.selectTemplateFormGroup.get('templateName').updateValueAndValidity({emitEvent: false});
    }
    this.filteredTemplate = this.selectTemplateFormGroup.get('templateName').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchTemplate(name)),
        share()
      );
  }

  ngAfterViewInit(): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectTemplateFormGroup.disable({emitEvent: false});
    } else {
      this.selectTemplateFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: EntityId | null): void {
    this.searchText = '';
    if (value != null) {
      this.notificationService.getNotificationTemplateById(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (entity) => {
          this.modelValue = entity.id;
          this.selectTemplateFormGroup.get('templateName').patchValue(entity, {emitEvent: false});
        },
        () => {
          this.modelValue = null;
          this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      );
    } else {
      this.modelValue = null;
      this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectTemplateFormGroup.get('templateName').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: EntityId | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayTemplateFn(template?: NotificationTemplate): string | undefined {
    return template ? template.name : undefined;
  }

  fetchTemplate(searchText?: string): Observable<Array<NotificationTemplate>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.notificationService.getNotificationTemplates(pageLink, this.notificationTypes, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<NotificationTemplate>())),
      map(pageData => {
        return pageData.data;
      })
    );
  }

  clear() {
    this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.templateInput.nativeElement.blur();
      this.templateInput.nativeElement.focus();
    }, 0);
  }
}
