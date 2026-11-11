// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { forceAceFontSize } from '@home/components/agent/component/ace-editor-font';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';

@Component({
  selector: 'tb-agent-compose-editor',
  template: '<div class="yaml-editor yaml-editor-ace" #host></div>',
  styleUrls: ['./agent-compose-editor.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class AgentComposeEditorComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() value = '';
  @Input() readOnly = false;

  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('host', { static: true }) hostRef: ElementRef<HTMLElement>;

  private editor: Ace.Editor | null = null;
  private settingValue = false;
  private destroyed = false;

  ngAfterViewInit(): void {
    getAce().subscribe((ace) => {
      if (this.destroyed) {
        return;
      }
      const editor: Ace.Editor = ace.edit(this.hostRef.nativeElement);
      editor.setTheme('ace/theme/textmate');
      editor.session.setMode('ace/mode/yaml');
      editor.session.setUseWrapMode(false);
      editor.setShowPrintMargin(false);
      editor.setOption('scrollPastEnd', 0);
      editor.renderer.setScrollMargin(0, 0, 0, 0);
      forceAceFontSize(editor, 12);
      editor.setOption('tabSize', 2);
      editor.setOption('useSoftTabs', true);
      editor.setOption('showLineNumbers', true);
      editor.setOption('highlightActiveLine', false);
      editor.setValue(this.value || '', -1);
      editor.getSession().on('change', () => {
        this.settingValue = true;
        this.valueChange.emit(editor.getValue());
        this.settingValue = false;
      });
      this.editor = editor;
      this.applyReadOnly();
      confineWheelToAceEditor(editor.container, editor,
        () => editor.isFocused());
      setTimeout(() => editor.resize(true), 0);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.editor) {
      return;
    }
    if (changes.value && !this.settingValue) {
      const current = this.editor.getValue();
      if (current !== (this.value || '')) {
        this.editor.setValue(this.value || '', -1);
      }
    }
    if (changes.readOnly) {
      this.applyReadOnly();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.editor) {
      try { this.editor.destroy(); } catch (e) { /* no-op */ }
      this.editor = null;
    }
  }

  private applyReadOnly(): void {
    if (!this.editor) {
      return;
    }
    this.editor.setReadOnly(this.readOnly);
    const cursorLayer = (this.editor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = this.readOnly ? 'none' : '';
    }
  }
}
