// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injector, StaticProvider, ViewContainerRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import {
  DateEntityTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import {
  AgentAppEvent,
  agentAppEventActionTypeTranslationMap,
  AgentApplication,
  AgentProcessingStatus,
  agentProcessingStatusTranslationMap,
  isAgentScopedAppEventActionType,
  processingStartStatusTranslationMap
} from '@shared/models/agent.models';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';
import {
  AGENT_APP_EVENT_FILTER_PANEL_DATA,
  AgentAppEventFilterPanelComponent,
  AgentAppEventFilterPanelData,
  AgentAppEventFilterValue
} from './agent-app-event-filter-panel.component';

/**
 * Shared shape of the agent event tables (per agent, per application, per bulk action):
 * action / execution / status / time columns, the cancel-or-show-error cell action,
 * the filter header actions and the row click that opens the progress dialog.
 * Subclasses add their leading columns and say how events are fetched and how the
 * application behind an event is resolved.
 */
export abstract class AbstractAgentAppEventTableConfig<E extends AgentAppEvent> extends EntityTableConfig<E> {

  protected filter: AgentAppEventFilterValue = { actionType: null, processingStatus: null };

  protected constructor(protected readonly agentService: AgentService,
                        protected readonly dialogService: DialogService,
                        protected readonly dialog: MatDialog,
                        protected readonly translate: TranslateService,
                        protected readonly datePipe: DatePipe,
                        protected readonly overlay: Overlay,
                        protected readonly viewContainerRef: ViewContainerRef,
                        noEntitiesKey: string) {
    super();

    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.entityTranslations = { noEntities: noEntitiesKey };
    this.entityResources = {} as EntityTypeResource<E>;

    this.cellActionDescriptors.push({
      name: this.translate.instant('agent.app-event-cancel'),
      nameFunction: (e) => this.isErrorRow(e)
        ? this.translate.instant('agent.app-event-show-error')
        : this.translate.instant('agent.app-event-cancel'),
      icon: 'cancel',
      iconFunction: (e) => {
        if (this.isErrorRow(e)) { return 'more_horiz'; }
        if (this.canCancel(e))  { return 'cancel'; }
        return '';
      },
      style: {},
      isEnabled: (e) => this.canCancel(e) || this.isErrorRow(e),
      onAction: ($event, e) => {
        if (this.isErrorRow(e)) {
          this.showEventError($event, e);
        } else if (this.canCancel(e)) {
          this.cancelEvent($event, e);
        }
      }
    });

    this.headerActionDescriptors.push(
      {
        name: this.translate.instant('agent.app-event-filter'),
        icon: 'filter_list',
        isEnabled: () => true,
        onAction: ($event) => this.openFilterPanel($event)
      },
      {
        name: this.translate.instant('action.clear'),
        icon: 'mdi:filter-variant-remove',
        isEnabled: () => this.hasActiveFilter(),
        onAction: () => this.clearFilter()
      }
    );

    this.entitiesFetchFunction = (pageLink) => this.fetchEvents(pageLink);
    this.handleRowClick = ($event, e) => this.onRowClick($event, e);
  }

  protected abstract fetchEvents(pageLink: PageLink): Observable<PageData<E>>;

  protected abstract resolveApplication(e: E): Observable<AgentApplication | null>;

