// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { map, Observable, of, switchMap } from 'rxjs';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { ReportTemplateService } from '@core/http/report-template.service';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import {
  ReportTemplatesTableConfigResolver
} from '@home/pages/reporting/template/report-templates-table-config.resolver';
import { ReportTemplate } from '@shared/models/report.models';
import { MenuId } from '@core/services/menu.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';

export interface ReportTemplateResolverData {
  reportTemplate: ReportTemplate;
  readonly: boolean;
}

@Injectable()
export class ReportTemplateResolver {

  constructor(private store: Store<AppState>,
              private reportTemplateService: ReportTemplateService,
              private userPermissionsService: UserPermissionsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<ReportTemplateResolverData> {
    const reportTemplateId = route.params.reportTemplateId;
    return this.reportTemplateService.getReportTemplate(reportTemplateId).pipe(
      switchMap(reportTemplate => this.resolveReadonly(reportTemplate).pipe(
        map(readonly => ({reportTemplate, readonly}))
      ))
    );
  }

  private resolveReadonly(reportTemplate: ReportTemplate): Observable<boolean> {
    if (!this.userPermissionsService.hasGenericPermission(Resource.REPORT_TEMPLATE, Operation.WRITE)) {
      return of(true);
    }
    const authUser = getCurrentAuthUser(this.store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      return of(false);
    }
    if (authUser.authority === Authority.CUSTOMER_USER && authUser.customerId === reportTemplate.customerId?.id) {
      return of(false);
    }
    return this.userPermissionsService.hasEntityPermission(reportTemplate.id, Operation.WRITE).pipe(
      map(hasPermission => !hasPermission)
    );
  }
}

export const reportTemplateBreadcumbLabelFunction: BreadCrumbLabelFunction<ReportTemplatePageComponent>
  = ((_route, _translate, component) => {
  return component.reportTemplate.name;
});

export const reportTemplatesRoute: Route = {
  path: 'templates',
  data: {
    breadcrumb: {
      menuId: MenuId.report_templates
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'report-template.report-templates'
      },
      resolve: {
        entitiesTableConfig: ReportTemplatesTableConfigResolver
      }
    },
    {
      path: ':reportTemplateId',
      component: ReportTemplatePageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: reportTemplateBreadcumbLabelFunction,
          icon: 'mdi:chart-box-outline'
        } as BreadCrumbConfig<ReportTemplatePageComponent>,
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'report-template.report-template',
        hideTabs: true
      },
      resolve: {
        reportTemplate: ReportTemplateResolver
      }
    }
  ]
}

@NgModule({
  providers: [
    ReportTemplatesTableConfigResolver,
    ReportTemplateResolver
  ]
})
export class ReportTemplateRoutingModule { }
