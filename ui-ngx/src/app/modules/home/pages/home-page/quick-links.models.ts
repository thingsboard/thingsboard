// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Params } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { DocumentationLink, QUICK_WEB_LINK_PREFIX, QuickLinks } from '@shared/models/user-settings.models';
import { MenuSection } from '@core/services/menu.models';

export const QUICK_LINKS_MAX = 12;

export const QUICK_LINK_ICONS = ['link', 'public', 'menu_book', 'description', 'school', 'help_outline', 'support_agent',
  'insert_chart', 'dashboard', 'map', 'code', 'build', 'cloud', 'storage', 'folder', 'chat'];

export interface QuickLinkPage {
  id: string;
  name: string;
  icon: string;
  path: string;
  queryParams?: Params;
  crumb: string;
}

export interface QuickLinkView {
  id: string;
  name: string;
  icon: string;
  page?: QuickLinkPage;
  web?: DocumentationLink;
}

export type QuickLinkDialogResult = {page: QuickLinkPage} | {web: DocumentationLink};

export const isQuickWebLinkId = (id: string): boolean => id.startsWith(QUICK_WEB_LINK_PREFIX);

export const quickWebLinkId = (): string => QUICK_WEB_LINK_PREFIX + crypto.randomUUID();

export const defaultQuickLinks = (authority: Authority, docsUrl: string, translate: (key: string) => string): QuickLinks => {
  const web = (id: string, name: string, icon: string, link: string) => ({[QUICK_WEB_LINK_PREFIX + id]: {name, icon, link}});
  switch (authority) {
    case Authority.SYS_ADMIN:
      return {
        links: ['getting-started', 'tenant-profiles', 'api', 'widgets-library'].map(id => QUICK_WEB_LINK_PREFIX + id),
        webLinks: {
          ...web('getting-started', translate('home.quick-links.getting-started'), 'rocket_launch', docsUrl + 'getting-started/'),
          ...web('tenant-profiles', translate('home.quick-links.tenant-profiles'), 'account_box', docsUrl + 'user-guide/tenant-profiles/'),
          ...web('api', translate('home.quick-links.api'), 'api', docsUrl + 'apis-and-sdks/'),
          ...web('widgets-library', translate('home.quick-links.widgets-library'), 'widgets', docsUrl + 'reference/widgets/widget-library/')
        }
      };
    case Authority.CUSTOMER_USER:
      return {
        links: ['assets', 'entity_views', 'notification_inbox', QUICK_WEB_LINK_PREFIX + 'user-guide'],
        webLinks: web('user-guide', translate('home.quick-links.user-guide'), 'menu_book', docsUrl + 'user-guide/')
      };
    default:
      return {
        links: ['assets', 'device_profiles', 'rule_chains', 'gateways', 'customers', QUICK_WEB_LINK_PREFIX + 'docs'],
        webLinks: web('docs', translate('home.quick-links.documentation'), 'menu_book', docsUrl)
      };
  }
};

export const quickLinkPages = (sections: MenuSection[], translateName: (section: MenuSection) => string,
                               result: QuickLinkPage[] = [], parents: string[] = []): QuickLinkPage[] => {
  for (const section of sections) {
    const name = translateName(section);
    if (section.type === 'link' && section.id !== 'home') {
      result.push({
        id: section.id,
        name,
        icon: section.outlinedIcon || section.icon,
        path: section.path,
        queryParams: section.queryParams,
        crumb: parents.join(' • ')
      });
    }
    if (section.pages?.length) {
      quickLinkPages(section.pages, translateName, result, [...parents, name]);
    }
  }
  return result;
};

export const quickLinkIconFromUrl = (url: URL): string => {
  const text = (url.hostname + url.pathname).toLowerCase();
  if (/(^|\.)docs?\.|\/docs?(\/|$)|wiki|confluence|readme|manual/.test(text)) {
    return 'menu_book';
  }
  if (/grafana|kibana|metabase|superset|powerbi|tableau/.test(text)) {
    return 'insert_chart';
  }
  if (/github|gitlab|bitbucket/.test(text)) {
    return 'code';
  }
  if (/support|helpdesk|zendesk|freshdesk|servicenow|jira/.test(text)) {
    return 'support_agent';
  }
  return 'link';
};

export const quickLinkNameFromUrl = (url: URL): string => {
  const segment = url.pathname.split('/').filter(Boolean).pop();
  if (segment) {
    let text: string;
    try {
      text = decodeURIComponent(segment);
    } catch {
      text = segment;
    }
    text = text.replace(/[-_]+/g, ' ').replace(/\.\w+$/, '').trim();
    if (text) {
      return text.charAt(0).toUpperCase() + text.slice(1);
    }
  }
  return url.hostname.replace(/^www\./, '');
};

export const parseQuickWebLink = (raw: string): {url?: URL; error?: 'scheme' | 'invalid'} => {
  const value = raw.trim();
  if (!value) {
    return {};
  }
  const withScheme = /^[a-z][a-z0-9+.-]*:/i.test(value) ? value : 'https://' + value;
  let url: URL;
  try {
    url = new URL(withScheme);
  } catch {
    return {error: 'invalid'};
  }
  if (!/^https?:$/.test(url.protocol)) {
    return {error: 'scheme'};
  }
  if (!/\./.test(url.hostname) && url.hostname !== 'localhost') {
    return {error: 'invalid'};
  }
  return {url};
};
