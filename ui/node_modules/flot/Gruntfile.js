/*jshint node: true */
module.exports = function(grunt) {

    // Project configuration.
    grunt.initConfig({
        // Metadata.
        pkg: grunt.file.readJSON("package.json"),
        banner: "/*! <%= pkg.name %> - v<%= pkg.version %> - " +
            "* Copyright (c) <%= grunt.template.today('yyyy') %> IOLA and <%= pkg.author.name %>;" +
            " Licensed <%= pkg.license %> */\n",
        // Task configuration.
        uglify: {
            options: {
                banner: "<%= banner %>"
            },
            dist: {
                expand: true,
                flatten: true,
                src: ["src/**/*.js"],
                dest: "dist/",
                rename: function(base, path) {
                    return base + path.replace(/\.js/, ".min.js");
                }
            }
        },
        jshint: {
            options: grunt.file.readJSON(".jshintrc"),
            gruntfile: {
                src: "Gruntfile.js"
            },
            flot: {
                src: ["src/**/*.js"]
            }
        },
        watch: {
            gruntfile: {
                files: "Gruntfile.js",
                tasks: ["jshint:gruntfile"]
            },
            flot: {
                files: "<%= jshint.flot.src %>",
                tasks: ["jshint:flot"]
            }
        },
        jscs: {
            options: {
                "requireCurlyBraces": [ "if", "else", "for", "while", "do" ],
                "requireSpaceAfterKeywords": [ "if", "else", "for", "while", "do", "switch", "return" ],
                "requireSpacesInFunctionExpression": {
                    "beforeOpeningCurlyBrace": true
                },
                "disallowSpacesInFunctionExpression": {
                    "beforeOpeningRoundBrace": true
                },
                "requireMultipleVarDecl": true,
                "requireSpacesInsideObjectBrackets": "all", // Different from jQuery preset
                "disallowSpacesInsideArrayBrackets": true, // Different from jQuery preset
                "disallowLeftStickedOperators": [ "?", "/", "*", "=", "==", "===", "!=", "!==", ">", ">=", "<", "<=" ],
                "disallowRightStickedOperators": [ "?", "/", "*", ":", "=", "==", "===", "!=", "!==", ">", ">=", "<", "<="],
                "requireSpaceBeforeBinaryOperators": ["+", "-", "/", "*", "=", "==", "===", "!=", "!=="],
                "disallowSpaceAfterPrefixUnaryOperators": ["++", "--", "+", "-"],
                "disallowSpaceBeforePostfixUnaryOperators": ["++", "--"],
                "requireRightStickedOperators": [ "!" ],
                "requireLeftStickedOperators": [ "," ],
                "disallowKeywords": [ "with" ],
                "disallowMultipleLineBreaks": true,
                "disallowKeywordsOnNewLine": [ "else" ],
                "requireLineFeedAtFileEnd": true,
                "disallowSpaceAfterObjectKeys": true,
                "validateJSDoc": {
                    "checkParamNames": true,
                    "checkRedundantParams": true,
                    "requireParamTypes": true
                },
                "validateQuoteMarks": "\""
            },
            flot: {
                src: "<%= jshint.flot.src %>"
            }

        }
    });

    // These plugins provide necessary tasks.
    grunt.loadNpmTasks("grunt-contrib-uglify");
    grunt.loadNpmTasks("grunt-contrib-jshint");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-jscs-checker");

    // Default task.
    grunt.registerTask("default", ["jscs", "jshint", "uglify"]);

};
