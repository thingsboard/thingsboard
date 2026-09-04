// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { DomainInfo } from '@shared/models/oauth2.models';
import { AppState } from '@core/core.state';
import { OAuth2Service } from '@core/http/oauth2.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WINDOW } from '@core/services/window.service';
import { isDefinedAndNotNull } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { ClientDialogComponent } from '@home/pages/admin/oauth2/clients/client-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-domain',
    templateUrl: './domain.component.html',
    styleUrls: [],
    standalone: false
})
export class DomainComponent extends EntityComponent<DomainInfo> {

  private loginProcessingUrl = '';

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private oauth2Service: OAuth2Service,
              @Inject('entity') protected entityValue: DomainInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<DomainInfo>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder,
              @Inject(WINDOW) private window: Window,
              private dialog: MatDialog) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    this.entityForm.get('name').setValue(this.window.location.hostname);
    this.entityForm.markAsDirty();
    this.oauth2Service.getLoginProcessingUrl().subscribe(url => {
      this.loginProcessingUrl = url;
    });
  }

  buildForm(entity: DomainInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity?.name ? entity.name : '', [
        Validators.required, Validators.maxLength(255), Validators.pattern('^(?:\\w+(?::\\w+)?@)?[^\\s/]+(?::\\d+)?$')]],
      oauth2Enabled: isDefinedAndNotNull(entity?.oauth2Enabled) ? entity.oauth2Enabled : true,
      oauth2ClientInfos: entity?.oauth2ClientInfos ? entity.oauth2ClientInfos.map(info => info.id.id) : [],
      propagateToEdge: isDefinedAndNotNull(entity?.propagateToEdge) ? entity.propagateToEdge : false
    });
  }

  updateForm(entity: DomainInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      oauth2Enabled: entity.oauth2Enabled,
      oauth2ClientInfos: entity.oauth2ClientInfos?.map(info => info.id ? info.id.id : info),
      propagateToEdge: entity.propagateToEdge
    });
  }

  redirectURI(): string {
    const domainName = this.entityForm.get('name').value;
    return domainName !== '' ? `${domainName}${this.loginProcessingUrl}` : '';
  }

  createClient($event: Event) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<ClientDialogComponent>(ClientDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((client) => {
        if (client) {
          const formValue = this.entityForm.get('oauth2ClientInfos').value ?
            [...this.entityForm.get('oauth2ClientInfos').value] : [];
          formValue.push(client.id.id);
          this.entityForm.get('oauth2ClientInfos').patchValue(formValue);
          this.entityForm.get('oauth2ClientInfos').markAsDirty();
        }
      });
  }

}
