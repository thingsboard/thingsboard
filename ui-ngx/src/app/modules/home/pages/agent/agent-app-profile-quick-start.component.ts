// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { AgentService } from '@core/http/agent.service';
import { PageLink } from '@shared/models/page/page-link';
import {
  AgentAppConfigType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  VirtualAgentAppProfile
} from '@shared/models/agent.models';
import { buildVirtualAppProfiles, VIRTUAL_APP_TYPES } from '@home/pages/agent/util/virtual-app-profiles';
import { EdgeTemplateCompatibilityService } from '@home/pages/agent/util/edge-template-compatibility.service';

type QuickStartFilter = 'ALL' | AgentApplicationType;

interface QuickStartData {
  entries: VirtualAgentAppProfile[];
  hasTemplates: boolean;
}

// "Quick start — predefined profiles" banner shown above the application profiles table:
// one card per template version the tenant has no profile for yet, created in one click
// via the materialize endpoint.
@Component({
  selector: 'tb-agent-app-profile-quick-start',
  templateUrl: './agent-app-profile-quick-start.component.html',
  styleUrls: ['./agent-app-profile-quick-start.component.scss'],
  animations: [
    trigger('cardAnim', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('200ms 80ms ease', style({ opacity: 1 }))
      ]),
      transition(':leave', [
        animate('250ms cubic-bezier(0.4, 0, 0.2, 1)', style({
          width: '0px',
          marginRight: '0px',
          opacity: 0,
          paddingLeft: '0px',
          paddingRight: '0px',
          borderLeftWidth: '0px',
          borderRightWidth: '0px'
        }))
      ])
    ]),
    trigger('fadeAnim', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('200ms 100ms ease', style({ opacity: 1 }))
      ])
    ])
  ],
  standalone: false
})
export class AgentAppProfileQuickStartComponent implements OnInit, OnDestroy {

  private static readonly PROFILES_PAGE_SIZE = 1024;

  @Input() reloadTrigger: Observable<void>;
  @Output() profileCreated = new EventEmitter<AgentAppProfile>();

  loaded = false;
  hasTemplates = false;
  activeFilter: QuickStartFilter = 'ALL';
  entries: VirtualAgentAppProfile[] = [];

  readonly filters: Array<{ key: QuickStartFilter; label: string }> = [
    { key: 'ALL', label: 'agent.quick-start-filter-all' },
    { key: AgentApplicationType.EDGE, label: 'agent.app-type-edge' },
    { key: AgentApplicationType.GATEWAY, label: 'agent.app-type-gateway' }
  ];

  private creatingVersions = new Set<string>();
  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private edgeTemplateCompatibility: EdgeTemplateCompatibilityService,
              private translate: TranslateService) {
  }

  ngOnInit() {
    if (this.reloadTrigger) {
      this.reloadTrigger.pipe(takeUntil(this.destroy$)).subscribe(() => this.reload$.next());
    }
    this.reload$.pipe(
      startWith(null),
      debounceTime(200),
      switchMap(() => this.fetchEntries()),
      takeUntil(this.destroy$)
    ).subscribe(data => {
      this.entries = data.entries;
      this.hasTemplates = data.hasTemplates;
      this.loaded = true;
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get visibleEntries(): VirtualAgentAppProfile[] {
    return this.activeFilter === 'ALL'
      ? this.entries
      : this.entries.filter(entry => entry.appType === this.activeFilter);
  }

  get allCreated(): boolean {
    return this.loaded && this.hasTemplates && this.entries.length === 0;
  }

  get noMatch(): boolean {
    return this.entries.length > 0 && this.visibleEntries.length === 0;
  }

  appIcon(appType: AgentApplicationType): string {
    switch (appType) {
      case AgentApplicationType.EDGE:
        return 'router';
      case AgentApplicationType.GATEWAY:
        return 'hub';
      default:
        return 'inventory_2';
    }
  }

  isCreating(entry: VirtualAgentAppProfile): boolean {
    return this.creatingVersions.has(entry.templateVersion);
  }

  create(entry: VirtualAgentAppProfile) {
    if (this.isCreating(entry)) {
      return;
    }
    this.creatingVersions.add(entry.templateVersion);
    this.agentService.materializeAgentAppProfile(entry.appType, entry.templateVersion, entry.defaultComposeType)
      .subscribe({
        next: profile => {
          this.creatingVersions.delete(entry.templateVersion);
          this.entries = this.entries.filter(e => e.templateVersion !== entry.templateVersion);
          this.store.dispatch(new ActionNotificationShow({
            message: this.translate.instant('agent.quick-start-profile-created', { profileName: profile.name }),
            type: 'success',
            duration: 2000,
            verticalPosition: 'bottom',
            horizontalPosition: 'right'
          }));
          this.profileCreated.emit(profile);
        },
        error: () => this.creatingVersions.delete(entry.templateVersion)
      });
  }

  private fetchEntries(): Observable<QuickStartData> {
    const requestConfig = { ignoreLoading: true, ignoreErrors: true };
    return forkJoin([
      this.agentService.getTenantAgentAppProfiles(
        new PageLink(AgentAppProfileQuickStartComponent.PROFILES_PAGE_SIZE), requestConfig
      ).pipe(catchError(() => of(null))),
      ...VIRTUAL_APP_TYPES.map(type =>
        this.agentService.getAgentAppTemplatesByAppType(type, AgentAppConfigType.DOCKER_COMPOSE, requestConfig)
          .pipe(catchError(() => of([] as AgentAppTemplate[]))))
    ]).pipe(
      map(([profilesPage, ...templateLists]) => {
        const profiles = profilesPage?.data || [];
        const entries: VirtualAgentAppProfile[] = [];
        let hasTemplates = false;
        VIRTUAL_APP_TYPES.forEach((type, i) => {
          const templates = this.edgeTemplateCompatibility.filterTemplates(templateLists[i] as AgentAppTemplate[]);
          hasTemplates = hasTemplates || templates.length > 0;
          entries.push(...buildVirtualAppProfiles(templates, profiles.filter(p => p.appType === type)));
        });
        return { entries, hasTemplates };
      })
    );
  }
}
