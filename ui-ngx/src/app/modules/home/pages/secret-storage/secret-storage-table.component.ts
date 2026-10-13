// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CellActionDescriptor,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { forkJoin, Observable, of } from 'rxjs';
import { TbPopoverService } from '@shared/components/popover.service';
import { catchError, map } from 'rxjs/operators';
import { parseHttpErrorMessage } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  SecretResourceInfo,
  SecretStorage,
  SecretStorageInfo,
  SecretStorageType,
  secretStorageTypeTranslationMap,
  toSecretDeleteResult
} from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { SecretStorageTableHeaderComponent } from '@home/pages/secret-storage/secret-storage-table-header.component';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import {
  EditSecretDescriptionPanelComponent
} from '@home/pages/secret-storage/edit-secret-description-panel.component';
import {
  ResourcesInUseDialogComponent,
  ResourcesInUseDialogData
} from '@shared/components/resource/resources-in-use-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { SecretsDatasource } from '@shared/components/secret-storage/secrets-datasource';
import {
  EditSecretValueDialogComponent,
  EditSecretValueDialogData
} from '@home/pages/secret-storage/edit-secret-value-dialog.component';
import {
  SecretStorageData,
  SecretStorageDialogComponent
} from '@shared/components/secret-storage/secret-storage-dialog.component';
import { Direction } from '@shared/models/page/sort-order';

@Component({
    selector: 'tb-secret-storage-table',
    templateUrl: './secret-storage-table.component.html',
    styleUrls: ['./secret-storage-table.component.scss'],
    standalone: false
})
export class SecretStorageTableComponent implements OnInit {

  readonly secretStorageTableConfig = new EntityTableConfig<SecretStorage>();

  constructor(private secretStorageService: SecretStorageService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private customTranslate: CustomTranslatePipe) {
  }

