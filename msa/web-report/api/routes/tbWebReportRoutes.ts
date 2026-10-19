// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Express } from 'express';
import { genDashboardReport } from '../controllers/tbWebReportController';
import { TbWebReportPageQueue } from '../controllers/tbWebReportPageQueue';

export function route(app: Express, queue: TbWebReportPageQueue) {
    app.route('/dashboardReport')
        .post(
            (req, res) => {
                genDashboardReport(req, res, queue);
            }
        );

}
