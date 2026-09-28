// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { Domain } from '@shared/models/oauth2.models';
import { DomainService } from '@core/http/domain.service';
import { DomainComponent } from '@home/pages/admin/oauth2/domains/domain.component';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-mobile-app-dialog',
    templateUrl: './domain-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: DomainDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class DomainDialogComponent extends DialogComponent<DomainDialogComponent, Domain> implements OnDestroy, AfterViewInit, ErrorStateMatcher {

  submitted = false;

  addTitle = entityTypeTranslations.get(EntityType.DOMAIN).add;
  helpId = entityTypeResources.get(EntityType.DOMAIN).helpLinkId;

  @ViewChild('domainComponent', {static: true}) domainComponent: DomainComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<DomainDialogComponent, Domain>,
              @Inject(MAT_DIALOG_DATA) public data: {name?: string},
              private domainService: DomainService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.domainComponent.isEdit = true;
      if (this.data.name) {
        this.domainComponent.entityForm.get('name').patchValue(this.data.name, {emitEvent: false});
      }
    }, 0);
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    this.submitted = true;
    if (this.domainComponent.entityForm.valid) {
      const oauth2ClientIds = this.domainComponent.entityFormValue().oauth2ClientInfos as Array<string> || [];
      this.domainService.saveDomain(
        this.domainComponent.entityFormValue(),
        oauth2ClientIds
      ).subscribe(
        domain => this.dialogRef.close(domain)
      )
    }
  }
}
