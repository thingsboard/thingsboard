
describe 'Angular Hotkeys', ->

  hotkeys = scope = $rootScope = $rootElement = $window = null

  beforeEach ->
    module 'cfp.hotkeys', (hotkeysProvider) ->
      hotkeysProvider.useNgRoute = true
      return

    result = null
    inject (_$rootElement_, _$rootScope_, _hotkeys_) ->
      hotkeys = _hotkeys_
      $rootElement = _$rootElement_
      $rootScope = _$rootScope_
      scope = $rootScope.$new()

  afterEach ->
    hotkeys.del('w')
    t = document.getElementById('cfp-test')
    if t
      t.parentNode.removeChild(t)

  it 'should insert the help menu into the dom', ->
    children = angular.element($rootElement).children()
    expect(children.hasClass('cfp-hotkeys-container')).toBe true

  it 'add(args)', ->
    hotkeys.add 'w', 'description here', ->
    expect(hotkeys.get('w').description).toBe 'description here'
    expect(hotkeys.get('x')).toBe false

  it 'add(object)', ->
    callback = false
    hotkeys.add
      combo: 'w'
      description: 'description'
      callback: () ->
        callback = true

    expect(hotkeys.get('w').description).toBe 'description'

    # Test callback:
    expect(callback).toBe false
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(callback).toBe true

  it 'description should be optional', ->
    # func argument style:
    hotkeys.add 'w', ->
    expect(hotkeys.get('w').description).toBe '$$undefined$$'

    # object style:
    hotkeys.add
      combo: 'e'
      callback: ->

    expect(hotkeys.get('e').description).toBe '$$undefined$$'

  it 'del()', ->
    hotkeys.add 'w', ->
    expect(hotkeys.get('w').description).toBe '$$undefined$$'
    hotkeys.del 'w'
    expect(hotkeys.get('w')).toBe false

  it 'should toggle help when ? is pressed', ->
    expect(angular.element($rootElement).children().hasClass('in')).toBe false
    KeyEvent.simulate('?'.charCodeAt(0), 90)
    expect(angular.element($rootElement).children().hasClass('in')).toBe true

  it 'should bind esc when the cheatsheet is shown', ->
    expect(hotkeys.get('esc')).toBe false
    expect(angular.element($rootElement).children().hasClass('in')).toBe false
    KeyEvent.simulate('?'.charCodeAt(0), 90)
    expect(angular.element($rootElement).children().hasClass('in')).toBe true
    expect(hotkeys.get('esc').combo).toEqual ['esc']
    KeyEvent.simulate('?'.charCodeAt(0), 90)
    expect(hotkeys.get('esc')).toBe false

  it 'should remember previously bound ESC when cheatsheet is shown', ->
    expect(hotkeys.get('esc')).toBe false

    # bind something to escape:
    hotkeys.add 'esc', 'temp', () ->
    expect(hotkeys.get('esc').description).toBe 'temp'
    originalCallback = hotkeys.get('esc').callback

    # show the cheat-sheet which will overwrite the esc key. however, we want to
    # show the original combo description in the callback, yet have the new
    # callback bound to remove the cheatsheet from view.
    KeyEvent.simulate('?'.charCodeAt(0), 90)
    expect(hotkeys.get('esc').description).toBe 'temp'
    expect(hotkeys.get('esc').callback).not.toBe originalCallback

    # hide the cheat sheet to verify the previous esc binding is back
    KeyEvent.simulate('?'.charCodeAt(0), 90)
    expect(hotkeys.get('esc').description).toBe 'temp'

  it 'should (un)bind based on route changes', ->
    # fake a route change:
    expect(hotkeys.get('w e s')).toBe false
    $rootScope.$broadcast('$routeChangeSuccess', { hotkeys: [['w e s', 'Do something Amazing!', 'callme("ishmael")']] });
    expect(hotkeys.get('w e s').combo).toEqual ['w e s']

    # ensure hotkey is unbound when the route changes
    $rootScope.$broadcast('$routeChangeSuccess', {});
    expect(hotkeys.get('w e s')).toBe false

  it 'should callback when the hotkey is pressed', ->
    executed = false

    hotkeys.add 'w', ->
      executed = true

    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executed).toBe true

  it 'should callback according to action', ->
    keypressA = false;
    keypressB = false;

    hotkeys.add 'a', ->
      keypressA = true
    , 'keyup'

    hotkeys.add 'b', ->
      keypressB = true

    KeyEvent.simulate('a'.charCodeAt(0), 90)
    KeyEvent.simulate('b'.charCodeAt(0), 90)
    expect(keypressA).toBe false
    expect(keypressB).toBe true
    expect(hotkeys.get('a').action).toBe 'keyup'

  it 'should allow to invoke hotkey.callback programmatically without event object', ->
    called = false;

    hotkeys.add 'a', ->
      called = true
    , 'keyup'

    hotkeys.get('a').callback()
    expect(called).toBe true


  it 'should run routes-defined hotkey callbacks when scope is available', ->
    executed = false
    passedArg = null

    $rootScope.callme = (arg) ->
      executed = true
      passedArg = arg

    $rootScope.$broadcast '$routeChangeSuccess',
      hotkeys: [['w', 'Do something Amazing!', 'callme("ishmael")']]
      scope: $rootScope

    expect(executed).toBe false
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executed).toBe true
    expect(passedArg).toBe 'ishmael'

  it 'should callback when hotkey is pressed in input field and allowIn INPUT is configured', ->
    executed = no

    $body = angular.element document.body
    $input = angular.element '<input id="cfp-test"/>'
    $body.prepend $input

    hotkeys.add
      combo: 'w'
      allowIn: ['INPUT']
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $input[0])
    expect(executed).toBe yes

  it 'should callback when hotkey is pressed in select field and allowIn SELECT is configured', ->
    executed = no

    $body = angular.element document.body
    $select = angular.element '<select id="cfp-test"/>'
    $body.prepend $select

    hotkeys.add
      combo: 'w'
      allowIn: ['SELECT']
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $select[0])
    expect(executed).toBe yes

  it 'should callback when hotkey is pressed in textarea field and allowIn TEXTAREA is configured', ->
    executed = no

    $body = angular.element document.body
    $textarea = angular.element '<textarea id="cfp-test"/>'
    $body.prepend $textarea

    hotkeys.add
      combo: 'w'
      allowIn: ['TEXTAREA']
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $textarea[0])
    expect(executed).toBe yes

  it 'should not callback when hotkey is pressed in input field without allowIn INPUT', ->
    executed = no

    $body = angular.element document.body
    $input = angular.element '<input id="cfp-test"/>'
    $body.prepend $input

    hotkeys.add
      combo: 'w'
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $input[0])
    expect(executed).toBe no

  it 'should not callback when hotkey is pressed in select field without allowIn SELECT', ->
    executed = no

    $body = angular.element document.body
    $select = angular.element '<select id="cfp-test"/>'
    $body.prepend $select

    hotkeys.add
      combo: 'w'
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $select[0])
    expect(executed).toBe no

  it 'should not callback when hotkey is pressed in textarea field without allowIn TEXTAREA', ->
    executed = no

    $body = angular.element document.body
    $textarea = angular.element '<textarea id="cfp-test"/>'
    $body.prepend $textarea

    hotkeys.add
      combo: 'w'
      callback: -> executed = yes

    KeyEvent.simulate('w'.charCodeAt(0), 90, undefined, $textarea[0])
    expect(executed).toBe no

  it 'should callback when the mousetrap class is present', ->
    executed = no

    $body = angular.element document.body
    $input = angular.element '<input class="mousetrap" id="cfp-test"/>'
    $body.prepend $input

    hotkeys.add
      combo: 'a'
      callback: -> executed = yes

    KeyEvent.simulate('a'.charCodeAt(0), 90, undefined, $input[0])
    expect(executed).toBe yes

  it 'should be capable of binding to a scope and auto-destroy itself', ->
    hotkeys.bindTo(scope)
    .add
      combo: ['w', 'e', 's']
      description: 'description for w'
      callback: () ->
      persistent: false
    .add
      combo: 'a'
      action: 'keyup'
      description: 'description for a',
      callback: () ->
    .add('b', 'description for b', () ->)
    .add('c', 'description for c', () ->)


    expect(hotkeys.get('w').combo).toEqual ['w', 'e', 's']
    expect(hotkeys.get('e').combo).toEqual ['w', 'e', 's']
    expect(hotkeys.get('a').combo).toEqual ['a']
    expect(hotkeys.get('b').combo).toEqual ['b']
    expect(hotkeys.get('c').combo).toEqual ['c']

    scope.$destroy()
    expect(hotkeys.get('w')).toBe false
    expect(hotkeys.get('e')).toBe false
    expect(hotkeys.get('s')).toBe false
    expect(hotkeys.get('a')).toBe false
    expect(hotkeys.get('b')).toBe false
    expect(hotkeys.get('c')).toBe false

  it 'should allow multiple calls to bindTo for same scope and still be auto-destroying', ->
    hotkeys.bindTo(scope)
    .add
      combo: ['w', 'e', 's']
      description: 'description for w'
      callback: () ->
      persistent: false

    hotkeys.bindTo(scope)
    .add
      combo: 'a'
      action: 'keyup'
      description: 'description for a',
      callback: () ->
    .add('b', 'description for b', () ->)
    .add('c', 'description for c', () ->)


    expect(hotkeys.get('w').combo).toEqual ['w', 'e', 's']
    expect(hotkeys.get('e').combo).toEqual ['w', 'e', 's']
    expect(hotkeys.get('a').combo).toEqual ['a']
    expect(hotkeys.get('b').combo).toEqual ['b']
    expect(hotkeys.get('c').combo).toEqual ['c']

    scope.$destroy()
    expect(hotkeys.get('w')).toBe false
    expect(hotkeys.get('e')).toBe false
    expect(hotkeys.get('s')).toBe false
    expect(hotkeys.get('a')).toBe false
    expect(hotkeys.get('b')).toBe false
    expect(hotkeys.get('c')).toBe false

  it 'should support pause/unpause for temporary disabling of hotkeys', ->
    executed = false

    hotkeys.add 'w', ->
      executed = true

    hotkeys.pause()
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executed).toBe false
    hotkeys.unpause()
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executed).toBe true


  describe 'misc regression tests', ->

    # allowIn arguments were not aligned (issue #40 and #36) so test to prevent regressions:
    it 'should unbind hotkeys that have also set allowIn (#36, #40)', ->
      # func argument style should be deprecated soon
      hotkeys.add 't', 'testing', () ->
        test = true
      , undefined, undefined, false

      hotkeys.add
        combo: 'w'
        description: 'description'
        callback: () ->
        persistent: false

      expect(hotkeys.get('t').combo).toEqual ['t']
      expect(hotkeys.get('w').combo).toEqual ['w']
      expect(hotkeys.get('t').persistent).toBe false
      expect(hotkeys.get('w').persistent).toBe false

      $rootScope.$broadcast('$routeChangeSuccess', {});
      expect(hotkeys.get('t')).toBe false
      expect(hotkeys.get('w')).toBe false

    it '#42 closing cheatsheet with x should use toggleCheatSheet so esc is unbound', ->
      expect(hotkeys.get('esc')).toBe false

      expect(angular.element($rootElement).children().hasClass('in')).toBe false
      KeyEvent.simulate('?'.charCodeAt(0), 90)
      expect(angular.element($rootElement).children().hasClass('in')).toBe true

      expect(hotkeys.get('esc').combo).toEqual ['esc']
      # hotkeys.toggleCheatSheet()
      scope.$$prevSibling.toggleCheatSheet()
      expect(hotkeys.get('esc')).toBe false


  describe 'multiple bindings', ->

    it 'get()', ->

      hotkeys.add ['a', 'b', 'c'], ->

      # Make sure they were added:
      expect(hotkeys.get('a').combo).toEqual ['a', 'b', 'c']
      expect(hotkeys.get('b').combo).toEqual ['a', 'b', 'c']
      expect(hotkeys.get('c').combo).toEqual ['a', 'b', 'c']
      expect(hotkeys.get('w')).toBe false
      # expect(hotkeys.get(['a', 'b'])).toEqual ['a', 'b', 'c']

    it 'should callback', ->
      executeCount = 0

      hotkeys.add ['a', 'b', 'c'], ->
        executeCount++

      # Make sure they work:
      KeyEvent.simulate('a'.charCodeAt(0), 90)
      expect(executeCount).toBe 1
      KeyEvent.simulate('b'.charCodeAt(0), 90)
      expect(executeCount).toBe 2
      KeyEvent.simulate('c'.charCodeAt(0), 90)
      expect(executeCount).toBe 3
      KeyEvent.simulate('w'.charCodeAt(0), 90)
      expect(executeCount).toBe 3

    it 'should delete', ->

      hotkeys.add ['w', 'e', 's'], ->

      expect(hotkeys.get('w').combo).toEqual ['w', 'e', 's']
      expect(hotkeys.del(['w', 'f'])).toBe false
      expect(hotkeys.get('w')).toBe false
      expect(hotkeys.get('e').combo).toEqual ['e', 's']
      expect(hotkeys.del(['e', 's'])).toBe true

    it 'should still callback when some combos remain', ->

      executeCount = 0
      hotkeys.add ['a', 'b', 'c'], ->
        executeCount++

      # Delete, but leave a hotkey:
      hotkeys.del ['a', 'b']
      KeyEvent.simulate('b'.charCodeAt(0), 90)
      expect(executeCount).toBe 0
      KeyEvent.simulate('c'.charCodeAt(0), 90)
      expect(executeCount).toBe 1

      expect(hotkeys.get('a')).toBe false
      expect(hotkeys.get('b')).toBe false
      expect(hotkeys.get('c').combo).toEqual ['c']

    it '#49 regression test', ->
      hotkeys.add
        combo: ['1','2','3','4','5','6','7','8','9']
        description: 'ensure no regressions'
        callback: () ->

      expect(hotkeys.get('1').combo).toEqual ['1','2','3','4','5','6','7','8','9']
      hotkeys.del(['1','2','3','4','5','6','7','8','9'])
      expect(hotkeys.get('1')).toBe false



