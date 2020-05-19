var less;

// Dist fallback for NPM-installed Less (for plugins that do testing)
try {
    less = require('../tmp/less.cjs.js');
}
catch (e) {
    less = require('../dist/less.cjs.js');
}

module.exports = less;