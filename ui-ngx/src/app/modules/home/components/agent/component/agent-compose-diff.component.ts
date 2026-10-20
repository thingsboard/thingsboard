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
import { getAceDiff } from '@shared/models/ace/ace.models';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';

@Component({
  selector: 'tb-agent-compose-diff',
  template: '<div class="diff-viewer" [class.readonly]="readOnly" #diffViewer></div>',
  styleUrls: ['./agent-compose-diff.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class AgentComposeDiffComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() left = '';
  @Input() right = '';
  @Input() readOnly = false;
  @Input() syncScroll = false;

  @Output() rightChange = new EventEmitter<string>();

  @ViewChild('diffViewer', { static: true }) elmRef: ElementRef<HTMLElement>;

  private differ: any = null;
  private resizeObserver: ResizeObserver | null = null;
  private building = false;
  private ready = false;
  private destroyed = false;
  private settling = false;
  private settleFrames = 0;
  private gutterRedrawHandle: number | null = null;
  private rebuildHandle: number | null = null;

  ngAfterViewInit(): void {
    this.observeResize();
    this.build();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.differ) {
      this.build();
      return;
    }
    if (changes.readOnly) {
      this.build();
      return;
    }
    if (changes.left || changes.right) {
      this.scheduleRebuild();
    }
    if (changes.syncScroll && this.syncScroll) {
      this.alignScroll();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.ready = false;
    if (this.gutterRedrawHandle != null) {
      cancelAnimationFrame(this.gutterRedrawHandle);
      this.gutterRedrawHandle = null;
    }
    if (this.rebuildHandle != null) {
      cancelAnimationFrame(this.rebuildHandle);
      this.rebuildHandle = null;
    }
    if (this.resizeObserver) {
      try { this.resizeObserver.disconnect(); } catch (e) { /* no-op */ }
      this.resizeObserver = null;
    }
    if (this.differ) {
      try { this.differ.destroy(); } catch (e) { /* no-op */ }
      this.differ = null;
    }
  }

  private observeResize(): void {
    const host = this.elmRef?.nativeElement;
    if (!host || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.resizeObserver = new ResizeObserver(() => {
      if (this.destroyed) {
        return;
      }
      if (this.differ) {
        this.resizeEditors();
      } else {
        this.build();
      }
    });
    this.resizeObserver.observe(host);
  }

  private resizeEditors(): void {
    const eds = this.differ?.getEditors?.();
    if (!eds?.left || !eds?.right) {
      return;
    }
    eds.left.resize(true);
    eds.right.resize(true);
    this.realign(eds.left);
  }

  private build(): void {
    const host = this.elmRef?.nativeElement;
    if (!host || this.building || this.destroyed) {
      return;
    }
    if (!host.offsetWidth || !host.offsetHeight) {
      return;
    }
    // The diff is hosted in a MatDialog whose enter animation is a CSS transform
    // (scale). On a reopen the ace-diff module is cached, so build() runs
    // synchronously mid-animation; ace then measures against the scaled box and
    // renders a broken gutter/alignment that never recovers (a transform never
    // refires the ResizeObserver). Wait until the box settles — getBoundingClientRect
    // matches the unscaled offset size — then build exactly once.
    const rect = host.getBoundingClientRect();
    const settled = Math.abs(rect.width - host.offsetWidth) <= 1 && Math.abs(rect.height - host.offsetHeight) <= 1;
    if (!settled) {
      if (!this.settling && this.settleFrames++ < 60) {
        this.settling = true;
        requestAnimationFrame(() => { this.settling = false; this.build(); });
      }
      return;
    }
    this.settleFrames = 0;
    this.building = true;
    this.ready = false;
    if (this.differ) {
      try { this.differ.destroy(); } catch (e) { /* no-op */ }
      this.differ = null;
    }
    host.innerHTML = '';
    getAceDiff().subscribe((AceDiffCtor) => {
      this.building = false;
      const el = this.elmRef?.nativeElement;
      if (!el || this.destroyed) {
        return;
      }
      this.differ = new AceDiffCtor({
        element: el,
        mode: 'ace/mode/text',
        lockScrolling: false,
        // AceDiff wires up the gutter SVG, the copy-arrow containers and the
        // measured lineHeight in a deferred setTimeout, then calls this once
        // its first diff() completes. Any diff()/realign() we fire before this
        // throws in addCopyArrows and draws connectors with a NaN lineHeight —
        // the blank-gutter/"stuck" render. Gate our own realigns on it.
        onDiffReady: () => { this.ready = true; },
        left: {
          copyLinkEnabled: !this.readOnly,
          editable: false,
          content: this.left
        },
        right: {
          copyLinkEnabled: false,
          editable: !this.readOnly,
          content: this.right
        }
      });
      const leftEditor: Ace.Editor = this.differ.getEditors().left;
      const rightEditor: Ace.Editor = this.differ.getEditors().right;
      leftEditor.setShowFoldWidgets(false);
      rightEditor.setShowFoldWidgets(false);
      leftEditor.getSession().setMode('ace/mode/yaml');
      rightEditor.getSession().setMode('ace/mode/yaml');
      leftEditor.setOption('scrollPastEnd', 0);
      rightEditor.setOption('scrollPastEnd', 0);
      leftEditor.renderer.setScrollMargin(0, 0, 0, 0);
      rightEditor.renderer.setScrollMargin(0, 0, 0, 0);
      forceAceFontSize(leftEditor, 12);
      forceAceFontSize(rightEditor, 12);
      confineWheelToAceEditor(leftEditor.container, leftEditor);
      confineWheelToAceEditor(rightEditor.container, rightEditor);
      confineWheelToAceEditor(host, rightEditor, () => true, false);
      if (!this.readOnly) {
        rightEditor.getSession().on('change', () => {
          this.rightChange.emit(rightEditor.getValue());
          this.realign(leftEditor);
        });
      }
      this.bindScrollSync(leftEditor, rightEditor);
      // forceAceFontSize() changes lineHeight after construction, so the diff
      // AceDiff computed in its deferred init is aligned to the wrong height.
      // Re-resize + realign, but only once AceDiff has finished that init.
      this.whenReady(() => this.resizeEditors());
    });
  }

  // Runs cb once AceDiff has fired onDiffReady (gutter + copy containers exist,
  // lineHeight is measured). Polls a bounded number of frames so a diff that
  // never readies (e.g. host torn down) can't spin forever.
  private whenReady(cb: () => void, frames = 60): void {
    if (this.destroyed || !this.differ) {
      return;
    }
    if (this.ready) {
      cb();
      return;
    }
    if (frames <= 0) {
      return;
    }
    requestAnimationFrame(() => this.whenReady(cb, frames - 1));
  }

  private scheduleRebuild(): void {
    if (this.rebuildHandle != null || this.destroyed) {
      return;
    }
    this.rebuildHandle = requestAnimationFrame(() => {
      this.rebuildHandle = null;
      if (!this.destroyed) {
        this.build();
      }
    });
  }

  private realign(editor: Ace.Editor): void {
    if (!this.differ || !this.ready) {
      return;
    }
    const lineHeight = editor?.renderer?.lineHeight;
    if (lineHeight) {
      this.differ.lineHeight = lineHeight;
    }
    this.differ.diff();
  }

  private alignScroll(): void {
    const eds = this.differ?.getEditors?.();
    const leftSession = eds?.left?.getSession?.();
    const rightSession = eds?.right?.getSession?.();
    if (!leftSession || !rightSession) {
      return;
    }
    rightSession.setScrollTop(leftSession.getScrollTop());
    rightSession.setScrollLeft(leftSession.getScrollLeft());
  }

  private bindScrollSync(leftEditor: Ace.Editor, rightEditor: Ace.Editor): void {
    const leftSession = leftEditor.getSession();
    const rightSession = rightEditor.getSession();
    let syncing = false;
    const link = (a: Ace.EditSession, b: Ace.EditSession) => {
      a.on('changeScrollTop', () => {
        this.scheduleGutterRedraw();
        if (!this.syncScroll || syncing) { return; }
        const next = a.getScrollTop();
        if (b.getScrollTop() === next) { return; }
        syncing = true;
        b.setScrollTop(next);
        syncing = false;
      });
      a.on('changeScrollLeft', () => {
        this.scheduleGutterRedraw();
        if (!this.syncScroll || syncing) { return; }
        const next = a.getScrollLeft();
        if (b.getScrollLeft() === next) { return; }
        syncing = true;
        b.setScrollLeft(next);
        syncing = false;
      });
    };
    link(leftSession, rightSession);
    link(rightSession, leftSession);
  }

  // ace-diff redraws the center connectors via a 16ms setTimeout throttle that
  // runs off the render frame, so the curve visibly lags / freezes during a
  // scroll until it settles. Coalesce a redraw into the next animation frame so
  // the connectors repaint in lockstep with the editors' own scroll render.
  private scheduleGutterRedraw(): void {
    if (this.gutterRedrawHandle != null || !this.differ || !this.ready || this.destroyed) {
      return;
    }
    this.gutterRedrawHandle = requestAnimationFrame(() => {
      this.gutterRedrawHandle = null;
      if (this.differ && this.ready && !this.destroyed) {
        try { this.differ.diff(); } catch (e) { /* no-op */ }
      }
    });
  }
}
