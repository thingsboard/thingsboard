///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { FunctionArg, FunctionArgType, TbEditorCompletions } from '@shared/models/ace/completion.models';

export const entityIdHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/id/entity-id.ts#L20">EntityId</a>';

export const entityTypeHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/entity-type.models.ts#L36">EntityType</a>';

export const pageDataHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/page/page-data.ts#L17">PageData</a>';

export const deviceInfoHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/device.models.ts#L33">DeviceInfo</a>';

export const deviceHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/device.models.ts#L24">Device</a>';

export const deviceCredentialsHref = '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/device.models.ts#L50">DeviceCredentials</a>';

export const pageLinkArg: FunctionArg = {
  name: 'pageLink',
  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/page/page-link.ts#L68">PageLink</a>',
  description: 'Page link object used to perform paginated request.'
};

export const requestConfigArg: FunctionArg = {
  name: 'config',
  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/http-utils.ts#L21">RequestConfig</a>',
  description: 'HTTP request configuration.',
  optional: true
};

export function observableReturnType(objectType: string): FunctionArgType {
  return {
    type: `Observable&lt;${objectType}&gt;`,
    description: `An <code>Observable</code> of <code>${objectType}</code> object.`
  };
}

export function observableVoid(): FunctionArgType {
  return {
    type: `Observable&lt;void&gt;`,
    description: `An <code>Observable</code>.`
  };
}

export function observableArrayReturnType(objectType: string): FunctionArgType {
  return {
    type: `Observable&lt;Array&lt;${objectType}&gt;&gt;`,
    description: `An <code>Observable</code> of array of <code>${objectType}</code> objects.`
  };
}

export function observablePageDataReturnType(objectType: string): FunctionArgType {
  return {
    type: `Observable&lt;${pageDataHref}&lt;${objectType}&gt;&gt;`,
    description: `An <code>Observable</code> of page result as a <code>${pageDataHref}</code> holding array of <code>${objectType}</code> objects.`
  };
}

