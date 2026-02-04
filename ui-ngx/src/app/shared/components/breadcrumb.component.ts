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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { BreadCrumb, BreadCrumbConfig } from './breadcrumb';
import { ActivatedRoute, ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { distinctUntilChanged, filter, first, map, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { guid } from '@core/utils';
import { BroadcastService } from '@core/services/broadcast.service';
import { ActiveComponentService } from '@core/services/active-component.service';
import { UtilsService } from '@core/services/utils.service';
import { MenuSection, menuSectionMap } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';

@Component({
    selector: 'tb-breadcrumb',
    templateUrl: './breadcrumb.component.html',
    styleUrls: ['./breadcrumb.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  activeComponentValue: any;
  updateBreadcrumbsSubscription: Subscription = null;

  setActiveComponent(activeComponent: any) {
    if (this.updateBreadcrumbsSubscription) {
      this.updateBreadcrumbsSubscription.unsubscribe();
      this.updateBreadcrumbsSubscription = null;
    }
    this.activeComponentValue = activeComponent;
    if (this.activeComponentValue && this.activeComponentValue.updateBreadcrumbs) {
      this.updateBreadcrumbsSubscription = this.activeComponentValue.updateBreadcrumbs.subscribe(() => {
        this.menuService.availableMenuSections().pipe(first()).subscribe((sections) => {
          this.breadcrumbs$.next(this.buildBreadCrumbs(this.activatedRoute.snapshot, sections));
        });
      });
    }
  }

  breadcrumbs$: Subject<Array<BreadCrumb>> = new BehaviorSubject<Array<BreadCrumb>>([]);

  routerEventsSubscription = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd ),
    distinctUntilChanged(),
    switchMap(() => this.menuService.availableMenuSections().pipe(first())),
    map( (sections) => this.buildBreadCrumbs(this.activatedRoute.snapshot, sections) )
  ).subscribe(breadcrumns => this.breadcrumbs$.next(breadcrumns) );

  activeComponentSubscription = this.activeComponentService.onActiveComponentChanged().subscribe(comp => this.setActiveComponent(comp));

  lastBreadcrumb$ = this.breadcrumbs$.pipe(
    map( breadcrumbs => breadcrumbs[breadcrumbs.length - 1])
  );

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private broadcast: BroadcastService,
              private activeComponentService: ActiveComponentService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private menuService: MenuService,
              public utils: UtilsService) {
  }

  ngOnInit(): void {
    this.broadcast.on('updateBreadcrumb', () => {
      this.cd.markForCheck();
    });
    this.setActiveComponent(this.activeComponentService.getCurrentActiveComponent());
  }

  ngOnDestroy(): void {
    if (this.routerEventsSubscription) {
      this.routerEventsSubscription.unsubscribe();
    }
    if (this.activeComponentSubscription) {
      this.activeComponentSubscription.unsubscribe();
    }
  }

  private lastChild(route: ActivatedRouteSnapshot) {
    let child = route;
    while (child.firstChild !== null) {
      child = child.firstChild;
    }
    return child;
  }

  buildBreadCrumbs(route: ActivatedRouteSnapshot, availableMenuSections: MenuSection[],
                   breadcrumbs: Array<BreadCrumb> = [],
                   lastChild?: ActivatedRouteSnapshot): Array<BreadCrumb> {
    if (!lastChild) {
      lastChild = this.lastChild(route);
    }
    let newBreadcrumbs = breadcrumbs;
    if (route.routeConfig && route.routeConfig.data) {
      const breadcrumbConfig = route.routeConfig.data.breadcrumb as BreadCrumbConfig<any>;
      if (breadcrumbConfig && !breadcrumbConfig.skip) {
        let labelFunction: () => string;
        let section: MenuSection = null;
        if (breadcrumbConfig.menuId) {
          section = availableMenuSections.find(menu => menu.id === breadcrumbConfig.menuId);
          if (!section) {
            section = menuSectionMap.get(breadcrumbConfig.menuId);
          }
        }
        const label = section?.name || breadcrumbConfig.label || 'home.home';
        const customTranslate = section?.customTranslate || false;
        if (breadcrumbConfig.labelFunction) {
          labelFunction = () => this.activeComponentValue ?
            breadcrumbConfig.labelFunction(route, this.translate, this.activeComponentValue, lastChild.data) : label;
        }
        const icon = section?.icon || breadcrumbConfig.icon || 'home';
        const link = [ route.pathFromRoot.map(v => v.url.map(segment => segment.toString()).join('/')).join('/') ];
        const breadcrumb = {
          id: guid(),
          label,
          customTranslate,
          labelFunction,
          icon,
          link,
          queryParams: null
        };
        newBreadcrumbs = [...breadcrumbs, breadcrumb];
      }
    }
    if (route.firstChild) {
      return this.buildBreadCrumbs(route.firstChild, availableMenuSections, newBreadcrumbs, lastChild);
    }
    return newBreadcrumbs;
  }

  trackByBreadcrumbs(_index: number, breadcrumb: BreadCrumb){
    return breadcrumb.id;
  }
}
