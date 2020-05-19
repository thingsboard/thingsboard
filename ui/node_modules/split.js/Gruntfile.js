
module.exports = function(grunt) {
    grunt.initConfig({
        jasmine: {
            test: {
                src: 'split.js',
                options: {
                    specs: ['test/split.spec.js'],
                },
            },
        },
    })

    grunt.loadNpmTasks('grunt-contrib-jasmine')
}
