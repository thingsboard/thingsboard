// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { of, Subject } from 'rxjs';
import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UserSettingsService } from '@core/http/user-settings.service';
import { DashboardService } from '@core/http/dashboard.service';
import { HomePermissionsService } from '@home/pages/home-page/home-permissions.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { HelpLinks } from '@shared/models/constants';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { HomeCardId, UserDashboardAction, UserDashboardsInfo } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { homeAgo, homeListShown } from '@home/pages/home-page/home-list.models';
import { HomeToggleOption } from '@home/pages/home-page/components/home-toggle.component';

type DashboardsTab = 'last' | 'starred';

const MINUTE = 60000;
const SEARCH_SIZE = 10;
const FILL_SIZE = 20;

export interface DashboardHomeRow {
  id: string;
  title: string;
  starred: boolean;
  lastVisited: number | null;
  ago: string;
  url: string;
}

interface StarCandidate {
  id: string;
  title: string;
  ago: string | null;
}

@Component({
  selector: 'tb-dashboards-home',
  templateUrl: './dashboards-home.component.html',
  styleUrls: ['./dashboards-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardsHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.DASHBOARDS];

  readonly tabOptions: HomeToggleOption<DashboardsTab>[] = [
    {value: 'last', label: 'home.dashboards.last-viewed'},
    {value: 'starred', label: 'home.dashboards.starred'}
  ];

  readonly howToLink = HelpLinks.linksMap.dashboards;

  private store = inject(Store<AppState>);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private userSettingsService = inject(UserSettingsService);
  private dashboardService = inject(DashboardService);
  private toast = inject(ToastNotificationService);

  private readonly authUser = getCurrentAuthUser(this.store);
  private readonly fullscreen = getCurrentAuthState(this.store).forceFullscreen;

  readonly canAdd = inject(HomePermissionsService).canReadAllEntities(EntityType.DASHBOARD) &&
    inject(HomePermissionsService).canCreateEntities(EntityType.DASHBOARD);
  readonly dashboardsLink = inject(HomePermissionsService).entitiesLink(EntityType.DASHBOARD);
  readonly canUseIotHub = inject(HomePermissionsService).canUseIotHub();

  readonly tab = signal<DashboardsTab>('last');
  readonly expanded = signal(false);
  readonly total = signal<number | null>(null);
  private readonly info = signal<UserDashboardsInfo | null>(null);
  private readonly others = signal<{id: string; title: string}[]>([]);
  private readonly now = signal(Date.now());

  readonly picking = signal(false);
  readonly query = signal('');
  readonly active = signal(0);
  private readonly searchResults = signal<StarCandidate[]>([]);
  private readonly search$ = new Subject<string>();

  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  readonly loaded = computed(() => this.total() !== null && this.info() !== null);

  private readonly starredIds = computed(() => new Set((this.info()?.starred ?? []).map(dashboard => dashboard.id)));

  private readonly visits = computed(() => new Map((this.info()?.last ?? []).map(dashboard => [dashboard.id, dashboard.lastVisited])));

  private readonly allRows = computed<DashboardHomeRow[]>(() => {
    const info = this.info();
    if (!info) {
      return [];
    }
    const now = this.now();
    const starred = this.starredIds();
    const visits = this.visits();
    if (this.tab() === 'last') {
      const viewed = [...info.last]
        .sort((a, b) => b.lastVisited - a.lastVisited)
        .map(dashboard => this.toRow(dashboard.id, dashboard.title, starred.has(dashboard.id), dashboard.lastVisited, now));
      const rest = this.others()
        .filter(dashboard => !visits.has(dashboard.id))
        .map(dashboard => this.toRow(dashboard.id, dashboard.title, starred.has(dashboard.id), null, now));
      return [...viewed, ...rest];
    }
    return [...info.starred]
      .sort((a, b) => (visits.get(b.id) ?? 0) - (visits.get(a.id) ?? 0) || b.starredAt - a.starredAt)
      .map(dashboard => this.toRow(dashboard.id, dashboard.title, true, visits.get(dashboard.id) ?? null, now));
  });

  readonly listed = computed(() => this.tab() === 'last' ? Math.max(this.total() ?? 0, this.allRows().length) : this.allRows().length);

  readonly rows = computed(() => this.allRows().slice(0, homeListShown(this.listed(), this.expanded())));

  readonly allStarred = computed(() => {
    const total = this.total();
    return total !== null && total > 0 && this.starredIds().size >= total;
  });

  readonly candidates = computed<StarCandidate[]>(() => {
    const starred = this.starredIds();
    if (this.query().trim()) {
      return this.searchResults().filter(candidate => !starred.has(candidate.id));
    }
    const now = this.now();
    return [...(this.info()?.last ?? [])]
      .filter(dashboard => !starred.has(dashboard.id))
      .sort((a, b) => b.lastVisited - a.lastVisited)
      .slice(0, SEARCH_SIZE)
      .map(dashboard => ({id: dashboard.id, title: dashboard.title, ago: homeAgo(this.translate, now - dashboard.lastVisited)}));
  });

  constructor() {
    if (this.dashboardsLink && this.authUser?.authority !== Authority.SYS_ADMIN) {
      this.userSettingsService.getUserDashboardsInfo({ignoreLoading: true}).subscribe(info => {
        this.info.set({last: info?.last ?? [], starred: info?.starred ?? []});
      });
      this.dashboardService.getUserDashboards(this.authUser.userId, null,
        new PageLink(FILL_SIZE, 0, null, {property: 'createdTime', direction: Direction.DESC}), {ignoreLoading: true}).pipe(
        catchError(() => of(null))
      ).subscribe(page => {
        this.others.set((page?.data ?? []).map(dashboard => ({id: dashboard.id.id, title: dashboard.title})));
        this.total.set(page?.totalElements ?? 0);
      });
    } else {
      this.info.set({last: [], starred: []});
      this.total.set(0);
    }
    this.search$.pipe(
      debounceTime(200),
      switchMap(query => query
        ? this.dashboardService.getUserDashboards(this.authUser.userId, null,
          new PageLink(SEARCH_SIZE + this.starredIds().size, 0, query, {property: 'title', direction: Direction.ASC}), {ignoreLoading: true}).pipe(
          map(page => page.data.map(dashboard => {
            const visited = this.visits().get(dashboard.id.id);
            return {id: dashboard.id.id, title: dashboard.title, ago: visited ? homeAgo(this.translate, Date.now() - visited) : null};
          })),
          catchError(() => of([]))
        )
        : of([])),
      takeUntilDestroyed()
    ).subscribe(results => {
      this.searchResults.set(results);
      this.active.set(0);
    });
    const tick = setInterval(() => this.now.set(Date.now()), MINUTE);
    inject(DestroyRef).onDestroy(() => clearInterval(tick));
  }

  setTab(tab: DashboardsTab): void {
    this.tab.set(tab);
    this.closePicker();
  }

  open(row: DashboardHomeRow): void {
    this.router.navigateByUrl(row.url);
  }

  toggleStar(row: DashboardHomeRow, $event: Event): void {
    $event.stopPropagation();
    const info = this.info();
    if (!info) {
      return;
    }
    if (row.starred) {
      this.info.set({...info, starred: info.starred.filter(dashboard => dashboard.id !== row.id)});
      this.userSettingsService.reportUserDashboardAction(row.id, UserDashboardAction.UNSTAR, {ignoreLoading: true}).subscribe();
    } else {
      this.star(row.id, row.title);
    }
  }

  openPicker(): void {
    this.query.set('');
    this.searchResults.set([]);
    this.active.set(0);
    this.picking.set(true);
    setTimeout(() => this.searchInput()?.nativeElement.focus());
  }

  closePicker(): void {
    this.picking.set(false);
  }

  onQuery(value: string): void {
    this.query.set(value);
    this.active.set(0);
    this.search$.next(value.trim());
  }

  onPickerKeydown(event: KeyboardEvent): void {
    const count = this.candidates().length;
    if (event.key === 'ArrowDown' && count) {
      event.preventDefault();
      this.active.set((this.active() + 1) % count);
    } else if (event.key === 'ArrowUp' && count) {
      event.preventDefault();
      this.active.set((this.active() - 1 + count) % count);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const candidate = this.candidates()[this.active()];
      if (candidate) {
        this.pick(candidate);
      }
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.closePicker();
    }
  }

  onPickerBlur(): void {
    if (document.hasFocus()) {
      this.closePicker();
    }
  }

  pick(candidate: StarCandidate): void {
    this.closePicker();
    this.star(candidate.id, candidate.title);
    this.toast.dispatchNotification({
      message: this.translate.instant('home.dashboards.starred-toast', {title: candidate.title}),
      type: 'info',
      duration: 4000,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast'
    });
  }

  private star(id: string, title: string): void {
    const info = this.info();
    if (!info || this.starredIds().has(id)) {
      return;
    }
    this.info.set({...info, starred: [...info.starred, {id, title, starred: true, starredAt: Date.now()}]});
    this.userSettingsService.reportUserDashboardAction(id, UserDashboardAction.STAR, {ignoreLoading: true}).subscribe();
  }

  private toRow(id: string, title: string, starred: boolean, lastVisited: number | null, now: number): DashboardHomeRow {
    return {
      id,
      title,
      starred,
      lastVisited,
      ago: lastVisited ? homeAgo(this.translate, now - lastVisited) : '',
      url: (this.fullscreen ? '/dashboard/' : '/dashboards/all/') + id
    };
  }
}
