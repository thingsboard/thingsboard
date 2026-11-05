// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { AiResourceService } from '@core/http/ai-resource.service';
import { AiResourceInfo } from '@shared/models/ai-chat.models';

export interface AiAttachmentPreviewDialogData {
  resource: AiResourceInfo;
}

interface AiAttachmentPreview {
  url?: string;
  error?: string;
}

@Component({
  selector: 'tb-ai-attachment-preview-dialog',
  templateUrl: './ai-attachment-preview-dialog.component.html',
  styleUrls: ['./ai-attachment-preview-dialog.component.scss'],
  standalone: false
})
export class AiAttachmentPreviewDialogComponent extends DialogComponent<AiAttachmentPreviewDialogComponent> {

  readonly preview$: Observable<AiAttachmentPreview>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AiAttachmentPreviewDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: AiAttachmentPreviewDialogData,
              private aiResourceService: AiResourceService,
              private translate: TranslateService,
              private sanitizer: DomSanitizer) {
    super(store, router, dialogRef);
    this.preview$ = this.aiResourceService.getUrl(data.resource.id).pipe(
      map(url => ({url})),
      catchError(error => of({error: this.errorMessage(error)}))
    );
  }

  download(): void {
    this.aiResourceService.download(this.data.resource).subscribe({
      error: error => this.store.dispatch(new ActionNotificationShow({
        message: this.errorMessage(error),
        target: 'aiAttachmentPreview',
        modern: true,
        type: 'error',
        verticalPosition: 'bottom',
        horizontalPosition: 'center'
      }))
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  private errorMessage(error: any): string {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    const message = error ? parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message : null;
    return message || this.translate.instant('ai-assistant.attachment-load-failed');
  }

}
