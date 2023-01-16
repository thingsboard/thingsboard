///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
