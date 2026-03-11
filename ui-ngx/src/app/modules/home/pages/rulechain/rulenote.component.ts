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
import {
  FC_RULE_NOTE_DEFAULT_APPLY_MARKDOWN_STYLE,
  FC_RULE_NOTE_DEFAULT_BACKGROUND_COLOR,
  FcRuleNote
} from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-rule-note',
    templateUrl: './rulenote.component.html',
    styleUrls: ['./rulenote.component.scss'],
    standalone: false
})
export class RuleNoteComponent extends FcNoteComponent implements OnInit, OnChanges {

  readonly defaultBackgroundColor = FC_RULE_NOTE_DEFAULT_BACKGROUND_COLOR;

  private static readonly HEADING_STYLE_OVERRIDE = '.tb-markdown-view h1 { font-size: 3rem; }';
  private static readonly PADDING_STYLE_OVERRIDE = '.tb-markdown-view p, .tb-markdown-view h1, .tb-markdown-view h2, .tb-markdown-view h3, .tb-markdown-view h4, .tb-markdown-view h5, .tb-markdown-view h6 {padding-left: 0 !important;}';

  applyDefault = FC_RULE_NOTE_DEFAULT_APPLY_MARKDOWN_STYLE;
  additionalStyles: string[];
  noteClass: string;
  note: FcRuleNote;

  ngOnInit(): void {
    super.ngOnInit();
    this.applyDefault = this.note?.applyDefaultMarkdownStyle ?? FC_RULE_NOTE_DEFAULT_APPLY_MARKDOWN_STYLE;
    this.processCss();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.note) {
      this.applyDefault = this.note?.applyDefaultMarkdownStyle ?? FC_RULE_NOTE_DEFAULT_APPLY_MARKDOWN_STYLE;
      this.processCss();
    }
  }

  private processCss(): void {
    let cssString = this.note?.markdownCss;
    const styles: string[] = [];
    if (this.applyDefault) {
      styles.push(RuleNoteComponent.HEADING_STYLE_OVERRIDE);
      styles.push(RuleNoteComponent.PADDING_STYLE_OVERRIDE);
    }
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      this.noteClass = 'rule-note-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = this.noteClass;
      cssParser.testMode = false;
      const cssObjects = cssParser.applyNamespacing(cssString);
      cssString = cssParser.getCSSForEditor(cssObjects);
      styles.push(cssString);
    } else {
      this.noteClass = undefined;
    }
    this.additionalStyles = styles.length ? styles : undefined;
  }

  noteEdit(event: MouseEvent): void {
    event.stopPropagation();
    this.userNoteCallbacks?.noteEdit?.(event, this.note);
  }

  noteDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.modelservice.notes.delete(this.note);
  }
}
