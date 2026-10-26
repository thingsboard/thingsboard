// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AiResourceInfo, isAiImageResource } from '@shared/models/ai-chat.models';
import { AiResourceService } from '@core/http/ai-resource.service';
import {
  aiAttachmentExtension,
  aiAttachmentIcon,
  AiAttachmentItem
} from '@home/components/ai/ai-attachment.models';
import {
  AiAttachmentPreviewDialogComponent,
  AiAttachmentPreviewDialogData
} from '@home/components/ai/ai-attachment-preview-dialog.component';

@Component({
  selector: 'tb-ai-attachments',
  templateUrl: './ai-attachments.component.html',
  styleUrls: ['./ai-attachments.component.scss'],
  standalone: false
})
export class AiAttachmentsComponent implements OnChanges {

  @Input() resources: AiResourceInfo[];

  @Input() items: AiAttachmentItem[];

  @Input() removable = false;

  @Output() removed = new EventEmitter<AiAttachmentItem>();

  shown: AiAttachmentItem[] = [];

  readonly icon = aiAttachmentIcon;

  readonly extension = aiAttachmentExtension;

  constructor(private aiResourceService: AiResourceService,
              private dialog: MatDialog) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.items || changes.resources) {
      this.shown = this.items ?? (this.resources ?? []).map(resource => ({
        key: resource.id,
        name: resource.name,
        mediaType: resource.mediaType,
        size: resource.size,
        image: isAiImageResource(resource),
        url$: isAiImageResource(resource) ? this.aiResourceService.getUrl(resource.id) : undefined,
        resource,
        status: 'ready'
      }));
    }
  }

  open(item: AiAttachmentItem): void {
    if (item.status === 'uploading' || item.status === 'error') {
      return;
    }
    if (item.image && item.url$) {
      this.dialog.open<AiAttachmentPreviewDialogComponent, AiAttachmentPreviewDialogData>(AiAttachmentPreviewDialogComponent, {
        panelClass: ['tb-dialog', 'tb-ai-attachment-preview-dialog'],
        maxWidth: '92vw',
        data: {item}
      });
    } else if (item.resource) {
      this.aiResourceService.download(item.resource).subscribe({error: () => {}});
    }
  }

  remove(event: Event, item: AiAttachmentItem): void {
    event.stopPropagation();
    this.removed.emit(item);
  }

}
