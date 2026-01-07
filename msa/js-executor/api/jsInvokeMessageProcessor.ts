///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import config from 'config';
import { _logger } from '../config/logger';
import { JsExecutor, TbScript } from './jsExecutor';
import { performance } from 'perf_hooks';
import { isString, parseJsErrorDetails, UUIDFromBuffer, UUIDToBits } from './utils';
import { IQueue } from '../queue/queue.models';
import {
    JsCompileRequest,
    JsCompileResponse,
    JsInvokeRequest,
    JsInvokeResponse,
    JsReleaseRequest,
    JsReleaseResponse,
    RemoteJsRequest,
    RemoteJsResponse,
    TbMessage
} from './jsExecutor.models';
import Long from 'long';

const COMPILATION_ERROR = 0;
const RUNTIME_ERROR = 1;
const TIMEOUT_ERROR = 2;
const NOT_FOUND_ERROR = 3;

const statFrequency = Number(config.get('script.stat_print_frequency'));
const memoryUsageTraceFrequency = Number(config.get('script.memory_usage_trace_frequency'));
const scriptBodyTraceFrequency = Number(config.get('script.script_body_trace_frequency'));
const useSandbox = config.get('script.use_sandbox') === 'true';
const maxActiveScripts = Number(config.get('script.max_active_scripts'));
const slowQueryLogMs = Number(config.get('script.slow_query_log_ms'));
const slowQueryLogBody = config.get('script.slow_query_log_body') === 'true';
const maxResultSize = Number(config.get('js.max_result_size'));

export class JsInvokeMessageProcessor {

    private logger = _logger(`JsInvokeMessageProcessor`);
    private producer: IQueue;
    private executor = new JsExecutor(useSandbox);
    private scriptMap = new Map<string, TbScript>();
    private scriptIds: string[] = [];
    private executedScriptIdsCounter: number[] = [];
    private executedScriptsCounter = 0;
    private lastStatTime = performance.now();
    private compilationTime = 0;

    constructor(produced: IQueue) {
        this.producer = produced;
    }

    onJsInvokeMessage(message: any) {
        const tStart = performance.now();
        let requestId = '';
        let responseTopic: string;
        let expireTs;
        let headers;
        let request: RemoteJsRequest = {};
        let buf: Buffer;
        try {
            request = JSON.parse(Buffer.from(message.data).toString('utf8'));
            headers = message.headers;
            buf = Buffer.from(headers.data['requestId']);
            requestId = UUIDFromBuffer(buf);
            buf = Buffer.from(headers.data['responseTopic']);
            responseTopic = buf.toString('utf8');
            buf = Buffer.from(headers.data['expireTs']);
            expireTs = Long.fromBytes(Array.from(buf), false, false).toNumber();

            const now = Date.now();

            if (expireTs && expireTs <= now) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug('Message expired! expireTs is %s, buf is %s. Now is %s, ms to expire left %s', expireTs, buf.toString('hex'), now, expireTs - now)
                }
                return;
            }

            this.logger.debug('[%s] Received request, responseTopic: [%s]', requestId, responseTopic);

