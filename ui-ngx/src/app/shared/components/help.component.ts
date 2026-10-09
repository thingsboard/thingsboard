// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input } from '@angular/core';
import { HelpLinks } from '@shared/models/constants';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
    selector: '[tb-help]',
    templateUrl: './help.component.html',
    standalone: false
})
export class HelpComponent {

  constructor(public wl: WhiteLabelingService) {
  }

  @Input('tb-help') helpLinkId: string;

  gotoHelpPage(): void {
    let helpUrl = HelpLinks.linksMap[this.helpLinkId];
    if (!helpUrl && this.helpLinkId &&
      (this.helpLinkId.startsWith('http://') || this.helpLinkId.startsWith('https://'))) {
      helpUrl = this.helpLinkId;
    }
    if (helpUrl) {
      const baseUrl =  this.wl.getHelpLinkBaseUrl();
      if (baseUrl) {
        helpUrl = helpUrl.replace('https://thingsboard.io', baseUrl);
      }
      window.open(helpUrl, '_blank');
    }
  }

}
