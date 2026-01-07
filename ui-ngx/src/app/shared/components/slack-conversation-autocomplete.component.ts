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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, ReplaySubject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { SlackChanelType, SlackConversation } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-slack-conversation-autocomplete',
  templateUrl: './slack-conversation-autocomplete.component.html',
  styleUrls: ['./slack-conversation-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SlackConversationAutocompleteComponent),
    multi: true
  }]
})
export class SlackConversationAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

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

  @ViewChild('slackInput', {static: true}) slackInput: ElementRef;

  slackConversation$: Observable<Array<SlackConversation>>;

  slackSearchText = '';

  private modelValue: SlackConversation | null;
  private dirty = false;
  private latestSearchConversetionResult: Array<SlackConversation> = null;
  private slackConversetionFetchObservable$: Observable<Array<SlackConversation>> = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private notificationService: NotificationService,
              private fb: FormBuilder) {
    this.conversationSlackFormGroup = this.fb.group({
      conversation: ['']
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
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
        switchMap(name => this.fetchSlackConversation(name)),
        share()
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

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: SlackConversation | null): void {
    this.slackSearchText = '';
    if (value != null) {
      this.modelValue = value;
      this.conversationSlackFormGroup.get('conversation').patchValue(value, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.conversationSlackFormGroup.get('conversation').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.dirty = false;
      this.conversationSlackFormGroup.get('conversation').updateValueAndValidity({onlySelf: true, emitEvent: true});
    }
  }

  updateView(value: SlackConversation | null) {
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
    if (this.slackSearchText !== searchText || this.latestSearchConversetionResult === null) {
      this.slackSearchText = searchText;
      const slackConversationFilter = this.createSlackConversationFilter(this.slackSearchText);
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

  clear() {
    this.conversationSlackFormGroup.get('conversation').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.slackInput.nativeElement.blur();
      this.slackInput.nativeElement.focus();
    }, 0);
  }
}
