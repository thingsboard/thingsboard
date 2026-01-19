///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  OnInit,
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
import {
  ArgumentEntityType,
  ArgumentType,
  CalculatedFieldArgumentValue,
  FORBIDDEN_NAMES
} from '@shared/models/calculated-field.models';
import { isDefined, isUndefinedOrNull } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
  selector: 'tb-propagate-arguments-table',
  templateUrl: './calculated-field-arguments-table.component.html',
  styleUrls: [`calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PropagateArgumentsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PropagateArgumentsTableComponent),
      multi: true
    }
  ],
})
export class PropagateArgumentsTableComponent extends CalculatedFieldArgumentsTableComponent implements OnInit {

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
    super(fb, popoverService, viewContainerRef, cd, renderer, entityService, destroyRef, store)
  }

  ngOnInit() {
    this.updatedValue();
  }

  protected changeIsScriptMode(): void {
    this.updatedValue();
    super.changeIsScriptMode();
  }

  private updatedValue() {
    if (this.isScript) {
      this.argumentNameColumn = 'common.name';
      this.argumentNameColumnCopy = 'calculated-fields.copy-argument-name';
      this.displayColumns = ['name', 'entityType', 'target', 'type', 'key', 'actions'];
      this.panelAdditionalCtx = null;
    } else {
      this.argumentNameColumn = 'calculated-fields.output-key';
      this.argumentNameColumnCopy = 'calculated-fields.copy-output-key';
      this.displayColumns = ['name', 'type', 'key', 'actions'];
      this.panelAdditionalCtx = {
        argumentEntityTypes: [ArgumentEntityType.Current],
        argumentNameContext: {
          label: 'calculated-fields.output-key',
          required: 'calculated-fields.hint.output-key-required',
          duplicate: 'calculated-fields.hint.output-key-duplicate',
          pattern: 'calculated-fields.hint.output-key-pattern',
          maxlength: 'calculated-fields.hint.output-key-max-length',
          forbidden: 'calculated-fields.hint.output-key-forbidden'
        },
        watchKeyChange: true,
        forbiddenNames: [...FORBIDDEN_NAMES, 'propagationCtx'],
      };
    }
  }

  protected isEditButtonShowBadge(argument: CalculatedFieldArgumentValue): boolean {
    if (!this.isScript && (isDefined(argument?.refEntityId) || isDefined(argument?.refDynamicSourceConfiguration))) {
      return false;
    }
    return super.isEditButtonShowBadge(argument);
  }

  protected updateErrorText(): void {
    if (!this.isScript && this.argumentsFormArray.controls.some(control => isDefined(control.value?.refEntityId) || isDefined(control.value.refDynamicSourceConfiguration))) {
      this.errorText = 'calculated-fields.hint.arguments-propagate-argument-entity-type';
    } else if (!this.isScript && this.argumentsFormArray.controls.some(control => control.value.refEntityKey.type === ArgumentType.Rolling)) {
      this.errorText = 'calculated-fields.hint.arguments-propagate-arguments-with-rolling';
    } else if (this.argumentsFormArray.controls.some(control => control.value.refEntityId?.id === NULL_UUID)) {
      this.errorText = 'calculated-fields.hint.arguments-entity-not-found';
    } else if (!this.argumentsFormArray.controls.length) {
      this.errorText = 'calculated-fields.hint.arguments-empty';
    } else if (this.isScript && !this.argumentsFormArray.controls.some(control => isUndefinedOrNull(control.value?.refEntityId) && isUndefinedOrNull(control.value.refDynamicSourceConfiguration))) {
      this.errorText = 'calculated-fields.hint.arguments-propagate-argument-must-current-entity';
    } else {
      this.errorText = '';
    }
  }
}
