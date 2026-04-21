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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, ReplaySubject, shareReplay } from 'rxjs';
import { catchError, debounceTime, finalize, map, share, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { SlackChanelType, SlackConversation } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { isEqual, objectRequired } from '@core/utils';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';

@Component({
    selector: 'tb-slack-conversation-autocomplete',
    templateUrl: './slack-conversation-autocomplete.component.html',
    styleUrls: ['./slack-conversation-autocomplete.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SlackConversationAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class SlackConversationAutocompleteComponent extends AutocompleteBaseDirective<SlackConversation, SlackConversation>
  implements ControlValueAccessor, OnInit, OnChanges {

  conversationSlackFormGroup: FormGroup;

  @Input()
  labelText = this.translate.instant('notification.conversation');

  @Input()
  requiredText = this.translate.instant('notification.conversation-required');

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
  slackChanelType: SlackChanelType;

  @Input()
  token: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @ViewChild('slackInput', {static: true}) slackInput: ElementRef;
  @ViewChild('slackInput', {read: MatAutocompleteTrigger}) autocompleteTrigger: MatAutocompleteTrigger;

  slackConversation$: Observable<Array<SlackConversation>>;

  private modelValue: SlackConversation | null;
  private latestSearchConversetionResult: Array<SlackConversation> = null;
  private slackConversetionFetchObservable$: Observable<Array<SlackConversation>> = null;

  private propagateChange = (v: any) => { };

  constructor(public translate: TranslateService,
              public truncate: TruncatePipe,
              private notificationService: NotificationService,
              private fb: FormBuilder) {
    super();
    this.conversationSlackFormGroup = this.fb.group({
      conversation: ['', [objectRequired()]]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  protected getControl(): FormControl {
    return this.conversationSlackFormGroup.get('conversation') as FormControl;
  }

  protected getAutocompleteTrigger(): MatAutocompleteTrigger {
    return this.autocompleteTrigger;
  }

  protected getInput(): ElementRef<HTMLInputElement> {
    return this.slackInput as ElementRef<HTMLInputElement>;
  }

  protected getFilteredEntities(): Observable<Array<SlackConversation>> {
    return this.slackConversation$;
  }

  protected getModelValue(): SlackConversation | null {
    return this.modelValue;
  }

  protected isCreateNew(): boolean {
    return false;
  }

  protected override getDisplayName(entity: SlackConversation): string {
    return entity.title ?? '';
  }

  protected override selectMatchedEntity(entity: SlackConversation): void {
    this.pendingBlur = false;
    this.searchText = entity.title ?? '';
    this.getControl().patchValue(entity, {emitEvent: false});
    this.updateView(entity);
    this.getAutocompleteTrigger()?.closePanel();
  }

  ngOnInit() {
    if (this.required) {
      this.conversationSlackFormGroup.get('conversation').addValidators(Validators.required);
      this.conversationSlackFormGroup.get('conversation').updateValueAndValidity({emitEvent: false});
    }
    this.slackConversation$ = this.conversationSlackFormGroup.get('conversation').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.title) : ''),
        switchMap(name => {
          this.isFetching = true;
          return this.fetchSlackConversation(name).pipe(
            finalize(() => this.isFetching = false)
          );
        }),
        tap(entities => {
          if (this.pendingBlur) {
            this.performValidation(entities);
          }
        }),
        shareReplay(1)
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'slackChanelType' || propName === 'token') {
          this.clearSlackCache();
          this.dirty = true;
          this.conversationSlackFormGroup.get('conversation').patchValue('');
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.conversationSlackFormGroup.disable({emitEvent: false});
    } else {
      this.conversationSlackFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SlackConversation | null): void {
    this.searchText = '';
    if (value != null) {
      this.modelValue = value;
      this.conversationSlackFormGroup.get('conversation').patchValue(value, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.conversationSlackFormGroup.get('conversation').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  protected updateView(value: SlackConversation | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displaySlackConversationFn(slackConversation?: SlackConversation): string | undefined {
    return slackConversation ? slackConversation.title : undefined;
  }

  private fetchSlackConversation(searchText?: string): Observable<Array<SlackConversation>> {
    if (this.dirty) {
      return of([]);
    }
    if (this.searchText !== searchText || this.latestSearchConversetionResult === null) {
      this.searchText = searchText;
      const slackConversationFilter = this.createSlackConversationFilter(this.searchText);
      return this.getSlackConversationByType().pipe(
        map(name => name.filter(slackConversationFilter)),
        tap(res => this.latestSearchConversetionResult = res)
      );
    }
    return of(this.latestSearchConversetionResult);
  }

  private getSlackConversationByType() {
    if (this.slackConversetionFetchObservable$ === null) {
      let fetchObservable: Observable<Array<SlackConversation>>;
      if (this.slackChanelType) {
        fetchObservable = this.notificationService.listSlackConversations(this.slackChanelType, this.token, {ignoreLoading: true});
      } else {
        fetchObservable = of([]);
      }
      this.slackConversetionFetchObservable$ = fetchObservable.pipe(
        catchError(() => of([])),
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
    }
    return this.slackConversetionFetchObservable$;
  }

  private createSlackConversationFilter(query: string): (key: SlackConversation) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.title.toLowerCase().includes(lowercaseQuery);
  }

  private clearSlackCache(): void {
    this.latestSearchConversetionResult = null;
    this.slackConversetionFetchObservable$ = null;
  }

}
