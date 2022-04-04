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

import { Directive, ElementRef, OnDestroy } from '@angular/core';
import { ResizeObserver } from '@juggle/resize-observer';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { Ace } from 'ace-builds';
import { guid } from '@core/utils';
import { AceMode, AceTheme, getAce } from '@shared/models/ace/ace.models';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class EditorAce implements OnDestroy {

  private editorResize$: ResizeObserver;
  private editorsResizeCaf: CancelAnimationFrame;

  protected editor: Ace.Editor;
  protected ignoreChange = false;

  toastTargetId = `contentEditor-${guid()}`;
  modelValue: string;

  abstract editorElementRef: ElementRef;
  abstract disabled: boolean;
  abstract readonly: boolean;

  abstract updateView();

  protected setAdvancedEditorSetting() { }
  protected cleanupErrors() { }

  protected constructor(
    protected raf: RafService
  ) {
  }

  ngOnDestroy() {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
  }

  initEditor(mode: AceMode|AceMode[], theme: AceTheme|AceTheme[], settings: Partial<Ace.EditorOptions>,
             wrapMode = false, value?: any) {
    const editorElement = this.editorElementRef.nativeElement;
    getAce(mode, theme).subscribe(
      (ace) => {
        this.editor = ace.edit(editorElement, settings);
        this.editor.session.setUseWrapMode(wrapMode);
        this.editor.setValue(value ? value : '', -1);
        this.editor.setReadOnly(settings.readOnly);
        this.editor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupErrors();
            this.updateView();
          }
        });
        this.setAdvancedEditorSetting();
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.editor.resize();
      this.editor.renderer.updateFull();
    });
  }

  setValue(value: any) {
    this.modelValue = value;
    if (this.editor) {
      this.ignoreChange = true;
      this.editor.setValue(value ? value : '', -1);
      this.ignoreChange = false;
    }
  }

  setDisabled(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.editor) {
      this.editor.setReadOnly(isDisabled || this.readonly);
    }
  }

  validateOnSubmit() { }

}
