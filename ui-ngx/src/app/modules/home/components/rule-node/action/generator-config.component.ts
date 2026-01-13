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

import { Component, EventEmitter, ViewChild } from '@angular/core';
import { getCurrentAuthState, isDefinedAndNotNull, NodeScriptTestService } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {
  RuleNodeConfiguration,
  RuleNodeConfigurationComponent,
  ScriptLanguage
} from '@app/shared/models/rule-node.models';
import type { JsFuncComponent } from '@app/shared/components/js-func.component';
import { EntityType } from '@app/shared/models/entity-type.models';
import { DebugRuleNodeEventBody } from '@shared/models/event.models';

@Component({
  selector: 'tb-action-node-generator-config',
  templateUrl: './generator-config.component.html',
  styleUrls: ['generator-config.component.scss']
})
export class GeneratorConfigComponent extends RuleNodeConfigurationComponent {

  @ViewChild('jsFuncComponent', {static: false}) jsFuncComponent: JsFuncComponent;
  @ViewChild('tbelFuncComponent', {static: false}) tbelFuncComponent: JsFuncComponent;

  generatorConfigForm: UntypedFormGroup;

  tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;

  scriptLanguage = ScriptLanguage;

  changeScript: EventEmitter<void> = new EventEmitter<void>();

  allowedEntityTypes = [
    EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.CUSTOMER,
    EntityType.USER, EntityType.DASHBOARD
  ];

  additionEntityTypes = {
    TENANT: this.translate.instant('rule-node-config.current-tenant'),
    RULE_NODE: this.translate.instant('rule-node-config.current-rule-node')
  };

  readonly hasScript = true;

  readonly testScriptLabel = 'rule-node-config.test-generator-function';

  constructor(private fb: UntypedFormBuilder,
              private nodeScriptTestService: NodeScriptTestService,
              private translate: TranslateService) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.generatorConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.generatorConfigForm = this.fb.group({
      msgCount: [configuration ? configuration.msgCount : null, [Validators.required, Validators.min(0)]],
      periodInSeconds: [configuration ? configuration.periodInSeconds : null, [Validators.required, Validators.min(1)]],
      originator: [configuration ? configuration.originator : {id: null, entityType: EntityType.RULE_NODE}, []],
      scriptLang: [configuration ? configuration.scriptLang : ScriptLanguage.JS, [Validators.required]],
      jsScript: [configuration ? configuration.jsScript : null, []],
      tbelScript: [configuration ? configuration.tbelScript : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['scriptLang'];
  }

  protected updateValidators(emitEvent: boolean) {
    let scriptLang: ScriptLanguage = this.generatorConfigForm.get('scriptLang').value;
    if (scriptLang === ScriptLanguage.TBEL && !this.tbelEnabled) {
      scriptLang = ScriptLanguage.JS;
      this.generatorConfigForm.get('scriptLang').patchValue(scriptLang, {emitEvent: false});
      setTimeout(() => {this.generatorConfigForm.updateValueAndValidity({emitEvent: true});});
    }
    this.generatorConfigForm.get('jsScript').setValidators(scriptLang === ScriptLanguage.JS ? [Validators.required] : []);
    this.generatorConfigForm.get('jsScript').updateValueAndValidity({emitEvent});
    this.generatorConfigForm.get('tbelScript').setValidators(scriptLang === ScriptLanguage.TBEL ? [Validators.required] : []);
    this.generatorConfigForm.get('tbelScript').updateValueAndValidity({emitEvent});
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      msgCount: isDefinedAndNotNull(configuration?.msgCount) ? configuration?.msgCount : 0,
      periodInSeconds: isDefinedAndNotNull(configuration?.periodInSeconds) ? configuration?.periodInSeconds : 1,
      originator: {
        id: isDefinedAndNotNull(configuration?.originatorId) ? configuration?.originatorId : null,
        entityType: isDefinedAndNotNull(configuration?.originatorType) ? configuration?.originatorType :  EntityType.RULE_NODE
      },
      scriptLang: isDefinedAndNotNull(configuration?.scriptLang) ? configuration?.scriptLang : ScriptLanguage.JS,
      tbelScript: isDefinedAndNotNull(configuration?.tbelScript) ? configuration?.tbelScript : null,
      jsScript: isDefinedAndNotNull(configuration?.jsScript) ? configuration?.jsScript : null,
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (configuration.originator) {
      configuration.originatorId = configuration.originator.id;
      configuration.originatorType = configuration.originator.entityType;
    } else {
      configuration.originatorId = null;
      configuration.originatorType = null;
    }
    delete configuration.originator;
    return configuration;
  }

  testScript(debugEventBody?: DebugRuleNodeEventBody) {
    const scriptLang: ScriptLanguage = this.generatorConfigForm.get('scriptLang').value;
    const scriptField = scriptLang === ScriptLanguage.JS ? 'jsScript' : 'tbelScript';
    const helpId = scriptLang === ScriptLanguage.JS ? 'rulenode/generator_node_script_fn' : 'rulenode/tbel/generator_node_script_fn';
    const script: string = this.generatorConfigForm.get(scriptField).value;
    this.nodeScriptTestService.testNodeScript(
      script,
      'generate',
      this.translate.instant('rule-node-config.generator'),
      'Generate',
      ['prevMsg', 'prevMetadata', 'prevMsgType'],
      this.ruleNodeId,
      helpId,
      scriptLang,
      debugEventBody
    ).subscribe((theScript) => {
      if (theScript) {
        this.generatorConfigForm.get(scriptField).setValue(theScript);
        this.changeScript.emit();
      }
    });
  }

  protected onValidate() {
    const scriptLang: ScriptLanguage = this.generatorConfigForm.get('scriptLang').value;
    if (scriptLang === ScriptLanguage.JS) {
      this.jsFuncComponent.validateOnSubmit();
    }
  }
}
