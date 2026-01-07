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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { StepperOrientation } from '@angular/cdk/stepper';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AiModel,
  AiModelMap,
  AiProvider,
  AiProviderTranslations,
  AuthenticationType,
  ModelType,
  ProviderFieldsAllList
} from '@shared/models/ai-model.models';
import { AiModelService } from '@core/http/ai-model.service';
import { CheckConnectivityDialogComponent } from '@home/components/ai-model/check-connectivity-dialog.component';
import { map } from 'rxjs/operators';
import { deepTrim } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';

export interface AIModelDialogData {
  AIModel?: AiModel;
  isAdd?: boolean;
  name?: string;
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

  AuthenticationType = AuthenticationType;

  provider: AiProvider = AiProvider.OPENAI;

  aiModelForms: FormGroup;

  isAdd = false;

  authenticationHint: string;

  apiKeyRequired = true;
  private readonly openAiDefaultBaseUrl = 'https://api.openai.com/v1';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<AIModelDialogComponent, AiModel>,
              @Inject(MAT_DIALOG_DATA) public data: AIModelDialogData,
              private fb: FormBuilder,
              private aiModelService: AiModelService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    if (this.data.isAdd) {
      this.isAdd = true;
    }

    this.provider = this.data.AIModel ? this.data.AIModel.configuration.provider : AiProvider.OPENAI;

