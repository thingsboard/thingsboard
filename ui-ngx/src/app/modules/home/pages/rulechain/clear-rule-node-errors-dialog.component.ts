///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { FcRuleNode } from "@shared/models/rule-node.models";
import { ContentType } from "@shared/models/constants";
import { sortObjectKeys } from "@core/utils";
import {
  EventContentDialogComponent,
  EventContentDialogData
} from "@home/components/event/event-content-dialog.component";

export interface ClearRuleNodeErrorsDialogData {
  node: FcRuleNode
}

@Component({
  selector: 'tb-clear-rule-node-errors-dialog',
  templateUrl: './clear-rule-node-errors-dialog.component.html',
  styleUrls: ['./clear-rule-node-errors-dialog.component.scss']
})
export class ClearRuleNodeErrorsDialogComponent extends DialogComponent<ClearRuleNodeErrorsDialogComponent> implements OnInit {

  @ViewChild('lastMessage', {static: true}) lastMessage: ElementRef;

  node:FcRuleNode = this.data.node;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ClearRuleNodeErrorsDialogData,
              public dialogRef: MatDialogRef<ClearRuleNodeErrorsDialogComponent, boolean>,
              public translate: TranslateService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.lastMessage.nativeElement.value = this.node.stats.lastErrorMsg ? this.node.stats.lastErrorMsg : '-';
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  acknowledge(): void {
    this.dialogRef.close(true);
  }

  showData($event) {
    this.showContent($event, this.node.stats.msgData, 'event.metadata', ContentType.JSON, true);
  }

  showMetadata($event) {
    this.showContent($event, JSON.stringify(this.node.stats.msgMetadata), 'event.metadata', ContentType.JSON, true);
  }

  viewOnDashboard($event: MouseEvent) {
    if ($event) {
      $event.stopPropagation();
    }
    const urlApiUsage = 'usage?state=W3siaWQiOiJkZWZhdWx0IiwicGFyYW1zIjp7fX0seyJpZCI6InJ1bGVfZW5naW5lX3N0YXRpc3RpY3MiLCJwYXJhbXMiOnt9fV0%253D';
    window.open(urlApiUsage, '_blank');
  }

  showContent($event: MouseEvent, content: string, title: string, contentType: ContentType = null, sortKeys = false): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (contentType === ContentType.JSON && sortKeys) {
      try {
        content = JSON.stringify(sortObjectKeys(JSON.parse(content)));
      } catch (e) {}
    }
    this.dialog.open<EventContentDialogComponent, EventContentDialogData>(EventContentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType
      }
    });
  }
}
