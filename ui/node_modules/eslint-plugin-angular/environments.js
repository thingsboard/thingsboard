'use strict';

module.exports = {
    angular: {
        globals: {
            angular: true
        }
    },
    // https://docs.angularjs.org/api/ngMock
    mocks: {
        globals: {
            angular: true,
            inject: true,
            module: true
        }
    },
    // http://www.protractortest.org/#/api
    protractor: {
        globals: {
            element: true,
            $: true,
            $$: true,
            browser: true,
            by: true,
            protractor: true
        }
    }
};
