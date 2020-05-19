
import functionRegistry from './../less/functions/function-registry';

export default () => {
    function imageSize() {
        throw {
            type: 'Runtime',
            message: 'Image size functions are not supported in browser version of less'
        };
    }

    const imageFunctions = {
        'image-size': function(filePathNode) {
            imageSize(this, filePathNode);
            return -1;
        },
        'image-width': function(filePathNode) {
            imageSize(this, filePathNode);
            return -1;
        },
        'image-height': function(filePathNode) {
            imageSize(this, filePathNode);
            return -1;
        }
    };

    functionRegistry.addMultiple(imageFunctions);
};
