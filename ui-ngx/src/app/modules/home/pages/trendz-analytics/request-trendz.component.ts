// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { RequestTrendzDialogComponent } from '@shared/components/dialog/request-trendz-dialog.component';
import { DynamicMatDialog } from '@shared/components/dialog/dynamic/dynamic-dialog';

@Component({
    selector: 'tb-request-trendz',
    templateUrl: './request-trendz.component.html',
    styleUrls: ['./request-trendz.component.scss'],
    standalone: false
})
export class RequestTrendzComponent extends PageComponent implements OnInit {

  constructor(private dialog: DynamicMatDialog,
              private elementRef: ElementRef) {
    super();
  }

  ngOnInit() {
    this.dialog.open<RequestTrendzDialogComponent>(RequestTrendzDialogComponent,
      {
        containerElement: this.elementRef.nativeElement,
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-lg'],
      });
  }
}
