///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
  selector: 'tb-release-notes-panel',
  templateUrl: './release-notes-panel.component.html',
  styleUrls: ['./release-notes-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReleaseNotesPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  releaseNotes: string;

  @Input()
  isLatest: boolean;

  @Input()
  popover: TbPopoverComponent<ReleaseNotesPanelComponent>;

  @Output()
  releaseNotesApplied = new EventEmitter<string>();

  title: string;

  releaseNotesControl: FormControl<string>;

  tinyMceOptions: Record<string, any> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['lists'],
    menubar: 'edit insert view format',
    toolbar: ['fontfamily fontsize | bold italic underline strikethrough forecolor backcolor',
      'alignleft aligncenter alignright alignjustify | bullist'],
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false
  };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.releaseNotesControl = this.fb.control(this.releaseNotes);
    this.title = this.isLatest ? 'mobile.latest-version-release-notes' : 'mobile.min-version-release-notes';
    if (this.disabled) {
      this.releaseNotesControl.disable({emitEvent: false});
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.releaseNotesControl.valid) {
      this.releaseNotesApplied.emit(this.releaseNotesControl.value);
    }
  }
}
