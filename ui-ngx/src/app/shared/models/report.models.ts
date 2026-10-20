// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityInfoData, HasTenantId, HasVersion } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import {
  Filter,
  Filters,
  KeyFilter,
  keyFilterInfosToKeyFilters,
  keyFiltersToKeyFilterInfos
} from '@shared/models/query/query.models';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { ReportId } from '@shared/models/id/report-id';
import { UserId } from '@shared/models/id/user-id';
import { PageLink } from '@shared/models/page/page-link';
import {
  isArraysEqualIgnoreUndefined,
  isDefinedAndNotNull,
  isEmpty,
  isEqualIgnoreUndefined,
  isUndefinedOrNull
} from '@core/utils';
import { NotificationTemplateId } from '@shared/models/id/notification-template-id';
import { SchedulerEventInfo } from '@shared/models/scheduler-event.models';

export const reportOriginatorTypes: EntityType[] = [
  EntityType.DEVICE,
  EntityType.ASSET,
  EntityType.ENTITY_VIEW,
  EntityType.CUSTOMER,
  EntityType.USER,
  EntityType.DASHBOARD,
  EntityType.EDGE
];

export interface Report extends BaseData<ReportId>, HasTenantId {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  format: TbReportFormat;
  userId: UserId;
  public?: boolean;
  publicKey?: string;
}

export interface ReportInfo extends Report {
  templateInfo: EntityInfoData;
  customerTitle: string;
  userName: string;
}

export enum ReportTemplateType {
  REPORT = 'REPORT',
  SUB_REPORT = 'SUB_REPORT'
}

export const reportTemplateTypes = Object.keys(ReportTemplateType) as ReportTemplateType[];

export const reportTemplateTypeTranslationMap = new Map<ReportTemplateType, string>(
  [
    [ReportTemplateType.REPORT, 'report-template.type-report'],
    [ReportTemplateType.SUB_REPORT, 'report-template.type-sub-report']
  ]
);

export interface BaseReportTemplate extends BaseData<ReportTemplateId>, HasTenantId, HasVersion, ExportableEntity<ReportTemplateId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  format: TbReportFormat;
  type: ReportTemplateType;
  description?: string;
}

export interface ReportTemplateInfo extends BaseReportTemplate {
  ownerId?: EntityId;
  ownerName?: string;
}

export interface HeaderFooter {
  enabled: boolean;
  components: ReportComponentConfig[];
  firstPage?: HeaderFooter;
}

export interface ReportDataFilter {
  id: string;
  filter: string;
  keyFilters: Array<KeyFilter>;
}

export const entityAliasesToList = (entityAliases: EntityAliases): EntityAlias[] => {
  const entityAliasesList: EntityAlias[] = [];
  for (const id of Object.keys(entityAliases)) {
    entityAliasesList.push(entityAliases[id]);
  }
  return entityAliasesList;
}

export const entityAliasesListToAliases = (entityAliasesList: EntityAlias[]): EntityAliases => {
  const entityAliases: EntityAliases = {};
  if (entityAliasesList) {
    for (const entityAlias of entityAliasesList) {
      entityAliases[entityAlias.id] = entityAlias;
    }
  }
  return entityAliases;
}

export const filtersToReportDataFilterList = (filters: Filters): ReportDataFilter[] => {
  const reportDataFilters: ReportDataFilter[] = [];
  for (const id of Object.keys(filters)) {
    reportDataFilters.push(filterToReportDataFilter(filters[id]));
  }
  return reportDataFilters;
}

export const reportDataFilterListToFilters = (reportDataFilters: ReportDataFilter[]): Filters => {
  const filters: Filters = {};
  if (reportDataFilters) {
    for (const filter of reportDataFilters) {
      filters[filter.id] = reportDataFilterToFilter(filter);
    }
  }
  return filters;
}

export const reportDataFilterToFilter = (reportDataFilter: ReportDataFilter): Filter => {
  const keyFilterInfos = keyFiltersToKeyFilterInfos(reportDataFilter.keyFilters);
  return {
    id: reportDataFilter.id,
    filter: reportDataFilter.filter,
    keyFilters: keyFilterInfos,
    editable: false
  };
}

