// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import {
  buildUserHome,
  buildUserMenu,
  HomeSection,
  MenuSection, sectionPath
} from '@core/services/menu.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { ActivationEnd, NavigationEnd, Params, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthState } from '@core/auth/auth.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private currentMenuSections: Array<MenuSection>;
  private menuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private homeSections$: Subject<Array<HomeSection>> = new ReplaySubject<Array<HomeSection>>(1);
  private _availableMenuSections: Array<MenuSection> = [];
  private availableMenuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );

  private currentCustomSection: MenuSection = null;
  private currentCustomChildSection: MenuSection = null;

  constructor(private store: Store<AppState>,
              private router: Router,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.customMenuService.customMenuConfigChanged$.subscribe(() => {
      this.buildMenu();
    });
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(() => {
        this.updateActiveMenuSections();
    });
  }

  public buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          const customMenu = this.customMenuService.getCustomMenu();
          this.currentMenuSections = buildUserMenu(authState, this.userPermissionsService, customMenu);
          this._availableMenuSections = this.allMenuSections(this.currentMenuSections);
          this.updateActiveMenuSections();
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          this.availableMenuSections$.next(this._availableMenuSections);
          const homeSections = buildUserHome(this.currentMenuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private updateOpenedMenuSections() {
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    if (this.currentMenuSections?.length) {
      this.currentMenuSections.filter(section => section.type === 'toggle' &&
        (openedMenuSections.includes(sectionPath(section)) || section.active)).forEach(
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

  public getCurrentCustomSection(): MenuSection {
    return this.currentCustomSection;
  }

  public getCurrentCustomChildSection(): MenuSection {
    return this.currentCustomChildSection;
  }

  private updateActiveMenuSections() {
    this.updateCurrentCustomSection();
    const url = this.router.url;
    const activeMenuSection = this._availableMenuSections.find(section => section.path === url);
    this._availableMenuSections.forEach((section: MenuSection) => {
      section.active = this.isSectionActive(section, activeMenuSection);
    });
  }

  private isSectionActive(section: MenuSection, activeMenuSection: MenuSection): boolean {
    if (section.isCustom && (this.currentCustomSection === section || this.currentCustomChildSection === section)) {
      return true;
    } else if (activeMenuSection === section) {
      return true;
    } else if (section.pages?.length) {
      for (const page of section.pages) {
        if (this.isSectionActive(page, activeMenuSection)) {
          return true;
        }
      }
    }
    return false;
  }

  private updateCurrentCustomSection() {
    const queryParams = this.extractQueryParams();
    this.currentCustomSection = this.detectCurrentCustomSection(queryParams);
    this.currentCustomChildSection = this.detectCurrentCustomChildSection(queryParams);
  }

  private detectCurrentCustomSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.stateId) {
      const stateId: string = queryParams.stateId;
      const found =
        this.currentMenuSections.find((section) => section.isCustom && section.stateId === stateId);
      if (found) {
        return found;
      }
    }
    return null;
  }

  private detectCurrentCustomChildSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.childStateId) {
      const stateId = queryParams.childStateId;
      for (const section of this.currentMenuSections) {
        if (section.isCustom && section.pages && section.pages.length) {
          const found =
            section.pages.find((childSection) => childSection.stateId === stateId);
          if (found) {
            return found;
          }
        }
      }
    }
    return null;
  }

  private extractQueryParams(): Params {
    const state = this.router.routerState;
    const snapshot =  state.snapshot;
    let lastChild = snapshot.root;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    return lastChild.queryParams;
  }

  public getRedirectPath(parentPath: string, redirectPath: string): Observable<string> {
    parentPath = '/' + parentPath.replace(/\./g, '/');
    if (!redirectPath.startsWith('/')) {
      redirectPath = `${parentPath}/${redirectPath}`;
    }
    return this.menuSections$.pipe(
      map((sections) => {
        const parentSection = this.findSectionByPath(sections, parentPath);
        if (parentSection) {
          if (parentSection.pages) {
            const childPages = parentSection.pages;
            if (childPages && childPages.length) {
              const redirectPage = childPages.filter((page) => page.path === redirectPath);
              if (!redirectPage || !redirectPage.length) {
                return childPages[0].path;
              }
            }
            return redirectPath;
          }
        }
        return redirectPath;
      })
    );
  }

  private findSectionByPath(sections: MenuSection[], sectionPath: string): MenuSection {
    for (const section of sections) {
      if (sectionPath === section.path) {
        return section;
      }
      if (section.pages?.length) {
        const found = this.findSectionByPath(section.pages, sectionPath);
        if (found) {
          return found;
        }
      }
    }
    return null;
  }

  public availableMenuLinks(): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$;
  }

  public availableMenuSections(): Observable<Array<MenuSection>> {
    return this.availableMenuSections$;
  }

  public menuLinkById(id: string): Observable<MenuSection | undefined> {
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
