// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
