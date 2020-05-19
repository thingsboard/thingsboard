export default {
    encodeBase64: function encodeBase64(str) {
        // Avoid Buffer constructor on newer versions of Node.js.
        const buffer = (Buffer.from ? Buffer.from(str) : (new Buffer(str)));
        return buffer.toString('base64');
    },
    mimeLookup: function (filename) {
        return require('mime').lookup(filename);
    },
    charsetLookup: function (mime) {
        return require('mime').charsets.lookup(mime);
    },
    getSourceMapGenerator: function getSourceMapGenerator() {
        return require('source-map').SourceMapGenerator;
    }
};
