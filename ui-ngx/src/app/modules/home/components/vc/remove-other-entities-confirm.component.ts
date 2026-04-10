///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
    selector: 'tb-remove-other-entities-confirm',
    templateUrl: './remove-other-entities-confirm.component.html',
    styleUrls: [],
    standalone: false
})
export class RemoveOtherEntitiesConfirmComponent extends PageComponent implements OnInit {

  @Input()
  onClose: (result: boolean | null) => void;

  confirmFormGroup: UntypedFormGroup;

  removeOtherEntitiesConfirmText: SafeHtml;

  removeOtherEntitiesVerificationText = 'remove other entities';

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: UntypedFormBuilder) {
    super(store);
    this.removeOtherEntitiesConfirmText = this.sanitizer.bypassSecurityTrustHtml(this.translate.instant('version-control.remove-other-entities-confirm-text'));
  }

  ngOnInit(): void {
    this.confirmFormGroup = this.fb.group({
      verification: [null, []]
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  confirm(): void {
    if (this.onClose) {
      this.onClose(true);
    }
  }
}