export const filterToReportDataFilter = (filter: Filter): ReportDataFilter => {
  const keyFilters = keyFilterInfosToKeyFilters(filter.keyFilters);
  return {
    id: filter.id,
    filter: filter.filter,
    keyFilters
  };
}

export enum TbReportFormat {
  PDF = 'PDF',
  CSV = 'CSV'
}

export const reportFormats = Object.keys(TbReportFormat) as TbReportFormat[];

export interface ReportTemplateConfig {
  format: TbReportFormat;
  namePattern: string;
  timeDataPattern?: string;
  entityAliases: EntityAlias[];
  filters: ReportDataFilter[];
  components: ReportComponentConfig[];
}

export interface ReportTemplateSettings {
  name: string;
  namePattern: string;
  timeDataPattern?: string;
  description?: string;
}

export enum PageSize {
  A4 = 'A4',
  LETTER = 'LETTER',
  LEGAL = 'LEGAL',
  A5 = 'A5',
  A3 = 'A3',
  TABLOID = 'TABLOID'
}

export const pageSizes = Object.keys(PageSize) as PageSize[];

export const paperSizeDisplayMap = new Map<PageSize, string>(
  [
    [PageSize.A4, 'A4'],
    [PageSize.LETTER, 'US Letter'],
    [PageSize.LEGAL, 'US Legal'],
    [PageSize.A5, 'A5'],
    [PageSize.A3, 'A3'],
    [PageSize.TABLOID, 'Tabloid']
  ]
);

export const paperSizeToPointsMap = new Map<PageSize, [number, number]>(
  [
    [PageSize.A4, [595, 842]],
    [PageSize.LETTER, [612, 792]],
    [PageSize.LEGAL, [612, 1008]],
    [PageSize.A5, [420, 595]],
    [PageSize.A3, [842, 1191]],
    [PageSize.TABLOID, [792, 1224]]
  ]
);

export enum PageOrientation {
  PORTRAIT = 'PORTRAIT',
  LANDSCAPE = 'LANDSCAPE'
}

export const pageOrientations = Object.keys(PageOrientation) as PageOrientation[];

export const pageOrientationTranslationMap = new Map<PageOrientation, string>(
  [
    [PageOrientation.PORTRAIT, 'report-template.orientation-portrait'],
    [PageOrientation.LANDSCAPE, 'report-template.orientation-landscape']
  ]
);

export interface Insets {
  left?: number;
  right?: number;
  top?: number;
  bottom?: number;
}

export interface PdfReportTemplateConfig extends ReportTemplateConfig {
  pageSize: PageSize;
  pageOrientation: PageOrientation;
  pageMargins: Insets;
  pageBackground?: string;
  header: HeaderFooter;
  footer: HeaderFooter;
  format: TbReportFormat.PDF;
}

export const isPdfReportTemplateConfig = (obj: any): obj is PdfReportTemplateConfig => {
  return typeof obj === 'object' && obj !== null && 'format' in obj && obj.format === TbReportFormat.PDF;
}

export interface CsvReportTemplateConfig extends ReportTemplateConfig {
  format: TbReportFormat.CSV;
}

export interface PdfReportTemplateSettings extends ReportTemplateSettings {
  pageSize: PageSize;
  pageOrientation: PageOrientation;
  pageMargins: Insets;
  pageBackground?: string;
}

export interface ReportTemplate<Config extends ReportTemplateConfig = ReportTemplateConfig> extends BaseReportTemplate {
  configuration: Config;
}

export const toReportTemplateSettings = (reportTemplate: ReportTemplate): ReportTemplateSettings => {
  const settings: ReportTemplateSettings = {
    name: reportTemplate.name,
    namePattern: reportTemplate.configuration.namePattern,
    timeDataPattern: reportTemplate.configuration.timeDataPattern,
    description: reportTemplate.description
  };
  if (reportTemplate.format === TbReportFormat.PDF) {
    const pdfSettings = settings as PdfReportTemplateSettings;
    const pdfConfig = reportTemplate.configuration as PdfReportTemplateConfig;
    pdfSettings.pageSize = pdfConfig.pageSize;
    pdfSettings.pageOrientation = pdfConfig.pageOrientation;
    pdfSettings.pageMargins = pdfConfig.pageMargins;
    pdfSettings.pageBackground = pdfConfig.pageBackground;
  }
  return settings;
}

