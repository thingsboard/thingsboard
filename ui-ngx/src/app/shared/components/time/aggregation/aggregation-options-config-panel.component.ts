// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnInit } from '@angular/core';
import { aggregationTranslations, AggregationType } from '@shared/models/time/time.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
    selector: 'tb-aggregation-options-config-panel',
    templateUrl: './aggregation-options-config-panel.component.html',
    styleUrls: ['./aggregation-options-config-panel.component.scss'],
    standalone: false
})
export class AggregationOptionsConfigPanelComponent implements OnInit {

  @Input()
  allowedAggregationTypes: Array<AggregationType>;

  @Input()
  onClose: (result: Array<AggregationType> | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  aggregationOptionsConfigForm: FormGroup;

  aggregationTypes = AggregationType;

  allAggregationTypes: Array<AggregationType> = Object.values(AggregationType);

  aggregationTypesTranslations = aggregationTranslations;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.aggregationOptionsConfigForm = this.fb.group({
      allowedAggregationTypes: [this.allowedAggregationTypes?.length ? this.allowedAggregationTypes : this.allAggregationTypes]
    });
  }

  update() {
    if (this.onClose) {
      const allowedAggregationTypes = this.aggregationOptionsConfigForm.get('allowedAggregationTypes').value;
      // if full list selected returns empty for optimization
      this.onClose(allowedAggregationTypes?.length < this.allAggregationTypes.length ? allowedAggregationTypes : []);
    }
  }

  cancel() {
    if (this.onClose) {
      this.onClose(null);
    }
  }

}
