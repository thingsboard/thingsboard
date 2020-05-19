// ng-annotate.js
// MIT licensed, see LICENSE file
// Copyright (c) 2013-2016 Olov Lassus <olov.lassus@gmail.com>

"use strict";

const t0 = Date.now();
const fs = require("fs");
const commander = require("commander");
const ngAnnotate = require("./ng-annotate-main");
const version = require("../package.json").version;

const program = new commander.Command()
    .version(version)
    .usage(
        "OPTIONS <file>\n\n" +
        "provide - instead of <file> to read from stdin\n" +
        "use -a and -r together to remove and add (rebuild) annotations in one go"
    )
    .option("-a, --add", "add dependency injection annotations where non-existing")
    .option("-r, --remove", "remove all existing dependency injection annotations")
    .option("-o <file>", "write output to <file>. output is written to stdout by default")
    .option("--sourcemap", "generate an inline sourcemap")
    .option("--sourceroot <sourceRoot>", "set the sourceRoot property of the generated sourcemap")
    .option("--single_quotes", "use single quotes (') instead of double quotes (\")")
    .option("--regexp <regexp>", "detect short form myMod.controller(...) iff myMod matches regexp")
    .option("--rename <to>", "rename declarations and annotated references\noldname1 newname1 oldname2 newname2 ...", "")
    .option("--plugin <plugin>", "use plugin with path (experimental)")
    .option("--enable <name>", "enable optional with name")
    .option("--list", "list all optional names")
    .option("--stats", "print statistics on stderr (experimental)")
    .parse(process.argv);

function exit(msg) {
    if (msg) {
        process.stderr.write(msg);
        process.stderr.write("\n");
    }
    process.exit(-1);
}

// special-case for --list
if (program.list) {
    const list = ngAnnotate("", {list: true}).list;
    if (list.length >= 1) {
        process.stdout.write(list.join("\n") + "\n");
    }
    process.exit(0);
}

// validate options
if (program.args.length !== 1) {
    program.outputHelp();
    exit("error: no input file provided");
}

if (!program.add && !program.remove) {
    program.outputHelp();
    exit("error: missing option --add and/or --remove");
}

const filename = program.args.shift();

(filename === "-" ? slurpStdin : slurpFile)(runAnnotate);


function slurpStdin(cb) {
    let buf = "";

    process.stdin.setEncoding("utf8");
    process.stdin.on("data", function(d) {
        buf += d;
    });
    process.stdin.on("end", function() {
        cb(null, buf);
    });
    process.stdin.resume();
}

function slurpFile(cb) {
    if (!fs.existsSync(filename)) {
        cb(new Error(`error: file not found ${filename}`));
    }

    fs.readFile(filename, cb);
}

function runAnnotate(err, src) {
    if (err) {
        exit(err.message);
    }

    src = String(src);

    let config;
    try {
        config = JSON.parse(String(fs.readFileSync("ng-annotate-config.json")));
    } catch (e) {
        config = {};
    }

    if (filename !== "-") {
        config.inFile = filename;
    }

    ["add", "remove", "o", "regexp", "rename", "single_quotes", "plugin", "enable", "stats"].forEach(function(opt) {
        if (opt in program) {
            config[opt] = program[opt];
        }
    });

    if (program.sourcemap) {
        config.map = { inline: true, sourceRoot: program.sourceroot };
        if (filename !== "-") {
            config.map.inFile = filename;
        }
    };

    if (config.enable && !Array.isArray(config.enable)) {
        config.enable = [config.enable];
    }

    if (config.plugin) {
        if (!Array.isArray(config.plugin)) {
            config.plugin = [config.plugin];
        }
        config.plugin = config.plugin.map(function(path) {
            let absPath;
            try {
                absPath = fs.realpathSync.bind(fs, path);
            } catch (e) {
                absPath = null;
            }
            if (!absPath) {
                exit(`error: plugin file not found ${path}`);
            }
            // the require below may throw an exception on parse-error
            try {
                return require(absPath);
            } catch (e) {
                // node will already print file:line and offending line to stderr
                exit(`error: couldn't require("${absPath}")`);
            }
        });
    }

    const trimmedRename = config.rename && config.rename.trim();
    if (trimmedRename) {
        const flattenRename = trimmedRename.split(" ");
        const renameArray = [];
        for (let i = 0; i < flattenRename.length; i = i + 2) {
            renameArray.push({
                "from": flattenRename[i],
                "to": flattenRename[i + 1],
            });
        }
        config.rename = renameArray;
    } else {
        config.rename = null;
    }

    const run_t0 = Date.now();
    const ret = ngAnnotate(src, config);
    const run_t1 = Date.now();

    if (ret.errors) {
        exit(ret.errors.join("\n"));
    }

    const stats = ret._stats;
    if (config.stats && stats) {
        const t1 = Date.now();
        const all = t1 - t0;
        const run_parser = stats.parser_parse_t1 - stats.parser_parse_t0;
        const all_parser = run_parser + (stats.parser_require_t1 - stats.parser_require_t0);
        const nga_run = (run_t1 - run_t0) - run_parser;
        const nga_init = all - all_parser - nga_run;

        const pct = function(n) {
            return Math.round(100 * n / all);
        }

        process.stderr.write(`[${all} ms] parser: ${all_parser}, nga init: ${nga_init}, nga run: ${nga_run}\n`);
        process.stderr.write(`[%] parser: ${pct(all_parser)}, nga init: ${pct(nga_init)}, nga run: ${pct(nga_run)}\n`);
    }

    if (ret.src && config.o) {
        try {
            fs.writeFileSync(config.o, ret.src);
        } catch (e) {
            exit(e.message);
        }
    } else if (ret.src) {
        process.stdout.write(ret.src);
    }
}
