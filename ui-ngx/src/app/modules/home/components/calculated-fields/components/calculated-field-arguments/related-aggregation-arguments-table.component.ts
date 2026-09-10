// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  Renderer2,
  ViewContainerRef,
} from '@angular/core';
import { FormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityService } from '@core/http/entity.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CalculatedFieldArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.component';
import { ArgumentEntityType, RelationPathLevel } from '@shared/models/calculated-field.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-related-aggregation-arguments-table',
    templateUrl: './calculated-field-arguments-table.component.html',
    styleUrls: [`calculated-field-arguments-table.component.scss`],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RelatedAggregationArgumentsTableComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => RelatedAggregationArgumentsTableComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RelatedAggregationArgumentsTableComponent extends CalculatedFieldArgumentsTableComponent {

  @Input({required: true})
  set relation(value: RelationPathLevel) {
    this.panelAdditionalCtx.predefinedEntityFilter = {
      type: AliasFilterType.relationsQuery,
      rootStateEntity: false,
      rootEntity: this.entityId,
      direction: value.direction,
      filters: [{
        relationType: value.relationType,
        entityTypes: [EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.TENANT]
      }],
      maxLevel: 1,
    };
  }

  constructor(
    protected fb: FormBuilder,
    protected popoverService: TbPopoverService,
    protected viewContainerRef: ViewContainerRef,
    protected cd: ChangeDetectorRef,
    protected renderer: Renderer2,
    protected entityService: EntityService,
    protected destroyRef: DestroyRef,
    protected store: Store<AppState>
  ) {
    super(fb, popoverService, viewContainerRef, cd, renderer, entityService, destroyRef, store);

    this.argumentNameColumn = 'calculated-fields.argument-name';
    this.displayColumns = ['name', 'type', 'key', 'actions'];
    this.panelAdditionalCtx = {
      hiddenEntityTypes: true,
      defaultValueRequired: true,
      argumentEntityTypes: [ArgumentEntityType.Current],
      hint: 'calculated-fields.hint.setting-arguments-aggregation',
      watchKeyChange: true,
    };

    this.isScript = false;
  }
}
