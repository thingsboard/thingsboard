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
import { AppState, getCurrentAuthState, NodeScriptTestService } from '@core/public-api';
import { Store } from '@ngrx/store';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import {
  RuleNodeConfiguration,
  RuleNodeConfigurationComponent,
  ScriptLanguage
} from '@app/shared/models/rule-node.models';
import type { JsFuncComponent } from '@app/shared/components/js-func.component';
import { AlarmSeverity, alarmSeverityTranslations } from '@app/shared/models/alarm.models';
import { DebugRuleNodeEventBody } from '@shared/models/event.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-action-node-create-alarm-config',
  templateUrl: './create-alarm-config.component.html',
  styleUrls: []
})
export class CreateAlarmConfigComponent extends RuleNodeConfigurationComponent {

  @ViewChild('jsFuncComponent', {static: false}) jsFuncComponent: JsFuncComponent;
  @ViewChild('tbelFuncComponent', {static: false}) tbelFuncComponent: JsFuncComponent;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityTranslationMap = alarmSeverityTranslations;
  createAlarmConfigForm: UntypedFormGroup;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;

  scriptLanguage = ScriptLanguage;

  changeScript: EventEmitter<void> = new EventEmitter<void>();

  readonly hasScript = true;

  readonly testScriptLabel = 'rule-node-config.test-details-function';

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private nodeScriptTestService: NodeScriptTestService,
              private translate: TranslateService) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.createAlarmConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.createAlarmConfigForm = this.fb.group({
      scriptLang: [configuration ? configuration.scriptLang : ScriptLanguage.JS, [Validators.required]],
      alarmDetailsBuildJs: [configuration ? configuration.alarmDetailsBuildJs : null, []],
      alarmDetailsBuildTbel: [configuration ? configuration.alarmDetailsBuildTbel : null, []],
      useMessageAlarmData: [configuration ? configuration.useMessageAlarmData : false, []],
      overwriteAlarmDetails: [configuration ? configuration.overwriteAlarmDetails : false, []],
      alarmType: [configuration ? configuration.alarmType : null, []],
      severity: [configuration ? configuration.severity : null, []],
      propagate: [configuration ? configuration.propagate : false, []],
      relationTypes: [configuration ? configuration.relationTypes : null, []],
      propagateToOwner: [configuration ? configuration.propagateToOwner : false, []],
      propagateToTenant: [configuration ? configuration.propagateToTenant : false, []],
      dynamicSeverity: false
    });

    this.createAlarmConfigForm.get('dynamicSeverity').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((dynamicSeverity) => {
      if(dynamicSeverity){
        this.createAlarmConfigForm.get('severity').patchValue('',{emitEvent:false});
      } else {
        this.createAlarmConfigForm.get('severity').patchValue(this.alarmSeverities[0],{emitEvent:false});
      }
    });

  }


  protected validatorTriggers(): string[] {
    return ['useMessageAlarmData', 'overwriteAlarmDetails', 'scriptLang'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useMessageAlarmData: boolean = this.createAlarmConfigForm.get('useMessageAlarmData').value;
    const overwriteAlarmDetails: boolean = this.createAlarmConfigForm.get('overwriteAlarmDetails').value;
    if (useMessageAlarmData) {
      this.createAlarmConfigForm.get('alarmType').setValidators([]);
      this.createAlarmConfigForm.get('severity').setValidators([]);
    } else {
      this.createAlarmConfigForm.get('alarmType').setValidators([Validators.required]);
      this.createAlarmConfigForm.get('severity').setValidators([Validators.required]);
    }
    this.createAlarmConfigForm.get('alarmType').updateValueAndValidity({emitEvent});
    this.createAlarmConfigForm.get('severity').updateValueAndValidity({emitEvent});

    let scriptLang: ScriptLanguage = this.createAlarmConfigForm.get('scriptLang').value;
    if (scriptLang === ScriptLanguage.TBEL && !this.tbelEnabled) {
      scriptLang = ScriptLanguage.JS;
      this.createAlarmConfigForm.get('scriptLang').patchValue(scriptLang, {emitEvent: false});
      setTimeout(() => {this.createAlarmConfigForm.updateValueAndValidity({emitEvent: true});});
    }
    const useAlarmDetailsBuildScript = useMessageAlarmData === false || overwriteAlarmDetails === true;
    this.createAlarmConfigForm.get('alarmDetailsBuildJs')
      .setValidators(useAlarmDetailsBuildScript && scriptLang === ScriptLanguage.JS ? [Validators.required] : []);
    this.createAlarmConfigForm.get('alarmDetailsBuildTbel')
      .setValidators(useAlarmDetailsBuildScript && scriptLang === ScriptLanguage.TBEL ? [Validators.required] : []);
    this.createAlarmConfigForm.get('alarmDetailsBuildJs').updateValueAndValidity({emitEvent});
    this.createAlarmConfigForm.get('alarmDetailsBuildTbel').updateValueAndValidity({emitEvent});
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (configuration) {
      if (!configuration.scriptLang) {
        configuration.scriptLang = ScriptLanguage.JS;
      }
    }
    return configuration;
  }

  testScript(debugEventBody?: DebugRuleNodeEventBody) {
    const scriptLang: ScriptLanguage = this.createAlarmConfigForm.get('scriptLang').value;
    const scriptField = scriptLang === ScriptLanguage.JS ? 'alarmDetailsBuildJs' : 'alarmDetailsBuildTbel';
    const helpId = scriptLang === ScriptLanguage.JS ? 'rulenode/create_alarm_node_script_fn' : 'rulenode/tbel/create_alarm_node_script_fn';
    const script: string = this.createAlarmConfigForm.get(scriptField).value;
    this.nodeScriptTestService.testNodeScript(
      script,
      'json',
      this.translate.instant('rule-node-config.details'),
      'Details',
      ['msg', 'metadata', 'msgType'],
      this.ruleNodeId,
      helpId,
      scriptLang,
      debugEventBody
    ).subscribe((theScript) => {
      if (theScript) {
        this.createAlarmConfigForm.get(scriptField).setValue(theScript);
        this.changeScript.emit();
      }
    });
  }

  removeKey(key: string, keysField: string): void {
    const keys: string[] = this.createAlarmConfigForm.get(keysField).value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.createAlarmConfigForm.get(keysField).setValue(keys, {emitEvent: true});
    }
  }

  addKey(event: MatChipInputEvent, keysField: string): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.createAlarmConfigForm.get(keysField).value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.createAlarmConfigForm.get(keysField).setValue(keys, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }

  protected onValidate() {
    const useMessageAlarmData: boolean = this.createAlarmConfigForm.get('useMessageAlarmData').value;
    const overwriteAlarmDetails: boolean = this.createAlarmConfigForm.get('overwriteAlarmDetails').value;
    if (!useMessageAlarmData || overwriteAlarmDetails) {
      const scriptLang: ScriptLanguage = this.createAlarmConfigForm.get('scriptLang').value;
      if (scriptLang === ScriptLanguage.JS) {
        this.jsFuncComponent.validateOnSubmit();
      }
    }
  }
}
