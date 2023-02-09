///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { AfterViewInit, Component, Inject, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WINDOW } from '@core/services/window.service';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '@core/services/menu.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { instanceOfSearchableComponent } from '@home/models/searchable-component.models';
import { BroadcastService } from '@core/services/broadcast.service';
import { ActiveComponentService } from '@core/services/active-component.service';

@Component({
  selector: 'tb-router-tabs',
  templateUrl: './router-tabs.component.html',
  styleUrls: ['./route-tabs.component.scss']
})
export class RouterTabsComponent extends PageComponent implements AfterViewInit, OnInit {

  tabs$: Observable<Array<MenuSection>> = this.menuService.menuSections().pipe(
    map(sections => {
      const found = sections.find(section => section.path === `/${this.activatedRoute.routeConfig.path}`);
      return found ? found.pages : [];
    })
  );

  constructor(protected store: Store<AppState>,
              private activatedRoute: ActivatedRoute,
              public router: Router,
              private menuService: MenuService,
              private activeComponentService: ActiveComponentService,
              @Inject(WINDOW) private window: Window,
              public breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit() {}

  activeComponentChanged(activeComponent: any) {
    this.activeComponentService.setCurrentActiveComponent(activeComponent);
  }

}