  /**
   * In-flight rows carry a marker span in the status cell; the wrapping components'
   * SCSS uses `:has()` on it to paint the whole row without touching the shared table.
   */
  protected eventColumns(): Array<EntityColumn<E>> {
    return [
      new EntityTableColumn<E>('actionType',
        'agent.app-event-action', '140px',
        (e) => this.translateKey(agentAppEventActionTypeTranslationMap.get(e.actionType) || e.actionType),
        () => ({}), true),
      new EntityTableColumn<E>('startStatus',
        'agent.app-event-execution', '140px',
        (e) => this.translateKey(processingStartStatusTranslationMap.get(e.startStatus)),
        () => ({}), true),
      new EntityTableColumn<E>('processingStatus',
        'agent.app-event-status', '160px',
        (e) => {
          const label = this.translateKey(agentProcessingStatusTranslationMap.get(e.processingStatus) || e.processingStatus);
          if (this.canCancel(e)) {
            return `<span class="tb-agent-app-event-inflight">${label}</span>`;
          }
          if (this.isFailure(e)) {
            return `<span class="tb-agent-app-event-error">${label}</span>`;
          }
          return label;
        },
        () => ({}), true),
      new DateEntityTableColumn<E>('createdTime',
        'agent.app-event-created', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss'),
      new DateEntityTableColumn<E>('updatedTime',
        'agent.app-event-updated', this.datePipe, '180px', 'yyyy-MM-dd HH:mm:ss')
    ];
  }

  protected applicationCell(e: E & { applicationName?: string }): string {
    if (e.applicationName) {
      return e.applicationName;
    }
    return isAgentScopedAppEventActionType(e.actionType) ? '' : this.translate.instant('agent.app-deleted');
  }

  protected applicationById(e: E): Observable<AgentApplication | null> {
    if (!e.applicationId?.id) {
      return of(null);
    }
    return this.agentService.getAgentApplicationInfoById(e.applicationId.id, { ignoreErrors: true }).pipe(
      catchError(() => of(null))
    );
  }

  protected canCancel(e: E): boolean {
    return e.processingStatus == null
      || e.processingStatus === AgentProcessingStatus.PENDING
      || e.processingStatus === AgentProcessingStatus.QUEUED
      || e.processingStatus === AgentProcessingStatus.PROCESSING;
  }

  protected isFailure(e: E): boolean {
    return e.processingStatus === AgentProcessingStatus.ERROR || e.processingStatus === AgentProcessingStatus.START_FAILED;
  }

  protected isErrorRow(e: E): boolean {
    return this.isFailure(e) && !!e.errorMessage;
  }

  private translateKey(key: string | undefined): string {
    return key ? this.translate.instant(key) : '';
  }

  private onRowClick($event: Event, e: E): boolean {
    if (!this.canCancel(e) || !e.applicationId) {
      return false;
    }
    if ($event) { $event.stopPropagation(); }
    this.resolveApplication(e).subscribe(application => this.openProgress(application, e));
    return true;
  }

  private openProgress(application: AgentApplication | null, event: E): void {
    this.dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
      AgentAppEventProgressDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: { application, event }
      }
    ).afterClosed().subscribe(() => this.updateData());
  }

  private cancelEvent($event: Event, e: E): void {
    if ($event) { $event.stopPropagation(); }
    if (!e.applicationId) { return; }
    this.dialogService.confirm(
      this.translate.instant('agent.app-event-cancel-title'),
      this.translate.instant('agent.app-event-cancel-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.cancelAgentAppEvent(e.applicationId.id, e.id.id).subscribe(() => this.updateData());
      }
    });
  }

  private showEventError($event: Event, e: E): void {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.alert(
      this.translate.instant('agent.app-event-error-title'),
      e.errorMessage || ''
    );
  }

  private hasActiveFilter(): boolean {
    return !!(this.filter.actionType || this.filter.processingStatus);
  }

  private clearFilter(): void {
    if (!this.hasActiveFilter()) { return; }
    this.filter = { actionType: null, processingStatus: null };
    this.getTable().paginator.pageIndex = 0;
    this.updateData();
  }

  private openFilterPanel($event: MouseEvent): void {
    if ($event) { $event.stopPropagation(); }
    const target = ($event.target || $event.currentTarget) as HTMLElement;
    const config = new OverlayConfig({
      panelClass: 'tb-panel-container',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      height: 'fit-content',
      maxHeight: '65vh'
    });
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(target)
      .withPositions([
        { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top' },
        { originX: 'end',   originY: 'bottom', overlayX: 'end',   overlayY: 'top' }
      ]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => overlayRef.dispose());

    const providers: StaticProvider[] = [
      {
        provide: AGENT_APP_EVENT_FILTER_PANEL_DATA,
        useValue: { value: { ...this.filter } } as AgentAppEventFilterPanelData
      },
      { provide: OverlayRef, useValue: overlayRef }
    ];
    const injector = Injector.create({ parent: this.viewContainerRef.injector, providers });
    const ref = overlayRef.attach(new ComponentPortal(
      AgentAppEventFilterPanelComponent, this.viewContainerRef, injector));
    ref.onDestroy(() => {
      const result = ref.instance.result;
      if (result && (result.actionType !== this.filter.actionType || result.processingStatus !== this.filter.processingStatus)) {
        this.filter = result;
        this.getTable().paginator.pageIndex = 0;
        this.updateData();
      }
    });
  }
}
