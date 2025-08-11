///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MatDialog } from '@angular/material/dialog';
import { AIModelDialogComponent, AIModelDialogData } from '@home/components/ai-model/ai-model-dialog.component';
import { AiModel, AiRuleNodeResponseFormatTypeOnlyText, ResponseFormat } from '@shared/models/ai-model.models';
import { deepTrim } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { jsonRequired } from '@shared/components/json-object-edit.component';

@Component({
  selector: 'tb-external-node-ai-config',
  templateUrl: './ai-config.component.html',
  styleUrls: []
})
export class AiConfigComponent extends RuleNodeConfigurationComponent {

  aiConfigForm: UntypedFormGroup;

  entityType = EntityType;

  responseFormat = ResponseFormat;

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aiConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aiConfigForm = this.fb.group({
      modelId: [configuration?.modelId ?? null, [Validators.required]],
      systemPrompt: [configuration?.systemPrompt ?? '', [Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      userPrompt: [configuration?.userPrompt ?? '', [Validators.required, Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      responseFormat: this.fb.group({
        type: [configuration?.responseFormat?.type ?? ResponseFormat.JSON, []],
        schema: [configuration?.responseFormat?.schema ?? null, [jsonRequired]],
      }),
      timeoutSeconds: [configuration?.timeoutSeconds ?? 60, []],
      forceAck: [configuration?.forceAck ?? true, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['responseFormat.type'];
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.aiConfigForm.get('responseFormat.type').value === ResponseFormat.JSON_SCHEMA) {
      this.aiConfigForm.get('responseFormat.schema').enable({emitEvent: false});
    } else {
      this.aiConfigForm.get('responseFormat.schema').disable({emitEvent: false});
    }
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (!this.aiConfigForm.get('systemPrompt').value) {
      delete configuration.systemPrompt;
    }
    return deepTrim(configuration);
  }

  onEntityChange($event: AiModel) {
    if ($event) {
      if (AiRuleNodeResponseFormatTypeOnlyText.includes($event.configuration.provider)) {
        if (this.aiConfigForm.get('responseFormat.type').value !== ResponseFormat.TEXT) {
          this.aiConfigForm.get('responseFormat.type').patchValue(ResponseFormat.TEXT, {emitEvent: true});
        }
        this.aiConfigForm.get('responseFormat.type').disable();
      }
    } else {
      this.aiConfigForm.get('responseFormat.type').enable();
    }
  }

  get getResponseFormatHint() {
    return this.translate.instant(`rule-node-config.ai.response-format-hint-${this.aiConfigForm.get('responseFormat.type').value}`);
  }

  createModelAi(formControl: string) {
    this.dialog.open<AIModelDialogComponent, AIModelDialogData, AiModel>(AIModelDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true
      }
    }).afterClosed()
      .subscribe((model) => {
        if (model) {
          this.aiConfigForm.get(formControl).patchValue(model.id);
          this.aiConfigForm.get(formControl).markAsDirty();
        }
      });
  }
}
