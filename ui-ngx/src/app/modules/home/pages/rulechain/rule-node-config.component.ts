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
  Component,
  ComponentRef,
  EventEmitter,
  forwardRef,
  HostBinding,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  IRuleNodeConfigurationComponent,
  RuleNodeConfiguration,
  RuleNodeDefinition
} from '@shared/models/rule-node.models';
import { Subscription } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { JsonObjectEditComponent } from '@shared/components/json-object-edit.component';
import { deepClone } from '@core/utils';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Component({
  selector: 'tb-rule-node-config',
  templateUrl: './rule-node-config.component.html',
  styleUrls: ['./rule-node-config.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleNodeConfigComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class RuleNodeConfigComponent implements ControlValueAccessor, OnDestroy {

  @ViewChild('definedConfigContent', {read: ViewContainerRef, static: true}) definedConfigContainer: ViewContainerRef;
  @ViewChild('jsonObjectEditComponent') jsonObjectEditComponent: JsonObjectEditComponent;

  @HostBinding('style.display') readonly styleDisplay = 'block';

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
  ruleNodeId: string;

  @Input()
  ruleChainId: string;

  @Input()
  ruleChainType: RuleChainType;

  @Output()
  initRuleNode = new EventEmitter<void>();

  @Output()
  changeScript = new EventEmitter<void>();

  nodeDefinitionValue: RuleNodeDefinition;

  @Input()
  set nodeDefinition(nodeDefinition: RuleNodeDefinition) {
    if (this.nodeDefinitionValue !== nodeDefinition) {
      this.nodeDefinitionValue = nodeDefinition;
      if (this.nodeDefinitionValue) {
        this.validateDefinedDirective();
      }
      setTimeout(() => this.initRuleNode.emit());
    }
  }

  get nodeDefinition(): RuleNodeDefinition {
    return this.nodeDefinitionValue;
  }

  definedDirectiveError: string;

  ruleNodeConfigFormGroup: UntypedFormGroup;

  changeSubscription: Subscription;

  changeScriptSubscription: Subscription;

  definedConfigComponent: IRuleNodeConfigurationComponent;

  private definedConfigComponentRef: ComponentRef<IRuleNodeConfigurationComponent>;

  private configuration: RuleNodeConfiguration;

  private propagateChange = (_v: any) => { };

  constructor(private ruleChainService: RuleChainService,
              private fb: UntypedFormBuilder) {
    this.ruleNodeConfigFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnDestroy(): void {
    if (this.definedConfigComponentRef) {
      this.definedConfigComponentRef.destroy();
    }
    if (this.changeSubscription) {
      this.changeSubscription.unsubscribe();
      this.changeSubscription = null;
    }
    if (this.changeScriptSubscription) {
      this.changeScriptSubscription.unsubscribe();
      this.changeScriptSubscription = null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.ruleNodeConfigFormGroup.disable({emitEvent: false});
    } else {
      this.ruleNodeConfigFormGroup.enable({emitEvent: false});
    }
    if (this.definedConfigComponent) {
      this.definedConfigComponent.disabled = this.disabled;
    }
  }

  writeValue(value: RuleNodeConfiguration): void {
    this.configuration = deepClone(value);
    if (this.changeSubscription) {
      this.changeSubscription.unsubscribe();
      this.changeSubscription = null;
    }
    if (this.definedConfigComponent) {
      this.definedConfigComponent.configuration = this.configuration;
      this.changeSubscription = this.definedConfigComponent.configurationChanged.subscribe((configuration) => {
        this.updateModel(configuration);
      });
    } else {
      this.ruleNodeConfigFormGroup.get('configuration').patchValue(this.configuration, {emitEvent: false});
      this.changeSubscription = this.ruleNodeConfigFormGroup.get('configuration').valueChanges.subscribe(
        (configuration: RuleNodeConfiguration) => {
          this.updateModel(configuration);
        }
      );
    }
  }

  useDefinedDirective(): boolean {
    return this.nodeDefinition &&
      (this.nodeDefinition.configDirective &&
       this.nodeDefinition.configDirective.length) && !this.definedDirectiveError;
  }

  private updateModel(configuration: RuleNodeConfiguration) {
    if (this.definedConfigComponent || this.ruleNodeConfigFormGroup.valid) {
      this.propagateChange(configuration);
    } else {
      this.propagateChange(this.required ? null : configuration);
    }
  }

  private validateDefinedDirective() {
    if (this.definedConfigComponentRef) {
      this.definedConfigComponentRef.destroy();
      this.definedConfigComponentRef = null;
    }
    if (this.nodeDefinition.uiResourceLoadError && this.nodeDefinition.uiResourceLoadError.length) {
      this.definedDirectiveError = this.nodeDefinition.uiResourceLoadError;
    } else if (this.nodeDefinition.configDirective && this.nodeDefinition.configDirective.length) {
      if (this.changeSubscription) {
        this.changeSubscription.unsubscribe();
        this.changeSubscription = null;
      }
      if (this.changeScriptSubscription) {
        this.changeScriptSubscription.unsubscribe();
        this.changeScriptSubscription = null;
      }
      this.definedConfigContainer.clear();
      const component = this.ruleChainService.getRuleNodeConfigComponent(this.nodeDefinition.configDirective);
      this.definedConfigComponentRef = this.definedConfigContainer.createComponent(component);
      this.definedConfigComponent = this.definedConfigComponentRef.instance;
      this.definedConfigComponent.ruleNodeId = this.ruleNodeId;
      this.definedConfigComponent.ruleChainId = this.ruleChainId;
      this.definedConfigComponent.ruleChainType = this.ruleChainType;
      this.definedConfigComponent.configuration = this.configuration;
      this.changeSubscription = this.definedConfigComponent.configurationChanged.subscribe((configuration) => {
        this.updateModel(configuration);
      });
      if (this.definedConfigComponent?.changeScript) {
        this.changeScriptSubscription = this.definedConfigComponent.changeScript.subscribe(() => this.changeScript.emit());
      }
    }
  }

  validate() {
    if (this.useDefinedDirective()) {
      this.definedConfigComponent.validate();
    }
  }
}
