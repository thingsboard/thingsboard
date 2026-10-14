// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Request } from 'express';

export interface RequestState {
    closed: boolean;
    timeout: boolean;
}

export type reportType = 'png' | 'jpeg' | 'pdf';

export interface GenerateReportRequest {
    baseUrl: string;
    dashboardId: string;
    type: reportType;
    name: string;
    accessToken?: string;
    publicId?: string;
    state?: string;
    pageWidth?: number;
    reportTimewindow?: string;
    timezone: string;
    reportContentType: ReportContentType;
}

export interface ReportContentType {
    contentType: string;
    ext: string;
}

export interface ReportResultMessage {
    success: boolean;
    error?: string;
    pageHeight?: number;
}

export interface WaitWidgetsMessage {
    timeout: number;
}

export interface OpenReportMessage {
    dashboardId: string;
    timeout: number;
    accessToken?: string;
    publicId?: string;
    state?: string;
    reportTimewindow?: object;
}

export const reportContentTypeMap = new Map<reportType, ReportContentType>(
    [
        [
            'pdf',
            {
                contentType: 'application/pdf',
                ext: '.pdf'
            }
        ],
        [
            'jpeg',
            {
                contentType: 'image/jpeg',
                ext: '.jpg'
            }
        ],
        [
            'png',
            {
                contentType: 'image/png',
                ext: '.png'
            }
        ]
    ]
);

export function parseGenerateReportRequest(req: Request, localhostBaseUrlOverride?: string): GenerateReportRequest {
    const body = req.body;
    if (body.baseUrl && body.dashboardId) {
        let baseUrl = body.baseUrl;
        let type: reportType = 'pdf';
        let state: string | undefined;
        let publicId: string | undefined;
        let reportTimewindow: string | undefined;
        let timezone = 'Europe/London';
        let pageWidth: number | undefined;
        if (localhostBaseUrlOverride) {
            const hostname = new URL(baseUrl).hostname.toLowerCase();
            if (hostname === 'localhost' || hostname === '127.0.0.1') {
                baseUrl = localhostBaseUrlOverride;
            }
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        const reportParams = body.reportParams;
        if (reportParams) {
            if (reportParams.type && reportParams.type.length) {
                type = reportParams.type;
            }
            state = reportParams.state;
            publicId = reportParams.publicId;
            if (reportParams.timewindow) {
                reportTimewindow = JSON.stringify(reportParams.timewindow);
            }
            if (typeof reportParams.timezone === 'string') {
                timezone = reportParams.timezone;
            }
            if (reportParams.pageWidth) {
                const pageWidthValue = Number.parseInt(reportParams.pageWidth);
                if (Number.isInteger(pageWidthValue) && pageWidthValue > 0) {
                    pageWidth = pageWidthValue;
                }
            }
        }
        const reportContentType = reportContentTypeMap.get(type);
        if (!reportContentType) {
            throw new Error(`Unsupported report type format: ${type}`);
        }
        const generateReportRequest: GenerateReportRequest = {
            accessToken: body.token,
            dashboardId: body.dashboardId,
            name: body.name,
            baseUrl,
            type,
            reportContentType,
            publicId,
            state,
            reportTimewindow,
            timezone,
            pageWidth
        };
        return generateReportRequest;
    } else {
        throw new Error('Base url or Dashboard Id parameters are missing');
    }
}
