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


import { isUndefined } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';

export interface ExceptionData {
  message?: string;
  name?: string;
  lineNumber?: number;
  columnNumber?: number;
}


export const parseException = (exception: any, lineOffset?: number): ExceptionData => {
  const data: ExceptionData = {};
  if (exception) {
    if (typeof exception === 'string') {
      data.message = exception;
    } else if (exception instanceof String) {
      data.message = exception.toString();
    } else {
      if (exception.name) {
        data.name = exception.name;
      } else {
        data.name = 'UnknownError';
      }
      if (exception.message) {
        data.message = exception.message;
      }
      if (exception.lineNumber) {
        data.lineNumber = exception.lineNumber;
        if (exception.columnNumber) {
          data.columnNumber = exception.columnNumber;
        }
      } else if (exception.stack) {
        const lineInfoRegexp = /(.*<anonymous>):(\d*)(:)?(\d*)?/g;
        const lineInfoGroups = lineInfoRegexp.exec(exception.stack);
        if (lineInfoGroups != null && lineInfoGroups.length >= 3) {
          if (isUndefined(lineOffset)) {
            lineOffset = -2;
          }
          data.lineNumber = Number(lineInfoGroups[2]) + lineOffset;
          if (lineInfoGroups.length >= 5) {
            data.columnNumber = Number(lineInfoGroups[4]);
          }
        }
      }
    }
  }
  return data;
}

export const parseError = (err: any): string =>
  parseException(err).message || 'Unknown Error';
