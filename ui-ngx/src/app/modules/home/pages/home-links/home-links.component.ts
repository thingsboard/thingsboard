// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HomeDashboard } from '@shared/models/dashboard.models';

@Component({
  selector: 'tb-home-links',
  templateUrl: './home-links.component.html',
  styleUrls: ['./home-links.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class HomeLinksComponent implements OnInit {

  homeDashboard: HomeDashboard = this.route.snapshot.data.homeDashboard;

  hideMainToolbar = true;

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.hideMainToolbar = (!!this.homeDashboard && !this.homeDashboard.isSystemDashboard);
  }
}
