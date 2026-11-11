// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Converter, ConverterType, getConverterHelpLink } from '@shared/models/converter.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationType } from '@shared/models/integration.models';
import { isNotEmptyStr } from '@core/utils';

export interface ConverterDialogData {
  isEdit: boolean;
  convertor: ConverterData
}

export interface ConverterData  {
  name: string;
  edgeTemplate?: boolean;
  type: ConverterType;
  integrationType?: IntegrationType;
  disabledIntegrationType?: boolean
}

@Component({
    selector: 'tb-converter-dialog',
    templateUrl: './converter-dialog.component.html',
    styleUrls: ['./converter-dialog.component.scss'],
    standalone: false
})
export class ConverterDialogComponent extends DialogComponent<ConverterDialogComponent, BaseData<HasId>>
  implements OnInit, AfterViewInit {

  isEdit: boolean;
  dialogTitle:string;
  converter: Converter;

  @ViewChild('converterComponent', {static: true}) converterComponent: ConverterComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ConverterDialogData,
              public dialogRef: MatDialogRef<ConverterDialogComponent, BaseData<HasId>>,
              private converterService: ConverterService) {
    super(store, router, dialogRef);
    this.isEdit = this.data.isEdit;
    this.dialogTitle = this.isEdit ? 'converter.edit' : entityTypeTranslations.get(EntityType.CONVERTER).add;
  }

  ngOnInit() {
    const {disabledIntegrationType, ...converterInfo} = this.data.convertor
    this.converter = converterInfo as Converter;
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.converterComponent.entityForm.get('type').disable({emitEvent: false});
      if (isNotEmptyStr(this.data.convertor.integrationType) || this.data.convertor.disabledIntegrationType) {
        this.converterComponent.entityForm.get('integrationType').disable({emitEvent: false});
      }
    }, 0);
  }

  helpLinkId(): string {
    return getConverterHelpLink(this.data.convertor as Converter);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.converterComponent.entityForm.valid) {
      this.converter = {...this.converter, ...this.converterComponent.entityFormValue()};
      this.converterService.saveConverter(this.converter).subscribe(
        (entity) => {
          this.dialogRef.close(entity);
        }
      );
    }
  }
}
