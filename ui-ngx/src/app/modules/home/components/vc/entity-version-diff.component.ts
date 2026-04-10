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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { getAceDiff } from '@shared/models/ace/ace.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { entityExportDataToJsonString, VersionLoadResult } from '@shared/models/vc.models';
import { Ace } from 'ace-builds';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityVersionRestoreComponent } from '@home/components/vc/entity-version-restore.component';

interface DiffInfo {
  leftStartLine: number;
  leftEndLine: number;
  rightStartLine: number;
  rightEndLine: number;
}

@Component({
    selector: 'tb-entity-version-diff',
    templateUrl: './entity-version-diff.component.html',
    styleUrls: ['./entity-version-diff.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityVersionDiffComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('diffViewer', {static: true})
  diffViewerElmRef: ElementRef<HTMLElement>;

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  entityId: EntityId;

  @Input()
  externalEntityId: EntityId;

  @Output()
  versionRestored = new EventEmitter<void>();

  @Input()
  onClose: () => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  differ: AceDiff;

  contentReady = false;

  preferredDiffHeight = '332px';

  isFullscreen = false;

  hasNext = false;
  hasPrevious = false;

  diffCount = 0;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private renderer: Renderer2,
              private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService) {
    super(store);
  }

  ngOnInit(): void {
    this.entitiesVersionControlService
      .compareEntityDataToVersion(this.entityId, this.versionId).subscribe((diffData) => {
      const leftContent = entityExportDataToJsonString(diffData.currentVersion);
      const rightContent = entityExportDataToJsonString(diffData.otherVersion);
      const leftLines = leftContent.split('\n').length;
      const rightLines = leftContent.split('\n').length;
      const totalLines = Math.max(leftLines, rightLines);
      let preferredLines = Math.max(10, totalLines);
      preferredLines = Math.min(40, preferredLines);
      this.preferredDiffHeight = (132 + preferredLines * 16) + 'px';
      getAceDiff().subscribe((aceDiff) => {
        this.contentReady = true;
        this.cd.detectChanges();
        if (this.popoverComponent) {
          this.popoverComponent.updatePosition();
        }
        setTimeout(() => {
          this.differ = new aceDiff(
            {
              element: this.diffViewerElmRef.nativeElement,
              mode: 'ace/mode/json',
              lockScrolling: false,
              left: {
                copyLinkEnabled: false,
                editable: false,
                content: leftContent
              },
              right: {
                copyLinkEnabled: false,
                editable: false,
                content: rightContent
              }
            } as AceDiff.AceDiffConstructorOpts
          );
          const leftEditor: Ace.Editor = this.differ.getEditors().left;
          const rightEditor: Ace.Editor = this.differ.getEditors().right;
          leftEditor.setShowFoldWidgets(false);
          rightEditor.setShowFoldWidgets(false);
          $('.acediff__left .ace_scrollbar-v', this.elementRef.nativeElement).on('scroll', () => {
            rightEditor.getSession().setScrollTop(leftEditor.getSession().getScrollTop());
          });
          $('.acediff__right .ace_scrollbar-v', this.elementRef.nativeElement).on('scroll', () => {
            leftEditor.getSession().setScrollTop(rightEditor.getSession().getScrollTop());
          });
          $('.acediff__left .ace_scrollbar-h', this.elementRef.nativeElement).on('scroll', () => {
            rightEditor.getSession().setScrollLeft(leftEditor.getSession().getScrollLeft());
          });
          $('.acediff__right .ace_scrollbar-h', this.elementRef.nativeElement).on('scroll', () => {
            leftEditor.getSession().setScrollLeft(rightEditor.getSession().getScrollLeft());
          });
          leftEditor.getSession().getSelection().on('changeCursor', () => this.updateHasNextAndPrevious());
          rightEditor.getSession().getSelection().on('changeCursor', () => this.updateHasNextAndPrevious());
          setTimeout(() => {
            this.diffCount = this.differ.getNumDiffs();
            this.updateHasNextAndPrevious();
          }, 2);
        });
      });
    });
  }

  versionIdContent(): string {
    let versionId = this.versionId;
    if (versionId.length > 7) {
      versionId = versionId.slice(0, 7);
    }
    return versionId + ' (' + this.versionName + ')';
  }

  prevDifference($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.moveToDiff(false);
  }

  nextDifference($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.moveToDiff(true);
  }

  private moveToDiff(next: boolean) {
    const currentRow = this.getCurrentRow();
    const diff = next ? this.findNextLine(currentRow) : this.findPrevLine(currentRow);
    if (diff) {
      const leftEditor: Ace.Editor = this.differ.getEditors().left;
      const rightEditor: Ace.Editor = this.differ.getEditors().right;
      leftEditor.scrollToLine(diff.leftStartLine + 1, true, true, () => {});
      leftEditor.gotoLine(diff.leftStartLine + 1, 0, true);
      rightEditor.scrollToLine(diff.rightStartLine + 1, true, true, () => {});
      rightEditor.gotoLine(diff.rightStartLine + 1, 0, true);
    }
  }

  onFullscreenChanged(fullscreen: boolean) {
    if (fullscreen) {
      this.resizeEditors();
    } else {
      setTimeout(() => {
        this.resizeEditors();
      });
    }
  }

  private getDiffs(): DiffInfo[] {
    if (this.differ) {
      // @ts-ignore
      return this.differ.diffs as DiffInfo[] || [];
    } else {
      return [];
    }
  }

  private getCurrentRow(): {row: number, left: boolean} {
    const leftEditor: Ace.Editor = this.differ.getEditors().left;
    const rightEditor: Ace.Editor = this.differ.getEditors().right;
    let currentRow = 0;
    let left = true;
    const leftRow = leftEditor.getSession().getSelection().getCursor().row;
    const rightRow = rightEditor.getSession().getSelection().getCursor().row;
    if (leftRow >= leftEditor.getFirstVisibleRow() && leftRow <= leftEditor.getLastVisibleRow()) {
      currentRow = leftRow;
    } else if (rightRow >= rightEditor.getFirstVisibleRow() && rightRow <= rightEditor.getLastVisibleRow()) {
      currentRow = rightRow;
      left = false;
    } else {
      currentRow = leftRow;
    }
    return {row: currentRow, left};
  }

  private nextDiff(currentLine: {row: number, left: boolean}): DiffInfo | undefined {
    const diffs = this.getDiffs();
    return diffs.find((diff) => (currentLine.left ? diff.leftStartLine : diff.rightStartLine) > currentLine.row);
  }

  private prevDiff(currentLine: {row: number, left: boolean}): DiffInfo | undefined {
    const diffs = this.getDiffs();
    return [...diffs].reverse().find((diff) => (currentLine.left ? diff.leftEndLine : diff.rightEndLine) < currentLine.row);
  }

  private findNextLine(currentLine: {row: number, left: boolean}): DiffInfo | undefined {
    let res = this.nextDiff(currentLine);
    const diffs = this.getDiffs();
    if (!res && diffs.length) {
      res = diffs[diffs.length - 1];
    }
    return res;
  }

  private findPrevLine(currentLine: {row: number, left: boolean}): DiffInfo | undefined {
    let res = this.prevDiff(currentLine);
    const diffs = this.getDiffs();
    if (!res && diffs.length) {
      res = diffs[0];
    }
    return res;
  }

  private updateHasNextAndPrevious() {
    const currentRow = this.getCurrentRow();
    this.hasNext = !!this.nextDiff(currentRow);
    this.hasPrevious = !!this.prevDiff(currentRow);
    this.cd.markForCheck();
  }

  private resizeEditors() {
    if (this.differ) {
      this.differ.diff();
      const leftEditor: Ace.Editor = this.differ.getEditors().left;
      const rightEditor: Ace.Editor = this.differ.getEditors().right;
      leftEditor.resize();
      leftEditor.renderer.updateFull();
      rightEditor.resize();
      rightEditor.renderer.updateFull();
    }
  }

  ngOnDestroy(): void {
    if (this.differ) {
      this.differ.destroy();
      this.differ = null;
    }
  }

  close(): void {
    if (this.popoverComponent) {
      this.popoverComponent.hide();
    }
  }

  toggleRestoreEntityVersion($event: Event, restoreVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = restoreVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const restoreVersionPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EntityVersionRestoreComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          versionName: this.versionName,
          versionId: this.versionId,
          externalEntityId: this.externalEntityId,
          onClose: (result: VersionLoadResult | null) => {
            restoreVersionPopover.hide();
            if (result && !result.error && result.result.length) {
              this.close();
              this.versionRestored.emit();
            }
          }
        },
        showCloseButton: false,
        isModal: true
      });
      restoreVersionPopover.tbComponentRef.instance.popoverComponent = restoreVersionPopover;
    }
  }
}
