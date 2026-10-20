// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  Inject,
  OnDestroy,
  OnInit, Renderer2,
  signal,
  ViewChild
} from '@angular/core';
import { combineLatest, Observable, skip, startWith, Subject } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { debounceTime, distinctUntilChanged, map, share, take, takeUntil } from 'rxjs/operators';

import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { MediaBreakpoints } from '@shared/models/constants';
import screenfull from 'screenfull';
import { MatSidenav } from '@angular/material/sidenav';
import { AuthState } from '@core/auth/auth.models';
import { WINDOW } from '@core/services/window.service';
import { instanceOfSearchableComponent, ISearchableComponent } from '@home/models/searchable-component.models';
import { ActiveComponentService } from '@core/services/active-component.service';
import { FormBuilder } from '@angular/forms';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { TranslateService } from '@ngx-translate/core';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { HomeService } from '@core/services/home.service';

@Component({
    selector: 'tb-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.scss'],
    standalone: false
})
export class HomeComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy {

  authState: AuthState = getCurrentAuthState(this.store);

  forceFullscreen = this.authState.forceFullscreen;

  activeComponent: any;
  searchableComponent: ISearchableComponent;

  sidenavMode: 'over' | 'push' | 'side' = 'side';
  sidenavOpened = true;

  sidenavDesktop = signal(true);
  sidenavCollapsed = signal(false);
  menuCollapsed= computed(() => this.sidenavDesktop() && this.sidenavCollapsed());

  @ViewChild('sidenav')
  sidenav: MatSidenav;

  @ViewChild('sidebarScroll', { static: true }) sidebarScroll: ElementRef<HTMLElement>;
  @ViewChild('sideMenu', { static: true, read: ElementRef<HTMLElement> }) sideMenu: ElementRef<HTMLElement>;

  @ViewChild('navHeader', { static: true }) navHeader: ElementRef<HTMLElement>;
  @ViewChild('sidebarBottom', { static: true }) sidebarBottom: ElementRef<HTMLElement>;

  @ViewChild('mainContent', { static: true }) mainContent: ElementRef<HTMLElement>;

  @ViewChild('searchInput') searchInputField: ElementRef;

  fullscreenEnabled = screenfull.isEnabled;

  searchEnabled = false;
  showSearch = false;
  textSearch = this.fb.control('', {nonNullable: true});

