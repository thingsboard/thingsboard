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

import { Injectable } from '@angular/core';
import { BehaviorSubject, merge, shareReplay, Subject, Subscription } from 'rxjs';
import { BreadCrumb, BreadCrumbConfig } from '@shared/components/breadcrumb';
import { ActivatedRoute, ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MenuService } from '@core/services/menu.service';
import { distinctUntilChanged, filter, first, map, switchMap } from 'rxjs/operators';
import { MenuSection, menuSectionMap } from '@core/services/menu.models';
import { guid } from '@core/utils';
import { ActiveComponentService } from '@core/services/active-component.service';

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbService {

  private updateBreadcrumbsSubscription: Subscription = null;
  private breadcrumbsSubject: Subject<Array<BreadCrumb>> = new BehaviorSubject<Array<BreadCrumb>>([]);
  private activeComponent: any;

  get breadcrumbs$() {
    return this.breadcrumbsSubject.asObservable().pipe(shareReplay(1));
  }

  get lastBreadcrumb$() {
    return this.breadcrumbs$.pipe<BreadCrumb>(
      map( (breadcrumbs: BreadCrumb[]) => breadcrumbs.length ? breadcrumbs[breadcrumbs.length - 1] : null)
    );
  }

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private translate: TranslateService,
              private menuService: MenuService,
              private activeComponentService: ActiveComponentService) {

    merge(this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd ),
      distinctUntilChanged()), this.menuService.availableMenuSections()).pipe(
      switchMap(() => this.menuService.availableMenuSections().pipe(first())),
      map( (sections: MenuSection[]) => this.buildBreadCrumbs(this.activatedRoute.snapshot, sections) )
    ).subscribe((breadcrumbs: BreadCrumb[]) => this.breadcrumbsSubject.next(breadcrumbs) );

    this.activeComponentService.onActiveComponentChanged().subscribe(comp => this.setActiveComponent(comp));
  }

  private setActiveComponent(activeComponent: any) {
    if (this.updateBreadcrumbsSubscription) {
      this.updateBreadcrumbsSubscription.unsubscribe();
      this.updateBreadcrumbsSubscription = null;
    }
    this.activeComponent = activeComponent;
    if (this.activeComponent) {
      if (this.activeComponent.updateBreadcrumbs) {
        this.updateBreadcrumbsSubscription = this.activeComponent.updateBreadcrumbs.subscribe(() => {
          this.menuService.availableMenuSections().pipe(first()).subscribe((sections: MenuSection[]) => {
            const breadcrumbs = this.buildBreadCrumbs(this.activatedRoute.snapshot, sections);
            this.breadcrumbsSubject.next(breadcrumbs);
          });
        });
      }
    }
  }

  private buildBreadCrumbs(route: ActivatedRouteSnapshot, availableMenuSections: MenuSection[],
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
          labelFunction = () => {
            if (this.activeComponent) {
              try {
                return breadcrumbConfig.labelFunction(route, this.translate, this.activeComponent, lastChild.data);
              } catch {
                return label;
              }
            } else {
              return label;
            }
          }
        }
        const link = [ route.pathFromRoot.map(v => v.url.map(segment => segment.toString()).join('/')).join('/') ];
        const breadcrumb = {
          id: guid(),
          label,
          customTranslate,
          labelFunction,
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

  private lastChild(route: ActivatedRouteSnapshot) {
    let child = route;
    while (child.firstChild !== null) {
      child = child.firstChild;
    }
    return child;
  }

}
