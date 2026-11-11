// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import './inspectorShim';
import express from 'express';
import bodyParser from 'body-parser';
import config from 'config';
import { _logger } from './config/logger';
import { chromium } from 'playwright-chromium';
import { Browser, LaunchOptions } from 'playwright-core';
import { route } from './api/routes/tbWebReportRoutes';
import { TbWebReportPageQueue } from './api/controllers/tbWebReportPageQueue';

const logger = _logger('main');
const app = express();

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

const address: string = config.get('server.address');
const port = Number(config.get('server.port'));
const maxPages = Number(config.get('browser.maxPages'));
const useNewPage = Boolean(config.get('browser.useNewPage'));

logger.info('Bind address: %s', address);
logger.info('Bind port: %s', port);

let browser: Browser;
let pagesQueue: TbWebReportPageQueue;

(async() => {
    try {
        logger.info('Starting ThingsBoard Web Report Microservice...');

        const browserOptions: LaunchOptions  = {
            headless: true,
            handleSIGHUP: false,
            handleSIGINT: false,
            handleSIGTERM: false,
            args: ['--no-sandbox'],
            timeout: Number(config.get('browser.launchTimeout')),
            channel: 'chrome'
        };
        if (typeof process.env.CHROME_EXECUTABLE !== 'undefined') {
            logger.info('Chrome headless browser executable: %s', process.env.CHROME_EXECUTABLE);
            browserOptions.executablePath = process.env.CHROME_EXECUTABLE;
        }
        browser = await chromium.launch(browserOptions);

        logger.info('Started chrome headless browser.');

        const ver = browser.version();

        logger.info('Headless chrome browser version: %s', ver);

        pagesQueue = new TbWebReportPageQueue(browser, maxPages, useNewPage);
        await pagesQueue.init();

        // @ts-ignore
        route(app, pagesQueue); //register the route
        app.use((req, res) => {
            res.statusMessage = req.originalUrl + ' not found';
            res.status(404).end();
        });
        app.listen(port, address, () => {
            logger.info('==> 🌎  ThingsBoard Web Report Service listening on http://%s:%s/.', address, port);
            logger.info('Started ThingsBoard Web Report Microservice.');
        }).on('error', async (error) => {
            logger.error('Failed to start ThingsBoard Web Report Microservice: %s', error.message);
            logger.error(error);
            await exit(-1);
        });
    } catch (e: any) {
        logger.error('Failed to start ThingsBoard Web Report Microservice: %s', e.message);
        logger.error(e);
        await exit(-1);
    }
})();

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    });
})

process.on('exit', async (code: number) => {
    logger.info(`ThingsBoard Web Reporting Microservice has been stopped. Exit code: ${code}.`);
});

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    if (pagesQueue) {
        await pagesQueue.destroy();
    }
    if (browser) {
        logger.info('Closing browser...');
        try {
            await browser.close();
        } catch (e) {
            logger.error(e);
        }
        logger.info('Browser closed.');
    }
    process.exit(status);
}
