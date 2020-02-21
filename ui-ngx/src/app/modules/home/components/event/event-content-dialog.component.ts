///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, ElementRef, Inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

import * as ace from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ContentType, contentTypesMap } from '@shared/models/constants';

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
export class EventContentDialogComponent extends DialogComponent<EventContentDialogData> implements OnInit {

  @ViewChild('eventContentEditor', {static: true})
  eventContentEditorElmRef: ElementRef;
  private eventContentEditor: ace.Ace.Editor;

  content: string;
  title: string;
  contentType: ContentType;

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

    this.eventContentEditor = this.createEditor(this.eventContentEditorElmRef, this.content);
  }

  createEditor(editorElementRef: ElementRef, content: string): ace.Ace.Editor {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
      if (this.contentType === ContentType.JSON && content) {
        content = js_beautify(content, {indent_size: 4});
      }
    }
    let editorOptions: Partial<ace.Ace.EditorOptions> = {
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
    const editor = ace.edit(editorElement, editorOptions);
    editor.session.setUseWrapMode(false);
    editor.setValue(content, -1);
    this.updateEditorSize(editorElement, content, editor);
    return editor;
  }

  updateEditorSize(editorElement: any, content: string, editor: ace.Ace.Editor) {
    let newHeight = 400;
    let newWidth = 600;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 16 * lines.length + 16;
      let maxLineLength = 0;
      lines.forEach((row) => {
        const line = row.replace(/\t/g, '    ').replace(/\n/g, '');
        const lineLength = line.length;
        maxLineLength = Math.max(maxLineLength, lineLength);
      });
      newWidth = 8 * maxLineLength + 16;
    }
    // newHeight = Math.min(400, newHeight);
    this.renderer.setStyle(editorElement, 'minHeight', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
    editor.resize();
  }

}
