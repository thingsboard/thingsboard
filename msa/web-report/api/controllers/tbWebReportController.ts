// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { _logger } from '../../config/logger';
import { TbWebReportPageQueue } from './tbWebReportPageQueue';
import { Request, Response } from 'express';
import config from 'config';
import { GenerateReportRequest, parseGenerateReportRequest, RequestState } from './tbWebReportModels';

const logger = _logger('ReportController');
const generateReportTimeout = Number(config.get('browser.generateReportTimeout'));
const localhostBaseUrlOverride = (config.get('browser.localhostBaseUrlOverride') as string ?? '').trim();

let activeRequestsCount = 0;

export function genDashboardReport(req: Request, res: Response, queue: TbWebReportPageQueue) {
    let request: GenerateReportRequest;
    try {
        request = parseGenerateReportRequest(req, localhostBaseUrlOverride);
    } catch (e: any) {
        res.statusMessage = 'Incorrect request';
        res.status(400).send(e.message);
        return;
    }
    activeRequestsCount++;
    logger.info('Generating dashboard report: baseUrl %s, dashboardId: %s. Active requests count: %s', request.baseUrl, request.dashboardId, activeRequestsCount);
    const requestState: RequestState = {
        closed: false,
        timeout: false
    };
    req.socket.on('close', () => {
        requestState.closed = true;
    });
    const timeoutTimer = setTimeout(() => {
        requestState.timeout = true;
    }, generateReportTimeout);
    queue.generateDashboardReport(requestState, request).then(
        (reportBuffer) => {
            clearTimeout(timeoutTimer);

            const originalFilename = request.name + request.reportContentType.ext;
            const encodedFilename = encodeURIComponent(originalFilename);
            res.set('Content-Disposition', `attachment; filename="${originalFilename.replace(/[^a-zA-Z0-9.-]/g, '_')}";  filename*=UTF-8''${encodedFilename}`);

            res.contentType(request.reportContentType.contentType);
            res.send(reportBuffer);
            activeRequestsCount--;
            logger.info('Report data sent. Active requests count: %s', activeRequestsCount);
        },
        (e) => {
            clearTimeout(timeoutTimer);
            logger.error(e);
            if (requestState.timeout) {
                res.statusMessage = 'Generate report timeout!';
                res.status(503).end();
            } else {
                res.statusMessage = `Failed to load dashboard page`;
                res.status(500).send(`Failed to load dashboard page: ${e.message || e}`);
            }
            activeRequestsCount--;
        }
    );
}
