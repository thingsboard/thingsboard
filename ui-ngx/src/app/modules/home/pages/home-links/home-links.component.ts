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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, DestroyRef,
  effect,
  EventEmitter,
  OnInit,
  viewChild
} from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { HomeSection } from '@core/services/menu.models';
import { ActivatedRoute } from '@angular/router';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { MainToolbarComponent } from '@home/models/main-toolbar.models';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-home-links',
  templateUrl: './home-links.component.html',
  styleUrls: ['./home-links.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class HomeLinksComponent implements MainToolbarComponent, OnInit {

  private dashboardPage = viewChild<DashboardPageComponent>('dashboardPage');

  homeSections$ = this.menuService.homeSections();

  cols = 2;

  homeDashboard: HomeDashboard = this.route.snapshot.data.homeDashboard;

  hideMainToolbar = false;
  toggleSideBar = new EventEmitter<void>();

  constructor(private menuService: MenuService,
              public breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef,
              private route: ActivatedRoute,
              private destroyRef: DestroyRef) {
    effect(() => {
      const dashboardPage = this.dashboardPage();
      if (dashboardPage) {
        dashboardPage.toggleSideBar.pipe(
          takeUntilDestroyed(this.destroyRef)
        ).subscribe(() => {
          this.toggleSideBar.emit();
        });
      }
    });
    this.hideMainToolbar = (!!this.homeDashboard && !this.homeDashboard.isSystemDashboard);
  }

  ngOnInit() {
    if (!this.homeDashboard) {
      this.updateColumnCount();
      this.breakpointObserver
        .observe([MediaBreakpoints.lg, MediaBreakpoints['gt-lg']])
        .subscribe((state: BreakpointState) => this.updateColumnCount());
    }
  }

  private updateColumnCount() {
    this.cols = 2;
    if (this.breakpointObserver.isMatched(MediaBreakpoints.lg)) {
      this.cols = 3;
    }
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-lg'])) {
      this.cols = 4;
    }
    this.cd.detectChanges();
  }

  sectionColspan(section: HomeSection): number {
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm'])) {
      let colspan = this.cols;
      if (section && section.places && section.places.length <= colspan) {
        colspan = section.places.length;
      }
      return colspan;
    } else {
      return 2;
    }
  }
}
