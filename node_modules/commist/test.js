
var test = require('tape').test

var commist = require('./')

test('registering a command', function (t) {
  t.plan(2)

  var program = commist()

  var result

  program.register('hello', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  result = program.parse(['hello', 'a', '-x', '23'])

  t.notOk(result, 'must return null, the command have been handled')
})

test('registering two commands', function (t) {
  t.plan(1)

  var program = commist()

  program.register('hello', function (args) {
    t.ok(false, 'must pick the right command')
  })

  program.register('world', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  program.parse(['world', 'a', '-x', '23'])
})

test('registering two commands (bis)', function (t) {
  t.plan(1)

  var program = commist()

  program.register('hello', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  program.register('world', function (args) {
    t.ok(false, 'must pick the right command')
  })

  program.parse(['hello', 'a', '-x', '23'])
})

test('registering two words commands', function (t) {
  t.plan(1)

  var program = commist()

  program.register('hello', function (args) {
    t.ok(false, 'must pick the right command')
  })

  program.register('hello world', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  program.parse(['hello', 'world', 'a', '-x', '23'])
})

test('registering two words commands (bis)', function (t) {
  t.plan(1)

  var program = commist()

  program.register('hello', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  program.register('hello world', function (args) {
    t.ok(false, 'must pick the right command')
  })

  program.parse(['hello', 'a', '-x', '23'])
})

test('registering ambiguous commands throws exception', function (t) {
  var program = commist()

  function noop () {}

  program.register('hello', noop)
  program.register('hello world', noop)
  program.register('hello world matteo', noop)

  try {
    program.register('hello world', noop)
    t.ok(false, 'must throw if double-registering the same command')
  } catch (err) {
  }

  t.end()
})

test('looking up commands', function (t) {
  var program = commist()

  function noop1 () {}
  function noop2 () {}
  function noop3 () {}

  program.register('hello', noop1)
  program.register('hello world matteo', noop3)
  program.register('hello world', noop2)

  t.equal(program.lookup('hello')[0].func, noop1)
  t.equal(program.lookup('hello world matteo')[0].func, noop3)
  t.equal(program.lookup('hello world')[0].func, noop2)

  t.end()
})

test('looking up commands with abbreviations', function (t) {
  var program = commist()

  function noop1 () {}
  function noop2 () {}
  function noop3 () {}

  program.register('hello', noop1)
  program.register('hello world matteo', noop3)
  program.register('hello world', noop2)

  t.equal(program.lookup('hel')[0].func, noop1)
  t.equal(program.lookup('hel wor mat')[0].func, noop3)
  t.equal(program.lookup('hel wor')[0].func, noop2)

  t.end()
})

test('looking up strict commands', function (t) {
  var program = commist()

  function noop1 () {}
  function noop2 () {}

  program.register({ command: 'restore', strict: true }, noop1)
  program.register({ command: 'rest', strict: true }, noop2)

  t.equal(program.lookup('restore')[0].func, noop1)
  t.equal(program.lookup('rest')[0].func, noop2)
  t.equal(program.lookup('remove')[0], undefined)

  t.end()
})

test('executing commands from abbreviations', function (t) {
  t.plan(1)

  var program = commist()

  program.register('hello', function (args) {
    t.deepEqual(args, ['a', '-x', '23'])
  })

  program.register('hello world', function (args) {
    t.ok(false, 'must pick the right command')
  })

  program.parse(['hel', 'a', '-x', '23'])
})

test('a command must be at least 3 chars', function (t) {
  var program = commist()

  function noop1 () {}

  try {
    program.register('h', noop1)
    t.ok(false, 'not thrown')
  } catch (err) {
  }

  t.end()
})

test('a command part must be at least 3 chars', function (t) {
  var program = commist()

  function noop1 () {}

  try {
    program.register('h b', noop1)
    t.ok(false, 'not thrown')
  } catch (err) {
  }

  t.end()
})
