// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

import { Router } from '@angular/router';
import {
  CellActionDescriptor,
  CellActionDescriptorType,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  Extension,
  extensionFullUrl,
  ExtensionState,
  extensionStateBackgroundColors,
  extensionStateColors,
  extensionStateTranslations,
  extensionUrlPath
} from '@shared/models/extension.models';
import { ExtensionService } from '@core/http/extension.service';
import { ExtensionComponent } from '@home/pages/extension/extension.component';
import { ExtensionQuotaPanelComponent } from '@home/pages/extension/extension-quota-panel.component';
import { ExtensionQuotaService } from '@home/pages/extension/extension-quota.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { Direction } from '@shared/models/page/sort-order';
import { tap } from 'rxjs/operators';

@Injectable()
export class ExtensionsTableConfigResolver {

  private readonly config: EntityTableConfig<Extension> = new EntityTableConfig<Extension>();

  constructor(private extensionService: ExtensionService,
              private quotaService: ExtensionQuotaService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private router: Router,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.EXTENSION;
    this.config.entityComponent = ExtensionComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.EXTENSION);
    this.config.entityResources = entityTypeResources.get(EntityType.EXTENSION);

    this.config.addDialogStyle = {width: '850px'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.entityTitle = (extension) => extension ? extension.name : '';
    this.config.footerComponent = ExtensionQuotaPanelComponent;

    this.config.columns.push(
      new DateEntityTableColumn<Extension>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Extension>('name', 'extension.name', '20%', this.config.entityTitle),
      // only the path is shown, to keep the cell on a single line (same idea as the OTA "direct URL"
      // column); the tooltip and the copy button still carry the full URL with the domain
      new EntityTableColumn<Extension>('url', 'extension.url', '25%', (extension) => extensionUrlPath(extension),
        () => ({'text-wrap': 'nowrap'}), false, () => ({}), (extension) => extensionFullUrl(extension), false,
        {
          name: this.translate.instant('extension.copyUrl'),
          icon: 'content_paste',
          style: {
            padding: '4px',
            'font-size': '16px',
            color: 'rgba(0,0,0,.87)'
          },
          isEnabled: (extension) => !!extensionFullUrl(extension),
          onAction: (_$event, entity) => extensionFullUrl(entity),
          type: CellActionDescriptorType.COPY_BUTTON
        }),
      new EntityTableColumn<Extension>('state', 'extension.state', '160px', (extension) => this.stateCell(extension)),
      new EntityTableColumn<Extension>('image', 'extension.docker-image', '25%',
        (extension) => extension.containerSpec ? `${extension.containerSpec.image}:${extension.containerSpec.tag}` : '',
        () => ({}), false),
      new EntityTableColumn<Extension>('replicas', 'extension.replicas-ready', '110px',
        (extension) => this.readyReplicasCell(extension),
        () => ({}), false)
    );

    this.config.deleteEntityTitle = extension =>
      this.translate.instant('extension.delete-extension-name-title', {extensionName: extension.name});
    // the full entity (details view) has no server-computed url — extensionFullUrl falls back to the slug
    this.config.deleteEntityContent = extension =>
      this.translate.instant('extension.delete-extension-text', {url: extensionFullUrl(extension)});
    this.config.deleteEntitiesTitle = count => this.translate.instant('extension.delete-extensions-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('extension.delete-extensions-text');
    // the quota panel under the table shows what is left, so refresh it whenever the rows change
    this.config.entitiesFetchFunction = pageLink => this.extensionService.getExtensions(pageLink).pipe(
      tap(() => this.quotaService.reload())
    );
    this.config.loadEntity = id => this.extensionService.getExtension(id.id);
    this.config.saveEntity = extension => this.extensionService.saveExtension(extension);
    this.config.deleteEntity = id => this.extensionService.deleteExtension(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.onEntityAction = action => this.onExtensionAction(action);
  }

  resolve(): EntityTableConfig<Extension> {
    this.config.tableTitle = this.translate.instant('extension.extensions');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    // Extensions are managed via the API / tb CLI, not from the UI table.
    // Hide the "Add extension" button and make the details panel read-only
    // (no edit/pencil mode; every field is view-only). Delete stays available.
    this.config.addEnabled = false;
    this.config.detailsReadonly = () => true;
    return this.config;
  }

  private stateCell(extension: Extension): string {
    const state = extension.state;
    if (!state) {
      return '';
    }
    const color = extensionStateColors.get(state);
    const background = extensionStateBackgroundColors.get(state);
    const label = this.translate.instant(extensionStateTranslations.get(state));
    return `<span style="display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 500;
      color: ${color}; background-color: ${background};">${label}</span>`;
  }

  private readyReplicasCell(extension: Extension): string {
    const ready = extension.readyReplicas !== null && extension.readyReplicas !== undefined ? extension.readyReplicas : '-';
    const desired = extension.desiredReplicas !== null && extension.desiredReplicas !== undefined
      ? extension.desiredReplicas
      : (extension.containerSpec ? extension.containerSpec.replicas : '-');
    return `${ready} / ${desired}`;
  }

  // A STOPPED extension is the only state you START from (backend rejects start otherwise);
  // every other non-DELETING state is running (or trying to) and can be STOPPED — including
  // FAILED and DEGRADED, so a broken extension can always be scaled back to zero.
  private isStopped(extension: Extension): boolean {
    return extension.state === ExtensionState.STOPPED;
  }

  // Restart (roll the pods) only makes sense where a deployment exists: ACTIVE, or a broken one
  // you want to retry (DEGRADED/FAILED). Not for STOPPED (nothing to roll — use Start),
  // transient states (PROVISIONING/UPDATING), or DELETING.
  private canRestart(extension: Extension): boolean {
    return extension.state === ExtensionState.ACTIVE
      || extension.state === ExtensionState.DEGRADED
      || extension.state === ExtensionState.FAILED;
  }

  // start/stop/restart all map to Operation.WRITE on the backend; hide them for read-only users so
  // they do not see enabled actions that would only return 403. Evaluated per call, not cached: the
  // resolver is a single instance for the SPA lifetime and logout does not reload the page, so a
  // snapshot taken in the constructor outlives the session it was taken in.
  private get canWrite(): boolean {
    return this.userPermissionsService.hasGenericPermission(Resource.EXTENSION, Operation.WRITE);
  }

  private configureCellActions(): Array<CellActionDescriptor<Extension>> {
    return [
      {
        // single Start/Stop toggle: Start only from STOPPED, Stop from any other non-DELETING state
        name: this.translate.instant('extension.start'),
        nameFunction: (extension) => this.translate.instant(this.isStopped(extension) ? 'extension.start' : 'extension.stop'),
        icon: 'play_arrow',
        iconFunction: (extension) => this.isStopped(extension) ? 'play_arrow' : 'stop',
        isEnabled: (extension) => this.canWrite && extension.state !== ExtensionState.DELETING,
        onAction: ($event, entity) => this.toggleExtensionState($event, entity)
      },
      {
        name: this.translate.instant('extension.restart'),
        icon: 'restart_alt',
        isEnabled: (extension) => this.canWrite && this.canRestart(extension),
        onAction: ($event, entity) => this.restartExtension($event, entity)
      }
    ];
  }

  private toggleExtensionState($event: Event, extension: Extension): void {
    $event?.stopPropagation();
    const action$ = this.isStopped(extension)
      ? this.extensionService.startExtension(extension.id.id)
      : this.extensionService.stopExtension(extension.id.id);
    action$.subscribe(() => this.config.updateData());
  }

  private restartExtension($event: Event, extension: Extension): void {
    $event?.stopPropagation();
    this.extensionService.restartExtension(extension.id.id).subscribe(() => this.config.updateData());
  }

  private openExtension($event: Event, extension: Extension) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['extensions', extension.id.id]);
    this.router.navigateByUrl(url);
  }

  onExtensionAction(action: EntityAction<Extension>): boolean {
    switch (action.action) {
      case 'open':
        this.openExtension(action.event, action.entity);
        return true;
    }
    return false;
  }

}
