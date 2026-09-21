// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
