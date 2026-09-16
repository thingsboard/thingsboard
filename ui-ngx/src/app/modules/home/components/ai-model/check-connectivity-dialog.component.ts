// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
