// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@app/shared/models/id/entity-id';
import { EntityViewInfo } from '@shared/models/entity-view.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-entity-view',
    templateUrl: './entity-view.component.html',
    styleUrls: ['./entity-view.component.scss'],
    standalone: false
})
export class EntityViewComponent extends GroupEntityComponent<EntityViewInfo> {

  entityType = EntityType;

  dataKeyType = DataKeyType;

  // entityViewScope: 'tenant' | 'customer' | 'customer_user';

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET];

  maxStartTimeMs: Observable<number | null>;
  minEndTimeMs: Observable<number | null>;

  selectedEntityId: Observable<EntityId | null>;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: EntityViewInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<EntityViewInfo> | GroupEntityTableConfig<EntityViewInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  ngOnInit() {
    // this.entityViewScope = this.entitiesTableConfig.componentsData.entityViewScope;
    super.ngOnInit();
    this.maxStartTimeMs = this.entityForm.get('endTimeMs').valueChanges;
    this.minEndTimeMs = this.entityForm.get('startTimeMs').valueChanges;
    this.selectedEntityId = this.entityForm.get('entityId').valueChanges;
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: EntityView): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: EntityViewInfo): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity ? entity.type : null, Validators.required],
        entityId: [entity ? entity.entityId : null, [Validators.required]],
        startTimeMs: [entity ? entity.startTimeMs : null],
        endTimeMs: [entity ? entity.endTimeMs : null],
        keys: this.fb.group(
          {
            attributes: this.fb.group(
              {
                cs: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.cs : null],
                sh: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.sh : null],
                ss: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.ss : null],
              }
            ),
            timeseries: [entity && entity.keys && entity.keys.timeseries ? entity.keys.timeseries : null]
          }
        ),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: EntityViewInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({entityId: entity.entityId});
    this.entityForm.patchValue({startTimeMs: entity.startTimeMs});
    this.entityForm.patchValue({endTimeMs: entity.endTimeMs});
    this.entityForm.patchValue({
      keys:
        {
          attributes: {
            cs: entity.keys && entity.keys.attributes ? entity.keys.attributes.cs : null,
            sh: entity.keys && entity.keys.attributes ? entity.keys.attributes.sh : null,
            ss: entity.keys && entity.keys.attributes ? entity.keys.attributes.ss : null,
          },
          timeseries: entity.keys && entity.keys.timeseries ? entity.keys.timeseries : null
        }
    });
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onEntityViewIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('entity-view.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
