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


export interface TbMessage {
    scriptHash: string;
}

export interface RemoteJsRequest {
    compileRequest?: JsCompileRequest;
    invokeRequest?: JsInvokeRequest;
    releaseRequest?: JsReleaseRequest;
}

export interface JsReleaseRequest extends TbMessage {
    functionName: string;
}

export interface JsInvokeRequest extends TbMessage {
    functionName: string;
    scriptBody: string;
    timeout: number;
    args: string[];
}

export interface JsCompileRequest extends TbMessage {
    functionName: string;
    scriptBody: string;
}


export interface  JsReleaseResponse extends TbMessage {
    success: boolean;
}

export interface JsCompileResponse extends TbMessage {
    success: boolean;
    errorCode?: number;
    errorDetails?: string;
}

export interface JsInvokeResponse {
    success: boolean;
    result?: string;
    errorCode?: number;
    errorDetails?: string;
}

export interface RemoteJsResponse {
    requestIdMSB: string;
    requestIdLSB: string;
    compileResponse?: JsCompileResponse;
    invokeResponse?: JsInvokeResponse;
    releaseResponse?: JsReleaseResponse;
}
