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

import { Component, ComponentRef, OnInit, Type, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MenuService } from '@core/services/menu.service';
import { distinctUntilChanged, filter, map, mergeMap, startWith, take } from 'rxjs/operators';
import { merge, Observable } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { ActiveComponentService } from '@core/services/active-component.service';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-router-tabs',
  templateUrl: './router-tabs.component.html',
  styleUrls: ['./router-tabs.component.scss']
})
export class RouterTabsComponent extends PageComponent implements OnInit {

  @ViewChild('tabsHeaderComponent', {static: true}) tabsHeaderComponentAnchor: TbAnchorComponent;

  tabsHeaderComponentRef: ComponentRef<any>;

  hideCurrentTabs = false;

  replaceUrl = false;

  tabs$: Observable<Array<MenuSection>>;

  constructor(protected store: Store<AppState>,
              private activatedRoute: ActivatedRoute,
              public router: Router,
              private menuService: MenuService,
              private activeComponentService: ActiveComponentService) {
    super(store);
  }

  ngOnInit() {
    if (this.activatedRoute.snapshot.data.useChildrenRoutesForTabs) {
      this.tabs$ = this.router.events.pipe(
        filter((event) => event instanceof NavigationEnd),
        startWith(''),
        map(() => this.buildTabsForRoutes(this.activatedRoute))
      );
    } else {
      this.tabs$ = merge(this.menuService.menuSections(),
        this.router.events.pipe(
          filter((event) => event instanceof NavigationEnd ),
          distinctUntilChanged())
      ).pipe(
        mergeMap(() => this.menuService.menuSections().pipe(take(1))),
        map((sections) => this.buildTabs(this.activatedRoute, sections))
      );
    }

    if (this.activatedRoute.snapshot.data.replaceUrl) {
      this.replaceUrl = true;
    }

    this.activatedRoute.data.subscribe(
      (data) => this.buildTabsHeaderComponent(data)
    );
  }

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

  private getSectionPath(activatedRoute: ActivatedRoute): string {
    return '/' + activatedRoute.pathFromRoot.map(r => r.snapshot.url)
      .filter(f => !!f[0]).map(f => f.map(f1 => f1.path).join('/')).join('/');
  }

  private buildTabs(activatedRoute: ActivatedRoute, sections: MenuSection[]): Array<MenuSection> {
    const sectionPath = this.getSectionPath(activatedRoute);
    const found = this.findRootSection(sections, sectionPath);
    if (found) {
      const rootPath = sectionPath.substring(0, sectionPath.length - found.path.length);
      const isRoot = rootPath === '';
      const tabs: Array<MenuSection> = found ? found.pages.filter(page => !page.rootOnly || isRoot) : [];
      return tabs.map((tab) => ({...tab, path: rootPath + tab.path}));
    }
    return [];
  }

  private buildTabsForRoutes(activatedRoute: ActivatedRoute): Array<MenuSection> {
    const sectionPath = this.getSectionPath(activatedRoute);
    const authority = getCurrentAuthUser(this.store).authority;
    const children = activatedRoute.routeConfig.children.filter(page => {
      if (page.path !== '') {
        if (page.data?.auth) {
          return page.data?.auth.includes(authority);
        } else {
          return true;
        }
      } else {
        return false;
      }
    });
    if (children.length) {
      return children.map(tab => ({
        id: tab.component.name,
        type: 'link',
        name: tab.data?.breadcrumb?.label ?? '',
        icon: tab.data?.breadcrumb?.icon ?? '',
        path: `${sectionPath}/${tab.path}`
      }));
    }
    return [];
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

  private buildTabsHeaderComponent(snapshotData: any) {
    const viewContainerRef = this.tabsHeaderComponentAnchor.viewContainerRef;
    if (this.tabsHeaderComponentRef) {
      this.tabsHeaderComponentRef.destroy();
      this.tabsHeaderComponentRef = null;
      viewContainerRef.clear();
    }
    const tabsHeaderComponentType: Type<any> = snapshotData.routerTabsHeaderComponent;
    if (tabsHeaderComponentType) {
      this.tabsHeaderComponentRef = viewContainerRef.createComponent(tabsHeaderComponentType, {index: 0});
    }
  }

}
