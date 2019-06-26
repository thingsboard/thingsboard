/*
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import injectTapEventPlugin from 'react-tap-event-plugin';
import UrlHandler from './url.handler';

/* eslint-disable import/no-unresolved, import/default */

import mdiIconSet from '../svg/mdi.svg';

/* eslint-enable import/no-unresolved, import/default */

const PRIMARY_BACKGROUND_COLOR = "#305680";//#2856b6";//"#3f51b5";
const SECONDARY_BACKGROUND_COLOR = "#527dad";
const HUE3_COLOR = "#a7c1de";

/*@ngInject*/
export default function AppConfig($provide,
                                  $urlRouterProvider,
                                  $locationProvider,
                                  $mdIconProvider,
                                  ngMdIconServiceProvider,
                                  $mdThemingProvider,
                                  $httpProvider,
                                  $translateProvider,
                                  storeProvider) {

    injectTapEventPlugin();
    $locationProvider.html5Mode(true);
    $urlRouterProvider.otherwise(UrlHandler);
    storeProvider.setCaching(false);
    
    $translateProvider.useSanitizeValueStrategy(null)
                      .useMissingTranslationHandler('tbMissingTranslationHandler')
                      .addInterpolation('$translateMessageFormatInterpolation')
                      .useStaticFilesLoader({
                          files: [
                              {
                                  prefix: PUBLIC_PATH + 'locale/locale.constant-', //eslint-disable-line
                                  suffix: '.json'
                              }
                          ]
                      })
                      .registerAvailableLanguageKeys(SUPPORTED_LANGS, getLanguageAliases(SUPPORTED_LANGS)) //eslint-disable-line
                      .fallbackLanguage('en_US') // must be before determinePreferredLanguage   
                      .uniformLanguageTag('java')  // must be before determinePreferredLanguage
                      .determinePreferredLanguage();                

    $httpProvider.interceptors.push('globalInterceptor');

    $provide.decorator("$exceptionHandler", ['$delegate', '$injector', function ($delegate/*, $injector*/) {
        return function (exception, cause) {
/*            var rootScope = $injector.get("$rootScope");
            var $window = $injector.get("$window");
            var utils = $injector.get("utils");
            if (rootScope.widgetEditMode) {
                var parentScope = $window.parent.angular.element($window.frameElement).scope();
                var data = utils.parseException(exception);
                parentScope.$emit('widgetException', data);
                parentScope.$apply();
            }*/
            $delegate(exception, cause);
        };
    }]);

    $mdIconProvider.iconSet('mdi', mdiIconSet);

    ngMdIconServiceProvider
        .addShape('alpha-a-circle-outline', '<path d="M11,7H13A2,2 0 0,1 15,9V17H13V13H11V17H9V9A2,2 0 0,1 11,7M11,9V11H13V9H11M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2Z" />');

    configureTheme();

    function blueGrayTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('blue-grey');
        var tbAccentPalette = $mdThemingProvider.extendPalette('orange', {
            'contrastDefaultColor': 'light'
        });

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-primary')
            .dark();
    }

    function indigoTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('indigo', {
            '500': PRIMARY_BACKGROUND_COLOR,
            '600': SECONDARY_BACKGROUND_COLOR,
            'A100': HUE3_COLOR
        });

        var tbAccentPalette = $mdThemingProvider.extendPalette('deep-orange');

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        var tbDarkPrimaryPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '500': '#9fa8da'
        });

        var tbDarkPrimaryBackgroundPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '800': PRIMARY_BACKGROUND_COLOR
        });

        $mdThemingProvider.definePalette('tb-dark-primary', tbDarkPrimaryPalette);
        $mdThemingProvider.definePalette('tb-dark-primary-background', tbDarkPrimaryBackgroundPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-dark-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-dark-primary-background')
            .dark();
    }

    function configureTheme() {

        var theme = 'indigo';

        if (theme === 'blueGray') {
            blueGrayTheme();
        } else {
            indigoTheme();
        }

        $mdThemingProvider.setDefaultTheme('default');
        //$mdThemingProvider.alwaysWatchTheme(true);
    }

    function getLanguageAliases(supportedLangs) {
        var aliases = {};

        supportedLangs.sort().forEach(function(item, index, array) {
            if (item.length === 2) { 
                aliases[item] = item;
                aliases[item + '_*'] = item;
            } else {
                var key = item.slice(0, 2);
                if (index === 0 || key !== array[index - 1].slice(0, 2)) {
                    aliases[key] = item;
                    aliases[key + '_*'] = item;
                } else {
                    aliases[item] = item;
                }
            }
        });
        
        return aliases;
    }
}