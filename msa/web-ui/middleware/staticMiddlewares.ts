// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import express, { Request, RequestHandler, Response } from 'express';
import compression from 'compression';
import historyApiFallback from 'connect-history-api-fallback';
import { precompressedStatic } from './precompressedStatic';

export interface StaticMiddlewaresOptions {
    compressionEnabled: boolean;
    precompressedEncodings: string[];
}

export function staticMiddlewares(root: string, {compressionEnabled, precompressedEncodings}: StaticMiddlewaresOptions): RequestHandler[] {
    const middlewares: RequestHandler[] = [historyApiFallback()];
    if (compressionEnabled) {
        if (precompressedEncodings.length) {
            middlewares.push(precompressedStatic(root, precompressedEncodings));
        }
        middlewares.push(compression({filter: compressibleResponse}));
    }
    middlewares.push(express.static(root));
    return middlewares;
}

// Content-Range of a 206 describes identity bytes, so compressing the slice would make it undecodable
function compressibleResponse(req: Request, res: Response): boolean {
    if (res.getHeader('Content-Range')) {
        // compression() adds Vary only to responses that pass its filter
        res.vary('Accept-Encoding');
        return false;
    }
    return compression.filter(req, res);
}
