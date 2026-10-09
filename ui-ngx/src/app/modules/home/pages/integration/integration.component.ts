// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  Integration,
  IntegrationInfo,
  IntegrationType,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { isDefined } from '@core/utils';
import { ConverterType } from '@shared/models/converter.models';
import { IntegrationService } from '@core/http/integration.service';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType } from '@shared/models/entity-type.models';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { DebugEventType } from '@shared/models/event.models';
import { MatDialog } from '@angular/material/dialog';
import { EventsDialogComponent, EventsDialogData } from '@home/dialogs/events-dialog.component';

@Component({
    selector: 'tb-integration',
    templateUrl: './integration.component.html',
    styleUrls: ['./integration.component.scss'],
    standalone: false
})
export class IntegrationComponent extends EntityComponent<Integration, PageLink, IntegrationInfo> implements OnInit {

  converterType = ConverterType;

  integrationScope: 'tenant' | 'edges' | 'edge';

  EntityType = EntityType;

  private integrationType: IntegrationType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Integration,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Integration, PageLink, IntegrationInfo>,
              protected fb: UntypedFormBuilder,
              protected integrationService: IntegrationService,
              protected cd: ChangeDetectorRef,
              private dialog: MatDialog) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.integrationScope = this.entitiesTableConfig.componentsData.integrationScope
      ? this.entitiesTableConfig.componentsData.integrationScope : 'tenant';
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Integration): UntypedFormGroup {
    this.integrationType = entity ? entity.type : null;
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
        type: [{value: this.integrationType, disabled: true}, [Validators.required]],
        enabled: [isDefined(entity?.enabled) ? entity.enabled : true],
        debugSettings: [entity?.debugSettings ?? { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 }],
        allowCreateDevicesOrAssets: [entity && isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true],
        defaultConverterId: [entity ? entity.defaultConverterId : null, [Validators.required]],
        downlinkConverterId: [entity ? entity.downlinkConverterId : null, []],
        remote: [entity ? entity.remote : null],
        routingKey: this.fb.control({ value: entity ? entity.routingKey : null, disabled: true }),
        secret: this.fb.control({ value: entity ? entity.secret : null, disabled: true }),
        configuration: this.fb.control([entity ? entity.configuration : null]),
        metadata: [entity && entity.configuration ? entity.configuration.metadata : {}],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('type').disable({ emitEvent: false });
    if (this.isEditValue && this.entityForm) {
      this.checkIsRemote(this.entityForm);
      this.entityForm.get('routingKey').disable({ emitEvent: false });
      this.entityForm.get('secret').disable({ emitEvent: false });
    }
  }

  private checkIsRemote(form: UntypedFormGroup) {
    const integrationType: IntegrationType = form.get('type').value;
    if (integrationType && integrationTypeInfoMap.get(integrationType).remote) {
      form.get('remote').patchValue(true, { emitEvent: false });
      form.get('remote').disable({ emitEvent: false });
    } else if (this.isEditValue) {
      form.get('remote').enable({ emitEvent: false });
    }
  }

  get showDownlinkConvector(): boolean {
    if (integrationTypeInfoMap.has(this.integrationType)) {
      return !integrationTypeInfoMap.get(this.integrationType).hideDownlink;
    }
    return true;
  }

  private get allowCheckConnection(): boolean {
    if (integrationTypeInfoMap.has(this.integrationType)) {
      return integrationTypeInfoMap.get(this.integrationType).checkConnection || false;
    }
    return false;
  }

  get isCheckConnectionAvailable(): boolean {
    return this.allowCheckConnection && !this.isEdgeTemplate && !this.isRemoteIntegration;
  }

  get isRemoteIntegration(): boolean {
    return this.entityForm ? this.entityForm.value.remote : false;
  }

  get isEdgeTemplate(): boolean {
    return this.integrationScope === 'edge' || this.integrationScope === 'edges';
  }

  get additionalActionConfig (): AdditionalDebugActionConfig {
    return {
      title: this.translate.instant('action.see-debug-events'),
      action: this.openDebugEventsDialog.bind(this)
    }
  }

  private openDebugEventsDialog(): void {
    this.dialog.open<EventsDialogComponent, EventsDialogData, null>(EventsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: 'integration.events',
        debugEventTypes: [DebugEventType.DEBUG_INTEGRATION],
        defaultEventType: DebugEventType.DEBUG_INTEGRATION,
        tenantId: this.entity.tenantId.id,
        entityId: this.entity.id
      }
    })
      .afterClosed()
      .subscribe();
  }

  updateForm(entity: Integration) {
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type,
      enabled: isDefined(entity.enabled) ? entity.enabled : true,
      debugSettings: entity?.debugSettings ?? { allEnabled: false, failuresEnabled: false, allEnabledUntil: 0 },
      allowCreateDevicesOrAssets: isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true,
      defaultConverterId: entity.defaultConverterId,
      downlinkConverterId: entity.downlinkConverterId,
      remote: entity.remote,
      routingKey: entity.routingKey,
      secret: entity.secret,
      metadata: entity.configuration ? entity.configuration.metadata : {},
      configuration: entity.configuration,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : '' }
      },
      {emitEvent: false}
    );
    this.integrationType = entity.type;
  }

  prepareFormValue(formValue: any): any {
    if (!formValue.configuration) {
      formValue.configuration = {};
    }
    formValue.configuration.metadata = formValue.metadata || {};
    formValue.name = formValue.name ? formValue.name.trim() : formValue.name;
    delete formValue.metadata;
    return formValue;
  }

  onIntegrationIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  onIntegrationInfoCopied(type: string) {
    const message = type === 'key' ? 'integration.integration-key-copied-message'
      : 'integration.integration-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  isExistingIntegration(): boolean {
    return !!(this.entity?.id?.id);
  }

  exportToIotHub(): void {
    const integrationId = this.entity?.id?.id;
    if (!integrationId) return;
    this.integrationService.exportIntegrationPackage(integrationId).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const filename = (this.entity.name || 'integration')
          .replace(/[^a-zA-Z0-9._-]/g, '_') + '-package.zip';
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Export failed', err);
      }
    });
  }

  onIntegrationCheck(){
    this.integrationService.checkIntegrationConnection(this.entityFormValue(), {ignoreErrors: true}).subscribe(() =>
    {
      this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.check-success'),
        type: 'success',
        duration: 5000,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
    },
    error => {
      this.store.dispatch(new ActionNotificationShow(
        {
          message: error.error.message,
          type: 'error',
          duration: 5000,
          verticalPosition: 'bottom',
          horizontalPosition: 'right',
          target: 'integrationRoot'
        }));
    });
  }
}