            if (request.compileRequest) {
                this.processCompileRequest(requestId, responseTopic, headers, request.compileRequest);
            } else if (request.invokeRequest) {
                this.processInvokeRequest(requestId, responseTopic, headers, request.invokeRequest);
            } else if (request.releaseRequest) {
                this.processReleaseRequest(requestId, responseTopic, headers, request.releaseRequest);
            } else {
                this.logger.error('[%s] Unknown request received!', requestId);
            }

        } catch (err: any) {
            this.logger.error('[%s] Failed to process request: %s', requestId, err.message);
            this.logger.error(err.stack);
        }

        const tFinish = performance.now();
        const tTook = tFinish - tStart;

        if (tTook > slowQueryLogMs) {
            let functionName;
            if (request.invokeRequest) {
                try {
                    buf = Buffer.from(request.invokeRequest['functionName']);
                    functionName = buf.toString('utf8');
                } catch (err: any) {
                    this.logger.error('[%s] Failed to read functionName from message header: %s', requestId, err.message);
                    this.logger.error(err.stack);
                }
            }
            this.logger.warn('[%s] SLOW PROCESSING [%s]ms, functionName [%s]', requestId, tTook, functionName);
            if (slowQueryLogBody) {
                this.logger.info('Slow request body: %s', JSON.stringify(request, null, 4))
            }
        }
    }

    processCompileRequest(requestId: string, responseTopic: string, headers: any, compileRequest: JsCompileRequest) {
        const scriptId = JsInvokeMessageProcessor.getScriptId(compileRequest);
        this.logger.debug('[%s] Processing compile request, scriptId: [%s], compileRequest [%s]', requestId, scriptId, compileRequest);
        if (this.scriptMap.has(scriptId)) {
            const compileResponse = JsInvokeMessageProcessor.createCompileResponse(scriptId, true);
            this.logger.debug('[%s] Script was already compiled, scriptId: [%s]', requestId, scriptId);
            this.sendResponse(requestId, responseTopic, headers, scriptId, compileResponse);
            return;
        }
        this.executor.compileScript(compileRequest.scriptBody).then(
            (script) => {
                this.cacheScript(scriptId, script);
                const compileResponse = JsInvokeMessageProcessor.createCompileResponse(scriptId, true);
                this.logger.debug('[%s] Sending success compile response, scriptId: [%s]', requestId, scriptId);
                this.sendResponse(requestId, responseTopic, headers, scriptId, compileResponse);
            },
            (err) => {
                const compileResponse = JsInvokeMessageProcessor.createCompileResponse(scriptId, false, COMPILATION_ERROR, err);
                this.logger.debug('[%s] Sending failed compile response, scriptId: [%s]', requestId, scriptId);
                this.sendResponse(requestId, responseTopic, headers, scriptId, compileResponse);
            }
        );
    }

    processInvokeRequest(requestId: string, responseTopic: string, headers: any, invokeRequest: JsInvokeRequest) {
        const scriptId = JsInvokeMessageProcessor.getScriptId(invokeRequest);
        this.logger.debug('[%s] Processing invoke request, scriptId: [%s], invokeRequest [%s]', requestId, scriptId, invokeRequest);
        this.executedScriptsCounter++;
        if (this.executedScriptsCounter % statFrequency == 0) {
            const nowMs = performance.now();
            const msSinceLastStat = nowMs - this.lastStatTime;
            const requestsPerSec = msSinceLastStat == 0 ? statFrequency : statFrequency / msSinceLastStat * 1000;
            this.lastStatTime = nowMs;
            this.logger.info('STAT[%s]: requests [%s], took [%s]ms, request/s [%s], compilation [%s]ms', this.executedScriptsCounter, statFrequency, msSinceLastStat, requestsPerSec, this.compilationTime);
            this.compilationTime = 0;
        }

        if (this.executedScriptsCounter % scriptBodyTraceFrequency == 0) {
            this.logger.info('[%s] Executing script body: [%s]', scriptId, invokeRequest.scriptBody);
        }
        if (this.executedScriptsCounter % memoryUsageTraceFrequency == 0) {
            this.logger.info('Current memory usage: %s', JSON.stringify(process.memoryUsage()));
        }

        this.getOrCompileScript(scriptId, invokeRequest.scriptBody).then(
            (script) => {
                this.executor.executeScript(script, invokeRequest.args, invokeRequest.timeout).then(
                    (result: string | undefined) => {
                        if (!result || result.length <= maxResultSize) {
                            const invokeResponse = JsInvokeMessageProcessor.createInvokeResponse(result, true);
                            this.logger.debug('[%s] Sending success invoke response, scriptId: [%s]', requestId, scriptId);
                            this.sendResponse(requestId, responseTopic, headers, scriptId, undefined, invokeResponse);
                        } else {
                            const err = {
                                name: 'Error',
                                message: 'script invocation result exceeds maximum allowed size of ' + maxResultSize + ' symbols'
                            }
                            const invokeResponse = JsInvokeMessageProcessor.createInvokeResponse("", false, RUNTIME_ERROR, err);
                            this.logger.debug('[%s] Script invocation result exceeds maximum allowed size of %s symbols, scriptId: [%s]', requestId, maxResultSize, scriptId);
                            this.sendResponse(requestId, responseTopic, headers, scriptId, undefined, invokeResponse);
                        }
                    },
                    (err: any) => {
                        let errorCode;
                        if (err && isString(err.message) && err.message.includes('Script execution timed out')) {
                            errorCode = TIMEOUT_ERROR;
                        } else {
                            errorCode = RUNTIME_ERROR;
                        }
                        const invokeResponse = JsInvokeMessageProcessor.createInvokeResponse("", false, errorCode, err);
                        this.logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, errorCode);
                        this.sendResponse(requestId, responseTopic, headers, scriptId, undefined, invokeResponse);
                    }
                )
            },
            (err: any) => {
                let errorCode = COMPILATION_ERROR;
                if (err?.name === 'script body not found') {
                    errorCode = NOT_FOUND_ERROR;
                }
                const invokeResponse = JsInvokeMessageProcessor.createInvokeResponse("", false, errorCode, err);
                this.logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, errorCode);
                this.sendResponse(requestId, responseTopic, headers, scriptId, undefined, invokeResponse);
            }
        );
    }

    processReleaseRequest(requestId: string, responseTopic: string, headers: any, releaseRequest: JsReleaseRequest) {
        const scriptId = JsInvokeMessageProcessor.getScriptId(releaseRequest);
        this.logger.debug('[%s] Processing release request, scriptId: [%s], releaseRequest [%s]', requestId, scriptId, releaseRequest);
        if (this.scriptMap.has(scriptId)) {
            const index = this.scriptIds.indexOf(scriptId);
            if (index > -1) {
                this.scriptIds.splice(index, 1);
                this.executedScriptIdsCounter.splice(index, 1);
            }
            this.scriptMap.delete(scriptId);
        }
        const releaseResponse = JsInvokeMessageProcessor.createReleaseResponse(scriptId, true);
        this.logger.debug('[%s] Sending success release response, scriptId: [%s]', requestId, scriptId);
        this.sendResponse(requestId, responseTopic, headers, scriptId, undefined, undefined, releaseResponse);
    }

    sendResponse(requestId: string, responseTopic: string, headers: any, scriptId: string,
                 compileResponse?: JsCompileResponse, invokeResponse?: JsInvokeResponse, releaseResponse?: JsReleaseResponse) {
        const tStartSending = performance.now();
        const remoteResponse = JsInvokeMessageProcessor.createRemoteResponse(requestId, compileResponse, invokeResponse, releaseResponse);
        const rawResponse = Buffer.from(JSON.stringify(remoteResponse), 'utf8');
        this.logger.debug('[%s] Sending response to queue, scriptId: [%s]', requestId, scriptId);
        this.producer.send(responseTopic, requestId, rawResponse, headers).then(
            () => {
                this.logger.debug('[%s] Response sent to queue, took [%s]ms, scriptId: [%s]', requestId, (performance.now() - tStartSending), scriptId);
            },
            (err: any) => {
                if (err) {
                    this.logger.error('[%s] Failed to send response to queue: %s', requestId, err.message);
                    this.logger.error(err.stack);
                }
            }
        );
    }

    getOrCompileScript(scriptId: string, scriptBody: string): Promise<TbScript> {
        const self = this;
        return new Promise(function (resolve, reject) {
            const script = self.scriptMap.get(scriptId);
            if (script) {
                self.incrementUseScriptId(scriptId);
                resolve(script);
            } else if (scriptBody) {
                const startTime = performance.now();
                self.executor.compileScript(scriptBody).then(
                    (compiledScript) => {
                        self.compilationTime += (performance.now() - startTime);
                        self.cacheScript(scriptId, compiledScript);
                        resolve(compiledScript);
                    },
                    (err) => {
                        self.compilationTime += (performance.now() - startTime);
                        reject(err);
                    }
                );
            } else {
                const err = {
                    name: 'script body not found',
                    message: ''
                }
                reject(err);
            }
        });
    }

    cacheScript(scriptId: string, script: TbScript) {
        if (!this.scriptMap.has(scriptId)) {
            this.scriptIds.push(scriptId);
            this.executedScriptIdsCounter.push(0);
            while (this.scriptIds.length > maxActiveScripts) {
                this.logger.info('Active scripts count [%s] exceeds maximum limit [%s]', this.scriptIds.length, maxActiveScripts);
                this.deleteMinUsedScript();
            }
        }
        this.scriptMap.set(scriptId, script);
        this.logger.info("scriptMap size is [%s]", this.scriptMap.size);
    }

    private static createRemoteResponse(requestId: string, compileResponse?: JsCompileResponse,
                                        invokeResponse?: JsInvokeResponse, releaseResponse?: JsReleaseResponse): RemoteJsResponse {
        const requestIdBits = UUIDToBits(requestId);
        return {
            requestIdMSB: requestIdBits[0],
            requestIdLSB: requestIdBits[1],
            compileResponse: compileResponse,
            invokeResponse: invokeResponse,
            releaseResponse: releaseResponse
        };
    }

    private static createCompileResponse(scriptId: string, success: boolean, errorCode?: number, err?: any): JsCompileResponse {
        return {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            scriptHash: scriptId
        };
    }

    private static createInvokeResponse(result: string | undefined, success: boolean, errorCode?: number, err?: any): JsInvokeResponse {
        return {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            result: result
        };
    }

    private static createReleaseResponse(scriptId: string, success: boolean): JsReleaseResponse {
        return {
            success: success,
            scriptHash: scriptId,
        };
    }

    private static getScriptId(request: TbMessage): string {
        return request.scriptHash;
    }

    private incrementUseScriptId(scriptId: string) {
        const index = this.scriptIds.indexOf(scriptId);
        if (this.executedScriptIdsCounter[index] < Number.MAX_SAFE_INTEGER) {
            this.executedScriptIdsCounter[index]++;
        }
    }

    private deleteMinUsedScript() {
        let min = Infinity;
        let minIndex = 0;
        const scriptIdsLength = this.executedScriptIdsCounter.length - 1; // ignored last added script
        for (let i = 0; i < scriptIdsLength; i++) {
            if (this.executedScriptIdsCounter[i] < min) {
                min = this.executedScriptIdsCounter[i];
                minIndex = i;
            }
        }
        const prevScriptId = this.scriptIds.splice(minIndex, 1)[0];
        this.executedScriptIdsCounter.splice(minIndex, 1)
        this.logger.info('Removing active script with id [%s]', prevScriptId);
        this.scriptMap.delete(prevScriptId);
    }
}
