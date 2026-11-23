// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0

// negotiator 1.x ships no types, and @types/negotiator (0.6) lacks the `preferred` option
declare module 'negotiator' {
    import { IncomingMessage } from 'http';

    export default class Negotiator {
        constructor(request: IncomingMessage);
        encoding(available: string[], options?: { preferred?: string[] }): string | undefined;
    }
}
