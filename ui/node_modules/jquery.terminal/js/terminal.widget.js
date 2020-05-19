/* global jQuery */

(function() {
    function get(url) {
        var element;
        if (url.match(/css$/)) {
            element = document.createElement('link');
            element.setAttribute('href', url);
            element.setAttribute('rel', 'stylesheet');
        } else if (url.match(/js$/)) {
            element = document.createElement('script');
            element.setAttribute('src', url);
        }
        console.log(element);
        return new Promise(function(resolve, reject) {
            console.log(element);
            if (element) {
                element.onload = resolve;
                var head = document.querySelector('head');
                head.appendChild(element);
            } else {
                reject();
            }
        });
    }
    var terminals = window.terminals || [];
    if (typeof jQuery === 'undefined') {
        get('http://code.jquery.com/jquery-3.2.1.min.js').then(function() {
            jQuery.noConflict();
            get('https://cdnjs.cloudflare.com/ajax/libs/jquery.terminal/1.7.2/css/jquery.terminal.min.css');
            return get('https://cdnjs.cloudflare.com/ajax/libs/jquery.terminal/1.7.2/js/jquery.terminal.min.js');
        }).then(function() {
            terminals.forEach(function(spec) {
                jQuery.fn.terminal.apply(jQuery(spec[0]), spec.slice(1));
            });
        });
    }
})();
