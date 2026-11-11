// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { MailTemplate, MailTemplatesSettings, mailTemplateTranslations } from '@shared/models/settings.models';
import { Operation, Resource } from '@shared/models/security.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { isDefinedAndNotNull } from '@core/utils';
import { EditorOptions } from 'hugerte';
import { defaultHugeRteOptions, HUGERTE_BODY_ID } from '@shared/models/hugerte/hugerte.models';

@Component({
    selector: 'tb-mail-templates',
    templateUrl: './mail-templates.component.html',
    styleUrls: ['./mail-templates.component.scss', './settings-card.scss'],
    standalone: false
})
export class MailTemplatesComponent extends PageComponent implements OnInit, HasDirtyFlag {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  mailTemplatesSettings: MailTemplatesSettings;

  mailTemplateTypes = [];
  mailTemplateTranslationsMap = mailTemplateTranslations;

  mailTemplate: MailTemplate = MailTemplate.test;
  useSystemMailSettings = false;

  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  isDirty = false;

  hugeRteOptions: Partial<EditorOptions>;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private wl: WhiteLabelingService,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.hugeRteOptions = defaultHugeRteOptions({
      height: 450,
      content_style: 'body { margin: 0; }'
    });

    if (this.readonly) {
      this.hugeRteOptions.plugins = [];
      this.hugeRteOptions.menubar = false;
      this.hugeRteOptions.toolbar = false;
      this.hugeRteOptions.statusbar = false;
      this.hugeRteOptions.resize = true;
      this.hugeRteOptions.readonly = true;
      this.hugeRteOptions.setup = (ed) => {
        ed.on('PreInit', () => {
          const document = $(ed.iframeElement.contentDocument);
          const body = $(`#${HUGERTE_BODY_ID}`, document);
          body.attr({contenteditable: false});
          body.css('pointerEvents', 'none');
          body.css('userSelect', 'none');
        });
      };
    } else {
      this.hugeRteOptions.plugins = ['link', 'table', 'image', 'code', 'fullscreen', 'lists'];
      this.hugeRteOptions.menubar = 'edit insert tools view format table';
      this.hugeRteOptions.toolbar_mode = 'sliding';
      this.hugeRteOptions.toolbar = 'fontfamily fontsize | bold italic strikethrough forecolor backcolor ' +
        '| link table image | alignleft aligncenter alignright alignjustify ' +
        '| numlist bullist outdent indent | blocks | removeformat code | fullscreen';
    }
    this.mailTemplatesSettings = this.route.snapshot.data.mailTemplatesSettings;
    this.mailTemplateTypes = Object.keys(MailTemplate).filter(type => Object.keys(this.mailTemplatesSettings).includes(type));
    if (this.isTenantAdmin()) {
      this.useSystemMailSettings = isDefinedAndNotNull(this.mailTemplatesSettings.useSystemMailSettings) ? this.mailTemplatesSettings.useSystemMailSettings : true;
    }
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  save() {
    if (this.isTenantAdmin()) {
      this.mailTemplatesSettings.useSystemMailSettings = this.useSystemMailSettings;
    }
    this.wl.saveMailTemplates(this.mailTemplatesSettings).subscribe(
      (adminSettings) => {
        this.mailTemplatesSettings = adminSettings;
        this.isDirty = false;
      }
    );
  }

}
