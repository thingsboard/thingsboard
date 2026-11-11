// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { aiFileName } from '@home/components/ai/ai-file.models';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AiResourceInfo, isAiImageResource } from '@shared/models/ai-chat.models';
import { AiResourceService } from '@core/http/ai-resource.service';
import { aiAttachmentExtension, aiAttachmentKind, aiResourceFact } from '@home/components/ai/ai-attachment.models';
import { TranslateService } from '@ngx-translate/core';
import {
  AiAttachmentPreviewDialogComponent,
  AiAttachmentPreviewDialogData
} from '@home/components/ai/ai-attachment-preview-dialog.component';

interface AiAttachmentItem {
  resource: AiResourceInfo;
  image: boolean;
  icon: string;
  extension: string;
  url$?: Observable<string | null>;
}

@Component({
  selector: 'tb-ai-attachments',
  templateUrl: './ai-attachments.component.html',
  styleUrls: ['./ai-attachments.component.scss'],
  standalone: false
})
export class AiAttachmentsComponent implements OnChanges {

  readonly fileName = aiFileName;

  @Input() resources: AiResourceInfo[];

  @Output() resourceError = new EventEmitter<any>();

  shown: AiAttachmentItem[] = [];

  factOf(item: AiAttachmentItem): string | null {
    return item.image ? null : aiResourceFact(item.resource.facts, this.translateService);
  }

  constructor(private aiResourceService: AiResourceService,
              private dialog: MatDialog,
              private translateService: TranslateService) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.resources) {
      this.shown = (this.resources ?? []).map(resource => {
        const image = isAiImageResource(resource);
        return {
          resource,
          image,
          icon: aiAttachmentKind(resource.mediaType, resource.name).icon,
          extension: aiAttachmentExtension(resource.name),
          url$: image ? this.aiResourceService.getUrl(resource.id).pipe(catchError(() => [null])) : undefined
        };
      });
    }
  }

  open(item: AiAttachmentItem): void {
    if (item.image) {
      this.dialog.open<AiAttachmentPreviewDialogComponent, AiAttachmentPreviewDialogData>(AiAttachmentPreviewDialogComponent, {
        panelClass: ['tb-dialog', 'tb-ai-attachment-preview-dialog'],
        maxWidth: '92vw',
        data: {resource: item.resource}
      });
    } else {
      this.aiResourceService.download(item.resource).subscribe({
        error: error => this.resourceError.emit(error)
      });
    }
  }

}
