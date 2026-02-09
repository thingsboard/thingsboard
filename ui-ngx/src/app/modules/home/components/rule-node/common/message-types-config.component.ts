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
import { LinkLabel, MessageType, messageTypeNames, PageComponent, TruncatePipe } from '@shared/public-api';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatChipGrid, MatChipInputEvent } from '@angular/material/chips';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, startWith } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
    selector: 'tb-message-types-config',
    templateUrl: './message-types-config.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MessageTypesConfigComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MessageTypesConfigComponent extends PageComponent implements ControlValueAccessor, OnInit {

  messageTypeConfigForm: FormGroup;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  label: string;

  @Input()
  placeholder = 'rule-node-config.add-message-type';

  @Input()
  disabled: boolean;

  @ViewChild('chipList', {static: false}) chipList: MatChipGrid;
  @ViewChild('messageTypeAutocomplete', {static: false}) matAutocomplete: MatAutocomplete;
  @ViewChild('messageTypeInput', {static: false}) messageTypeInput: ElementRef<HTMLInputElement>;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  filteredMessageTypes: Observable<Array<LinkLabel>>;

  messageTypes: Array<LinkLabel> = [];

  private messageTypesList: Array<LinkLabel> = [];

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(public translate: TranslateService,
              public truncate: TruncatePipe,
              private fb: FormBuilder) {
    super();
    this.messageTypeConfigForm = this.fb.group({
      messageType: [null]
    });
    for (const type of Object.keys(MessageType)) {
      this.messageTypesList.push(
        {
          name: messageTypeNames.get(MessageType[type]),
          value: type
        }
      );
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredMessageTypes = this.messageTypeConfigForm.get('messageType').valueChanges
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchMessageTypes(name)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.messageTypeConfigForm.disable({emitEvent: false});
    } else {
      this.messageTypeConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    this.messageTypes.length = 0;
    if (value) {
      value.forEach((type: string) => {
        const found = this.messageTypesList.find((messageType => messageType.value === type));
        if (found) {
          this.messageTypes.push({
            name: found.name,
            value: found.value
          });
        } else {
          this.messageTypes.push({
            name: type,
            value: type
          });
        }
      });
    }
  }

  displayMessageTypeFn(messageType?: LinkLabel): string | undefined {
    return messageType ? messageType.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return text && text.length > 0;
  }

  createMessageType($event: Event, value: string) {
    $event.preventDefault();
    this.transformMessageType(value);
  }

  add(event: MatChipInputEvent): void {
    this.transformMessageType(event.value);
  }

  private fetchMessageTypes(searchText?: string): Observable<Array<LinkLabel>> {
    this.searchText = searchText;
    if (this.searchText && this.searchText.length) {
      const search = this.searchText.toUpperCase();
      return of(this.messageTypesList.filter(messageType => messageType.name.toUpperCase().includes(search)));
    } else {
      return of(this.messageTypesList);
    }
  }

  private transformMessageType(value: string) {
    if ((value || '').trim()) {
      let newMessageType: LinkLabel;
      const messageTypeName = value.trim();
      const existingMessageType = this.messageTypesList.find(messageType => messageType.name === messageTypeName);
      if (existingMessageType) {
        newMessageType = {
          name: existingMessageType.name,
          value: existingMessageType.value
        };
      } else {
        newMessageType = {
          name: messageTypeName,
          value: messageTypeName
        };
      }
      if (newMessageType) {
        this.addMessageType(newMessageType);
      }
    }
    this.clear('');
  }

  remove(messageType: LinkLabel) {
    const index = this.messageTypes.indexOf(messageType);
    if (index >= 0) {
      this.messageTypes.splice(index, 1);
      this.updateModel();
    }
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.addMessageType(event.option.value);
    this.clear('');
  }

  addMessageType(messageType: LinkLabel): void {
    const index = this.messageTypes.findIndex(existingMessageType => existingMessageType.value === messageType.value);
    if (index === -1) {
      this.messageTypes.push(messageType);
      this.updateModel();
    }
  }

  onFocus() {
    this.messageTypeConfigForm.get('messageType').updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  clear(value: string = '') {
    this.messageTypeInput.nativeElement.value = value;
    this.messageTypeConfigForm.get('messageType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.messageTypeInput.nativeElement.blur();
      this.messageTypeInput.nativeElement.focus();
    }, 0);
  }

  private updateModel() {
    const value = this.messageTypes.map((messageType => messageType.value));
    if (this.required) {
      this.chipList.errorState = !value.length;
      this.propagateChange(value.length > 0 ? value : null);
    } else {
      this.chipList.errorState = false;
      this.propagateChange(value);
    }
  }

}
