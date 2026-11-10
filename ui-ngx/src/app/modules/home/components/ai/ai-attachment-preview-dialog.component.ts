// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, SecurityContext } from '@angular/core';
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

import { aiServerMessage } from '@home/components/ai/ai-attachment.models';
import { AiResourceService } from '@core/http/ai-resource.service';
import { AiResourceInfo } from '@shared/models/ai-chat.models';

export interface AiAttachmentPreviewDialogData {
  resource?: AiResourceInfo;
  name?: string;
  url?: string;
  vector?: boolean;
  download?: () => void;
  downloadPng?: () => Promise<void>;
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
  readonly title: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AiAttachmentPreviewDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: AiAttachmentPreviewDialogData,
              private aiResourceService: AiResourceService,
              private translate: TranslateService,
              private sanitizer: DomSanitizer) {
    super(store, router, dialogRef);
    this.title = data.resource?.name ?? data.name;
    this.preview$ = data.url ? of({url: data.url}) : this.aiResourceService.getUrl(data.resource.id).pipe(
      map(url => ({url})),
      catchError(error => of({error: this.errorMessage(error)}))
    );
  }

  download(): void {
    if (this.data.download) {
      this.data.download();
      return;
    }
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

  downloadPng(): void {
    this.data.downloadPng().catch(() => this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('ai-assistant.file.png-failed'),
      target: 'aiAttachmentPreview',
      modern: true,
      type: 'error',
      verticalPosition: 'bottom',
      horizontalPosition: 'center'
    })));
  }

  close(): void {
    this.dialogRef.close();
  }

  private errorMessage(error: unknown): string {
    const message = aiServerMessage(error);
    return message ? this.sanitizer.sanitize(SecurityContext.HTML, message) ?? message : this.translate.instant('ai-assistant.attachment-load-failed');
  }

}
