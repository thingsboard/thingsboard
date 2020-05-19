'use strict';
module.exports = function (grunt) {
    grunt.initConfig({
        sass: {
            compile: {
                files: {
                    'styles.css': 'styles.scss'
                },
                options: {
                    'outputStyle': 'nested'
                }
            }
        }
    });
    grunt.loadNpmTasks('grunt-sass');
    grunt.registerTask('default', ['sass']);
};
