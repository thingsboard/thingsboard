// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface IQueue {
    name: string;
    init(): Promise<void>;
    send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any>;
    destroy(): Promise<void>;
}
