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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { QueueInfo, ServiceType } from '@shared/models/queue.models';
import { QueueService } from '@core/http/queue.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';

@Component({
    selector: 'tb-queue-autocomplete',
    templateUrl: './queue-autocomplete.component.html',
    styleUrls: ['./queue-autocomplete.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => QueueAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class QueueAutocompleteComponent extends AutocompleteBaseDirective
    implements ControlValueAccessor, OnInit {

  selectQueueFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  autocompleteHint: string;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  queueType: ServiceType;

  @Input()
  disabled: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @ViewChild('queueInput', {static: true}) queueInput: ElementRef;

  @ViewChild('queueInput', {read: MatAutocompleteTrigger}) autocompleteTrigger: MatAutocompleteTrigger;

  filteredQueues: Observable<Array<QueueInfo>>;

  constructor(public translate: TranslateService,
              public truncate: TruncatePipe,
              private queueService: QueueService,
              private fb: FormBuilder) {
    super();
    this.selectQueueFormGroup = this.fb.group({
      queueName: [null]
    });
  }

  protected getControl(): FormControl {
    return this.selectQueueFormGroup.get('queueName') as FormControl;
  }

  protected getInput(): ElementRef<HTMLInputElement> {
    return this.queueInput as ElementRef<HTMLInputElement>;
  }

  ngOnInit() {
    const queueControl = this.selectQueueFormGroup.get('queueName');
    if (this.required) {
      queueControl.addValidators(Validators.required);
      queueControl.updateValueAndValidity({ emitEvent: false });
    }

    this.filteredQueues = queueControl.valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.name;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchQueue(name)),
        shareReplay(1)
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectQueueFormGroup.disable({emitEvent: false});
    } else {
      this.selectQueueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      this.queueService.getQueueByName(value, {ignoreLoading: true, ignoreErrors: true}).subscribe({
        next: (entity) => {
          this.modelValue = entity.name;
          this.selectQueueFormGroup.get('queueName').patchValue(entity, {emitEvent: false});
        },
        error: () => {
          this.modelValue = null;
          this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      });
    } else {
      this.modelValue = null;
      this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayQueueFn(queue?: QueueInfo): string | undefined {
    return queue ? queue.name : undefined;
  }

  fetchQueue(searchText?: string): Observable<Array<QueueInfo>> {
    this.searchText = searchText ?? '';
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.queueService.getTenantQueuesByServiceType(pageLink, this.queueType, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<QueueInfo>())),
      map(pageData => pageData.data)
    );
  }

  getDescription(value: QueueInfo): string {
    return value.additionalInfo?.description ? value.additionalInfo.description :
      this.translate.instant(
        'queue.alt-description',
        {submitStrategy: value.submitStrategy.type, processingStrategy: value.processingStrategy.type}
      );
  }
}
