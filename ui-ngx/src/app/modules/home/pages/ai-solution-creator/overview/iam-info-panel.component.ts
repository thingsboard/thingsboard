// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { SolutionDescriptorIam } from '@shared/models/solution-creator.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-iam-info-panel',
  templateUrl: './iam-info-panel.component.html',
  styleUrls: ['./entity-info-panel.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class IamInfoPanelComponent implements OnInit {

  @Input()
  iam: SolutionDescriptorIam;

  operations: string;

  constructor(private popover: TbPopoverComponent<IamInfoPanelComponent>,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.operations = this.iam.permissions.split('')
      .map(operation => this.translate.instant(`solution-creator.operation.${operation}`))
      .join(', ');
  }

  cancel() {
    this.popover.hide();
  }
}
