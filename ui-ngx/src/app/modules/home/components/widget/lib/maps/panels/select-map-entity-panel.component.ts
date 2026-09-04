// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UnplacedMapDataItem } from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';

@Component({
    selector: 'tb-select-map-entity-panel',
    templateUrl: './select-map-entity-panel.component.html',
    providers: [],
    styleUrls: ['./select-map-entity-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SelectMapEntityPanelComponent implements OnInit {

  @Input()
  entities: UnplacedMapDataItem[];

  @Output()
  entitySelected = new EventEmitter<UnplacedMapDataItem>();

  selectEntityFormGroup: UntypedFormGroup;

  selectedEntity: UnplacedMapDataItem = null;

  constructor(private fb: UntypedFormBuilder,
              private popover: TbPopoverComponent) {
  }

  ngOnInit(): void {
    this.selectEntityFormGroup = this.fb.group(
      {
        entity: ['', Validators.required]
      }
    );
    this.popover.tbDestroy.subscribe(() => {
      this.entitySelected.emit(this.selectedEntity);
    });
  }

  cancel() {
    this.popover.hide();
  }

  selectEntity() {
    this.selectedEntity = this.selectEntityFormGroup.value.entity;
    this.popover.hide();
  }
}
