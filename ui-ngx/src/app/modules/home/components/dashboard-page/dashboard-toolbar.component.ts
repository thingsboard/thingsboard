// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';

@Component({
    selector: 'tb-dashboard-toolbar',
    templateUrl: './dashboard-toolbar.component.html',
    styleUrls: ['./dashboard-toolbar.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DashboardToolbarComponent implements OnInit {

  @Input()
  toolbarOpened: boolean;

  @Input()
  forceFullscreen: boolean;

  @Output()
  triggerClick = new EventEmitter<void>();

  constructor() {
  }

  ngOnInit(): void {
  }

  onTriggerClick() {
    this.triggerClick.emit();
  }

}
