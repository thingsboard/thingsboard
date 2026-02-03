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
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetAction, WidgetActionType, widgetType } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbPopoverService } from '@shared/components/popover.service';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import {
  WidgetActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/widget-action-settings-panel.component';
import { MatButton } from '@angular/material/button';
import { TranslateService } from '@ngx-translate/core';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-map-tooltip-tag-actions-panel',
    templateUrl: './map-tooltip-tag-actions.component.html',
    styleUrls: ['./map-tooltip-tag-actions.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapTooltipTagActionsComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MapTooltipTagActionsComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  actionsFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.actionsFormGroup = this.fb.group({
      actions: [null, []]
    });
    this.actionsFormGroup.get('actions').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (val) => this.propagateChange(val)
    );
  }

  writeValue(actions?: WidgetAction[]): void {
    this.actionsFormGroup.get('actions').patchValue(actions || [], {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.actionsFormGroup.disable({emitEvent: false});
    } else {
      this.actionsFormGroup.enable({emitEvent: false});
    }
  }

  removeAction(index: number): void {
    const actions: WidgetAction[] = this.actionsFormGroup.get('actions').value;
    if (actions[index]) {
      actions.splice(index, 1);
      this.actionsFormGroup.get('actions').patchValue(actions);
    }
  }

  addAction($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    const action: WidgetAction = {
      name: '',
      type: WidgetActionType.doNothing
    };
    const trigger = matButton._elementRef.nativeElement;
    const actionNames = (this.actionsFormGroup.get('actions').value as WidgetAction[] || []).map(action => action.name);
    this.openActionSettingsPopup(trigger, action, actionNames, true, (added) => {
      if (added) {
        const actions: WidgetAction[] = this.actionsFormGroup.get('actions').value || [];
        actions.push(added);
        this.actionsFormGroup.get('actions').patchValue(actions);
      }
    });
  }

  editAction($event: Event, matButton: MatButton, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    const actions: WidgetAction[] = this.actionsFormGroup.get('actions').value;
    if (actions[index]) {
      const action = deepClone(actions[index]);
      const trigger = matButton._elementRef.nativeElement;
      const actionNames = actions.filter((_action, current) => current !== index).map(action => action.name);
      this.openActionSettingsPopup(trigger, action, actionNames, false, (updated) => {
        if (updated) {
          actions[index] = updated;
          this.actionsFormGroup.get('actions').patchValue(actions);
        }
      });
    }
  }

  private openActionSettingsPopup(trigger: Element, action: WidgetAction, actionNames: string[], isAdd: boolean, callback: (action?: WidgetAction) => void) {
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const title = this.translate.instant(isAdd ? 'widgets.maps.data-layer.add-tooltip-tag-action' : 'widgets.maps.data-layer.edit-tooltip-tag-action');
      const applyTitle = this.translate.instant(isAdd ? 'action.add' : 'action.apply');
      const widgetActionSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: WidgetActionSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          widgetAction: action,
          withName: true,
          actionNames,
          panelTitle: title,
          applyTitle,
          widgetType: widgetType.latest,
          callbacks: this.context.callbacks
        },
        isModal: true
      });
      widgetActionSettingsPanelPopover.tbComponentRef.instance.widgetActionApplied.subscribe((widgetAction) => {
        widgetActionSettingsPanelPopover.hide();
        callback(widgetAction);
      });
    }
  }

}