describe 'hotkey directive', ->

  elSimple = elAllowIn = elMultiple = scope = hotkeys = $compile = $document = executedSimple = executedAllowIn = null

  beforeEach ->
    module('cfp.hotkeys')
    executedSimple = no
    executedAllowIn = no

    inject ($rootScope, _$compile_, _$document_, _hotkeys_) ->
      hotkeys = _hotkeys_
      $compile = _$compile_
      # el = angular.element()
      scope = $rootScope.$new()
      scope.callmeSimple = () ->
        executedSimple = yes
      scope.callmeAllowIn = () ->
        executedAllowIn = yes
      scope.callmeMultiple = () ->
      elSimple = $compile('<div hotkey="{e: callmeSimple}" hotkey-description="testing simple case"></div>')(scope)
      elAllowIn = $compile('<div hotkey="{w: callmeAllowIn}" hotkey-description="testing with allowIn" hotkey-allow-in="INPUT, TEXTAREA"></div>')(scope)
      elMultiple = $compile('<div hotkey="{a: callmeMultiple, b: callmeMultiple}" hotkey-description="testing with multiple hotkeys"></div>')(scope)
      scope.$digest()

  it 'should allow hotkey binding via directive', ->
    expect(hotkeys.get('e').combo).toEqual ['e']
    expect(hotkeys.get('w').combo).toEqual ['w']
    expect(executedSimple).toBe no
    expect(executedAllowIn).toBe no
    KeyEvent.simulate('e'.charCodeAt(0), 90)
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executedSimple).toBe yes
    expect(executedAllowIn).toBe yes

  it 'should accept allowIn arguments', ->

    $body = angular.element document.body
    $input = angular.element '<input id="cfp-test"/>'
    $body.prepend $input

    expect(executedAllowIn).toBe no
    KeyEvent.simulate('w'.charCodeAt(0), 90)
    expect(executedAllowIn).toBe yes
    expect(hotkeys.get('w').allowIn).toEqual ['INPUT', 'TEXTAREA']

  it 'should unbind the hotkey when the directive is destroyed', ->
    expect(hotkeys.get('e').combo).toEqual ['e']
    expect(hotkeys.get('w').combo).toEqual ['w']
    expect(hotkeys.get('a').combo).toEqual ['a']
    expect(hotkeys.get('b').combo).toEqual ['b']
    elSimple.remove()
    elAllowIn.remove()
    elMultiple.remove()
    expect(hotkeys.get('e')).toBe no
    expect(hotkeys.get('w')).toBe no
    expect(hotkeys.get('a')).toBe no
    expect(hotkeys.get('b')).toBe no


