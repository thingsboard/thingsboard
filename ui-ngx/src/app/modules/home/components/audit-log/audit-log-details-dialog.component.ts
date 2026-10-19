// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ElementRef, Inject, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionStatus, AuditLog } from '@shared/models/audit-log.models';
import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { Observable, of } from 'rxjs';
import { isObject } from '@core/utils';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { beautifyJs } from '@shared/models/beautify.models';

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

  displayFailureDetails: boolean;

  private auditLog: AuditLog;
  private aceEditors: Ace.Editor[] = [];

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

    this.createEditor(this.actionDataEditorElmRef, this.auditLog.actionData);
    if (this.displayFailureDetails) {
      this.createEditor(this.failureDetailsEditorElmRef, this.auditLog.actionFailureDetails);
    }
  }

  ngOnDestroy(): void {
    this.aceEditors.forEach(editor => editor.destroy());
    super.ngOnDestroy();
  }

  createEditor(editorElementRef: ElementRef, content: string | object): void {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    let content$: Observable<string> = null;
    let contentType = ContentType.TEXT;
    if (content && isObject(content)) {
      contentType = ContentType.JSON;
      mode = contentTypesMap.get(contentType).code;
      content$ = beautifyJs(JSON.stringify(content), {indent_size: 2});
    }
    if (!content$) {
      content$ = of(content as string);
    }
    content$.subscribe((processedContent) => {
      const isJSON = contentType === ContentType.JSON
      const editorOptions: Partial<Ace.EditorOptions> = {
        mode: `ace/mode/${mode}`,
        theme: 'ace/theme/github',
        showGutter: isJSON,
        showFoldWidgets: true,
        foldStyle: 'markbeginend',
        showPrintMargin: false,
        readOnly: true,
        enableSnippets: false,
        enableBasicAutocompletion: false,
        enableLiveAutocompletion: false,
      };
      getAce().subscribe(
        (ace) => {
          const editor = ace.edit(editorElement, editorOptions);
          this.aceEditors.push(editor);
          editor.session.setUseWrapMode(false);
          editor.setValue(processedContent, -1);
          updateEditorSize(editorElement, processedContent, editor, this.renderer, {showGutter: isJSON});
        }
      )
    });
  }

}
