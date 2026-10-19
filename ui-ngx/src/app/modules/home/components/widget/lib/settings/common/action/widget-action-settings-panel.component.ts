// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetAction, WidgetActionType, widgetType } from '@shared/models/widget.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-widget-action-settings-panel',
    templateUrl: './widget-action-settings-panel.component.html',
    providers: [],
    styleUrls: ['./action-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WidgetActionSettingsPanelComponent implements OnInit {

  @Input()
  widgetAction: WidgetAction;

  @Input()
  panelTitle: string;

  @Input()
  widgetType: widgetType;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  @coerceBoolean()
  withName = false;

  @Input()
  actionNames: string[];

  @Input()
  applyTitle = this.translate.instant('action.apply');

  @Input()
  additionalWidgetActionTypes: WidgetActionType[];

  @Output()
  widgetActionApplied = new EventEmitter<WidgetAction>();

  widgetActionFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private popover: TbPopoverComponent) {
  }

  ngOnInit(): void {
    this.widgetActionFormGroup = this.fb.group(
      {
        widgetAction: [this.widgetAction, []]
      }
    );
  }

  cancel() {
    this.popover?.hide();
  }

  applyWidgetAction() {
    const widgetAction: WidgetAction = this.widgetActionFormGroup.get('widgetAction').getRawValue();
    this.widgetActionApplied.emit(widgetAction);
  }
}
