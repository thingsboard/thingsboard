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

import { TbEditorCompletion, TbEditorCompletions } from '@shared/models/ace/completion.models';
import { entityIdHref, serviceCompletions } from '@shared/models/ace/service-completion.models';

export const timewindowCompletion: TbEditorCompletion = {
  description: 'Timewindow configuration object',
  meta: 'property',
  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L80">Timewindow</a>',
  children: {
    displayValue: {
      description: 'Current timewindow display value.',
      meta: 'property',
      type: 'string'
    },
    hideInterval: {
      description: 'Whether to hide interval selection in timewindow panel.',
      meta: 'property',
      type: 'boolean'
    },
    hideAggregation: {
      description: 'Whether to hide aggregation selection in timewindow panel.',
      meta: 'property',
      type: 'boolean'
    },
    hideAggInterval: {
      description: 'Whether to hide aggregation interval selection in timewindow panel.',
      meta: 'property',
      type: 'boolean'
    },
    selectedTab: {
      description: 'Current selected timewindow type (0 - realtime, 1 - history).',
      meta: 'property',
      type: 'number'
    },
    realtime: {
      description: 'Realtime timewindow configuration object.',
      meta: 'property',
      type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L39">IntervalWindow</a>',
      children: {
        interval: {
          description: 'Timewindow aggregation interval in milliseconds',
          meta: 'property',
          type: 'number'
        },
        timewindowMs: {
          description: 'Timewindow interval in milliseconds',
          meta: 'property',
          type: 'number'
        }
      }
    },
    history: {
      description: 'History timewindow configuration object.',
      meta: 'property',
      type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L49">HistoryWindow</a>',
      children: {
        historyType: {
          description: 'History timewindow type (0 - last interval, 1 - fixed)',
          meta: 'property',
          type: 'number'
        },
        interval: {
          description: 'Timewindow aggregation interval in milliseconds',
          meta: 'property',
          type: 'number'
        },
        timewindowMs: {
          description: 'Timewindow interval in milliseconds',
          meta: 'property',
          type: 'number'
        },
        fixedTimewindow: {
          description: 'Fixed history timewindow configuration object',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L44">FixedWindow</a>',
          children: {
            startTimeMs: {
              description: 'Timewindow start time in UTC milliseconds',
              meta: 'property',
              type: 'number'
            },
            endTimeMs: {
              description: 'Timewindow end time in UTC milliseconds',
              meta: 'property',
              type: 'number'
            }
          }
        }
      }
    },
    aggregation: {
      description: 'Timewindow aggregation configuration object.',
      meta: 'property',
      type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L74">Aggregation</a>',
      children: {
        interval: {
          description: 'Aggregation interval in milliseconds',
          meta: 'property',
          type: 'number'
        },
        type: {
          description: 'Aggregation type',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L54">AggregationType</a>'
        },
        limit: {
          description: 'Maximum allowed datapoints when aggregation is disabled (<code>AggregationType == \'NONE\'</code>)',
          meta: 'property',
          type: 'number'
        }
      }
    }
  }
}