export const updateFromReportTemplateSettings =
  (reportTemplate: ReportTemplate, settings: ReportTemplateSettings): void => {
    reportTemplate.name = settings.name;
    reportTemplate.configuration.namePattern = settings.namePattern;
    reportTemplate.configuration.timeDataPattern = settings.timeDataPattern;
    reportTemplate.description = settings.description;
    if (reportTemplate.format === TbReportFormat.PDF) {
      const pdfConfiguration = reportTemplate.configuration as PdfReportTemplateConfig;
      const pdfSettings = settings as PdfReportTemplateSettings;
      pdfConfiguration.pageSize = pdfSettings.pageSize;
      pdfConfiguration.pageOrientation = pdfSettings.pageOrientation;
      pdfConfiguration.pageMargins = pdfSettings.pageMargins;
      pdfConfiguration.pageBackground = pdfSettings.pageBackground;
    }
}

export interface ReportRequest {
  reportTemplateId?: ReportTemplateId;
  reportTemplateConfig?: ReportTemplateConfig;
  userId?: string;
  timezone?: string;
  originator?: EntityId;
  targets?: Array<string>;
  notificationTemplateId?: NotificationTemplateId;
  makePublic?: boolean;
}

export interface ReportConfig {
  reportTemplateId: ReportTemplateId;
  userId: UserId;
  timezone: string;
  targets?: Array<string>;
  notificationTemplateId?: NotificationTemplateId;
  makePublic?: boolean;
}

export interface ReportTemplateFilter {
  includeCustomers?: boolean;
  formatList?: TbReportFormat[];
  typeList?: ReportTemplateType[];
}


export const reportTemplateFiltersEquals = (filter1?: ReportTemplateFilter, filter2?: ReportTemplateFilter): boolean => {
  if (filter1 === filter2) {
    return true;
  }
  if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
    return true;
  } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
    if (!isArraysEqualIgnoreUndefined(filter1.typeList, filter2.typeList)) {
      return false;
    }
    if (!isArraysEqualIgnoreUndefined(filter1.formatList, filter2.formatList)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.includeCustomers, filter2.includeCustomers)) {
      return false;
    }
    return true;
  }
  return false;
}

export class ReportTemplateQuery {

  pageLink: PageLink;
  includeCustomers: boolean;
  formatList: TbReportFormat[];
  typeList: ReportTemplateType[];

  constructor(pageLink: PageLink,
              reportTemplateFilter: ReportTemplateFilter) {
    this.pageLink = pageLink;
    this.includeCustomers = reportTemplateFilter.includeCustomers;
    this.formatList = reportTemplateFilter.formatList;
    this.typeList = reportTemplateFilter.typeList;
  }

  public toQuery(): string {
    let query = this.pageLink.toQuery();
    if (this.includeCustomers) {
      query += '&includeCustomers=true';
    }
    if (this.formatList && this.formatList.length) {
      query += `&formatList=${this.formatList.join(',')}`;
    }
    if (this.typeList && this.typeList.length) {
      query += `&typeList=${this.typeList.join(',')}`;
    }
    return query;
  }

}

export const defaultPdfReportTemplateConfig: PdfReportTemplateConfig = {
  format: TbReportFormat.PDF,
  namePattern: 'report-%d{yyyy-MM-dd_HH:mm:ss}',
  timeDataPattern: 'yyyy-MM-dd HH:mm:ss',
  pageSize: PageSize.A4,
  pageOrientation: PageOrientation.PORTRAIT,
  pageMargins: {
    left: 20,
    right: 20,
    top: 20,
    bottom: 20
  },
  pageBackground: '#fff',
  header: {
    enabled: true,
    components: []
  },
  footer: {
    enabled: true,
    components: []
  },
  entityAliases: [],
  filters: [],
  components: []
};

export const defaultCsvReportTemplateConfig: CsvReportTemplateConfig = {
  format: TbReportFormat.CSV,
  namePattern: 'report-%d{yyyy-MM-dd_HH:mm:ss}',
  timeDataPattern: 'yyyy-MM-dd HH:mm:ss',
  entityAliases: [],
  filters: [],
  components: []
};

