# Implicit matching
ng-annotate uses static analysis to detect common AngularJS code patterns. 
There are patterns it does not and never will understand and for those you
should use `"ngInject"` instead, see [README.md](README.md).


## Declaration forms
ng-annotate understands the two common declaration forms:

Long form:

```js
angular.module("MyMod").controller("MyCtrl", function($scope, $timeout) {
});
```

Short form:

```js
myMod.controller("MyCtrl", function($scope, $timeout) {
});
```

It's not limited to `.controller` of course. It understands `.config`, `.factory`,
`.directive`, `.filter`, `.run`, `.controller`, `.provider`, `.service`, `.decorator`,
`.component`, `.animation` and `.invoke`.

For short forms it does not need to see the declaration of `myMod` so you can run it
on your individual source files without concatenating. If ng-annotate detects a short form
false positive then you can use the `--regexp` option to limit the module identifier.
Examples: `--regexp "^myMod$"` (match only `myMod`) or `--regexp "^$"` (ignore short forms).
You can also use `--regexp` to opt-in for more advanced method callee matching, for
example `--regexp "^require(.*)$"` to detect and transform
`require('app-module').controller(..)`. Not using the option is the same as passing
`--regexp "^[a-zA-Z0-9_\$\.\s]+$"`, which means that the callee can be a (non-unicode)
identifier (`foo`), possibly with dot notation (`foo.bar`).

ng-annotate understands `angular.module("MyMod", function(dep) ..)` as an alternative to
`angular.module("MyMod").config(function(dep) ..)`.

ng-annotate understands `this.$get = function($scope) ..` and
`{.., $get: function($scope) ..}` inside a `provider`. `self` and `that` can be used as
aliases for `this`.

ng-annotate understands `return {.., controller: function($scope) ..}` inside a
`directive`.

ng-annotate understands `$provide.decorator("bar", function($scope) ..)`, `$provide.service`,
`$provide.factory` and `$provide.provider`.

ng-annotate understands `$routeProvider.when("path", { .. })`.

ng-annotate understands `$controllerProvider.register("foo", function($scope) ..)`.

ng-annotate understands `$httpProvider.interceptors.push(function($scope) ..)` and
`$httpProvider.responseInterceptors.push(function($scope) ..)`.

ng-annotate understands `$injector.invoke(function ..)`.

ng-annotate understands [ui-router](https://github.com/angular-ui/ui-router) (`$stateProvider` and
`$urlRouterProvider`).

ng-annotate understands `$uibModal.open` (and `$modal.open`) ([angular-ui/bootstrap](http://angular-ui.github.io/bootstrap/)).

ng-annotate understands `$mdDialog.show`, `$mdToast.show` and `$mdBottomSheet.show`
([angular material design](https://material.angularjs.org/#/api/material.components.dialog/service/$mdDialog)).

ng-annotate understands `myMod.store("MyCtrl", function ..)`
([flux-angular](https://github.com/christianalfoni/flux-angular)).

ng-annotate understands chaining.

ng-annotate understands IIFE's and attempts to match through them, so
`(function() { return function($scope) .. })()` works anywhere
`function($scope) ..` does (for any IIFE args and params).

ng-annotate understands [angular-dashboard-framework](https://github.com/sdorra/angular-dashboard-framework)
via optional `--enable angular-dashboard-framework`.


## Reference-following
ng-annotate follows references. This works if and only if the referenced declaration is
a) a function declaration or
b) a variable declaration with an initializer.
Modifications to a reference outside of its declaration site are ignored by ng-annotate.

These examples will get annotated:

```js
function MyCtrl($scope, $timeout) {
}
var MyCtrl2 = function($scope) {};

angular.module("MyMod").controller("MyCtrl", MyCtrl);
angular.module("MyMod").controller("MyCtrl", MyCtrl2);
```


