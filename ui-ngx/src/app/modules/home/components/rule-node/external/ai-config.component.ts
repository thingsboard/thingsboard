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
import { AIModelDialogComponent, AIModelDialogData } from '@shared/components/ai-model/ai-model-dialog.component';
import { AiModel, AiProvider } from '@shared/models/ai-model.models';
import { deepTrim } from '@core/utils';

enum ResponseFormat {
  TEXT = 'TEXT',
  JSON = 'JSON',
  JSON_SCHEMA = 'JSON_SCHEMA'
}

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
              private dialog: MatDialog) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aiConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aiConfigForm = this.fb.group({
      modelSettingsId: [configuration ? configuration.modelSettingsId : null, [Validators.required]],
      systemPrompt: [configuration ? configuration.systemPrompt : '', [Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      userPrompt: [configuration ? configuration.userPrompt : '', [Validators.required, Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      responseFormat: this.fb.group({
        type: [configuration ? configuration.responseFormat.type : ResponseFormat.JSON, []],
        schema: [configuration ? configuration.responseFormat.schema : null, []],
      }),
      timeoutSeconds: [configuration ? configuration.timeoutSeconds : 60, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['responseFormat.type'];
  }

  protected updateValidators(emitEvent: boolean) {
    const responseFormatType = this.aiConfigForm.get('responseFormat.type').value;
    if (responseFormatType === ResponseFormat.JSON_SCHEMA) {
      this.aiConfigForm.get('responseFormat.schema').setValidators([Validators.required]);
      this.aiConfigForm.get('responseFormat.schema').enable();
    } else {
      this.aiConfigForm.get('responseFormat.schema').setValidators([]);
      this.aiConfigForm.get('responseFormat.schema').disable();
    }
    this.aiConfigForm.get('responseFormat.schema').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  onEntityChange($event: AiModel) {
    if ($event) {
      if ($event.configuration.provider === AiProvider.AMAZON_BEDROCK ||
        $event.configuration.provider === AiProvider.ANTHROPIC ||
        $event.configuration.provider === AiProvider.GITHUB_MODELS) {
        this.aiConfigForm.get('responseFormat.type').patchValue(ResponseFormat.TEXT, {emitEvent: false});
        this.aiConfigForm.get('responseFormat.type').disable({emitEvent: false});
      }
    } else {
      this.aiConfigForm.get('responseFormat.type').enable({emitEvent: false});
    }
  }

  createModelAi(formControl: string,) {
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