export const validateAndUpdateReportTemplate = (reportTemplate: ReportTemplate): ReportTemplate => {
  if (!reportTemplate.format) {
    reportTemplate.format = TbReportFormat.PDF;
  }
  if (!reportTemplate.configuration.format) {
    reportTemplate.configuration.format = reportTemplate.format;
  }
  const configuration = reportTemplate.configuration;
  if (!configuration.components) {
    configuration.components = [];
  }
  configuration.components = configuration.components.map(c => validateAndUpdateReportComponent(c));
  if (reportTemplate.configuration.format === TbReportFormat.PDF) {
    const pdfConfiguration = reportTemplate.configuration as PdfReportTemplateConfig;
    if (!pdfConfiguration.pageSize) {
      pdfConfiguration.pageSize = PageSize.A4;
    }
    if (!pdfConfiguration.pageOrientation) {
      pdfConfiguration.pageOrientation = PageOrientation.PORTRAIT;
    }
    if (!pdfConfiguration.pageMargins) {
      pdfConfiguration.pageMargins = {
        left: 20,
        right: 20,
        top: 20,
        bottom: 20
      };
    }
    if (!pdfConfiguration.pageBackground) {
      pdfConfiguration.pageBackground = '#fff';
    }
    pdfConfiguration.header = validateAndUpdateReportTemplateHeaderFooter(pdfConfiguration.header);
    pdfConfiguration.footer = validateAndUpdateReportTemplateHeaderFooter(pdfConfiguration.footer);
  }
  return reportTemplate;
}

const validateAndUpdateReportTemplateHeaderFooter = (headerFooter: HeaderFooter): HeaderFooter => {
  if (!headerFooter) {
    headerFooter = { enabled: true, components: [], firstPage: { enabled: false, components: [] } };
  }
  if (!headerFooter.components) {
    headerFooter.components = [];
  }
  headerFooter.components = headerFooter.components.map(c => validateAndUpdateReportComponent(c));
  if (!headerFooter.firstPage) {
    headerFooter.firstPage = { enabled: false, components: [] };
  }
  if (!headerFooter.firstPage.components) {
    headerFooter.firstPage.components = [];
  }
  headerFooter.firstPage.components = headerFooter.firstPage.components.map(c => validateAndUpdateReportComponent(c));
  return headerFooter;
}

export const validateAndUpdateReportComponent = (component: ReportComponentConfig): ReportComponentConfig => {
  return component;
}

export interface ScheduledReportInfo extends SchedulerEventInfo {
  templateInfo: EntityInfoData;
  userName: string;
  customerTitle: string;
}

export interface ReportFilter {
  includeCustomers?: boolean;
  reportTemplateId?: ReportTemplateId;
  userId?: UserId;
}

export const reportFiltersEquals = (filter1?: ReportFilter, filter2?: ReportFilter): boolean => {
  if (filter1 === filter2) {
    return true;
  }
  if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
    return true;
  } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
    if (!isEqualIgnoreUndefined(filter1.reportTemplateId, filter2.reportTemplateId)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.userId, filter2.userId)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.includeCustomers, filter2.includeCustomers)) {
      return false;
    }
    return true;
  }
  return false;
}

export class ReportQuery {

  pageLink: PageLink;
  includeCustomers: boolean;
  reportTemplateId?: ReportTemplateId;
  userId?: UserId;

  constructor(pageLink: PageLink,
              reportFilter: ReportFilter) {
    this.pageLink = pageLink;
    this.includeCustomers = reportFilter.includeCustomers;
    this.reportTemplateId = reportFilter.reportTemplateId;
    this.userId = reportFilter.userId;
  }

  public toQuery(): string {
    let query = this.pageLink.toQuery();
    if (this.includeCustomers) {
      query += '&includeCustomers=true';
    }
    if (this.reportTemplateId?.id) {
      query += `&reportTemplateId=${this.reportTemplateId.id}`;
    }
    if (this.userId?.id) {
      query += `&userId=${this.userId.id}`;
    }
    return query;
  }

}
