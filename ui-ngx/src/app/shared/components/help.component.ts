// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input } from '@angular/core';
import { HelpLinks } from '@shared/models/constants';

@Component({
    selector: '[tb-help]',
    templateUrl: './help.component.html',
    standalone: false
})
export class HelpComponent {

  @Input('tb-help') helpLinkId: string;

  gotoHelpPage(): void {
    let helpUrl = HelpLinks.linksMap[this.helpLinkId];
    if (!helpUrl && this.helpLinkId &&
      (this.helpLinkId.startsWith('http://') || this.helpLinkId.startsWith('https://'))) {
      helpUrl = this.helpLinkId;
    }
    if (helpUrl) {
      window.open(helpUrl, '_blank');
    }
  }

}
