<a name="1.0.10"></a>
# 1.1.0 (2017-02-03)


## Bug Fixes

- resolve the loader with created elements
 ([3351e44d](https://github.com/ocombe/ocLazyLoad/commit/3351e44d2d76b7599eadfe59dec5a5674b1aff69), [#292](https://github.com/ocombe/ocLazyLoad/pull/292))
- ensure CSS loader uses loading patch for PhantomJS 1.9 ([54d68e92](https://github.com/ocombe/ocLazyLoad/commit/54d68e92f83b1c9f5eba805a764c147566843544), [#280](https://github.com/ocombe/ocLazyLoad/pull/280))


## Features

- angular 1.6 compatibility
 ([17d372fc](https://github.com/ocombe/ocLazyLoad/commit/17d372fce194a840a0d23f7a8b417601f769f52a),
 [#377](https://github.com/ocombe/ocLazyLoad/issues/377))
- allow hot reloading via systemjs-hot-reloader ([94088482](https://github.com/ocombe/ocLazyLoad/commit/94088482aad2bba0f49c3d873886e993bcdfa13c), [#291](https://github.com/ocombe/ocLazyLoad/pull/291))


<a name="1.0.9"></a>
# 1.0.9 (2015-11-24)


## Bug Fixes

- success callback for requirejs wrapper
 ([fd9df8d1](https://github.com/ocombe/ocLazyLoad/commit/fd9df8d1507bb4f0c690ef3781b04a15f3b8eb6b),
 [#260](https://github.com/ocombe/ocLazyLoad/issues/260))
 
 
## Features

- adding `_unregister` internal function
 ([5aba0dc7](https://github.com/ocombe/ocLazyLoad/commit/5aba0dc77b1fa4f1a1a27419cbc8a54d614a728c),
 [#265](https://github.com/ocombe/ocLazyLoad/issues/265))


<a name="1.0.8"></a>
# 1.0.8 (2015-11-02)


## Bug Fixes

- better fix for $compile problems in IE
 ([ed4d425b](https://github.com/ocombe/ocLazyLoad/commit/ed4d425bfcf746901e2b956172c6a0b71e237bae),
 [#261](https://github.com/ocombe/ocLazyLoad/issues/261))


<a name="1.0.7"></a>
# 1.0.7 (2015-11-02)


## Bug Fixes

- requirejs error callback
 ([11130942](https://github.com/ocombe/ocLazyLoad/commit/11130942ab3dbed497a64ab7eac5175e9b3597c4))
- infinite loop in inject
 ([70859980](https://github.com/ocombe/ocLazyLoad/commit/70859980d0537780c46e5a096d8c3c9cff86de83))
- IE "Invalid calling object" error (attach to DOM and then compile)
 ([d99ab925](https://github.com/ocombe/ocLazyLoad/commit/d99ab92533ef4bdfa334926307af9f32097592a3))


<a name="1.0.6"></a>
# 1.0.6 (2015-10-01)


## Bug Fixes
- calling inject from loaders should pass the "real" module param
 ([953584e8](https://github.com/ocombe/ocLazyLoad/commit/953584e8989de7ed1c2166ca193c899bad8a3478),
 [#221](https://github.com/ocombe/ocLazyLoad/issues/221))
- directive compile original contents after dependency is loaded.
 ([a48e3ceb](https://github.com/ocombe/ocLazyLoad/commit/a48e3ceba1945e74478a0a7f964f9aa84e027799),
 [#168](https://github.com/ocombe/ocLazyLoad/issues/168),
 [#194](https://github.com/ocombe/ocLazyLoad/issues/194))


<a name="1.0.5"></a>
# 1.0.5 (2015-09-11)


## Bug Fixes
- loading a module with dependencies with multiple oc-lazy-load directives
 ([098e391b](https://github.com/ocombe/ocLazyLoad/commit/098e391b0e084997c95a3125e66a41484a257cc1),
 [#213](https://github.com/ocombe/ocLazyLoad/issues/213))
- changing semver dependency for Angular
 ([30626401](https://github.com/ocombe/ocLazyLoad/commit/30626401664d1be8fc748bb53c88f39cb58742c0),
 [#195](https://github.com/ocombe/ocLazyLoad/issues/195))


## Features

- optimise signature calls and onInvoke function
 ([c56e727e](https://github.com/ocombe/ocLazyLoad/commit/c56e727ef832c591920c58a32646c5a8f05f655c))


<a name="1.0.4"></a>
# 1.0.4 (2015-07-30)


## Bug Fixes

- don't let unmet dependencies slip through (thank you unit tests!)
 ([23eb666d](https://github.com/ocombe/ocLazyLoad/commit/23eb666d6627416e40aaa97783b9e81ec7153fe9))
- don't try to call angular.module on config names
 ([52219f92](https://github.com/ocombe/ocLazyLoad/commit/52219f923319e0856da47a6bce064b6ffb361641),
 [#217](https://github.com/ocombe/ocLazyLoad/issues/217), [#218](https://github.com/ocombe/ocLazyLoad/issues/218))


<a name="1.0.3"></a>
# 1.0.3 (2015-07-24)


## Bug Fixes

- check for config names when needed
 ([023e4bb1](https://github.com/ocombe/ocLazyLoad/commit/023e4bb1e43a922ac4b9a4ef09ff475f1fec867a),
 [#214](https://github.com/ocombe/ocLazyLoad/issues/214), [#198](https://github.com/ocombe/ocLazyLoad/issues/198))


<a name="1.0.2"></a>
# 1.0.2 (2015-07-10)


## Bug Fixes

- add extra condition to improve karma testing
 ([f0c33aae](https://github.com/ocombe/ocLazyLoad/commit/f0c33aaea84511a276dd946dd48bfe2cb20d1e73))


## Features

- add interoperability with CommonJS
 ([b0536ad4](https://github.com/ocombe/ocLazyLoad/commit/b0536ad4104467922c36bcf55a8a072343d102bc))


<a name="1.0.1"></a>
# 1.0.1 (2015-06-01)


## Bug Fixes

- don't remove filecache for files that were successfully loaded
 ([e2ed37c0](https://github.com/ocombe/ocLazyLoad/commit/e2ed37c0eff32d34419af6851bfc355e7fb6f3ad))


<a name="1.0.0"></a>
# 1.0.0 (2015-05-29)


## Bug Fixes

- use parent element instead of head to insert files in native loaded
 ([ad4276a3](https://github.com/ocombe/ocLazyLoad/commit/ad4276a39cddf8ebfd8f247690e98fc306c2d3bb),
 [#164](https://github.com/ocombe/ocLazyLoad/issues/164))
- don't compile text nodes in the directive
 ([8900e493](https://github.com/ocombe/ocLazyLoad/commit/8900e493b8245084f4871d129250ffc54e565639),
 [#168](https://github.com/ocombe/ocLazyLoad/issues/168))
- files cache should be cleaned upon resolution of the promise
 ([9a186c93](https://github.com/ocombe/ocLazyLoad/commit/9a186c93ccb72c63a45e40c6c1e86319d9d004fa),
 [#189](https://github.com/ocombe/ocLazyLoad/issues/189))
- reject promise when calling 'load' instead of 'inject'
 ([31595472](https://github.com/ocombe/ocLazyLoad/commit/315954729aaa609d43aa7eb7750e8804cff9bf70),
 [#147](https://github.com/ocombe/ocLazyLoad/issues/147))
- make inject work as a standalone function when no params are given
 ([499bd72d](https://github.com/ocombe/ocLazyLoad/commit/499bd72ddaf6addbf2c649a48776bd2b6ff35227),
 [#171](https://github.com/ocombe/ocLazyLoad/issues/171))
- guard against null-refs when parsing Safari user-agents
 ([818aa5d0](https://github.com/ocombe/ocLazyLoad/commit/818aa5d0ddaa3909109d42b38f8921e9d4b18cda),
 [#188](https://github.com/ocombe/ocLazyLoad/issues/188))
- checking if we're not registering a component with a reserved name (such at `toString`)
 ([7362ca49](https://github.com/ocombe/ocLazyLoad/commit/7362ca493384c5b14e203b9c013085cbcab980f8 ),
 [#184](https://github.com/ocombe/ocLazyLoad/issues/184))


<a name="1.0.0-beta.2"></a>
# 1.0.0-beta.2 (2015-04-20)


## Bug Fixes

- Die infinite loops! You are not fun anymore (with param serie:true)
 ([dab34c0a](https://github.com/ocombe/ocLazyLoad/commit/dab34c0a3513061665850f68d983c1f2729f5f5a),
 [#166](https://github.com/ocombe/ocLazyLoad/issues/166))


<a name="1.0.0-beta.1"></a>
# 1.0.0-beta.1 (2015-04-16)


## Bug Fixes

- use document.querySelector for insertBefore when jQuery isn't available
 ([6e8fa8c3](https://github.com/ocombe/ocLazyLoad/commit/6e8fa8c37f4305c50241288db7fddc5ecae0ab8f),
 [#164](https://github.com/ocombe/ocLazyLoad/issues/164))


## Documentation

- adding a plunkr for issues
 ([2f408d27](https://github.com/ocombe/ocLazyLoad/commit/2f408d2729eaf3df9cc8434375611a5b26181c0b))


<a name="1.0.0-alpha.3"></a>
# 1.0.0-alpha.3 (2015-04-09)


## Bug Fixes

- components can be registered as object maps now
 ([08ed860e](https://github.com/ocombe/ocLazyLoad/commit/08ed860e7051f1f0dd132d760b958c5be1114177),
 [#156](https://github.com/ocombe/ocLazyLoad/issues/156))
- make a real copy of the params
 ([6a5d3d4c](https://github.com/ocombe/ocLazyLoad/commit/6a5d3d4ca3fca1e90468aed10ef96f06669cd7f9),
 [#160](https://github.com/ocombe/ocLazyLoad/issues/160))


## Features

- ES6fy all the things!
 ([9cae48c8](https://github.com/ocombe/ocLazyLoad/commit/9cae48c828665e58132950d6db138d082f6bf2a2))


<a name="1.0.0-alpha2"></a>
# 1.0.0-alpha2 (2015-03-23)


## Bug Fixes

- hash shouldn't prevent file type detection
 ([9e1d0894](https://github.com/ocombe/ocLazyLoad/commit/9e1d089413e09b14b7b46d5ff5de4612613be5e9),
 [#153](https://github.com/ocombe/ocLazyLoad/issues/153))


<a name="1.0.0-alpha1"></a>
# 1.0.0-alpha1 (2015-03-19)


## Features

- ocLazyLoad is now modular and (partially) written in ES6! It should be easier to write new loaders (or even extensions), and you can cherry picks the parts that you like. For example, you can use the injector without the loaders. Also, all of the internal functions are available (preceded by an underscore, and undocumented), use them at your own risk (in fact you shouldn't need them unless you're writing an extension).


## Bug Fixes

- the directive should append the content and not add it after
- only the modules added via angular.bootstrap should be considered "already loaded"
 [#147](https://github.com/ocombe/ocLazyLoad/issues/147)
 
## TODO before the release
- try to remove most of the promises for perfs/tests
- use moaaar ES6
- clean up the code


<a name="0.6.3"></a>
# 0.6.3 (2015-03-09)


## Bug Fixes

- detect file type when path contains url parameters
 ([57e1801d](https://github.com/ocombe/ocLazyLoad/commit/57e1801d933f978060954bd8707f586b51544906),
 [#137](https://github.com/ocombe/ocLazyLoad/issues/137))
- rejected promise should be returned immediately
 ([887a67c4](https://github.com/ocombe/ocLazyLoad/commit/887a67c4196fa4bbd65c34f6eba1d8b2bca9fed3))


<a name="0.6.2"></a>
# 0.6.2 (2015-03-05)


## Features

- first step on supporting systemjs & import
 ([cb8dd62e](https://github.com/ocombe/ocLazyLoad/commit/cb8dd62ed9052995cbaf132d94092d1d103dd74d))


<a name="0.6.1"></a>
# 0.6.1 (2015-03-05)


## Bug Fixes

- karma hack isn't needed anymore
 ([3108296e](https://github.com/ocombe/ocLazyLoad/commit/3108296e9d78da822e58333f2f7d674531ae937b))
- angular.bootstrap now adds modules to init, not replace them
 ([bdc03dd9](https://github.com/ocombe/ocLazyLoad/commit/bdc03dd9128eca7fca2421317b9f7b103c9b419c))
- fixed TypeError: Converting circular structure to JSON
 ([11da36d9](https://github.com/ocombe/ocLazyLoad/commit/11da36d90bc5bae588fa3770430d371d5f935aae))
- don't watch for angular.module calls when you're not lazy loading
 ([35f7eb5b](https://github.com/ocombe/ocLazyLoad/commit/35f7eb5be57f7753a20d7460c5a380f44e3ac175))


## Performance Improvements

- hash the signature to optimize memory consumption
 ([1cd9676e](https://github.com/ocombe/ocLazyLoad/commit/1cd9676e8799cff03458f7d2d4d144f624da9cfa))


<a name="0.6.0"></a>
# 0.6.0 (2015-02-27)


## Bug Fixes

- staged lines missing from last commit
 ([dd24bcdd](https://github.com/ocombe/ocLazyLoad/commit/dd24bcdd573821ce7def60c173a15cbee2540de7))
- don't throw for karma
 ([633bec8b](https://github.com/ocombe/ocLazyLoad/commit/633bec8b38635e7d78aaa0e4ea8f1a8cdb85050e),
 [#129](https://github.com/ocombe/ocLazyLoad/issues/129))
- RequireJS should be able to load js files with no extension (default behavior)
 ([4f60d05d](https://github.com/ocombe/ocLazyLoad/commit/4f60d05d02039b700908545b60b71c3e2ca9bbf6))
- null constants should work
 ([83d416f9](https://github.com/ocombe/ocLazyLoad/commit/83d416f97d357d148efe97bafbaf2836ed7b3a3d),
 [#111](https://github.com/ocombe/ocLazyLoad/issues/111))
- keep track of components signatures instead of just the names
 ([6bbaed97](https://github.com/ocombe/ocLazyLoad/commit/6bbaed971cf2d23bb35a6ba5f29c6e6162edc5b5),
 [#120](https://github.com/ocombe/ocLazyLoad/issues/120))
- improve bootstrap & added compatibility with karma
 ([ff6afcf5](https://github.com/ocombe/ocLazyLoad/commit/ff6afcf5d3ef00e8e931fd548051f3103225cea8),
 [#111](https://github.com/ocombe/ocLazyLoad/issues/111))


## Features

- you don't need to specify the name of the lazy loaded modules anymore!!
 ([6634cbee](https://github.com/ocombe/ocLazyLoad/commit/6634cbee6c5ce84363be84ae5529a61a633585b5))
- added support for specifying the type of a file.
 ([a3549eea](https://github.com/ocombe/ocLazyLoad/commit/a3549eea93c67cfc4881ebe9d44c73c220790461))


## Documentation

- adding a table of contents
 ([98aad141](https://github.com/ocombe/ocLazyLoad/commit/98aad14141e2eae1d04f9fc1fe09d85cd4b14713))


<a name="0.5.2"></a>
# 0.5.2 (2014-12-30)


## Bug Fixes

- use init for bootstrapped apps & removed the need for loadedModules
 ([01936cd6](https://github.com/ocombe/ocLazyLoad/commit/01936cd6fe0e0f89a203408ee0bbb927f5b44d07),
 [#84](https://github.com/ocombe/ocLazyLoad/issues/84), [#102](https://github.com/ocombe/ocLazyLoad/issues/102), [#109](https://github.com/ocombe/ocLazyLoad/issues/109))


## Documentation

- added a link to a new lesson from egghead.io
 ([ef8d2871](https://github.com/ocombe/ocLazyLoad/commit/ef8d2871a445b29588f779a27cb3b702d0da6a13))


<a name="0.5.1"></a>
# 0.5.1 (2014-11-20)


## Bug Fixes

- don't use async when you load files in serie
 ([9af93ed3](https://github.com/ocombe/ocLazyLoad/commit/9af93ed30cf05c6c64594d206dc9bf36a318f46e),
 [#95](https://github.com/ocombe/ocLazyLoad/issues/95))
- avoid errors thrown on empty template files
 ([768b9d75](https://github.com/ocombe/ocLazyLoad/commit/768b9d751a613a0a10cb476d5c3eac5fdf44f627))
- compatibility with jasmine
 ([d4985e1d](https://github.com/ocombe/ocLazyLoad/commit/d4985e1d7ce98315ca64a72730d8c10524929d58),
 [#94](https://github.com/ocombe/ocLazyLoad/issues/94))


<a name="0.5.0"></a>
# 0.5.0 (2014-11-11)


## Features

- added a new param `insertBefore`
 ([c4f10385](https://github.com/ocombe/ocLazyLoad/commit/c4f10385cb6a9122c3a03d28b1bb6837710cc3f7),
 [#91](https://github.com/ocombe/ocLazyLoad/issues/91))
- started unit tests
 ([dcc4ff63](https://github.com/ocombe/ocLazyLoad/commit/dcc4ff639df23a1b934899b020a483e47e6ab290))


## Documentation

- updated loaders signatures
 ([ba022894](https://github.com/ocombe/ocLazyLoad/commit/ba022894841222989cf699f07fe21f04f7ad3307))


<a name="0.4.2"></a>
# 0.4.2 (2014-11-10)


## Bug Fixes

- extend config to params for the register method
 ([31157941](https://github.com/ocombe/ocLazyLoad/commit/31157941ccabfa8f8c55edc00dc2b5758bf073b2),
 [#89](https://github.com/ocombe/ocLazyLoad/issues/89))


<a name="0.4.1"></a>
# 0.4.1 (2014-11-09)


## Bug Fixes

- keep global params pristine when loading files
 ([6b2306b7](https://github.com/ocombe/ocLazyLoad/commit/6b2306b71543542c9b592766644c7bba1297bae4),
 [#89](https://github.com/ocombe/ocLazyLoad/issues/89))
- defining new run blocks will replace previous ones
 ([af2627b5](https://github.com/ocombe/ocLazyLoad/commit/af2627b5e627b2b4d83cdd043eff68b1c1430740),
 [#89](https://github.com/ocombe/ocLazyLoad/issues/89))


<a name="0.4.0"></a>
# 0.4.0 (2014-11-09)


## Features

- new parameter `serie` to load files in serie
 ([4ae7a3f3](https://github.com/ocombe/ocLazyLoad/commit/4ae7a3f3de6ad4de74baa6cc771aee556bce812e),
 [#47](https://github.com/ocombe/ocLazyLoad/issues/47), [#86](https://github.com/ocombe/ocLazyLoad/issues/86))
- new parameter `rerun` to rerun the run blocks
 ([26a64a38](https://github.com/ocombe/ocLazyLoad/commit/26a64a38b0c21b6ca28cfa7e512b0b290fdca619),
 [#89](https://github.com/ocombe/ocLazyLoad/issues/89))
- new function: `isLoaded` to check if a module has been loaded
 ([364c9e9f](https://github.com/ocombe/ocLazyLoad/commit/364c9e9ffd8350e5ca46a708bd3846ea6de9421c),
 [#79](https://github.com/ocombe/ocLazyLoad/issues/79))


<a name="0.3.10"></a>
# 0.3.10 (2014-11-09)


## Bug Fixes

- fix for error:[$compile:multidir] Multiple directives
 ([61fd4dd3](https://github.com/ocombe/ocLazyLoad/commit/61fd4dd3b8131245d33eb2314dcf37a9188a6728),
 [#84](https://github.com/ocombe/ocLazyLoad/issues/84),
 [#78](https://github.com/ocombe/ocLazyLoad/issues/78),
 [#73](https://github.com/ocombe/ocLazyLoad/issues/73),
 [#58](https://github.com/ocombe/ocLazyLoad/issues/58))
- css onload patch for some old browsers
 ([14ce3406](https://github.com/ocombe/ocLazyLoad/commit/14ce34066e0e865c8fa86f663d38e046f7a32abb))
- content inside the oc-lazy-load directive is now compiled on load
 ([9962e2ef](https://github.com/ocombe/ocLazyLoad/commit/9962e2ef163e9449e295dd3297f6019267a0e0e1),
 [#80](https://github.com/ocombe/ocLazyLoad/issues/80))


<a name="0.3.9"></a>
# 0.3.9 (2014-11-02)


## Bug Fixes

- allow components with the same name from different types/modules
 ([f981c337](https://github.com/ocombe/ocLazyLoad/commit/f981c33749e4e61fa4dfd7c3c41df9beffcbf734),
 [#67](https://github.com/ocombe/ocLazyLoad/issues/67))
- initial modules not registered
 ([bcf50004](https://github.com/ocombe/ocLazyLoad/commit/bcf50004b8a1172aff4c769746fdcb9e5d5d9cba),
 [#58](https://github.com/ocombe/ocLazyLoad/issues/58), [#71](https://github.com/ocombe/ocLazyLoad/issues/71), [#73](https://github.com/ocombe/ocLazyLoad/issues/73), [#77](https://github.com/ocombe/ocLazyLoad/issues/77))
- add support for angular 1.3 in bower
 ([bda921b6](https://github.com/ocombe/ocLazyLoad/commit/bda921b68ce30645d992982325adc4eebfdcd361),
 [#76](https://github.com/ocombe/ocLazyLoad/issues/76))


## Features

- broadcast for componentLoaded event provides more info (module name and type)
 ([d41b9f53](https://github.com/ocombe/ocLazyLoad/commit/d41b9f53a46ff8c97b780d4c24f6f64e16017b89))
- example1 now uses ui-grid instead of ng-grid
 ([e7cf1e83](https://github.com/ocombe/ocLazyLoad/commit/e7cf1e83ff1453ee5adb8112052d393f9dc09e27))


## Documentation

- added link to a new article by @kbdaitch
 ([cc6b41db](https://github.com/ocombe/ocLazyLoad/commit/cc6b41db5e0dbcfe68754df325bf9f09e5709bf2))
- added a link to a new lesson from egghead.io
 ([e231f3cb](https://github.com/ocombe/ocLazyLoad/commit/e231f3cbfd6fb3338479a5f4d8a9ce00d374646e))
- added a link to a new lesson from egghead.io
 ([9b3c48e4](https://github.com/ocombe/ocLazyLoad/commit/9b3c48e49800dd3ed6a01dad7c1d958f8625eddb))


<a name="0.3.8"></a>
# 0.3.8 (2014-09-25)


## Bug Fixes

- reject on load error
 ([d83f52b5](https://github.com/ocombe/ocLazyLoad/commit/d83f52b56a77a5cdb230260c497ee2db7283e077),
 [#66](https://github.com/ocombe/ocLazyLoad/issues/66))


<a name="0.3.7"></a>
# 0.3.7 (2014-09-10)


## Bug Fixes

- don't reload a dependency that was just loaded
 ([6752bb94](https://github.com/ocombe/ocLazyLoad/commit/6752bb948093f196311572530d814231dc2dcd3a),
 [#64](https://github.com/ocombe/ocLazyLoad/issues/64))


## Features

- new event ocLazyLoad.moduleReloaded
 ([5010d144](https://github.com/ocombe/ocLazyLoad/commit/5010d144d1b250424be2bcfa98faf50c6782bf96))


<a name="0.3.6"></a>
# 0.3.6 (2014-09-02)


## Bug Fixes

- concurrency lazy loads (thanks @BenBlazely)
 ([4899ea1a](https://github.com/ocombe/ocLazyLoad/commit/4899ea1a09bee145f70aec3dd964f885060422d8),
 [#44](https://github.com/ocombe/ocLazyLoad/issues/44))


## Documentation

- added a few links to other examples


<a name="0.3.5"></a>
# 0.3.5 (2014-08-26)


## Bug Fixes

- fixed cases where the config block would not be called
 ([1e29c9d4](https://github.com/ocombe/ocLazyLoad/commit/1e29c9d438d494cd053cd7533921e02e3fe5e5d0),
 [#5](https://github.com/ocombe/ocLazyLoad/issues/5)).
 The config block would not be called if:
  - defined multiple times (only the first 1 would be invoked)
  - defined with an auto injected module: ['...', function() {}]
  - defined after another component: angular.module().controler().config()


<a name="0.3.4"></a>
# 0.3.4 (2014-08-26)


## Bug Fixes

- make sure reconfig:true always run all invoke blocks
 ([361ae6b7](https://github.com/ocombe/ocLazyLoad/commit/361ae6b7d319cb5ada1ab022a6761d4a67a31b58),
 [#54](https://github.com/ocombe/ocLazyLoad/issues/54))
- the config/run blocks were not invoked without reconfig: true
 ([300882a0](https://github.com/ocombe/ocLazyLoad/commit/300882a016e4f9d538e322be9718f21740048296),
 [#5](https://github.com/ocombe/ocLazyLoad/issues/5))
- indexOf polyfill for IE8
 ([5f71c09c](https://github.com/ocombe/ocLazyLoad/commit/5f71c09cad4255932e84c760b07d16a4a2b016d9),
 [#52](https://github.com/ocombe/ocLazyLoad/issues/52))


## Features

- more log messages for debug
 ([bcbca814](https://github.com/ocombe/ocLazyLoad/commit/bcbca814049863b4dd7a6c5c1071efd760094966))


<a name="0.3.3"></a>
# 0.3.3 (2014-07-23)


## Bug Fixes

- don't execute config blocks multiple times by default
 ([e2fec59e](https://github.com/ocombe/ocLazyLoad/commit/e2fec59ee7ff1e95e7e78ef8397c4fe500d8e7c0),
 [#43](https://github.com/ocombe/ocLazyLoad/issues/43), [#41](https://github.com/ocombe/ocLazyLoad/issues/41))
- don't test for .js in path because of requirejs
 ([6045214b](https://github.com/ocombe/ocLazyLoad/commit/6045214b6a4cc2d9dee1c1f2f89946687d963828))
- test order
 ([8412cb43](https://github.com/ocombe/ocLazyLoad/commit/8412cb431bfc742f2c4151e5b089f3313a70035e))


<a name="0.3.2"></a>
# 0.3.2 (2014-07-23)


## Bug Fixes

- allow $ocLazyLoadProvider.config to be called multiple times
 ([c590579c](https://github.com/ocombe/ocLazyLoad/commit/c590579c9512e0dd3fae2c33c0aefc0bb0f7ca7e),
 [#43](https://github.com/ocombe/ocLazyLoad/issues/43))
- prevent duplicate loadings
 ([12bc6b2b](https://github.com/ocombe/ocLazyLoad/commit/12bc6b2b2d1561517d56c14c56c15c332d578344),
 [#35](https://github.com/ocombe/ocLazyLoad/issues/35),
 [#38](https://github.com/ocombe/ocLazyLoad/issues/38))


<a name="0.3.1"></a>
# 0.3.1 (2014-07-14)


## Bug Fixes

- don't reject file load with custom file loaders such as requirejs
 ([91ed522f](https://github.com/ocombe/ocLazyLoad/commit/91ed522f724c3d384146053623bbd1e7c2c86751),
 [#33](https://github.com/ocombe/ocLazyLoad/issues/33))


## Features

- auto changelog from commits msg
 ([c089e085](https://github.com/ocombe/ocLazyLoad/commit/c089e085431d9f1a968e94c78f3c5ac5af71fa72))
- prevent duplicate loadings & add a cache busting param
 ([5a5d7f10](https://github.com/ocombe/ocLazyLoad/commit/5a5d7f108578fe31c5ca1f7c8dfc2d3bccfd1106),
 [#38](https://github.com/ocombe/ocLazyLoad/issues/38))


# 0.3.0 (17 June 2014)

## Features

- $ocLazyLoad will now reject promises on errors
- Use the parameter `debug` to show log messages in the console
- JS / CSS / Template loaders are available by default in $ocLazyLoad but you can overwrite them with the config
- Better doc (finally! \o/)
- Example1 is now much better !
- Events broadcasted on module / component / file load (#21)


# 0.2.0 (20 May 2014)
* Added support for $animateProvider #19
* Added support for CSS Loading (And perhaps other file types in the future) #19
* Added loadAll function for use when a state requires resolve on more than one asset. #19
* FIX: Angular JS 1.3.0-beta.8 changed the way config blocks are handled (now invoked last) #19
* Adopted the MIT license as requested in #20
* Added a gulpfile to build dist files (and, in the future, to do tests before the build). Run `npm install` to install the new dependencies and `npm build` to build the dist files.
* **Breaking change** moved the src files to /src and the dist files to /dist. Installations via bower will only see the dist folder
* Moved the examples to /examples

# 0.1.3 (30 April 2014)
* Fix for bug #18: merged files and incorrect module loading

# 0.1.2 (14 April 2014)
* Fix for bug #16: config blocks didn't work for module dependencies

# 0.1.1 (08 April 2014)
* Fix for bug #8: runBlocks can now load new modules (thanks to @rolandzwaga)
* Added an example that makes use of requirejs and uses ngGrid as a lazy loaded module (thanks to @rolandzwaga)

# 0.1.0 (04 April 2014)
* Added a changelog !
* Added ```loadTemplateFile``` function.
* Merge pull request #6 from BenBlazely/master (Extension of lazy loading to the angular.module DI block, refactored to use promises for tracking progress.)
* Merge pull request #7 from rolandzwaga/master (Added some improvements for apps using angular.boostrap & for duplicated modules)
* Fixed a bug with run blocks not working when they used unloaded modules. Not a complete fix though, more to come when bug #8 is fixed
