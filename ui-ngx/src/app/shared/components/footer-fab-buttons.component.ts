// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, HostListener, Input } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { speedDialFabAnimations } from '@shared/animations/speed-dial-fab.animations';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

export interface FooterFabButton {
  name: string;
  icon: string;
  onAction: ($event: Event) => void;
}

export interface FooterFabButtons {
  fabTogglerName: string;
  fabTogglerIcon: string;
  buttons: Array<FooterFabButton>;
}

@Component({
    selector: 'tb-footer-fab-buttons',
    templateUrl: './footer-fab-buttons.component.html',
    styleUrls: ['./footer-fab-buttons.component.scss'],
    animations: speedDialFabAnimations,
    standalone: false
})
export class FooterFabButtonsComponent extends PageComponent {

  @Input()
  footerFabButtons: FooterFabButtons;

  private relativeValue: boolean;
  get relative(): boolean {
    return this.relativeValue;
  }
  @Input()
  set relative(value: boolean) {
    this.relativeValue = coerceBooleanProperty(value);
  }

  buttons: Array<FooterFabButton> = [];
  fabTogglerState = 'inactive';

  closeTimeout = null;

  @HostListener('focusout', ['$event'])
  onFocusOut($event) {
    if (!this.closeTimeout) {
      this.closeTimeout = setTimeout(() => {
        this.hideItems();
      }, 100);
    }
  }

  @HostListener('focusin', ['$event'])
  onFocusIn($event) {
    if (this.closeTimeout) {
      clearTimeout(this.closeTimeout);
      this.closeTimeout = null;
    }
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  showItems() {
    this.fabTogglerState = 'active';
    this.buttons = this.footerFabButtons.buttons;
  }

  hideItems() {
    this.fabTogglerState = 'inactive';
    this.buttons = [];
  }

  onToggleFab() {
    this.buttons.length ? this.hideItems() : this.showItems();
  }
}
