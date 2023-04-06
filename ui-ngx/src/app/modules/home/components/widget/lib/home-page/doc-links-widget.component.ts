///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { map } from 'rxjs';
import { DocumentationLink, DocumentationLinks } from '@shared/models/user-settings.models';
import { UserSettingsService } from '@core/http/user-settings.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  ImportDialogCsvComponent,
  ImportDialogCsvData
} from '@home/components/import-export/import-dialog-csv.component';
import { MatDialog } from '@angular/material/dialog';
import { AddDocLinkDialogComponent } from '@home/components/widget/lib/home-page/add-doc-link-dialog.component';
import {
  EditDocLinksDialogComponent,
  EditDocLinksDialogData
} from '@home/components/widget/lib/home-page/edit-doc-links-dialog.component';

const defaultDocLinksMap = new Map<Authority, DocumentationLinks>(
  [
    [Authority.SYS_ADMIN, {
      links: [
        {
          icon: 'rocket',
          name: 'Getting started',
          link: 'https://thingsboard.io/docs/getting-started-guides/helloworld/'
        },
        {
          icon: 'title',
          name: 'Tenant profiles',
          link: 'https://thingsboard.io/docs/user-guide/tenant-profiles/'
        },
        {
          icon: 'insert_chart',
          name: 'API',
          link: 'https://thingsboard.io/docs/api/'
        },
        {
          icon: 'now_widgets',
          name: 'Widgets Library',
          link: 'https://thingsboard.io/docs/user-guide/ui/widget-library/'
        }
      ]
    }],
    [Authority.TENANT_ADMIN, {
      links: [
        {
          icon: 'rocket',
          name: 'Getting started',
          link: 'https://thingsboard.io/docs/getting-started-guides/helloworld/'
        },
        {
          icon: 'settings_ethernet',
          name: 'Rule engine',
          link: 'https://thingsboard.io/docs/user-guide/rule-engine-2-0/re-getting-started/'
        },
        {
          icon: 'insert_chart',
          name: 'API',
          link: 'https://thingsboard.io/docs/api/'
        },
        {
          icon: 'devices',
          name: 'Device profiles',
          link: 'https://thingsboard.io/docs/user-guide/device-profiles/'
        }
      ]
    }],
    [Authority.CUSTOMER_USER, {
      links: []
    }]
  ]
);

interface DocLinksWidgetSettings {
  columns: number;
}

@Component({
  selector: 'tb-doc-links-widget',
  templateUrl: './doc-links-widget.component.html',
  styleUrls: ['./doc-links-widget.component.scss']
})
export class DocLinksWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  settings: DocLinksWidgetSettings;
  columns: number;

  documentationLinks: DocumentationLinks;
  authUser = getCurrentAuthUser(this.store);

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private userSettingsService: UserSettingsService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    this.settings = this.ctx.settings;
    this.columns = this.settings.columns || 3;
    this.loadDocLinks();
  }

  private loadDocLinks() {
    this.userSettingsService.getDocumentationLinks().pipe(
      map((documentationLinks) => {
        if (!documentationLinks || !documentationLinks.links) {
          return defaultDocLinksMap.get(this.authUser.authority);
        } else {
          return documentationLinks;
        }
      })
    ).subscribe(
      (documentationLinks) => {
        this.documentationLinks = documentationLinks;
        this.cd.markForCheck();
      }
    );
  }

  edit() {
    this.dialog.open<EditDocLinksDialogComponent, EditDocLinksDialogData,
      boolean>(EditDocLinksDialogComponent, {
      disableClose: true,
      autoFocus: false,
      data: {
        docLinks: this.documentationLinks
      },
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.loadDocLinks();
        }
      });
  }

  addLink() {
    this.dialog.open<AddDocLinkDialogComponent, any,
      DocumentationLink>(AddDocLinkDialogComponent, {
      disableClose: true,
      autoFocus: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (docLink) => {
        if (docLink) {
          this.documentationLinks.links.push(docLink);
          this.cd.markForCheck();
          this.userSettingsService.updateDocumentationLinks(this.documentationLinks).subscribe();
        }
    });
  }
}