  ngOnInit() {
    this.secretStorageTableConfig.tableTitle = '';
    this.secretStorageTableConfig.entityType = EntityType.SECRET;
    this.secretStorageTableConfig.headerComponent = SecretStorageTableHeaderComponent;
    this.secretStorageTableConfig.detailsPanelEnabled = false;
    this.secretStorageTableConfig.searchEnabled = true;
    this.secretStorageTableConfig.selectionEnabled = true;
    this.secretStorageTableConfig.deleteEnabled = () => false;
    this.secretStorageTableConfig.entityTranslations = entityTypeTranslations.get(EntityType.SECRET);
    this.secretStorageTableConfig.entityResources = entityTypeResources.get(EntityType.SECRET);
    this.secretStorageTableConfig.entitiesFetchFunction = pageLink => this.secretStorageService.getSecrets(pageLink);
    this.secretStorageTableConfig.defaultSortOrder = {property: 'name', direction: Direction.ASC}
    this.secretStorageTableConfig.onEntityAction = action => this.onSecretAction(action);
    this.secretStorageTableConfig.entitiesDeleteEnabled = false;
    this.secretStorageTableConfig.saveEntity = secret => this.secretStorageService.saveSecret(secret as SecretStorageInfo);
    const readonly = !this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.WRITE);
    const allowDelete = this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.DELETE);
    this.secretStorageTableConfig.addEnabled = this.userPermissionsService.hasGenericPermission(Resource.SECRET, Operation.CREATE);
    this.secretStorageTableConfig.addEntity = () => this.addSecret();

    this.secretStorageTableConfig.cellActionDescriptors = this.configureCellActions(allowDelete, readonly);

    this.secretStorageTableConfig.groupActionDescriptors = [{
      name: this.translate.instant('action.delete'),
      icon: 'delete',
      isEnabled: allowDelete,
      onAction: ($event, entities) => this.deleteSecrets($event, entities)
    }];

    this.secretStorageTableConfig.columns.push(
      new EntityTableColumn<SecretStorage>('name', 'secret-storage.name', '40%', this.secretStorageTableConfig.entityTitle),
      new EntityTableColumn<SecretStorage>('type', 'secret-storage.type', '150px', (secret) => {
        return this.translate.instant(secretStorageTypeTranslationMap.get(secret.type));
      }),
      new EntityTableColumn<SecretStorage>('description', 'secret-storage.description', '60%',
        (secret) => secret?.description ? this.customTranslate.transform(secret?.description) : this.translate.instant('secret-storage.set-description'),
        (secret) => (!secret?.description ? {color: 'rgba(0, 0, 0, 0.38)'} : {}), false, () => ({}),
        (secret) => secret?.description?.length > 80 ? this.customTranslate.transform(secret?.description) : undefined, false,
        {
          name: this.translate.instant('secret-storage.edit-description'),
          icon: 'edit',
          isEnabled: () => !readonly,
          onAction: ($event, entity) => this.updateSecretDescription($event, entity)
        })
    );
  }

  private onSecretAction(action: EntityAction<SecretStorage>): boolean {
    switch (action.action) {
      case 'deleteSecret':
        this.deleteSecret(action.event, action.entity);
    }
    return false;
  }

  private configureCellActions(allowDelete: boolean, readonly: boolean): Array<CellActionDescriptor<SecretStorage>> {
    const actions: Array<CellActionDescriptor<SecretStorage>> = [];
    actions.push(
      {
        name: this.translate.instant('secret-storage.change-value'),
        icon: 'lock_reset',
        isEnabled: () => !readonly,
        onAction: ($event, entity) => this.changeValue($event, entity)
      },
    );
    actions.push(
      {
        name: this.translate.instant('action.delete'),
        icon: 'delete',
        isEnabled: () => allowDelete,
        onAction: ($event, entity) => this.deleteSecret($event, entity)
      },
    );
    return actions;
  }

  private changeValue($event: Event, entity: SecretStorage) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EditSecretValueDialogComponent, EditSecretValueDialogData, string>(EditSecretValueDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        type: entity.type
      }
    }).afterClosed()
      .subscribe((value) => {
        if (value) {
          this.secretStorageService.updateSecretValue(entity.id.id, value).subscribe();
        }
      });
  }

  private addSecret(): Observable<SecretStorage> {
    return this.dialog.open<SecretStorageDialogComponent, SecretStorageData, SecretStorage>(SecretStorageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        type: SecretStorageType.TEXT,
        value: null,
        fileName: null,
        hideType: false,
        onlyCreateNew: true
      }
    }).afterClosed().pipe(map(res => {
      if (res) {
        this.secretStorageTableConfig.updateData();
      } else {
        return null;
      }
    }));
  }

  private updateSecretDescription($event: Event, secret: SecretStorage) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = ($event.target || $event.srcElement || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const editSecretDescriptionPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EditSecretDescriptionPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['right', 'bottom', 'top'],
        context: {
          secretId: secret.id.id,
          description: secret.description
        },
        isModal: true
      });
      editSecretDescriptionPanelPopover.tbComponentRef.instance.descriptionApplied.subscribe(() => {
        editSecretDescriptionPanelPopover.hide();
        this.secretStorageTableConfig.updateData();
      });
    }
  }

  private deleteSecret($event: Event, secret: SecretStorage) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('secret-storage.delete-secret-title', { secretName: secret.name });
    const content = this.translate.instant('secret-storage.delete-secret-text');
    this.dialogService.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe((result) => {
      if (result) {
        this.secretStorageService.deleteSecret(secret.id.id, {ignoreErrors: true}).pipe(
          map(() => toSecretDeleteResult(secret)),
          catchError((err) => of(toSecretDeleteResult(secret, err)))
        ).subscribe(
          (deleteResult) => {
            if (deleteResult.success) {
              this.secretStorageTableConfig.updateData();
            } else if (deleteResult.resourceIsReferencedError) {
              const secrets = [{...secret, ...{references: deleteResult.references}}];
              const data = {
                multiple: false,
                resources: secrets,
                configuration: {
                  title: 'secret-storage.used-secret-title',
                  message: this.translate.instant('secret-storage.secrets-is-in-use-text'),
                  deleteText: 'secret-storage.delete-secrets-in-use-text',
                  selectedText: 'secret-storage.selected-secrets',
                  columns: ['name', 'references']
                },
                allowForceDelete: false
              };
              this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                SecretResourceInfo[]>(ResourcesInUseDialogComponent, {
                disableClose: true,
                panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                data
              }).afterClosed();
            } else {
              const errorMessageWithTimeout = parseHttpErrorMessage(deleteResult.error, this.translate);
              setTimeout(() => {
                this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
              }, errorMessageWithTimeout.timeout);
            }
          });
      }
    });
  }

  private deleteSecrets($event: Event, entities: SecretStorage[]) {
    if (entities && entities.length === 1) {
      this.deleteSecret($event, entities[0]);
    } else {
      if ($event) {
        $event.stopPropagation();
      }
      if (entities && entities.length) {
        const title = this.translate.instant('secret-storage.delete-secrets-title', {count: entities.length});
        const content = this.translate.instant('secret-storage.delete-secrets-text');
        this.dialogService.confirm(title, content,
          this.translate.instant('action.no'),
          this.translate.instant('action.yes')).subscribe((result) => {
          if (result) {
            const tasks = entities.map((secret) =>
              this.secretStorageService.deleteSecret(secret.id.id, {ignoreErrors: true}).pipe(
                map(() => toSecretDeleteResult(secret)),
                catchError((err) => of(toSecretDeleteResult(secret, err)))
              )
            );
            forkJoin(tasks).subscribe(
              (deleteResults) => {
                const anySuccess = deleteResults.some(res => res.success);
                const referenceErrors = deleteResults.filter(res => res.resourceIsReferencedError);
                const otherError = deleteResults.find(res => !res.success);
                if (anySuccess) {
                  this.secretStorageTableConfig.updateData();
                }
                if (referenceErrors?.length) {
                  const secretsWithReferences =
                    referenceErrors.map(ref => ({...ref.resource, ...{references: ref.references}}));
                  const data = {
                    multiple: true,
                    resources: secretsWithReferences,
                    configuration: {
                      title: 'secret-storage.used-secret-title',
                      message: this.translate.instant('secret-storage.secrets-are-in-use-text'),
                      deleteText: 'secret-storage.delete-secrets-in-use-text',
                      selectedText: 'secret-storage.selected-secrets',
                      columns: ['name', 'references'],
                      datasource: new SecretsDatasource(null, secretsWithReferences, entity => true)
                    },
                    allowForceDelete: false
                  };
                  this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                    SecretResourceInfo[]>(ResourcesInUseDialogComponent, {
                    disableClose: true,
                    panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                    data
                  }).afterClosed();
                } else if (otherError) {
                  const errorMessageWithTimeout = parseHttpErrorMessage(otherError.error, this.translate);
                  setTimeout(() => {
                    this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
                  }, errorMessageWithTimeout.timeout);
                }
              }
            );
          }
        });
      }
    }
  }
}
