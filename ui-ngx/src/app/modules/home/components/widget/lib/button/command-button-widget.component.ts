// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { BasicActionWidgetComponent, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import { WidgetButtonAppearance } from '@shared/components/button/widget-button.models';
import {
  commandButtonDefaultSettings,
  CommandButtonWidgetSettings
} from '@home/components/widget/lib/button/command-button-widget.models';

@Component({
    selector: 'tb-command-button-widget',
    templateUrl: './command-button-widget.component.html',
    styleUrls: ['../action/action-widget.scss', './command-button-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CommandButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  settings: CommandButtonWidgetSettings;

  disabled = false;

  appearance: WidgetButtonAppearance;
  borderRadius = '4px';

  private clickValueSetter: ValueSetter<any>;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...commandButtonDefaultSettings, ...this.ctx.settings};

    this.appearance = this.settings.appearance;

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.button-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    const onClickStateSettings = {...this.settings.onClickState,
      actionLabel: this.ctx.translate.instant('widgets.command-button.on-click')};
    this.clickValueSetter = this.createValueSetter(onClickStateSettings);
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    this.borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.cd.detectChanges();
  }

  public onClick(_$event: MouseEvent) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      this.updateValue(this.clickValueSetter, null);
    }
  }

  private onDisabled(value: boolean): void {
    this.disabled = !!value;
    this.cd.markForCheck();
  }

}
