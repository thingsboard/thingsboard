// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { AgentAppProfile } from '@shared/models/agent.models';

// Application profiles list page: the resolved entities table with the predefined-profiles
// quick-start panel above it.
@Component({
  selector: 'tb-agent-app-profiles-page',
  template: `
    <div class="tb-app-profiles-page">
      @if (entitiesTableConfig?.addEnabled) {
        <div class="tb-app-profiles-page-quick-start"
          [class.collapsed]="detailsOpen">
          <div class="tb-app-profiles-page-quick-start-inner">
            <tb-agent-app-profile-quick-start
              [reloadTrigger]="tableDataFetched$"
              (profileCreated)="onProfileCreated()">
            </tb-agent-app-profile-quick-start>
          </div>
        </div>
      }
      @if (entitiesTableConfig) {
        <tb-entities-table
          [entitiesTableConfig]="entitiesTableConfig"
          class="tb-app-profiles-page-table">
        </tb-entities-table>
      }
    </div>
    `,
  styleUrls: ['./agent-app-profiles-page.component.scss'],
  standalone: false
})
export class AgentAppProfilesPageComponent implements OnInit, OnDestroy {

  @ViewChild(EntitiesTableComponent) private entitiesTable: EntitiesTableComponent;

  entitiesTableConfig: EntityTableConfig<AgentAppProfile> | null = null;

  readonly tableDataFetched$ = new Subject<void>();

  get detailsOpen(): boolean {
    return !!this.entitiesTable?.isDetailsOpen;
  }

  private readonly destroy$ = new Subject<void>();
  private wrappedConfig: EntityTableConfig<AgentAppProfile> | null = null;
  private originalFetchFunction: EntityTableConfig<AgentAppProfile>['entitiesFetchFunction'] | null = null;

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      const config = data.entitiesTableConfig as EntityTableConfig<AgentAppProfile> | undefined;
      if (!config) {
        return;
      }
      this.unwrapFetchFunction();
      this.wrappedConfig = config;
      this.originalFetchFunction = config.entitiesFetchFunction;
      config.entitiesFetchFunction = pageLink => this.originalFetchFunction(pageLink).pipe(
        tap(() => this.tableDataFetched$.next())
      );
      this.entitiesTableConfig = config;
    });
  }

  ngOnDestroy(): void {
    this.unwrapFetchFunction();
    this.destroy$.next();
    this.destroy$.complete();
    this.tableDataFetched$.complete();
  }

  private unwrapFetchFunction(): void {
    if (this.wrappedConfig) {
      this.wrappedConfig.entitiesFetchFunction = this.originalFetchFunction;
      this.wrappedConfig = null;
      this.originalFetchFunction = null;
    }
  }

  onProfileCreated(): void {
    this.entitiesTableConfig?.updateData();
  }
}
