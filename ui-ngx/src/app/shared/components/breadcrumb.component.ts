///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { BreadCrumb, BreadCrumbConfig } from './breadcrumb';
import { ActivatedRoute, ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { guid } from '@core/utils';

@Component({
  selector: 'tb-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  activeComponentValue: any;

  @Input()
  set activeComponent(activeComponent: any) {
    this.activeComponentValue = activeComponent;
  }

  breadcrumbs$: Subject<Array<BreadCrumb>> = new BehaviorSubject<Array<BreadCrumb>>(this.buildBreadCrumbs(this.activatedRoute.snapshot));

  routerEventsSubscription = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd ),
    distinctUntilChanged(),
    map( () => this.buildBreadCrumbs(this.activatedRoute.snapshot) )
  ).subscribe(breadcrumns => this.breadcrumbs$.next(breadcrumns) );

  lastBreadcrumb$ = this.breadcrumbs$.pipe(
    map( breadcrumbs => breadcrumbs[breadcrumbs.length - 1])
  );

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    if (this.routerEventsSubscription) {
      this.routerEventsSubscription.unsubscribe();
    }
  }

  private lastChild(route: ActivatedRouteSnapshot) {
    let child = route;
    while (child.firstChild !== null) {
      child = child.firstChild;
    }
    return child;
  }

  buildBreadCrumbs(route: ActivatedRouteSnapshot, breadcrumbs: Array<BreadCrumb> = [],
                   lastChild?: ActivatedRouteSnapshot): Array<BreadCrumb> {
    if (!lastChild) {
      lastChild = this.lastChild(route);
    }
    let newBreadcrumbs = breadcrumbs;
    if (route.routeConfig && route.routeConfig.data) {
      const breadcrumbConfig = route.routeConfig.data.breadcrumb as BreadCrumbConfig<any>;
      if (breadcrumbConfig && !breadcrumbConfig.skip) {
        let label;
        let labelFunction;
        let ignoreTranslate;
        if (breadcrumbConfig.labelFunction) {
          labelFunction = () => {
            return breadcrumbConfig.labelFunction(route, this.translate, this.activeComponentValue, lastChild.data);
          };
          ignoreTranslate = true;
        } else {
          label = breadcrumbConfig.label || 'home.home';
          ignoreTranslate = false;
        }
        const icon = breadcrumbConfig.icon || 'home';
        const isMdiIcon = icon.startsWith('mdi:');
        const link = [ route.pathFromRoot.map(v => v.url.map(segment => segment.toString()).join('/')).join('/') ];
        const breadcrumb = {
          id: guid(),
          label,
          labelFunction,
          ignoreTranslate,
          icon,
          isMdiIcon,
          link,
          queryParams: null
        };
        newBreadcrumbs = [...breadcrumbs, breadcrumb];
      }
    }
    if (route.firstChild) {
      return this.buildBreadCrumbs(route.firstChild, newBreadcrumbs, lastChild);
    }
    return newBreadcrumbs;
  }

  trackByBreadcrumbs(index: number, breadcrumb: BreadCrumb){
    return breadcrumb.id;
  }
}
