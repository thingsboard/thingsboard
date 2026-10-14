// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, OnInit } from '@angular/core';
import { RequestEdgeDialogComponent } from '@shared/components/dialog/request-edge-dialog.component';
import { DynamicMatDialog } from '@shared/components/dialog/dynamic/dynamic-dialog';

@Component({
    selector: 'tb-request-edge',
    templateUrl: './request-edge.component.html',
    styleUrls: ['./request-edge.component.scss'],
    standalone: false
})
export class RequestEdgeComponent implements OnInit {

  constructor(private dialog: DynamicMatDialog,
              private elementRef: ElementRef) {
  }

  ngOnInit() {
    this.dialog.open<RequestEdgeDialogComponent>(RequestEdgeDialogComponent,
      {
        containerElement: this.elementRef.nativeElement,
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-lg'],
      });
  }

}
