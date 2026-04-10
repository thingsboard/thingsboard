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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AiModel, AiModelWithUserMsg, ModelType } from '@shared/models/ai-model.models';
import { AiModelService } from '@core/http/ai-model.service';

export interface AIModelDialogData {
  AIModel?: AiModel;
}

@Component({
    selector: 'tb-check-connectivity-dialog',
    templateUrl: './check-connectivity-dialog.component.html',
    styleUrls: ['./check-connectivity-dialog.component.scss'],
    standalone: false
})
export class CheckConnectivityDialogComponent extends DialogComponent<CheckConnectivityDialogComponent> {

  showCheckSuccess = false;
  checkErrMsg = '';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<CheckConnectivityDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: AIModelDialogData,
              private aiModelService: AiModelService) {
    super(store, router, dialogRef);

    if (this.data.AIModel) {
      const aiModelWithMsg: AiModelWithUserMsg = {
        userMessage: {
          contents: [
            {
              contentType: "TEXT",
              text: "What is the capital of Ukraine?"
            }
          ]
        },
        chatModelConfig: {
          modelType: ModelType.CHAT,
          ...this.data.AIModel.configuration,
          maxRetries: 0,
          timeoutSeconds: 20
        }
      }
      this.aiModelService.checkConnectivity(aiModelWithMsg, {
        ignoreErrors: true,
        ignoreLoading: true
      }).subscribe({
        next: (result) => {
          if (result.status === 'SUCCESS') {
            this.showCheckSuccess = true;
          } else {
            try {
              this.checkErrMsg = JSON.parse(result.errorDetails);
            } catch (e) {
              this.checkErrMsg = result.errorDetails;
            }
          }
        },
        error: err => this.checkErrMsg = err.error.message
      });
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
