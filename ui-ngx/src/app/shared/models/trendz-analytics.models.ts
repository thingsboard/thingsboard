// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
export interface TrendzSummary {
    metricSummaryItems: MetricSummaryItem[],
    anomalyModelSummaryItems: AnomalyModelSummaryItem[],
    calculationFieldSummaryItems: CalculationFieldSummaryItem[],
    predictionModelSummaryItems: PredictionModelSummaryItem[],
    viewSummaryItems: ViewSummaryItem[],
    aiSummaryItems: AiSummaryItem[],
}

export interface BaseTrendzSummaryItem {
    entityId: string,
    entityName: string,
    updatedTs: number,
}

export interface MetricSummaryItem extends BaseTrendzSummaryItem {
    itemId: string,
    itemName: string,
    metricData: {
        metricId: string,
        metricName: string,
    },
    fieldData: {
        fieldId: string,
        fieldName: string,
        fieldType: string
    }
}

export interface AnomalyModelSummaryItem extends BaseTrendzSummaryItem {
    modelName: string,
    enabled: boolean,
    modelId: string,
}

export interface CalculationFieldSummaryItem extends BaseTrendzSummaryItem {
    calculationName: string,
    enabled: boolean,
    calculationId: string,
}

export interface PredictionModelSummaryItem extends BaseTrendzSummaryItem {
    modelName: string,
    enabled: boolean,
    modelId: string,
}

export interface ViewSummaryItem extends BaseTrendzSummaryItem {
    viewName: string,
    viewType: string,
    viewConfigId: string,
}

export interface AiSummaryItem extends BaseTrendzSummaryItem {
    chatSummary: string,
    lastMessage: string,
    messageCount: number,
    chatId: string,
}

export interface TrendzSummaryItemParam {
    name: string,
    enabled?: boolean,
    updatedTs?: number,
    entityId?: string,
    itemId?: string
}

export interface BaseTrendzSyncInfo {
    type: TrendzSynchronizationResultType,
    status: TrendzSynchronizationStatus,
    version?: string,
}

export interface TrendzSynchronization extends BaseTrendzSyncInfo {
    updatedTs: number;
}

export interface TrendzHealthcheckResult extends BaseTrendzSyncInfo {
    message?: string;
}

export interface TrendzStatus {
    type: TrendzSynchronizationResultType,
    syncStatus: TrendzSynchronizationStatus,
    healthcheckStatus: TrendzSynchronizationStatus,
}

export enum TrendzSynchronizationStatus {
    NOT_AVAILABLE = 'NOT_AVAILABLE',
    AVAILABLE = 'AVAILABLE',
    SYNCED = 'SYNCED',
}

export enum TrendzSynchronizationResultType {
    SYNC_NOT_INITIALIZED = 'SYNC_NOT_INITIALIZED',
    SYNC_COMPLETED = 'SYNC_COMPLETED',
    SYNC_DISABLED = 'SYNC_DISABLED',
    TRENDZ_UNSUPPORTED_VERSION = 'TRENDZ_UNSUPPORTED_VERSION',
    TRENDZ_AUTH_INVALID = 'TRENDZ_AUTH_INVALID',
    TRENDZ_URL_UNREACHABLE = 'TRENDZ_URL_UNREACHABLE',
    TB_URL_MISMATCH = 'TB_URL_MISMATCH',
    TB_URL_UNREACHABLE = 'TB_URL_UNREACHABLE',
    TB_AUTH_INVALID = 'TB_AUTH_INVALID',
    SYNC_INTERNAL_ERROR = 'SYNC_INTERNAL_ERROR',
}

export const TrendzSynchronizationResultTypeTranslationMap = new Map<TrendzSynchronizationResultType, string>([
    [TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, 'trendz-analytics.sync.sync-not-initialized'],
    [TrendzSynchronizationResultType.SYNC_COMPLETED, 'trendz-analytics.sync.sync-completed'],
    [TrendzSynchronizationResultType.SYNC_DISABLED, 'trendz-analytics.sync.sync-disabled'],
    [TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION, 'trendz-analytics.sync.trendz-unsupported-version'],
    [TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID, 'trendz-analytics.sync.trendz-auth-invalid'],
    [TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE, 'trendz-analytics.sync.trendz-url-unreachable'],
    [TrendzSynchronizationResultType.TB_URL_MISMATCH, 'trendz-analytics.sync.tb-url-mismatch'],
    [TrendzSynchronizationResultType.TB_URL_UNREACHABLE, 'trendz-analytics.sync.tb-url-unreachable'],
    [TrendzSynchronizationResultType.TB_AUTH_INVALID, 'trendz-analytics.sync.tb-auth-invalid'],
    [TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR, 'trendz-analytics.sync.sync-internal-error'],
]);

export enum TrendzViewType {
    BAR = 'trendz-analytics.view-type.bar',
    LINE = 'trendz-analytics.view-type.line',
    TABLE = 'trendz-analytics.view-type.table',
    HEATMAP = 'trendz-analytics.view-type.heatmap',
    HEATMAP_CALENDAR = 'trendz-analytics.view-type.heatmap-calendar',
    PIE = 'trendz-analytics.view-type.pie',
    SCATTER_PLOT = 'trendz-analytics.view-type.scatter-plot',
    CARD = 'trendz-analytics.view-type.card',
    CARD_WITH_LINE = 'trendz-analytics.view-type.card-with-line',
    AI_CARD = 'trendz-analytics.view-type.ai-card',
}

export const getMetricLink = (metric: MetricSummaryItem) => {
    const metricId = metric.metricData?.metricId ?? metric.fieldData?.fieldId;
    return `/trendz/metric-explorer?itemId=${encodeURIComponent(metric.itemId)}&metricId=${encodeURIComponent(metricId)}`;
}

export interface TrendzConfiguration {
    trendzUrl: string,
    tbUrl: string
}
