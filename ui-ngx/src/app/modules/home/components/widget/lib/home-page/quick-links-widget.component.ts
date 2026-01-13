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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { map, Observable, of, Subscription } from 'rxjs';
import { QuickLinks } from '@shared/models/user-settings.models';
import { UserSettingsService } from '@core/http/user-settings.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { MatDialog } from '@angular/material/dialog';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { AddQuickLinkDialogComponent } from '@home/components/widget/lib/home-page/add-quick-link-dialog.component';
import {
  EditLinksDialogComponent,
  EditLinksDialogData
} from '@home/components/widget/lib/home-page/edit-links-dialog.component';

const defaultQuickLinksMap = new Map<Authority, QuickLinks>(
  [
    [Authority.SYS_ADMIN, {
      links: ['tenants', 'tenant_profiles']
    }],
    [Authority.TENANT_ADMIN, {
      links: ['alarms', 'dashboards', 'devices']
    }],
    [Authority.CUSTOMER_USER, {
      links: ['alarms', 'dashboards', 'devices']
    }]
  ]
);

interface QuickLinksWidgetSettings {
  columns: number;
}

@Component({
  selector: 'tb-quick-links-widget',
  templateUrl: './quick-links-widget.component.html',
  styleUrls: ['./home-page-widget.scss', './links-widget.component.scss']
})
export class QuickLinksWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  settings: QuickLinksWidgetSettings;
  columns: number;
  rowHeight = '55px';
  gutterSize = '12px';

  quickLinks: QuickLinks;
  authUser = getCurrentAuthUser(this.store);

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private userSettingsService: UserSettingsService,
              private dialog: MatDialog,
              private menuService: MenuService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    this.settings = this.ctx.settings;
    this.columns = this.settings.columns || 3;
    const isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    this.rowHeight = isMdLg ? '18px' : '55px';
    this.gutterSize = isMdLg ? '8px' : '12px';
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['md-lg'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.rowHeight = '18px';
            this.gutterSize = '8px';
          } else {
            this.rowHeight = '55px';
            this.gutterSize = '12px';
          }
          this.cd.markForCheck();
        }
      );
    this.loadQuickLinks();
  }

  ngOnDestroy() {
    if (this.observeBreakpointSubscription) {
      this.observeBreakpointSubscription.unsubscribe();
    }
    super.ngOnDestroy();
  }

  menuLinks$(): Observable<Array<MenuSection>> {
    return this.quickLinks ? this.menuService.menuLinksByIds(this.quickLinks.links) : of([]);
  }

  private loadQuickLinks() {
    this.userSettingsService.getQuickLinks().pipe(
      map((quickLinks) => {
        if (!quickLinks || !quickLinks.links) {
          return defaultQuickLinksMap.get(this.authUser.authority);
        } else {
          return quickLinks;
        }
      })
    ).subscribe(
      (quickLinks) => {
        this.quickLinks = quickLinks;
        this.cd.markForCheck();
      }
    );
  }

  edit() {
    this.dialog.open<EditLinksDialogComponent, EditLinksDialogData,
      boolean>(EditLinksDialogComponent, {
      disableClose: true,
      autoFocus: false,
      data: {
        mode: 'quickLinks',
        links: this.quickLinks
      },
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.loadQuickLinks();
        }
      });
  }

  addLink() {
    this.dialog.open<AddQuickLinkDialogComponent, any,
      string>(AddQuickLinkDialogComponent, {
      disableClose: true,
      autoFocus: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (link) => {
        if (link) {
          this.quickLinks.links.push(link);
          this.cd.markForCheck();
          this.userSettingsService.updateQuickLinks(this.quickLinks).subscribe();
        }
      });
  }
}
