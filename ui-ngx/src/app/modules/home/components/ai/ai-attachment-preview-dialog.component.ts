// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AiResourceService } from '@core/http/ai-resource.service';
import { AiAttachmentItem } from '@home/components/ai/ai-attachment.models';

export interface AiAttachmentPreviewDialogData {
  item: AiAttachmentItem;
}

@Component({
  selector: 'tb-ai-attachment-preview-dialog',
  templateUrl: './ai-attachment-preview-dialog.component.html',
  styleUrls: ['./ai-attachment-preview-dialog.component.scss'],
  standalone: false
})
export class AiAttachmentPreviewDialogComponent {

  constructor(@Inject(MAT_DIALOG_DATA) public data: AiAttachmentPreviewDialogData,
              private dialogRef: MatDialogRef<AiAttachmentPreviewDialogComponent>,
              private aiResourceService: AiResourceService) {
  }

  download(): void {
    if (this.data.item.resource) {
      this.aiResourceService.download(this.data.item.resource).subscribe({error: () => {}});
    }
  }

  close(): void {
    this.dialogRef.close();
  }

}
