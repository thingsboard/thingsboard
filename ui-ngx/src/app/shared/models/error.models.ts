// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
