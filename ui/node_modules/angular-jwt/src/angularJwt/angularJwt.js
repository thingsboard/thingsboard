// Create all modules and define dependencies to make sure they exist
// and are loaded in the correct order to satisfy dependency injection
// before all nested files are concatenated by Grunt

// Modules
angular.module('angular-jwt',
    [
        'angular-jwt.options',
        'angular-jwt.interceptor',
        'angular-jwt.jwt',
        'angular-jwt.authManager'
    ]);
