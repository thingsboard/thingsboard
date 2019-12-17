///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  Component, ElementRef,
  EventEmitter, forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, NgForm, Validators } from '@angular/forms';
import { FcRuleNode, FcRuleEdge } from './rulechain-page.models';
import { RuleNodeType, LinkLabel } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of, Subscription } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { deepClone } from '@core/utils';
import { EntityAlias } from '@shared/models/alias.models';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatChipList, MatAutocomplete, MatChipInputEvent, MatAutocompleteSelectedEvent } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { map, mergeMap, share } from 'rxjs/operators';

@Component({
  selector: 'tb-rule-node-link',
  templateUrl: './rule-node-link.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleNodeLinkComponent),
    multi: true
  }]
})
export class RuleNodeLinkComponent implements ControlValueAccessor, OnInit {

  @ViewChild('ruleNodeLinkForm', {static: true}) ruleNodeLinkForm: NgForm;

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

  private allowCustomValue: boolean;
  get allowCustom(): boolean {
    return this.allowCustomValue;
  }
  @Input()
  set allowCustom(value: boolean) {
    this.allowCustomValue = coerceBooleanProperty(value);
  }

  @Input()
  allowedLabels: {[label: string]: LinkLabel};

  ruleNodeLinkFormGroup: FormGroup;

  modelValue: FcRuleEdge;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              public truncate: TruncatePipe,
              public translate: TranslateService) {
    this.ruleNodeLinkFormGroup = this.fb.group({
      labels: [[], Validators.required]
    });
    this.ruleNodeLinkFormGroup.get('labels').valueChanges.subscribe(
      (labels: string[]) => this.updateModel(labels)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.ruleNodeLinkFormGroup.disable({emitEvent: false});
    } else {
      this.ruleNodeLinkFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FcRuleEdge): void {
    this.modelValue = value;
    const labels = this.modelValue && this.modelValue.labels ? this.modelValue.labels : [];
    this.ruleNodeLinkFormGroup.get('labels').patchValue(labels, {emitEvent: false});
  }

  private updateModel(labels: string[]) {
    if (labels && labels.length) {
      this.modelValue.labels = labels;
      this.modelValue.label = labels.join(' / ');
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(this.required ? null : this.modelValue);
    }
  }
}
