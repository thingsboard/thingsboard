// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EditorOptions } from 'hugerte';
import { defaultHugeRteOptions } from '@shared/models/hugerte/hugerte.models';

@Component({
    selector: 'tb-release-notes-panel',
    templateUrl: './editor-panel.component.html',
    styleUrls: ['./editor-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditorPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  content: string;

  @Input()
  title: string;

  @Input()
  popover: TbPopoverComponent<EditorPanelComponent>;

  @Output()
  editorContentApplied = new EventEmitter<string>();

  editorControl: FormControl<string>;

  hugeRteOptions: Partial<EditorOptions> = defaultHugeRteOptions({
    plugins: ['link', 'table', 'image', 'lists', 'fullscreen'],
    menubar: 'edit insert view format',
    toolbar: ['fontfamily fontsize | bold italic underline strikethrough forecolor backcolor',
      'alignleft aligncenter alignright alignjustify | bullist | link table image | fullscreen'],
    toolbar_mode: 'sliding',
    height: 400,
    resize: false
  });

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.editorControl = this.fb.control(this.content);
    if (this.disabled) {
      this.editorControl.disable({emitEvent: false});
      this.hugeRteOptions.toolbar = false;
      this.hugeRteOptions.menubar = false;
      this.hugeRteOptions.statusbar = false;
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.editorControl.valid) {
      this.editorContentApplied.emit(this.editorControl.value);
    }
  }
}
