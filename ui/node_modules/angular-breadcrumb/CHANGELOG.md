<a name="0.4.1"></a>
### 0.4.1 (2015-08-09)


#### Features

* add the scope-based ncyBreadcrumbIgnore flag ([934c5523](http://github.com/ncuillery/angular-breadcrumb/commit/934c5523208a9615d7cfa3abcb397bbe131332ac), closes [#42](http://github.com/ncuillery/angular-breadcrumb/issues/42), [#62](http://github.com/ncuillery/angular-breadcrumb/issues/42))


<a name="0.4.0"></a>
### 0.4.0 (2015-05-17)


#### Bug Fixes

* **$breadcrumb:** Handle parents provided by StateObject references ([f4288d37](http://github.com/ncuillery/angular-breadcrumb/commit/f4288d375fd1090ffec1d67e85c6300d74d86d37), closes [#82](http://github.com/ncuillery/angular-breadcrumb/issues/82))
* **ncyBreadcrumb:**
  * Prevent memory leak when label is a binding ([264e10f6](http://github.com/ncuillery/angular-breadcrumb/commit/264e10f680e1bbb8d1e00cf500de39cac4222cfd), closes [#88](http://github.com/ncuillery/angular-breadcrumb/issues/88))
  * Removed trailing spaces from breadcrumb items([bc276ed5](http://github.com/ncuillery/angular-breadcrumb/commit/bc276ed5351a586d4a6dc83ada0687e6ca485344), closes [#77](http://github.com/ncuillery/angular-breadcrumb/issues/77))

#### Features

* Add force to ncyBreadcrumb options ([31125a38](http://github.com/ncuillery/angular-breadcrumb/commit/31125a386d706dd76df807b3b02e1fccea38fb59), closes [#77](http://github.com/ncuillery/angular-breadcrumb/issues/78))
* **ncyBreadcrumbText:** Add ncyBreadcrumbText directive ([82b2b443](http://github.com/ncuillery/angular-breadcrumb/commit/82b2b443fab220cd9ac7d3a8c90c1edc4291e54a), closes [#71](http://github.com/ncuillery/angular-breadcrumb/issues/71), [#83](http://github.com/ncuillery/angular-breadcrumb/issues/83))


<a name="0.3.3"></a>
### 0.3.3 (2014-12-16)


#### Bug Fixes

* **ncyBreadcrumb:** define `$$templates` with var instead of attaching it to `window` ([c35c9d25](http://github.com/ncuillery/angular-breadcrumb/commit/c35c9d255b5e2585d225a961d1efdb51d18f6a55), closes [#55](http://github.com/ncuillery/angular-breadcrumb/issues/55))


<a name="0.3.2"></a>
### 0.3.2 (2014-11-15)

* **npm:** nothing, it's only a blank release due to a network problem during the last `npm publish` (f...ing npm doesn't allow a republish with the same version number [npm-registry-couchapp#148](https://github.com/npm/npm-registry-couchapp/issues/148)). 

<a name="0.3.1"></a>
### 0.3.1 (2014-11-15)


#### Bug Fixes

* **npm:** update package.json after (unclean) npm publish ([ab8161c2](http://github.com/ncuillery/angular-breadcrumb/commit/ab8161c25f98613f725b5e5ff8fe147acd60b365), closes [#52](http://github.com/ncuillery/angular-breadcrumb/issues/52))
* **sample:** Send correct url params for the room link in booking view ([876de49a](http://github.com/ncuillery/angular-breadcrumb/commit/876de49a9c5d6e2d75714a606238e9041ed49baf))


<a name="0.3.0"></a>
## 0.3.0 (2014-10-29)


#### Bug Fixes

* organize state-level options in `ncyBreadcrumb` key instead of `data` ([1ea436d3](http://github.com/ncuillery/angular-breadcrumb/commit/1ea436d3f6d5470b7ae3e71e71259dbd2422bc00), closes [#30](http://github.com/ncuillery/angular-breadcrumb/issues/30))
* curly braces appearing on title of sample app ([855e76cb](http://github.com/ncuillery/angular-breadcrumb/commit/855e76cb33fda607fa3caa230564b77b48262c40))


#### Features

* Add a global option to include abstract states ([6f0461ea](http://github.com/ncuillery/angular-breadcrumb/commit/6f0461ea7db36d8e10c29ed10de1f1c08d215a19), closes [#35](http://github.com/ncuillery/angular-breadcrumb/issues/35), [#28](http://github.com/ncuillery/angular-breadcrumb/issues/28))
* **$breadcrumb:**
  * Support url params when using `ncyBreadcrumb.parent` property ([55730045](http://github.com/ncuillery/angular-breadcrumb/commit/55730045dcf3b4fb1048c67f1e18953505563ed4), closes [#46](http://github.com/ncuillery/angular-breadcrumb/issues/46))
  * add the customization of the parent state with a function ([ada09015](http://github.com/ncuillery/angular-breadcrumb/commit/ada09015c49f05a94349dabf078f1ed621811aaa), closes [#32](http://github.com/ncuillery/angular-breadcrumb/issues/32))
* **ncyBreadcrumbLast:** Add a new directive rendering the last step ([1eef24fb](http://github.com/ncuillery/angular-breadcrumb/commit/1eef24fbe862a1e3308181c38f50755843cf4426), closes [#37](http://github.com/ncuillery/angular-breadcrumb/issues/37))


#### Breaking Changes

* state-level options has been moved under the custom key
`ncyBreadcrumb` in state's configuration.

To migrate the code follow the example below:
```
// Before
$stateProvider.state('A', {
  url: '/a',
  data: {
    ncyBreadcrumbLabel: 'State A'
  }
});
```

```
// After
$stateProvider.state('A', {
  url: '/a',
  ncyBreadcrumb: {
    label: 'State A'
  }
});
```
See [API reference](https://github.com/ncuillery/angular-breadcrumb/wiki/API-Reference) for more informations.
 ([1ea436d3](http://github.com/ncuillery/angular-breadcrumb/commit/1ea436d3f6d5470b7ae3e71e71259dbd2422bc00))


<a name="0.2.3"></a>
### 0.2.3 (2014-07-26)


#### Bug Fixes

* **$breadcrumb:** use `$stateParams` in case of unhierarchical states ([1c3c05e0](http://github.com/ncuillery/angular-breadcrumb/commit/1c3c05e0acac191fe2e76db2ef18da339caefaaa), closes [#29](http://github.com/ncuillery/angular-breadcrumb/issues/29))


<a name="0.2.2"></a>
### 0.2.2 (2014-06-23)


#### Bug Fixes

* catch the `$viewContentLoaded` earlier ([bb47dd54](http://github.com/ncuillery/angular-breadcrumb/commit/bb47dd54deb5efc579ccb9b1575e686803dee1c5), closes [#14](http://github.com/ncuillery/angular-breadcrumb/issues/14))
* **sample:**
  * make the CRU(D) about rooms working ([3ca89ec7](http://github.com/ncuillery/angular-breadcrumb/commit/3ca89ec771fd20dc4ab2d733612bdcfb96ced703))
  * prevent direct URL access to a day disabled in the datepicker ([95236916](http://github.com/ncuillery/angular-breadcrumb/commit/95236916e00b19464a3dfe3584ef1b18da9ffb25), closes [#17](http://github.com/ncuillery/angular-breadcrumb/issues/17))
  * use the same variable in the datepicker and from url params for state `booking.day` ([646f7060](http://github.com/ncuillery/angular-breadcrumb/commit/646f70607e494f0e5e3c2483ed69f689684b2742), closes [#16](http://github.com/ncuillery/angular-breadcrumb/issues/16))


#### Features

* **ncyBreadcrumb:** watch every expression founded in labels ([1363515e](http://github.com/ncuillery/angular-breadcrumb/commit/1363515e20977ce2f39a1f5e5e1d701f0d7af296), closes [#20](http://github.com/ncuillery/angular-breadcrumb/issues/20))


<a name="0.2.1"></a>
### 0.2.1 (2014-05-16)


#### Bug Fixes

* **$breadcrumb:** check if a state has a parent when looking for an inheritated property ([77e668b5](http://github.com/ncuillery/angular-breadcrumb/commit/77e668b5eb759570a64c2a885e81580953af3201), closes [#11](http://github.com/ncuillery/angular-breadcrumb/issues/11))


<a name="0.2.0"></a>
### 0.2.0 (2014-05-08)


#### Bug Fixes

* **$breadcrumb:** remove abstract states from breadcrumb ([8a06c5ab](http://github.com/ncuillery/angular-breadcrumb/commit/8a06c5abce749027d48f7309d1aabea1e447dfd5), closes [#8](http://github.com/ncuillery/angular-breadcrumb/issues/8))
* **ncyBreadcrumb:** display the correct breadcrumb in case of direct access ([e1f455ba](http://github.com/ncuillery/angular-breadcrumb/commit/e1f455ba4def97d3fc76b53772867b5f9daf4232), closes [#10](http://github.com/ncuillery/angular-breadcrumb/issues/10))


#### Features

* **$breadcrumb:**
  * add a configuration property for skipping a state in the breadcrumb ([dd255d90](http://github.com/ncuillery/angular-breadcrumb/commit/dd255d906c4231f44b48f066d4db197a9c6b9e27), closes [#9](http://github.com/ncuillery/angular-breadcrumb/issues/9))
  * allow chain of states customization ([028e493a](http://github.com/ncuillery/angular-breadcrumb/commit/028e493a1ebcae5ae60b8a9d42b949262000d7df), closes [#7](http://github.com/ncuillery/angular-breadcrumb/issues/7))
* **ncyBreadcrumb:** add 'Element' declaration style '<ncy-breadcrumb />' ([b51441ea](http://github.com/ncuillery/angular-breadcrumb/commit/b51441eafb1659b782fea1f8668c7f455e1d6b4d))

