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

import { Component, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { FcNoteComponent } from 'ngx-flowchart';
import cssjs from '@core/css/css';
import { hashCode, isNotEmptyStr } from '@core/utils';
import tinycolor from 'tinycolor2';
import { FcRuleNote } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-rule-note',
    templateUrl: './rulenote.component.html',
    styleUrls: ['./rulenote.component.scss'],
    standalone: false
})
export class RuleNoteComponent extends FcNoteComponent implements OnInit, OnChanges {

  additionalStyles: string[];
  noteClass: string;
  note: FcRuleNote;

  ngOnInit(): void {
    super.ngOnInit();
    this.processCss();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.note) {
      this.processCss();
    }
  }

  borderColorFor(backgroundColor: string): string {
    return tinycolor(backgroundColor || '#FFF9C4').darken(20).toString();
  }

  private processCss(): void {
    let cssString = this.note?.markdownCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      this.noteClass = 'rule-note-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = this.noteClass;
      cssParser.testMode = false;
      const cssObjects = cssParser.applyNamespacing(cssString);
      cssString = cssParser.getCSSForEditor(cssObjects);
      this.additionalStyles = [cssString];
    } else {
      this.noteClass = undefined;
      this.additionalStyles = undefined;
    }
  }

  noteEdit(event: MouseEvent): void {
    event.stopPropagation();
    if (this.userNoteCallbacks?.noteEdit) {
      this.userNoteCallbacks.noteEdit(event, this.note);
    }
  }

  noteDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.modelservice.notes.delete(this.note);
  }
}
