// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { AttributeData, AttributeScope, LatestTelemetry, TelemetryType } from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { Observable } from 'rxjs';

export interface AddAttributeDialogData {
  entityId: EntityId;
  attributeScope: TelemetryType;
}

@Component({
    selector: 'tb-add-attribute-dialog',
    templateUrl: './add-attribute-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AddAttributeDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class AddAttributeDialogComponent extends DialogComponent<AddAttributeDialogComponent, boolean>
  implements OnInit, ErrorStateMatcher {

  attributeFormGroup: FormGroup;

  submitted = false;

  isTelemetry = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddAttributeDialogData,
              private attributeService: AttributeService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddAttributeDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.attributeFormGroup = this.fb.group({
      key: ['', [Validators.required, Validators.maxLength(255)]],
      value: [null, [Validators.required]]
    });
    this.isTelemetry = this.data.attributeScope === LatestTelemetry.LATEST_TELEMETRY;
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  invalid(): boolean {
    const value = this.attributeFormGroup.get('value').value;
    return !Array.isArray(value) && this.attributeFormGroup.invalid;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  add(): void {
    this.submitted = true;
    const attribute: AttributeData = {
      lastUpdateTs: null,
      key: this.attributeFormGroup.get('key').value.trim(),
      value: this.attributeFormGroup.get('value').value
    };
    let task: Observable<any>;
    if (this.data.attributeScope === LatestTelemetry.LATEST_TELEMETRY) {
      task = this.attributeService.saveEntityTimeseries(this.data.entityId,
        this.data.attributeScope, [attribute]);
    } else {
      task = this.attributeService.saveEntityAttributes(this.data.entityId,
        this.data.attributeScope as AttributeScope, [attribute]);
    }
    task.subscribe(() => this.dialogRef.close(true));
  }
}
