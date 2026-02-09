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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';

@Component({
    selector: 'tb-markdown-editor',
    templateUrl: './markdown-editor.component.html',
    styleUrls: ['./markdown-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MarkdownEditorComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MarkdownEditorComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() readonly: boolean;

  @Input() helpId: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @ViewChild('markdownEditor', {static: true})
  markdownEditorElmRef: ElementRef;

  editorMode = true;

  fullscreen = false;

  markdownValue: string;
  renderValue: string;

  private markdownEditor: Ace.Editor;
  private ignoreChange = false;

  private editorResize$: ResizeObserver;
  private editorsResizeCaf: CancelAnimationFrame;
  private propagateChange: (value: any) => void = () => {};

  constructor(private cd: ChangeDetectorRef,
              private raf: RafService) {
  }

  ngOnInit(): void {
    if (!this.readonly) {
      const editorElement = this.markdownEditorElmRef.nativeElement;
      let editorOptions: Partial<Ace.EditorOptions> = {
        mode: 'ace/mode/markdown',
        showGutter: true,
        showPrintMargin: false,
        readOnly: false
      };

      const advancedOptions = {
        enableSnippets: true,
        enableBasicAutocompletion: true,
        enableLiveAutocompletion: true
      };

      editorOptions = {...editorOptions, ...advancedOptions};

      getAce().subscribe(
        (ace) => {
          this.markdownEditor = ace.edit(editorElement, editorOptions);
          this.markdownEditor.session.setUseWrapMode(false);
          this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
          this.markdownEditor.on('change', () => {
            if (!this.ignoreChange) {
              this.updateView();
            }
          });
          this.editorResize$ = new ResizeObserver(() => {
            this.onAceEditorResize();
          });
          this.editorResize$.observe(editorElement);
        }
      );

    }
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    if (this.markdownEditor) {
      this.markdownEditor.destroy();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string): void {
    this.editorMode = true;
    this.markdownValue = value;
    this.renderValue = this.markdownValue ? this.markdownValue : ' ';
    if (this.markdownEditor) {
      this.ignoreChange = true;
      this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
      this.ignoreChange = false;
    }
  }

  onFullscreen() {
    if (this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }

  toggleEditMode() {
    this.editorMode = !this.editorMode;
    if (this.editorMode && this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }

  private updateView() {
    const editorValue = this.markdownEditor.getValue();
    if (this.markdownValue !== editorValue) {
      this.markdownValue = editorValue;
      this.renderValue = this.markdownValue ? this.markdownValue : ' ';
      this.propagateChange(this.markdownValue);
      this.cd.markForCheck();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.markdownEditor.resize();
      this.markdownEditor.renderer.updateFull();
    });
  }
}