export const widgetContextCompletions: TbEditorCompletions = {
  ctx: {
    description: 'A reference to widget context that has all necessary API<br>and data used by widget instance.',
    meta: 'object',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L83">WidgetContext</a>',
    children: {
      ...{
        $container: {
          description: 'Container element of the widget.<br>Can be used to dynamically access or modify widget DOM using jQuery API.',
          meta: 'property',
          type: 'jQuery Object'
        },
        $scope: {
          description: 'Reference to the current widget component.<br>Can be used to access/modify component properties when widget is built using Angular approach.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L274">IDynamicWidgetComponent</a>'
        },
        width: {
          description: 'Current width of widget container in pixels.',
          meta: 'property',
          type: 'number'
        },
        height: {
          description: 'Current height of widget container in pixels.',
          meta: 'property',
          type: 'number'
        },
        isEdit: {
          description: 'Indicates whether the dashboard is in in the view or editing state.',
          meta: 'property',
          type: 'boolean'
        },
        isMobile: {
          description: 'Indicates whether the dashboard view is less then 960px width (default mobile breakpoint).',
          meta: 'property',
          type: 'boolean'
        },
        widgetConfig: {
          description: 'Common widget configuration containing properties such as <code>color</code> (text color), <code>backgroundColor</code> (widget background color), etc.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L341">WidgetConfig</a>',
          children: {
            title: {
              description: 'Widget title.',
              meta: 'property',
              type: 'string'
            },
            titleIcon: {
              description: 'Widget title icon.',
              meta: 'property',
              type: 'string'
            },
            showTitle: {
              description: 'Whether to show widget title.',
              meta: 'property',
              type: 'boolean'
            },
            showTitleIcon: {
              description: 'Whether to show widget title icon.',
              meta: 'property',
              type: 'boolean'
            },
            iconColor: {
              description: 'Widget title icon color.',
              meta: 'property',
              type: 'string'
            },
            iconSize: {
              description: 'Widget title icon size.',
              meta: 'property',
              type: 'string'
            },
            titleTooltip: {
              description: 'Widget title tooltip content.',
              meta: 'property',
              type: 'string'
            },
            dropShadow: {
              description: 'Enable/disable widget card shadow.',
              meta: 'property',
              type: 'boolean'
            },
            enableFullscreen: {
              description: 'Whether to enable fullscreen button on widget.',
              meta: 'property',
              type: 'boolean'
            },
            useDashboardTimewindow: {
              description: 'Whether to use dashboard timewindow (applicable for timeseries widgets).',
              meta: 'property',
              type: 'boolean'
            },
            displayTimewindow: {
              description: 'Whether to display timewindow (applicable for timeseries widgets).',
              meta: 'property',
              type: 'boolean'
            },
            showLegend: {
              description: 'Whether to show legend.',
              meta: 'property',
              type: 'boolean'
            },
            legendConfig: {
              description: 'Legend configuration.',
              meta: 'property',
              type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L198">LegendConfig</a>',
              children: {
                position: {
                  description: 'Legend position. Possible values: <code>\'top\', \'bottom\', \'left\', \'right\'</code>',
                  meta: 'property',
                  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L182">LegendPosition</a>',
                },
                direction: {
                  description: 'Legend direction. Possible values: <code>\'column\', \'row\'</code>',
                  meta: 'property',
                  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L170">LegendDirection</a>',
                },
                showMin: {
                  description: 'Whether to display aggregated min values.',
                  meta: 'property',
                  type: 'boolean',
                },
                showMax: {
                  description: 'Whether to display aggregated max values.',
                  meta: 'property',
                  type: 'boolean',
                },
                showAvg: {
                  description: 'Whether to display aggregated average values.',
                  meta: 'property',
                  type: 'boolean',
                },
                showTotal: {
                  description: 'Whether to display aggregated total values.',
                  meta: 'property',
                  type: 'boolean',
                }
              }
            },
            timewindow: timewindowCompletion,
            mobileHeight: {
              description: 'Widget height in mobile mode.',
              meta: 'property',
              type: 'number'
            },
            mobileOrder: {
              description: 'Widget order in mobile mode.',
              meta: 'property',
              type: 'number'
            },
            color: {
              description: 'Widget text color.',
              meta: 'property',
              type: 'string'
            },
            backgroundColor: {
              description: 'Widget background color.',
              meta: 'property',
              type: 'string'
            },
            padding: {
              description: 'Widget card padding.',
              meta: 'property',
              type: 'string'
            },
            margin: {
              description: 'Widget card margin.',
              meta: 'property',
              type: 'string'
            },
            widgetStyle: {
              description: 'Widget element style object.',
              meta: 'property',
              type: 'object'
            },
            titleStyle: {
              description: 'Widget title element style object.',
              meta: 'property',
              type: 'object'
            },
            units: {
              description: 'Optional property defining units text of values displayed by widget. Useful for simple widgets like cards or gauges.',
              meta: 'property',
              type: 'string'
            },
            decimals: {
              description: 'Optional property defining how many positions should be used to display decimal part of the value number.',
              meta: 'property',
              type: 'number'
            },
            actions: {
              description: 'Map of configured widget actions.',
              meta: 'property',
              type: 'object'
            },
            settings: {
              description: 'Object holding widget settings according to widget type.',
              meta: 'property',
              type: 'object'
            },
            alarmSource: {
              description: 'Configured alarm source for alarm widget type.',
              meta: 'property',
              type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L250">Datasource</a>'
            },
            alarmSearchStatus: {
              description: 'Configured default alarm search status for alarm widget type.',
              meta: 'property',
              type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/alarm.models.ts#L41">AlarmSearchStatus</a>'
            },
            alarmsPollingInterval: {
              description: 'Configured alarms polling interval for alarm widget type.',
              meta: 'property',
              type: 'number'
            },
            alarmsMaxCountLoad: {
              description: 'Configured maximum alarms to load for alarm widget type.',
              meta: 'property',
              type: 'number'
            },
            alarmsFetchSize: {
              description: 'Configured alarms page size used to load alarms.',
              meta: 'property',
              type: 'number'
            },
            datasources: {
              description: 'Array of configured widget datasources.',
              meta: 'property',
              type: 'Array&lt;<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L250">Datasource</a>&gt;'
            }
          }
        },
        settings: {
          description: 'Widget settings containing widget specific properties according to the defined <a href="https://thingsboard.io/docs/user-guide/contribution/widgets-development/#settings-schema-section">settings json schema</a>',
          meta: 'property',
          type: 'object'
        },
        datasources: {
          description: 'Array of resolved widget datasources.',
          meta: 'property',
          type: 'Array&lt;<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L250">Datasource</a>&gt;'
        },
        data: {
          description: 'Array of latest datasources data.',
          meta: 'property',
          type: 'Array&lt;<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L275">DatasourceData</a>&gt;'
        },
        timeWindow: {
          description: 'Current widget timewindow (applicable for timeseries widgets).',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/time/time.models.ts#L104">WidgetTimewindow</a>'
        },
        units: {
          description: 'Optional property defining units text of values displayed by widget. Useful for simple widgets like cards or gauges.',
          meta: 'property',
          type: 'string'
        },
        decimals: {
          description: 'Optional property defining how many positions should be used to display decimal part of the value number.',
          meta: 'property',
          type: 'number'
        },
        hideTitlePanel: {
          description: 'Manages visibility of widget title panel. Useful for widget with custom title panels or different states. <b>updateWidgetParams()</b> function must be called after this property change.',
          meta: 'property',
          type: 'boolean'
        },
        widgetTitle: {
          description: 'If set, will override configured widget title text. <b>updateWidgetParams()</b> function must be called after this property change.',
          meta: 'property',
          type: 'string'
        },
        detectChanges: {
          description: 'Trigger change detection for current widget. Must be invoked when widget HTML template bindings should be updated due to widget data changes.',
          meta: 'function'
        },
        updateWidgetParams: {
          description: 'Updates widget with runtime set properties such as <b>widgetTitle</b>, <b>hideTitlePanel</b>, etc. Must be invoked in order these properties changes take effect.',
          meta: 'function'
        },
        defaultSubscription: {
          description: 'Default widget subscription object contains all subscription information,<br>including current data, according to the widget type.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L220">IWidgetSubscription</a>'
        },
        timewindowFunctions: {
          description: 'Object with timewindow functions used to manage widget data time frame. Can by used by Time-series or Alarm widgets.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L45">TimewindowFunctions</a>',
          children: {
            onUpdateTimewindow: {
              description: 'This function can be used to update current subscription time frame<br>to historical one identified by <code>startTimeMs</code> and <code>endTimeMs</code> arguments.',
              meta: 'function',
              args: [
                {
                  name: 'startTimeMs',
                  description: 'Timewindow start time in UTC milliseconds',
                  type: 'number'
                },
                {
                  name: 'endTimeMs',
                  description: 'Timewindow end time in UTC milliseconds',
                  type: 'number'
                }
              ]
            },
            onResetTimewindow: {
              description: 'Resets subscription time frame to default defined by widget timewindow component<br>or dashboard timewindow depending on widget settings.',
              meta: 'function'
            }
          }
        },
        controlApi: {
          description: 'Object that provides API functions for RPC (Control) widgets.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L58">RpcApi</a>',
          children: {
            sendOneWayCommand: {
              description: 'Sends one way (without response) RPC command to the device.',
              meta: 'function',
              args: [
                {
                  name: 'method',
                  description: 'RPC method name',
                  type: 'string'
                },
                {
                  name: 'params',
                  description: 'RPC method params, custom json object',
                  type: 'object',
                  optional: true
                },
                {
                  name: 'timeout',
                  description: 'Maximum delay in milliseconds to wait until response/acknowledgement is received.',
                  type: 'number',
                  optional: true
                }
              ],
              return: {
                description: 'A command execution Observable.',
                type: 'Observable&lt;any&gt;'
              }
            },
            sendTwoWayCommand: {
              description: 'Sends two way (with response) RPC command to the device.',
              meta: 'function',
              args: [
                {
                  name: 'method',
                  description: 'RPC method name',
                  type: 'string'
                },
                {
                  name: 'params',
                  description: 'RPC method params, custom json object',
                  type: 'object',
                  optional: true
                },
                {
                  name: 'timeout',
                  description: 'Maximum delay in milliseconds to wait until response/acknowledgement is received.',
                  type: 'number',
                  optional: true
                }
              ],
              return: {
                description: 'A command execution Observable of response body.',
                type: 'Observable&lt;any&gt;'
              }
            }
          }
        },
        actionsApi: {
          description: 'Set of API functions to work with user defined actions.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L67">WidgetActionsApi</a>',
          children: {
            getActionDescriptors: {
              description: 'Get list of action descriptors for provided <code>actionSourceId</code>.',
              meta: 'function',
              args: [
                {
                  name: 'actionSourceId',
                  description: 'Id of widget action source',
                  type: 'string'
                }
              ],
              return: {
                description: 'The list of action descriptors',
                type: 'Array&lt;<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L323">WidgetActionDescriptor</a>&gt;'
              }
            },
            handleWidgetAction: {
              description: 'Handle action produced by particular action source.',
              meta: 'function',
              args: [
                {
                  name: '$event',
                  description: 'DOM event object associated with action.',
                  type: 'Event'
                },
                {
                  name: 'descriptor',
                  description: 'An action descriptor.',
                  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L323">WidgetActionDescriptor</a>'
                },
                {
                  name: 'entityId',
                  description: 'Current entity id provided by action source if available.',
                  type: entityIdHref,
                  optional: true
                },
                {
                  name: 'entityName',
                  description: 'Current entity name provided by action source if available.',
                  type: 'string',
                  optional: true
                }
              ]
            }
          }
        },
        stateController: {
          description: 'Reference to Dashboard state controller, providing API to manage current dashboard state.',
          meta: 'property',
          type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L121">IStateController</a>',
          children: {
            openState: {
              description: 'Navigate to new dashboard state.',
              meta: 'function',
              args: [
                {
                  name: 'id',
                  description: 'An id of the target dashboard state.',
                  type: 'string'
                },
                {
                  name: 'params',
                  description: 'An object with state parameters to use by the new state.',
                  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L111">StateParams</a>',
                  optional: true
                },
                {
                  name: 'openRightLayout',
                  description: 'An optional boolean argument to force open right dashboard layout if present in mobile view mode.',
                  type: 'boolean',
                  optional: true
                }
              ]
            },
            updateState: {
              description: 'Updates current dashboard state.',
              meta: 'function',
              args: [
                {
                  name: 'id',
                  description: 'An optional id of the target dashboard state to replace current state id.',
                  type: 'string',
                  optional: true
                },
                {
                  name: 'params',
                  description: 'An object with state parameters to update current state parameters.',
                  type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L111">StateParams</a>',
                  optional: true
                },
                {
                  name: 'openRightLayout',
                  description: 'An optional boolean argument to force open right dashboard layout if present in mobile view mode.',
                  type: 'boolean',
                  optional: true
                }
              ]
            },
            getStateId: {
              description: 'Get current dashboard state id.',
              meta: 'function',
              return: {
                description: 'current dashboard state id.',
                type: 'string'
              }
            },
            getStateParams: {
              description: 'Get current dashboard state parameters.',
              meta: 'function',
              return: {
                description: 'current dashboard state parameters.',
                type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L111">StateParams</a>'
              }
            },
            getStateParamsByStateId: {
              description: 'Get state parameters for particular dashboard state identified by <code>id</code>.',
              meta: 'function',
              args: [
                {
                  name: 'id',
                  description: 'An id of the target dashboard state.',
                  type: 'string'
                }
              ],
              return: {
                description: 'current dashboard state parameters.',
                type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/core/api/widget-api.models.ts#L111">StateParams</a>'
              }
            }
          }
        }
      },
      ...serviceCompletions
    }
  }
}