describe 'Platform specific things', ->
  beforeEach ->
    windowMock =
      navigator:
        platform: 'Macintosh'

    module 'cfp.hotkeys'

  it 'should display mac key combos', ->
    module ($provide) ->
      $provide.value '$window', angular.extend window,
        navigator:
          platform: 'Macintosh'
      return

    inject (hotkeys) ->
      hotkeys.add 'mod+e', 'description'
      expect(hotkeys.get('mod+e').format()[0]).toBe '⌘ + e'

  it 'should display win/linux key combos', ->
    module ($provide) ->
      $provide.value '$window', angular.extend window,
        navigator:
          platform: 'Linux x86_64'
      return

    inject (hotkeys) ->
      hotkeys.add 'mod+e', 'description'
      expect(hotkeys.get('mod+e').format()[0]).toBe 'ctrl + e'


describe 'Configuration options', ->

  it 'should disable the cheatsheet when configured', ->
    module 'cfp.hotkeys', (hotkeysProvider) ->
      hotkeysProvider.includeCheatSheet = false
      return
    inject ($rootElement, hotkeys) ->
      children = angular.element($rootElement).children()
      expect(children.length).toBe 0

  it 'should enable the cheatsheet when configured', ->
    module 'cfp.hotkeys', (hotkeysProvider) ->
      hotkeysProvider.includeCheatSheet = true
      return
    inject ($rootElement, hotkeys) ->
      children = angular.element($rootElement).children()
      expect(children.length).toBe 1

  it 'should accept an alternate template to inject', ->
    module 'cfp.hotkeys', (hotkeysProvider) ->
      hotkeysProvider.template = '<div class="little-teapot">boo</div>'
      return
    inject ($rootElement, hotkeys) ->
      children = angular.element($rootElement).children()
      expect(children.hasClass('little-teapot')).toBe true

  it 'should run and inject itself so it is always available', ->
    module 'cfp.hotkeys'

    inject ($rootElement) ->
      children = angular.element($rootElement).children()
      expect(children.hasClass('cfp-hotkeys-container')).toBe true

  it 'should attach to body if $rootElement is document (#8)', inject ($rootElement) ->

    injected = angular.element(document.body).find('div')
    expect(injected.length).toBe 0

    injector = angular.bootstrap(document, ['cfp.hotkeys'])
    injected = angular.element(document.body).find('div')
    expect(injected.length).toBe 3
    expect(injected.hasClass('cfp-hotkeys-container')).toBe true

  it 'should have a configurable hotkey and description', ->
    module 'cfp.hotkeys', (hotkeysProvider) ->
      hotkeysProvider.cheatSheetHotkey = 'h'
      hotkeysProvider.cheatSheetDescription = 'Alternate description'
      return

    inject ($rootElement, hotkeys) ->
      expect(hotkeys.get('h')).not.toBe false
      expect(angular.element($rootElement).children().hasClass('in')).toBe false
      KeyEvent.simulate('?'.charCodeAt(0), 90)
      expect(angular.element($rootElement).children().hasClass('in')).toBe false
      KeyEvent.simulate('h'.charCodeAt(0), 90)
      expect(angular.element($rootElement).children().hasClass('in')).toBe true

      expect(hotkeys.get('h').description).toBe 'Alternate description'

  it 'should have a configurable useNgRoute defaulted to false if ngRoute is not loaded', ->
    module 'cfp.hotkeys'
    inject (hotkeys) ->
      expect(hotkeys.useNgRoute).toBe false

  it 'should have a configurable useNgRoute defaulted to true if ngRoute is loaded', ->
    module 'ngRoute'
    module 'cfp.hotkeys'
    inject (hotkeys) ->
      expect(hotkeys.useNgRoute).toBe true
