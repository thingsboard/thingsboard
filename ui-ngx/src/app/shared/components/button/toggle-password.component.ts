// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, Component, ElementRef } from '@angular/core';

@Component({
    selector: 'tb-toggle-password',
    templateUrl: 'toggle-password.component.html',
    styleUrls: [],
    standalone: false
})
export class TogglePasswordComponent implements AfterViewInit {
  showPassword = false;
  hideToggle = false;

  private input: HTMLInputElement = null;

  constructor(private hostElement: ElementRef) { }

  togglePassword($event: Event) {
    $event.stopPropagation();
    this.showPassword = !this.showPassword;
    this.input.type = this.showPassword ? 'text' : 'password';
  }

  ngAfterViewInit() {
    this.input = this.hostElement.nativeElement.closest('mat-form-field').querySelector('input[type="password"]');
    if (this.input === null) {
      this.hideToggle = true;
    }
  }
}
