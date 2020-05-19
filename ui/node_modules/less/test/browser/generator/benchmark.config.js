
module.exports = {
    current: {
    // src is used to build list of less files to compile
        src: [
            "benchmark/benchmark.less"
        ],
        options: {
            helpers: "benchmark/browseroptions.js",
            specs: "benchmark/browserspec.js",
            outfile: "tmp/browser/test-runner-benchmark-current.html"
        }
    },
    v3_10_3: {
        // src is used to build list of less files to compile
        src: [
            "benchmark/benchmark.less"
        ],
        options: {
            helpers: "benchmark/browseroptions.js",
            specs: "benchmark/browserspec.js",
            outfile: "tmp/browser/test-runner-benchmark-v3_10_3.html",
            less: "https://cdnjs.cloudflare.com/ajax/libs/less.js/3.10.3/less.min.js"
        }
    },
    v3_9_0: {
        // src is used to build list of less files to compile
        src: [
            "benchmark/benchmark.less"
        ],
        options: {
            helpers: "benchmark/browseroptions.js",
            specs: "benchmark/browserspec.js",
            outfile: "tmp/browser/test-runner-benchmark-v3_9_0.html",
            less: "https://cdnjs.cloudflare.com/ajax/libs/less.js/3.9.0/less.min.js"
        }
    },
    v2_7_3: {
        // src is used to build list of less files to compile
        src: [
            "benchmark/benchmark.less"
        ],
        options: {
            helpers: "benchmark/browseroptions.js",
            specs: "benchmark/browserspec.js",
            outfile: "tmp/browser/test-runner-benchmark-v2_7_3.html",
            less: "https://cdnjs.cloudflare.com/ajax/libs/less.js/2.7.3/less.min.js"
        }
    }
}