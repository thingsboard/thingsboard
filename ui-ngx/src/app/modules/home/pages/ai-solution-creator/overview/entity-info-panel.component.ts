// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  SolutionDescriptorEntityBaseRelation,
  SolutionDescriptorEntityBaseTelemetry,
  SolutionEntity
} from '@shared/models/solution-creator.models';

@Component({
  selector: 'tb-entity-info-panel',
  templateUrl: './entity-info-panel.component.html',
  styleUrls: ['./entity-info-panel.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class EntityInfoPanelComponent implements OnInit {

  @Input()
  entity: SolutionEntity;

  @Input()
  relations: SolutionDescriptorEntityBaseRelation[] = [];

  entityInfoType: 'ATTRIBUTE' | 'TIMESERIES' | 'RELATION';

  attributes: SolutionDescriptorEntityBaseTelemetry[];
  timeseries: SolutionDescriptorEntityBaseTelemetry[];

  constructor(private popover: TbPopoverComponent<EntityInfoPanelComponent>) {
  }

  ngOnInit() {
    this.attributes = this.entity.telemetry?.filter((item) => item.telemetryType === 'ATTRIBUTE');
    this.timeseries = this.entity.telemetry?.filter((item) => item.telemetryType === 'TIMESERIES');
    if (this.attributes?.length) {
      this.entityInfoType = 'ATTRIBUTE';
    } else if (this.timeseries?.length) {
      this.entityInfoType = 'TIMESERIES';
    } else if (this.relations?.length) {
      this.entityInfoType = 'RELATION';
    }
  }

  cancel() {
    this.popover.hide();
  }
}
