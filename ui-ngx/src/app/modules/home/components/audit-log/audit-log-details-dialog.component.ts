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

import { Component, ElementRef, Inject, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionStatus, AuditLog } from '@shared/models/audit-log.models';
import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { getAce } from '@shared/models/ace/ace.models';

export interface AuditLogDetailsDialogData {
  auditLog: AuditLog;
}

@Component({
    selector: 'tb-audit-log-details-dialog',
    templateUrl: './audit-log-details-dialog.component.html',
    styleUrls: ['./audit-log-details-dialog.component.scss'],
    standalone: false
})
export class AuditLogDetailsDialogComponent extends DialogComponent<AuditLogDetailsDialogComponent> implements OnInit, OnDestroy {

  @ViewChild('actionDataEditor', {static: true})
  actionDataEditorElmRef: ElementRef;

  @ViewChild('failureDetailsEditor', {static: true})
  failureDetailsEditorElmRef: ElementRef;

  auditLog: AuditLog;
  displayFailureDetails: boolean;
  actionData: string;
  actionFailureDetails: string;
  aceEditors: Ace.Editor[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AuditLogDetailsDialogData,
              public dialogRef: MatDialogRef<AuditLogDetailsDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.auditLog = this.data.auditLog;
    this.displayFailureDetails = this.auditLog.actionStatus === ActionStatus.FAILURE;
    this.actionData = this.auditLog.actionData ? JSON.stringify(this.auditLog.actionData, null, 2) : '';
    this.actionFailureDetails = this.auditLog.actionFailureDetails;

    this.createEditor(this.actionDataEditorElmRef, this.actionData);
    if (this.displayFailureDetails) {
      this.createEditor(this.failureDetailsEditorElmRef, this.actionFailureDetails);
    }
  }

  ngOnDestroy(): void {
    this.aceEditors.forEach(editor => editor.destroy());
    super.ngOnDestroy();
  }

  createEditor(editorElementRef: ElementRef, content: string): void {
    const editorElement = editorElementRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/java',
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true
    };

    const advancedOptions = {
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        const editor = ace.edit(editorElement, editorOptions);
        this.aceEditors.push(editor);
        editor.session.setUseWrapMode(false);
        editor.setValue(content, -1);
        this.updateEditorSize(editorElement, content, editor);
      }
    );
  }

  updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 200;
    let newWidth = 600;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 18 * lines.length + 16;
      let maxLineLength = 0;
      lines.forEach((row) => {
        const line = row.replace(/\t/g, '    ').replace(/\n/g, '');
        const lineLength = line.length;
        maxLineLength = Math.max(maxLineLength, lineLength);
      });
      newWidth = 9 * maxLineLength + 16;
    }
    // newHeight = Math.min(400, newHeight);
    this.renderer.setStyle(editorElement, 'minHeight', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
    editor.resize();
  }

}
