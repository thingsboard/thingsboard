/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import addLocaleKorean from './locale/locale.constant-ko';
import addLocaleChinese from './locale/locale.constant-zh';
import addLocaleRussian from './locale/locale.constant-ru';
import addLocaleSpanish from './locale/locale.constant-es';

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
                                  $mdThemingProvider,
                                  $httpProvider,
                                  $translateProvider,
                                  storeProvider,
                                  locales) {

    injectTapEventPlugin();
    $locationProvider.html5Mode(true);
    $urlRouterProvider.otherwise(UrlHandler);
    storeProvider.setCaching(false);

    $translateProvider.useSanitizeValueStrategy('sce');
    $translateProvider.preferredLanguage('en_US');
    $translateProvider.useLocalStorage();
    $translateProvider.useMissingTranslationHandler('tbMissingTranslationHandler');
    $translateProvider.addInterpolation('$translateMessageFormatInterpolation');

    addLocaleKorean(locales);
    addLocaleChinese(locales);
    addLocaleRussian(locales);
    addLocaleSpanish(locales);

    var $window = angular.injector(['ng']).get('$window');
    var lang = $window.navigator.language || $window.navigator.userLanguage;
    if (lang === 'ko') {
        $translateProvider.useSanitizeValueStrategy(null);
        $translateProvider.preferredLanguage('ko_KR');
    } else if (lang === 'zh') {
        $translateProvider.useSanitizeValueStrategy(null);
        $translateProvider.preferredLanguage('zh_CN');
    } else if (lang === 'es') {
        $translateProvider.useSanitizeValueStrategy(null);
        $translateProvider.preferredLanguage('es_ES');
    }

    for (var langKey in locales) {
        var translationTable = locales[langKey];
        $translateProvider.translations(langKey, translationTable);
    }

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

}