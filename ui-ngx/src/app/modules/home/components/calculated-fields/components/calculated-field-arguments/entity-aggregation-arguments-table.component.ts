// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, DestroyRef, forwardRef, Renderer2, ViewContainerRef, } from '@angular/core';
import { FormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityService } from '@core/http/entity.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CalculatedFieldArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.component';
import { ArgumentEntityType } from '@shared/models/calculated-field.models';

@Component({
    selector: 'tb-entity-aggregation-arguments-table',
    templateUrl: './calculated-field-arguments-table.component.html',
    styleUrls: [`calculated-field-arguments-table.component.scss`],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityAggregationArgumentsTableComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => EntityAggregationArgumentsTableComponent),
            multi: true
        }
    ],
    standalone: false
})
export class EntityAggregationArgumentsTableComponent extends CalculatedFieldArgumentsTableComponent {

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
      argumentEntityTypes: [ArgumentEntityType.Current],
      hint: 'calculated-fields.entity-aggregation.argument-setting-hint',
      hiddenDefaultValue: true,
      hiddenEntityKeyTypes: true,
      watchKeyChange: true,
    };

    this.isScript = false;
  }
}
