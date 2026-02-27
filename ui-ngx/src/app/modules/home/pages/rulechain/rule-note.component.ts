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

import { Component, inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { FcRuleNote } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-rule-note',
    templateUrl: './rule-note.component.html',
    standalone: false
})
export class RuleNoteComponent implements OnChanges {

  @Input()
  note: FcRuleNote;

  private fb = inject(FormBuilder)

  noteForm = this.fb.group({
    content: [''],
    backgroundColor: ['#FFF9C4'],
    applyDefaultMarkdownStyle: [true],
    markdownCss: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.note) {
      this.updatedForm();
    }
  }

  private updatedForm(): void {
    this.noteForm.setValue({
      content: this.note?.content || '',
      backgroundColor: this.note?.backgroundColor || '#FFF9C4',
      applyDefaultMarkdownStyle: this.note?.applyDefaultMarkdownStyle ?? true,
      markdownCss: this.note?.markdownCss || ''
    })
    setTimeout(() => {
      this.noteForm.markAsPristine();
    }, 0);
  }
}
