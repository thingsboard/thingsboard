// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import './inspectorShim';
import express from 'express';
import bodyParser from 'body-parser';
import config from 'config';
import { _logger, flushLogs } from './config/logger';
import { Browser, chromium, LaunchOptions } from 'playwright-core';
import { resolveBrowserExecutable } from './browserExecutable';
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
let logsFlushed = false;
let exiting: Promise<void> | undefined;

(async() => {
    try {
        logger.info('Starting ThingsBoard Web Report Microservice...');

        const executable = resolveBrowserExecutable(message => logger.warn(message));
        logger.info('Chrome headless browser executable: %s (%s)', executable.path, executable.source);
        const executablePath = executable.path;

        const browserOptions: LaunchOptions  = {
            headless: true,
            handleSIGHUP: false,
            handleSIGINT: false,
            handleSIGTERM: false,
            args: ['--no-sandbox'],
            timeout: Number(config.get('browser.launchTimeout')),
            executablePath
        };
        browser = await chromium.launch(browserOptions);

        logger.info('Started chrome headless browser.');

        const ver = browser.version();

        logger.info('Headless chrome browser version: %s', ver);

        pagesQueue = new TbWebReportPageQueue(browser, maxPages, useNewPage, () => exit(-1));
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
    if (!logsFlushed) {
        logger.info(`ThingsBoard Web Reporting Microservice has been stopped. Exit code: ${code}.`);
    }
});

// The only way the service exits, so that logs are flushed. Later calls (another signal, an error during shutdown)
// wait for the first one.
function exit(status: number): Promise<void> {
    if (!exiting) {
        exiting = shutdown(status);
    }
    return exiting;
}

async function shutdown(status: number) {
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
    logger.info(`ThingsBoard Web Reporting Microservice has been stopped. Exit code: ${status}.`);
    await flushLogs();
    logsFlushed = true;
    process.exit(status);
}
