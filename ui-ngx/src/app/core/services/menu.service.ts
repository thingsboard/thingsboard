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
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import { buildUserHome, buildUserMenu, HomeSection, MenuId, MenuSection } from '@core/services/menu.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { AuthState } from '@core/auth/auth.models';
import { NavigationEnd, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private currentMenuSections: Array<MenuSection>;
  private menuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private homeSections$: Subject<Array<HomeSection>> = new ReplaySubject<Array<HomeSection>>(1);
  private availableMenuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );

  constructor(private store: Store<AppState>,
              private router: Router) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          this.currentMenuSections = buildUserMenu(authState);
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          const availableMenuSections = this.allMenuSections(this.currentMenuSections);
          this.availableMenuSections$.next(availableMenuSections);
          const homeSections = buildUserHome(authState, availableMenuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    if (this.currentMenuSections?.length) {
      this.currentMenuSections.filter(section => section.type === 'toggle' &&
        (url.startsWith(section.path) || openedMenuSections.includes(section.path))).forEach(
        section => section.opened = true
      );
    }
  }

  private allMenuLinks(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      if (section.type === 'link') {
        result.push(section);
      }
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuLinks(section.pages));
      }
    }
    return result;
  }

  private allMenuSections(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      result.push(section);
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuSections(section.pages));
      }
    }
    return result;
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

  public availableMenuLinks(): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$;
  }

  public availableMenuSections(): Observable<Array<MenuSection>> {
    return this.availableMenuSections$;
  }

  public menuLinkById(id: MenuId | string): Observable<MenuSection | undefined> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.find(link => link.id === id))
    );
  }

  public menuLinksByIds(ids: string[]): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.filter(link => ids.includes(link.id)).sort((a, b) => {
        const i1 = ids.indexOf(a.id);
        const i2 = ids.indexOf(b.id);
        return i1 - i2;
      }))
    );
  }

}
