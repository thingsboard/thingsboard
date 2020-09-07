///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject, InjectionToken, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { OverlayRef } from '@angular/cdk/overlay';

export const EDIT_REDIRECT_URI_PANEL_DATA = new InjectionToken<any>('EditRedirectUriPanelData');

export interface EditRedirectUriPanelData {
  redirectURI: any;
}

@Component({
  selector: 'tb-edit-redirect-uri-panel',
  templateUrl: './edit-redirect-uri-panel.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EditRedirectUriPanelComponent}],
  styleUrls: ['./edit-redirect-uri-panel.component.scss']
})
export class EditRedirectUriPanelComponent extends PageComponent implements OnInit, ErrorStateMatcher {

  private URL_REGEXP = /^[A-Za-z][A-Za-z\d.+-]*:\/*(?:\w+(?::\w+)?@)?[^\s/]+(?::\d+)?(?:\/[\w#!:.?+=&%@\-/]*)?$/;

  redirectURIFormGroup: FormGroup;

  result: any = null;

  submitted = false;

  constructor(protected store: Store<AppState>,
              @Inject(EDIT_REDIRECT_URI_PANEL_DATA) public data: EditRedirectUriPanelData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public overlayRef: OverlayRef,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.redirectURIFormGroup = this.fb.group({
      value: [this.data.redirectURI, [Validators.required, Validators.pattern(this.URL_REGEXP)]]
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  update(): void {
    this.submitted = true;
    this.result = this.redirectURIFormGroup.get('value').value;
    this.overlayRef.dispose();
  }
}
