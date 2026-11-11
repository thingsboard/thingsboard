// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyCss } from '@shared/models/beautify.models';

export interface CustomCssDialogData {
  customCss: string;
  readonly: boolean;
}

@Component({
    selector: 'tb-custom-css-dialog',
    templateUrl: './custom-css-dialog.component.html',
    styleUrls: ['./custom-css-dialog.component.scss'],
    standalone: false
})
export class CustomCssDialogComponent extends DialogComponent<CustomCssDialogComponent, string> implements OnInit, OnDestroy {

  @ViewChild('cssEditor', {static: true})
  cssEditorElmRef: ElementRef;

  private cssEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;

  customCss: string;
  readonly: boolean;

  fullscreen = false;

  isDirty = false;
  valid = true;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CustomCssDialogData,
              public dialogRef: MatDialogRef<CustomCssDialogComponent, string>,
              private raf: RafService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.customCss = this.data.customCss;
    this.readonly = this.data.readonly;

    const editorElement = this.cssEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/css`,
      showGutter: true,
      showPrintMargin: true,
      readOnly: this.readonly,
      enableMobileMenu: false,
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};

    getAce().subscribe(
      (ace) => {
        this.cssEditor = ace.edit(editorElement, editorOptions);
        this.cssEditor.session.setUseWrapMode(true);
        this.cssEditor.setValue(this.customCss ? this.customCss : '', -1);
        this.cssEditor.on('change', () => {
          this.updateValue();
        });
        // @ts-ignore
        this.cssEditor.session.on('changeAnnotation', () => {
          this.validate();
        });
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.cssEditor) {
      this.cssEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.cssEditor.resize();
      this.cssEditor.renderer.updateFull();
    });
  }

  private updateValue() {
    const editorValue = this.cssEditor.getValue();
    if (this.customCss !== editorValue) {
      this.customCss = editorValue;
      this.isDirty = true;
    }
  }

  private validate() {
    const annotations = this.cssEditor.session.getAnnotations();
    if (annotations) {
      this.valid = annotations.filter((annotation) => annotation.type === 'error').length === 0;
    } else {
      this.valid = true;
    }
  }

  beautifyCss(): void {
    beautifyCss(this.customCss, {indent_size: 4}).subscribe(
      (res) => {
        if (this.customCss !== res) {
          this.isDirty = true;
          this.customCss = res;
          this.cssEditor.setValue(this.customCss ? this.customCss : '', -1);
        }
      }
    );
  }

  onFullscreen() {
    if (this.cssEditor) {
      setTimeout(() => {
        this.cssEditor.resize();
      }, 0);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (this.valid) {
      this.isDirty = false;
      this.dialogRef.close(this.customCss);
    }
  }
}
