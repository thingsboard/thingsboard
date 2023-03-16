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
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MenuService } from '@core/services/menu.service';
import { distinctUntilChanged, filter, map, mergeMap } from 'rxjs/operators';
import { merge } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { ActiveComponentService } from '@core/services/active-component.service';

@Component({
  selector: 'tb-router-tabs',
  templateUrl: './router-tabs.component.html',
  styleUrls: ['./router-tabs.component.scss']
})
export class RouterTabsComponent extends PageComponent implements AfterViewInit, OnInit {

  hideCurrentTabs = false;

  tabs$ = merge(this.menuService.menuSections(),
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd ),
      distinctUntilChanged()
    )
  ).pipe(
    mergeMap(() => this.menuService.menuSections()),
    map((sections) => this.buildTabs(this.activatedRoute, sections))
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
    let snapshot = this.router.routerState.snapshot.root;
    this.hideCurrentTabs = false;
    let found = false;
    while (snapshot.children.length) {
      if (snapshot.component && snapshot.component === RouterTabsComponent) {
        if (this.activatedRoute.snapshot === snapshot) {
          found = true;
        } else if (found) {
          this.hideCurrentTabs = true;
          break;
        }
      }
      snapshot = snapshot.children[0];
    }
  }

  private buildTabs(activatedRoute: ActivatedRoute, sections: MenuSection[]): Array<MenuSection> {
    const sectionPath = '/' + activatedRoute.pathFromRoot.map(r => r.snapshot.url)
      .filter(f => !!f[0]).map(f => f.map(f1 => f1.path).join('/')).join('/');
    const found = this.findRootSection(sections, sectionPath);
    const rootPath = sectionPath.substring(0, sectionPath.length - found.path.length);
    const isRoot = rootPath === '';
    const tabs: Array<MenuSection> = found ? found.pages.filter(page => !page.disabled && (!page.rootOnly || isRoot)) : [];
    return tabs.map((tab) => ({...tab, path: rootPath + tab.path}));
  }

  private findRootSection(sections: MenuSection[], sectionPath: string): MenuSection {
    for (const section of sections) {
      if (sectionPath.endsWith(section.path)) {
        return section;
      }
      if (section.pages?.length) {
        const found = this.findRootSection(section.pages, sectionPath);
        if (found) {
          return found;
        }
      }
    }
    return null;
  }

}