    this.aiModelForms = this.fb.group({
      name: [this.data.AIModel ? this.data.AIModel.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/.*\S.*/)]],
      modelType: [ModelType.CHAT],
      configuration: this.fb.group({
        provider: [this.provider, []],
        providerConfig: this.fb.group({
          apiKey: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.apiKey : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          personalAccessToken: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.personalAccessToken : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          endpoint: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.endpoint : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          serviceVersion: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.serviceVersion : ''],
          projectId: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.projectId : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          location: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.location : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          serviceAccountKey: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.serviceAccountKey : '', [Validators.required]],
          fileName: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.fileName : '', [Validators.required]],
          region: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.region : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          accessKeyId: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.accessKeyId : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          secretAccessKey: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.secretAccessKey : '', [Validators.required, Validators.pattern(/.*\S.*/)]],
          baseUrl: [this.data.AIModel ? this.data.AIModel.configuration.providerConfig?.baseUrl : this.openAiDefaultBaseUrl, [Validators.required, Validators.pattern(/.*\S.*/)]],
          auth: this.fb.group({
            type: [this.data.AIModel?.configuration?.providerConfig?.auth?.type ?? AuthenticationType.NONE],
            username: [this.data.AIModel?.configuration?.providerConfig?.auth?.username ?? '', [Validators.required, Validators.pattern(/.*\S.*/)]],
            password: [this.data.AIModel?.configuration?.providerConfig?.auth?.password ?? '', [Validators.required, Validators.pattern(/.*\S.*/)]],
            token: [this.data.AIModel?.configuration?.providerConfig?.auth?.token ?? '', [Validators.required, Validators.pattern(/.*\S.*/)]]
          })
        }),
        modelId: [this.data.AIModel ? this.data.AIModel.configuration?.modelId : '', [Validators.required]],
        temperature: [this.data.AIModel ? this.data.AIModel.configuration?.temperature : null, [Validators.min(0)]],
        topP: [this.data.AIModel ? this.data.AIModel.configuration?.topP : null, [Validators.min(0.1), Validators.max(1)]],
        topK: [this.data.AIModel ? this.data.AIModel.configuration?.topK : null, [Validators.min(0)]],
        frequencyPenalty: [this.data.AIModel ? this.data.AIModel.configuration?.frequencyPenalty : null],
        presencePenalty: [this.data.AIModel ? this.data.AIModel.configuration?.presencePenalty : null],
        maxOutputTokens: [this.data.AIModel ? this.data.AIModel.configuration?.maxOutputTokens : null],
        contextLength: [this.data.AIModel ? this.data.AIModel.configuration?.contextLength : null]
      })
    });

    if (this.data.name) {
      this.aiModelForms.get('name').patchValue(this.data.name, {emitEvent: false});
    }

    this.aiModelForms.get('configuration.provider').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((provider: AiProvider) => {
      this.provider = provider;
      this.aiModelForms.get('configuration.modelId').reset('');
      this.aiModelForms.get('configuration.providerConfig').reset({});
      this.updateValidation(provider);
      if (provider === AiProvider.OPENAI) {
        this.aiModelForms.get('configuration.providerConfig.baseUrl').patchValue(this.openAiDefaultBaseUrl, {emitEvent: false});
        this.updateApiKeyValidatorForOpenAIProvider(this.openAiDefaultBaseUrl);
      }
    });

    this.aiModelForms.get('configuration.providerConfig.baseUrl').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((url: string) => {
      if (this.provider === AiProvider.OPENAI) {
        this.updateApiKeyValidatorForOpenAIProvider(url);
      }
    });

    this.aiModelForms.get('configuration.providerConfig.auth.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type: AuthenticationType) => {
      this.getAuthenticationHint(type);
      this.aiModelForms.get('configuration.providerConfig.auth.username').disable();
      this.aiModelForms.get('configuration.providerConfig.auth.password').disable();
      this.aiModelForms.get('configuration.providerConfig.auth.token').disable();
      if (type === AuthenticationType.BASIC) {
        this.aiModelForms.get('configuration.providerConfig.auth.username').enable();
        this.aiModelForms.get('configuration.providerConfig.auth.password').enable();
      }
      if (type === AuthenticationType.TOKEN) {
        this.aiModelForms.get('configuration.providerConfig.auth.token').enable();
      }
    });

    this.updateValidation(this.provider);
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
      const search = searchText ? searchText?.toLowerCase() : '';
      return of(this.provider ? AiModelMap.get(this.provider).modelList || [] : []).pipe(
        map(name => name?.filter(option => option.toLowerCase().includes(search))),
      );
  }

  private updateApiKeyValidatorForOpenAIProvider(url: string) {
    if (url !== this.openAiDefaultBaseUrl) {
      this.aiModelForms.get('configuration.providerConfig.apiKey').removeValidators(Validators.required);
      this.apiKeyRequired = false;
    } else {
      this.aiModelForms.get('configuration.providerConfig.apiKey').addValidators(Validators.required);
      this.apiKeyRequired = true;
    }
    this.aiModelForms.get('configuration.providerConfig.apiKey').updateValueAndValidity({emitEvent: false});
  }

  private getAuthenticationHint(type: AuthenticationType) {
    if (type === AuthenticationType.BASIC) {
      this.authenticationHint = this.translate.instant('ai-models.authentication-basic-hint');
    } else if (type === AuthenticationType.TOKEN) {
      this.authenticationHint = this.translate.instant('ai-models.authentication-token-hint');
    } else {
      this.authenticationHint = null;
    }
  }

  private updateValidation(provider: AiProvider) {
    ProviderFieldsAllList.forEach(key => {
      if (AiModelMap.get(provider).providerFieldsList.includes(key)) {
        this.aiModelForms.get('configuration.providerConfig').get(key).enable();
      } else {
        this.aiModelForms.get('configuration.providerConfig').get(key).disable();
      }
    });
    if (provider === AiProvider.OLLAMA) {
      this.aiModelForms.get('configuration.providerConfig.auth').enable();
      this.aiModelForms.get('configuration.providerConfig.auth.type').patchValue(this.data.AIModel?.configuration?.providerConfig?.auth?.type ?? AuthenticationType.NONE, {emitEvent: true});
    } else {
      this.aiModelForms.get('configuration.providerConfig.auth').disable();
    }
  }

  get providerFieldsList(): string[] {
    return AiModelMap.get(this.provider).providerFieldsList;
  }
  get modelFieldsList(): string[] {
    return AiModelMap.get(this.provider).modelFieldsList;
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
    this.aiModelService.saveAiModel(deepTrim(aiModel)).subscribe(aiModel => this.dialogRef.close(aiModel));
  }
}
