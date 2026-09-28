// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable } from 'rxjs/internal/Observable';

export interface SignupDialogData {
  title: string;
  content$: Observable<string>;
}

@Component({
    selector: 'tb-signup-dialog',
    templateUrl: './signup-dialog.component.html',
    styleUrls: ['./signup-dialog.component.scss'],
    standalone: false
})
export class SignupDialogComponent extends DialogComponent<SignupDialogComponent, boolean> implements OnInit {

  @HostBinding('class') class = 'tb-custom-css';

  title: string;
  dialogText: SafeHtml;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SignupDialogData,
              private domSanitizer: DomSanitizer,
              public dialogRef: MatDialogRef<SignupDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.title = this.data.title;
  }

  ngOnInit(): void {
    this.data.content$.subscribe((content) => {
      this.dialogText = this.domSanitizer.bypassSecurityTrustHtml(content);
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  accept(): void {
    this.dialogRef.close(true);
  }
}
