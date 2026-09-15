// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, inject, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatExpansionPanel } from '@angular/material/expansion';
import { FormBuilder, Validators } from '@angular/forms';
import {
  FC_RULE_NOTE_DEFAULT_APPLY_MARKDOWN_STYLE as DEFAULT_APPLY_MARKDOWN_STYLE,
  FC_RULE_NOTE_DEFAULT_BACKGROUND_COLOR as DEFAULT_BACKGROUND_COLOR,
  FC_RULE_NOTE_DEFAULT_BORDER_WIDTH as DEFAULT_BORDER_WIDTH,
  FcRuleNote
} from '@shared/models/rule-node.models';
import { isNotEmptyStr } from '@core/utils';
import tinycolor from 'tinycolor2';

@Component({
  selector: 'tb-rule-note-editor',
  templateUrl: './rule-note-editor.component.html',
  styleUrls: ['./rule-note-editor.component.scss'],
  standalone: false
})
export class RuleNoteEditorComponent implements OnChanges, OnInit {

  @Input()
  note: FcRuleNote;

  @ViewChild('advancedPanel') advancedPanel: MatExpansionPanel;

  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  noteForm = this.fb.group({
    content: ['', Validators.maxLength(65536)],
    backgroundColor: [DEFAULT_BACKGROUND_COLOR],
    borderColor: [tinycolor(DEFAULT_BACKGROUND_COLOR).darken(20).toString()],
    borderWidth: [DEFAULT_BORDER_WIDTH, [Validators.min(0)]],
    applyDefaultMarkdownStyle: [DEFAULT_APPLY_MARKDOWN_STYLE],
    markdownCss: ['', Validators.maxLength(65536)]
  });

  ngOnInit(): void {
    this.noteForm.get('backgroundColor').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(color => {
        const borderControl = this.noteForm.get('borderColor');
        if (borderControl.pristine) {
          borderControl.setValue(this.borderColorFrom(color), { emitEvent: false });
        }
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.note) {
      this.updatedForm();
    }
  }

  private borderColorFrom(bg: string): string {
    const color = tinycolor(bg || DEFAULT_BACKGROUND_COLOR);
    return color.isDark() ? color.lighten(20).toString() : color.darken(20).toString();
  }

  private updatedForm(): void {
    const bg = this.note?.backgroundColor || DEFAULT_BACKGROUND_COLOR;
    this.noteForm.setValue({
      content: this.note?.content || '',
      backgroundColor: bg,
      borderColor: this.note?.borderColor || this.borderColorFrom(bg),
      borderWidth: this.note?.borderWidth ?? DEFAULT_BORDER_WIDTH,
      applyDefaultMarkdownStyle: this.note?.applyDefaultMarkdownStyle ?? DEFAULT_APPLY_MARKDOWN_STYLE,
      markdownCss: this.note?.markdownCss || ''
    });
    const shouldExpand = this.hasNonDefaultAdvancedSettings(bg);
    setTimeout(() => {
      if (shouldExpand) {
        this.advancedPanel?.open();
      } else {
        this.advancedPanel?.close();
      }
      this.noteForm.markAsPristine();
    }, 0);
  }

  private hasNonDefaultAdvancedSettings(bg: string): boolean {
    const note = this.note;
    if (!note) return false;
    return (note.borderColor != null && note.borderColor !== this.borderColorFrom(bg))
      || (note.borderWidth != null && note.borderWidth !== DEFAULT_BORDER_WIDTH)
      || note.applyDefaultMarkdownStyle === false
      || isNotEmptyStr(note.markdownCss);
  }
}
