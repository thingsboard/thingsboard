const template = require('./template')
let config
const fs = require('fs-extra')
const path = require('path')
const globby = require('globby')
const { runner } = require('mocha-headless-chrome')


if (process.argv[2]) {
    config = require(`./${process.argv[2]}.config`)
} else {
    config = require('./runner.config')
}

/**
 * Generate templates and run tests
 */
const tests = []
const cwd = process.cwd()
const tmpDir = path.join(cwd, 'tmp', 'browser')
fs.ensureDirSync(tmpDir)
fs.copySync(path.join(cwd, 'test', 'browser', 'common.js'), path.join(tmpDir, 'common.js'))

let numTests = 0
let passedTests = 0
let failedTests = 0

/** Will run the runners in a series */
function runSerial(tasks) {
    var result = Promise.resolve()
    start = Date.now()
    tasks.forEach(task => {
        result = result.then(result => {
            if (result && result.result && result.result.stats) {
                const stats = result.result.stats
                numTests += stats.tests
                passedTests += stats.passes
                failedTests += stats.failures
            }
            return task()
        }, err => {
            console.log(err)
            failedTests += 1
        })
    })
    return result
}

Object.entries(config).forEach(entry => {
    const test = entry[1]
    const paths = globby.sync(test.src)
    const templateString = template(paths, test.options.helpers, test.options.specs)
    fs.writeFileSync(path.join(cwd, test.options.outfile), templateString)
    tests.push(() => {
        const file = 'http://localhost:8081/' + test.options.outfile
        console.log(file)
        return runner({
            file,
            timeout: 2000,
            args: ['disable-web-security']
        })
    })
})

module.exports = () => runSerial(tests).then(() => {
    if (failedTests > 0) {
        process.stderr.write(failedTests + ' Failed, ' + passedTests + ' passed\n');
    } else {
        process.stdout.write('All Passed ' + passedTests + ' run\n');
    }
    if (failedTests) {
        process.on('exit', function() { process.reallyExit(1); });
    }
    process.exit()
}, err => {
    process.stderr.write(err.message);
    process.exit()
})
