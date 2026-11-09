// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { scadaSymbolActionFunctionHighlightRules } from '@home/pages/scada-symbol/scada-symbol-editor.models';
import {
  activeActionTriggers,
  exclusiveActionTrigger,
  ScadaSymbolActionTrigger,
  scadaSymbolActionTriggerInfo,
  scadaSymbolActionTriggers,
  ScadaSymbolTagActions
} from '@home/components/widget/lib/scada/scada-symbol.models';

@Component({
    selector: 'tb-scada-symbol-metadata-tag-actions-panel',
    templateUrl: './scada-symbol-metadata-tag-actions-panel.component.html',
    styleUrls: ['./scada-symbol-metadata-tag-actions-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScadaSymbolMetadataTagActionsPanelComponent implements OnInit {

  @Input()
  tag: string;

  @Input()
  actions: ScadaSymbolTagActions;

  @Input()
  completer: TbEditorCompleter;

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolMetadataTagActionsPanelComponent>;

  @Output()
  actionsApplied = new EventEmitter<ScadaSymbolTagActions>();

  actionTriggers = scadaSymbolActionTriggers;

  actionTriggerInfo = scadaSymbolActionTriggerInfo;

  actionFunctionArgs = ['ctx', 'element', 'event'];

  highlightRules = scadaSymbolActionFunctionHighlightRules;

  actionsFormGroup: UntypedFormGroup;

  selectedTrigger: ScadaSymbolActionTrigger;

  exclusiveActionTrigger = exclusiveActionTrigger;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    const activeTriggers = activeActionTriggers(this.actions);
    const triggerGroups: {[trigger: string]: UntypedFormGroup} = {};
    for (const trigger of this.actionTriggers) {
      triggerGroups[trigger] = this.fb.group({
        enabled: [activeTriggers.includes(trigger)],
        actionFunction: [this.actions?.[trigger]?.actionFunction, [Validators.required]]
      });
    }
    this.actionsFormGroup = this.fb.group(triggerGroups);
    if (this.actionTriggers.some(trigger => !!this.actions?.[trigger] && !activeTriggers.includes(trigger))) {
      this.actionsFormGroup.markAsDirty();
    }
    if (this.disabled) {
      this.actionsFormGroup.disable({emitEvent: false});
    } else {
      this.actionTriggers.forEach(trigger => this.updateActionFunctionState(trigger));
    }
    this.selectedTrigger = this.actionTriggers.find(trigger => this.isEnabled(trigger)) ?? this.actionTriggers[0];
    for (const trigger of this.actionTriggers) {
      this.actionsFormGroup.get([trigger, 'enabled']).valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((enabled: boolean) => {
        const exclusiveTrigger = exclusiveActionTrigger(trigger);
        if (enabled && exclusiveTrigger) {
          this.actionsFormGroup.get([exclusiveTrigger, 'enabled']).setValue(false, {emitEvent: false});
          this.updateActionFunctionState(exclusiveTrigger);
        }
        this.updateActionFunctionState(trigger);
      });
    }
  }

  isEnabled(trigger: ScadaSymbolActionTrigger): boolean {
    return !!this.actionsFormGroup.get([trigger, 'enabled']).value;
  }

  cancel() {
    this.popover?.hide();
  }

  applyActions() {
    const value = this.actionsFormGroup.getRawValue();
    let actions: ScadaSymbolTagActions = null;
    for (const trigger of this.actionTriggers) {
      if (value[trigger].enabled) {
        actions = actions || {};
        actions[trigger] = {
          actionFunction: value[trigger].actionFunction
        };
      }
    }
    this.actionsApplied.emit(actions);
  }

  private updateActionFunctionState(trigger: ScadaSymbolActionTrigger) {
    const actionFunctionControl = this.actionsFormGroup.get([trigger, 'actionFunction']);
    if (this.isEnabled(trigger)) {
      actionFunctionControl.enable({emitEvent: false});
    } else {
      actionFunctionControl.disable({emitEvent: false});
    }
  }
}
