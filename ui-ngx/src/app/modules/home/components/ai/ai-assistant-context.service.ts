// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { filter, map, startWith } from 'rxjs/operators';
import { aiClientOs, aiPageView, AiChatClientContext } from '@shared/models/ai-chat.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { BreadCrumb } from '@shared/components/breadcrumb';
import { BreadcrumbService } from '@core/services/breadcrumb.service';
import { UtilsService } from '@core/services/utils.service';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { aiWelcome } from '@home/components/ai/ai-welcome.models';

@Injectable({providedIn: 'root'})
export class AiAssistantContextService {

  private panelService = inject(AiAssistantPanelService);
  private translate = inject(TranslateService);
  private utils = inject(UtilsService);
  private router = inject(Router);

  private pageTitle = toSignal(inject(BreadcrumbService).breadcrumbs$.pipe(
    map(crumbs => crumbs.map(crumb => this.crumbLabel(crumb)).filter(label => !!label).join(' › '))
  ), {initialValue: ''});

  readonly pagePath = toSignal(this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    map(event => event.urlAfterRedirects),
    startWith(this.router.url),
    map(url => (url || '/').split(/[?#]/)[0])
  ), {initialValue: '/'});

  readonly clientContext = computed<AiChatClientContext | undefined>(() => {
    const context = this.panelService.clientContext();
    if (context) {
      const name = this.panelService.viewName();
      return context.view?.entityId && name && !context.view.title ? {...context, view: {...context.view, title: name}} : context;
    }
    const title = this.pageTitle();
    const path = this.pagePath();
    const type = aiPageView(path);
    return title || path !== '/' ? {view: {type, title, path}, timeZone: getDefaultTimezone(), os: aiClientOs()} : undefined;
  });

  readonly welcome = computed(() => aiWelcome(this.clientContext()?.view, this.panelService.viewName(), this.panelService.config(),
    this.translate));

  private crumbLabel(crumb: BreadCrumb): string {
    try {
      return crumb.labelFunction ? crumb.labelFunction() : crumb.customTranslate ? this.utils.customTranslation(crumb.label, crumb.label)
        : this.translate.instant(crumb.label);
    } catch {
      return '';
    }
  }
}
