// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, Component, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { getProviderHelpLink, OAuth2Client } from '@shared/models/oauth2.models';
import { OAuth2Service } from '@core/http/oauth2.service';
import { ClientComponent } from '@home/pages/admin/oauth2/clients/client.component';
import { ErrorStateMatcher } from '@angular/material/core';

@Component({
    selector: 'tb-client-dialog',
    templateUrl: './client-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: ClientDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class ClientDialogComponent extends DialogComponent<ClientDialogComponent, OAuth2Client> implements OnDestroy, AfterViewInit {

  submitted = false;

  @ViewChild('clientComponent', {static: true}) clientComponent: ClientComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<ClientDialogComponent, OAuth2Client>,
              private oauth2Service: OAuth2Service,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.clientComponent.entityForm.markAsDirty();
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
    if (this.clientComponent.entityForm.valid) {
      this.oauth2Service.saveOAuth2Client(this.clientComponent.entityFormValue()).subscribe(
        (client) => {
          this.dialogRef.close(client);
        }
      );
    }
  }

  helpLinkId() {
    return getProviderHelpLink(this.clientComponent.entityForm.get('additionalInfo.providerName')?.value);
  }
}
