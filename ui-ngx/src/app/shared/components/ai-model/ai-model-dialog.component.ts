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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { StepperOrientation } from '@angular/cdk/stepper';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AiModel, AiProvider, AiProviderTranslations, AiProviderWithApiKey } from '@shared/models/ai-model.models';
import { AiModelService } from '@core/http/ai-model.service';
import { CheckConnectivityDialogComponent } from '@shared/components/ai-model/check-connectivity-dialog.component';

export interface AIModelDialogData {
  AIModel?: AiModel;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-ai-model-dialog',
  templateUrl: './ai-model-dialog.component.html',
  styleUrls: ['./ai-model-dialog.component.scss']
})
export class AIModelDialogComponent extends DialogComponent<AIModelDialogComponent, AiModel> {

  readonly entityType = EntityType;

  selectedIndex = 0;

  dialogTitle = 'ai-models.ai-model';

  stepperOrientation: Observable<StepperOrientation>;

  aiProvider = AiProvider;
  providerMap: AiProvider[] = Object.keys(AiProvider) as AiProvider[];
  providerTranslationMap = AiProviderTranslations;

  aiProviderWithApiKey: AiProvider[] = AiProviderWithApiKey;

  provider: AiProvider = AiProvider.OPENAI;

  aiModelForms: FormGroup;

  isAdd = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<AIModelDialogComponent, AiModel>,
              @Inject(MAT_DIALOG_DATA) public data: AIModelDialogData,
              private fb: FormBuilder,
              private aiModelService: AiModelService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    if (this.data.isAdd) {
      this.isAdd = true;
    }

    this.provider = this.data.AIModel ? this.data.AIModel.configuration.provider : AiProvider.OPENAI;

    this.aiModelForms = this.fb.group({
      name: [this.data.AIModel ? this.data.AIModel.name : '', [Validators.required, Validators.maxLength(255)]],
      modelType: ['CHAT'],
      configuration: this.fb.group({
        provider: [this.provider, [Validators.required]],
        providerConfig: this.fb.group({
          apiKey: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.apiKey : '', [Validators.required]],
          personalAccessToken: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.personalAccessToken : '', [Validators.required]],
          endpoint: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.endpoint : '', [Validators.required]],
          serviceVersion: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.serviceVersion : ''],
          projectId: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.projectId : '', [Validators.required]],
          location: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.location : '', [Validators.required]],
          serviceAccountKey: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.serviceAccountKey : '', [Validators.required]],
          serviceAccountKeyFileName: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.serviceAccountKeyFileName : '', [Validators.required]],
        }),
        modelId: [this.data.AIModel ? this.data.AIModel.configuration?.modelId : '', [Validators.required]],
        temperature: [this.data.AIModel ? this.data.AIModel.configuration?.temperature : null, [Validators.min(0)]],
        topP: this.data.AIModel ? this.data.AIModel.configuration?.topP : [null, [Validators.min(0.1), Validators.max(1)]],
        topK: [this.data.AIModel ? this.data.AIModel.configuration?.topK : null, [Validators.min(0)]],
        frequencyPenalty: [this.data.AIModel ? this.data.AIModel.configuration?.frequencyPenalty : null],
        presencePenalty: [this.data.AIModel ? this.data.AIModel.configuration?.presencePenalty : null],
        maxOutputTokens: [this.data.AIModel ? this.data.AIModel.configuration?.maxOutputTokens : null, [Validators.min(1)]]
      })
    });

    this.aiModelForms.get('configuration.provider').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((provider: AiProvider) => {
      this.provider = provider;
      // this.aiModelForms.get('configuration').reset({});
      this.aiModelForms.get('configuration.providerConfig').reset({});
      this.updateValidation(provider);
    })

    this.updateValidation(this.provider);
  }

  private updateValidation(provider: AiProvider) {
    const providerConfig = this.aiModelForms.get('configuration.providerConfig');
    if (this.aiProviderWithApiKey.includes(provider)) {
      providerConfig.get('apiKey').enable();
    } else {
      providerConfig.get('apiKey').disable();
    }
    if (provider === AiProvider.GITHUB_MODELS) {
      providerConfig.get('personalAccessToken').enable();
    } else {
      providerConfig.get('personalAccessToken').disable();
    }
    if (provider === AiProvider.GOOGLE_VERTEX_AI_GEMINI) {
      providerConfig.get('projectId').enable();
      providerConfig.get('location').enable();
      providerConfig.get('serviceAccountKey').enable();
      providerConfig.get('serviceAccountKeyFileName').enable();
    } else {
      providerConfig.get('projectId').disable();
      providerConfig.get('location').disable();
      providerConfig.get('serviceAccountKey').disable();
      providerConfig.get('serviceAccountKeyFileName').disable();
    }
    if (provider === AiProvider.AZURE_OPENAI) {
      providerConfig.get('endpoint').enable();
      providerConfig.get('serviceVersion').enable();
    } else {
      providerConfig.get('endpoint').disable();
      providerConfig.get('serviceVersion').disable();
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  checkConnectivity() {
    return this.dialog.open<CheckConnectivityDialogComponent, AIModelDialogData>(CheckConnectivityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        AIModel: this.aiModelForms.value
      }
    }).afterClosed();
  }

  add(): void {
    const aiModel = {...this.data.AIModel, ...this.aiModelForms.value} as AiModel;
    this.aiModelService.saveAiModel(aiModel).subscribe(aiModel => this.dialogRef.close(aiModel));
  }
}