  private updateScrollShadows = this._updateScrollShadows.bind(this);
  private scrollShadowWatcher$: ResizeObserver;
  private sidebarScrollUnlisten: () => void;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private activeComponentService: ActiveComponentService,
              private fb: FormBuilder,
              private renderer: Renderer2,
              public wl: WhiteLabelingService,
              public translate: TranslateService,
              public breakpointObserver: BreakpointObserver,
              public homeService: HomeService,
              public panelService: AiAssistantPanelService) {
    super(store);
  }

  ngOnInit() {

    const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    this.sidenavMode = isGtSm ? 'side' : 'over';
    this.sidenavOpened = isGtSm;
    this.sidenavDesktop.set(isGtSm);
    this.store.pipe(select(selectUserSettingsProperty('menuCollapsed'))).pipe(
      take(1)
    ).subscribe((collapsed: boolean) => {
      this.sidenavCollapsed.set(collapsed);
    });

    this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .pipe(takeUntil(this.destroy$))
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.sidenavMode = 'side';
            this.sidenavOpened = true;
            this.sidenavDesktop.set(true);
          } else {
            this.sidenavMode = 'over';
            this.sidenavOpened = false;
            this.sidenavDesktop.set(false);
          }
        }
      );

    this.homeService.toggleSideBar.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.sidenav.toggle();
    });
  }

  ngOnDestroy() {
    if (this.scrollShadowWatcher$) {
      this.scrollShadowWatcher$.disconnect();
    }
    if (this.sidebarScrollUnlisten) {
      this.sidebarScrollUnlisten();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      startWith(''),
      distinctUntilChanged((a: string, b: string) => a.trim() === b.trim()),
      skip(1),
      takeUntil(this.destroy$)
    ).subscribe(value => this.searchTextUpdated(value.trim()));

    this.scrollShadowWatcher$ = new ResizeObserver(this.updateScrollShadows);
    this.scrollShadowWatcher$.observe(this.sideMenu.nativeElement);
    this.scrollShadowWatcher$.observe(this.sidebarScroll.nativeElement);
    this.sidebarScrollUnlisten = this.renderer.listen(this.sidebarScroll.nativeElement, 'scroll', this.updateScrollShadows);
  }

  sidenavClicked() {
    if (this.sidenavMode === 'over') {
      this.sidenav.toggle();
    }
  }

  toggleSidenav() {
    this.sidenavCollapsed.update(state => !state);
    this.store.dispatch(new ActionPreferencesPutUserSettings({ menuCollapsed: this.sidenavCollapsed() }));
  }

  toggleFullscreen() {
    if (screenfull.isEnabled) {
      screenfull.toggle();
    }
  }

  isFullscreen() {
    return screenfull.isFullscreen;
  }

  goBack() {
    this.window.history.back();
  }

  activeComponentChanged(activeComponent: any) {
    this.activeComponentService.setCurrentActiveComponent(activeComponent);
    this.mainContent?.nativeElement?.scrollTo({ top: 0, left: 0 });
    if (!this.activeComponent) {
      setTimeout(() => {
        this.updateActiveComponent(activeComponent);
      }, 0);
    } else {
      this.updateActiveComponent(activeComponent);
    }
  }

  private _updateScrollShadows(): void {
    const sidebarScrollElm = this.sidebarScroll.nativeElement;
    if (sidebarScrollElm.scrollTop > 0) {
      this.renderer.addClass(this.navHeader.nativeElement, 'tb-scrolled');
    } else {
      this.renderer.removeClass(this.navHeader.nativeElement, 'tb-scrolled');
    }
    const moreBelow = sidebarScrollElm.scrollTop + sidebarScrollElm.clientHeight < sidebarScrollElm.scrollHeight - 1;
    if (moreBelow) {
      this.renderer.addClass(this.sidebarBottom.nativeElement, 'tb-scrolled');
    } else {
      this.renderer.removeClass(this.sidebarBottom.nativeElement, 'tb-scrolled');
    }
  }

  private updateActiveComponent(activeComponent: any) {
    this.showSearch = false;
    this.textSearch.reset('', {emitEvent: false});
    this.activeComponent = activeComponent;

    if (this.activeComponent && instanceOfSearchableComponent(this.activeComponent)) {
      this.searchEnabled = true;
      this.searchableComponent = this.activeComponent;
    } else {
      this.searchEnabled = false;
      this.searchableComponent = null;
    }
  }

  displaySearchMode(): boolean {
    return this.searchEnabled && this.showSearch;
  }

  openSearch() {
    if (this.searchEnabled) {
      this.showSearch = true;
      setTimeout(() => {
        this.searchInputField.nativeElement.focus();
        this.searchInputField.nativeElement.setSelectionRange(0, 0);
      }, 10);
    }
  }

  closeSearch() {
    if (this.searchEnabled) {
      this.showSearch = false;
      if (this.textSearch.value.length) {
        this.textSearch.reset();
      }
    }
  }

  platformNameAndVersion$(): Observable<string> {
    return combineLatest([this.wl.getPlatformName$(), this.wl.getPlatformVersion$()]).pipe(
      map((res) => this.translate.instant('white-labeling.version-mask', {name: res[0], version: res[1]})),
      share()
    );
  }

  private searchTextUpdated(searchText: string) {
    if (this.searchableComponent) {
      this.searchableComponent.onSearchTextUpdated(searchText);
    }
  }
}
