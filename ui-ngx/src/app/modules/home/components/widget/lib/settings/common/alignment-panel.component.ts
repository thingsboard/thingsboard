// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import {
  alignment,
  alignmentIcons,
  alignmentTranslations,
  horizontalAlignments, verticalAlignments
} from '@shared/models/widget-settings.models';

@Component({
    selector: 'tb-alignment-panel',
    templateUrl: './alignment-panel.component.html',
    styleUrls: ['./alignment-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class AlignmentPanelComponent implements OnInit {

  alignments: alignment[];
  alignmentTranslations = alignmentTranslations;
  alignmentIcons = alignmentIcons;

  @Input()
  alignment: alignment;

  @Input()
  horizontal: boolean;

  @Input()
  allowedAlignments: alignment[];

  @Output()
  alignmentSelected = new EventEmitter<alignment>();

  constructor() {
  }

  ngOnInit() {
    if (this.allowedAlignments?.length) {
      this.alignments = this.allowedAlignments;
    } else {
      this.alignments = this.horizontal ? horizontalAlignments : verticalAlignments;
    }
  }

  selectAlignment(alignment: alignment) {
    this.alignmentSelected.emit(alignment);
  }
}
