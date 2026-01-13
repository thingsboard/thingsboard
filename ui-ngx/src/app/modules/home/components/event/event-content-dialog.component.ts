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

import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { getAce } from '@shared/models/ace/ace.models';
import { Observable } from 'rxjs/internal/Observable';
import { beautifyJs } from '@shared/models/beautify.models';
import { of } from 'rxjs';
import { base64toString, isLiteralObject } from '@core/utils';

export interface EventContentDialogData {
  content: string;
  title: string;
  contentType: ContentType;
}

@Component({
  selector: 'tb-event-content-dialog',
  templateUrl: './event-content-dialog.component.html',
  styleUrls: ['./event-content-dialog.component.scss']
})
export class EventContentDialogComponent extends DialogComponent<EventContentDialogData> implements OnInit, OnDestroy {

  @ViewChild('eventContentEditor', {static: true})
  eventContentEditorElmRef: ElementRef;

  content: string;
  title: string;
  contentType: ContentType;
  aceEditor: Ace.Editor;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EventContentDialogData,
              public dialogRef: MatDialogRef<EventContentDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.content = this.data.content;
    this.title = this.data.title;
    this.contentType = this.data.contentType;

    this.createEditor(this.eventContentEditorElmRef, this.content);
  }

  ngOnDestroy(): void {
    if (this.aceEditor) {
      this.aceEditor.destroy();
    }
    super.ngOnDestroy();
  }

  isJson(str) {
    try {
      return isLiteralObject(JSON.parse(str));
    } catch (e) {
      return false;
    }
  }

  createEditor(editorElementRef: ElementRef, content: string) {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    let content$: Observable<string> = null;
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
      if (this.contentType === ContentType.JSON && content) {
        content$ = beautifyJs(content, {indent_size: 4});
      } else if (this.contentType === ContentType.BINARY && content) {
        try {
          const decodedData = base64toString(content);
          if (this.isJson(decodedData)) {
            mode = 'json';
            content$ = beautifyJs(decodedData, {indent_size: 4});
          } else {
            content$ = of(decodedData);
          }
        } catch (e) {}
      }
    }
    if (!content$) {
      content$ = of(content);
    }

    content$.subscribe(
      (processedContent) => {
        let editorOptions: Partial<Ace.EditorOptions> = {
          mode: `ace/mode/${mode}`,
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
            this.aceEditor = ace.edit(editorElement, editorOptions);
            this.aceEditor.session.setUseWrapMode(false);
            this.aceEditor.setValue(processedContent, -1);
            this.updateEditorSize(editorElement, processedContent, this.aceEditor);
          }
        );
      }
    );
  }

  updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 400;
    let newWidth = 600;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 17 * lines.length + 16;
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