export const serviceCompletions: TbEditorCompletions = {
  deviceService: {
    description: 'Device Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/device.service.ts#L37">DeviceService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/device.service.ts#L37">DeviceService</a>',
    children: {
      getTenantDeviceInfos: {
        description: 'Get tenant devices',
        meta: 'function',
        args: [
          pageLinkArg,
          { name: 'type', type: 'string', optional: true, description: 'Device type'},
          requestConfigArg
        ],
        return: observablePageDataReturnType(deviceInfoHref)
      },
      getCustomerDeviceInfos: {
        description: 'Get customer devices',
        meta: 'function',
        args: [
          { name: 'customerId', type: 'string', description: 'Id of the customer'},
          pageLinkArg,
          { name: 'type', type: 'string', optional: true, description: 'Device type'},
          requestConfigArg
        ],
        return: observablePageDataReturnType(deviceInfoHref)
      },
      getDevice: {
        description: 'Get device by id',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableReturnType(deviceHref)
      },
      getDevices: {
        description: 'Get devices by ids',
        meta: 'function',
        args: [
          { name: 'deviceIds', type: 'Array<string>', description: 'List of device ids'},
          requestConfigArg
        ],
        return: observableArrayReturnType(deviceHref)
      },
      getDeviceInfo: {
        description: 'Get device info by id',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableReturnType(deviceInfoHref)
      },
      saveDevice: {
        description: 'Save device',
        meta: 'function',
        args: [
          { name: 'device', type: deviceHref, description: 'Device object to save'},
          requestConfigArg
        ],
        return: observableReturnType(deviceHref)
      },
      deleteDevice: {
        description: 'Delete device by id',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableVoid()
      },
      getDeviceTypes: {
        description: 'Get all available devices types',
        meta: 'function',
        args: [
          requestConfigArg
        ],
        return: observableArrayReturnType('<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/entity-type.models.ts#L295">EntitySubtype</a>')
      },
      getDeviceCredentials: {
        description: 'Get device credentials by device id',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          { name: 'sync', type: 'boolean', description: 'Whether to execute HTTP request synchronously (false by default)', optional: true},
          requestConfigArg
        ],
        return: observableReturnType(deviceCredentialsHref)
      },
      saveDeviceCredentials: {
        description: 'Save device credentials',
        meta: 'function',
        args: [
          { name: 'deviceCredentials', type: deviceCredentialsHref, description: 'Device credentials object to save'},
          requestConfigArg
        ],
        return: observableReturnType(deviceCredentialsHref)
      },
      makeDevicePublic: {
        description: 'Make device public (available from public dashboard)',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableReturnType(deviceHref)
      },
      assignDeviceToCustomer: {
        description: 'Assign device to specific customer',
        meta: 'function',
        args: [
          { name: 'customerId', type: 'string', description: 'Id of the customer'},
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableReturnType(deviceHref)
      },
      unassignDeviceFromCustomer: {
        description: 'Unassign device from any customer',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          requestConfigArg
        ],
        return: observableVoid()
      },
      sendOneWayRpcCommand: {
        description: 'Send one way (without response) RPC command to the device.',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          { name: 'requestBody', type: 'object', description: 'Request body to be sent to device'},
          requestConfigArg
        ],
        return: {
          type: `Observable&lt;any&gt;`,
          description: `A command execution <code>Observable</code>.`
        }
      },
      sendTwoWayRpcCommand: {
        description: 'Sends two way (with response) RPC command to the device.',
        meta: 'function',
        args: [
          { name: 'deviceId', type: 'string', description: 'Id of the device'},
          { name: 'requestBody', type: 'object', description: 'Request body to be sent to device'},
          requestConfigArg
        ],
        return: {
          type: `Observable&lt;any&gt;`,
          description: `A command execution <code>Observable</code> of response body.`
        }
      },
      findByQuery: {
        description: 'Find devices by search query',
        meta: 'function',
        args: [
          { name: 'query', type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/device.models.ts#L57">DeviceSearchQuery</a>',
            description: 'Device search query object'},
          requestConfigArg
        ],
        return: observableArrayReturnType(deviceHref)
      },
      findByName: {
        description: 'Find device by name',
        meta: 'function',
        args: [
          { name: 'deviceName', type: 'string',
            description: 'Search device name'},
          requestConfigArg
        ],
        return: observableReturnType(deviceHref)
      },
      claimDevice: {
        description: 'Send claim device request',
        meta: 'function',
        args: [
          { name: 'deviceName', type: 'string',
            description: 'Claiming device name'},
          requestConfigArg
        ],
        return: observableReturnType('<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/device.models.ts#L71">ClaimResult</a>')
      },
      unclaimDevice: {
        description: 'Send un-claim device request',
        meta: 'function',
        args: [
          { name: 'deviceName', type: 'string',
            description: 'Device name to un-claim'},
          requestConfigArg
        ],
        return: observableVoid()
      }
    }
  },
  assetService: {
    description: 'Asset Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/asset.service.ts#L29">AssetService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/asset.service.ts#L29">AssetService</a>'
  },
  entityViewService: {
    description: 'EntityView Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity-view.service.ts#L29">EntityViewService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity-view.service.ts#L29">EntityViewService</a>'
  },
  customerService: {
    description: 'Customer Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/customer.service.ts#L28">CustomerService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/customer.service.ts#L28">CustomerService</a>'
  },
  dashboardService: {
    description: 'Dashboard Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/dashboard.service.ts#L32">DashboardService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/dashboard.service.ts#L32">DashboardService</a>'
  },
  userService: {
    description: 'User Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/user.service.ts#L29">UserService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/user.service.ts#L29">UserService</a>'
  },
  attributeService: {
    description: 'Attribute Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/attribute.service.ts#L28">AttributeService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/attribute.service.ts#L28">AttributeService</a>'
  },
  entityRelationService: {
    description: 'Entity Relation Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity-relation.service.ts#L27">EntityRelationService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity-relation.service.ts#L27">EntityRelationService</a>'
  },
  entityService: {
    description: 'Entity Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity.service.ts#L64">EntityService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/http/entity.service.ts#L64">EntityService</a>'
  },
  dialogs: {
    description: 'Dialogs Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/services/dialog.service.ts#L39">DialogService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/services/dialog.service.ts#L39">DialogService</a>'
  },
  customDialog: {
    description: 'Custom Dialog Service API<br>' +
      'See <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/components/widget/dialog/custom-dialog.service.ts#L33">CustomDialogService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/components/widget/dialog/custom-dialog.service.ts#L33">CustomDialogService</a>'
  },
  date: {
    description: 'Date Pipe<br>Formats a date value according to locale rules.<br>' +
      'See <a href="https://angular.io/api/common/DatePipe">DatePipe</a> for API reference.',
    meta: 'service',
    type: '<a href="https://angular.io/api/common/DatePipe">DatePipe</a>'
  },
  translate: {
    description: 'Translate Service API<br>' +
      'See <a href="https://github.com/ngx-translate/core#translateservice">TranslateService</a> for API reference.',
    meta: 'service',
    type: '<a href="https://github.com/ngx-translate/core#translateservice">TranslateService</a>'
  },
  http: {
    description: 'HTTP Client Service<br>' +
      'See <a href="https://angular.io/api/common/http/HttpClient">HttpClient</a> for API reference.',
    meta: 'service',
    type: '<a href="https://angular.io/api/common/http/HttpClient">HttpClient</a>'
  }
}
