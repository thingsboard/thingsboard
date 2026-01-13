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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { FcRuleEdge, LinkLabel } from '@shared/models/rule-node.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

  @Input()
  sourceRuleChainId: string;

  ruleNodeLinkFormGroup: UntypedFormGroup;

  modelValue: FcRuleEdge;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              public truncate: TruncatePipe,
              public translate: TranslateService) {
    this.ruleNodeLinkFormGroup = this.fb.group({
      labels: [[], Validators.required]
    });
    this.ruleNodeLinkFormGroup.get('labels').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
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
