angular.module('truncate',[])
    .filter('characters', function() {
        return function(input, chars) {
            if (isNaN(chars)) return input;
            if (chars <= 0) return '';
            if(input && input.length >= chars)
            {
                input = input.substring(0, chars);

                var lastspace = input.lastIndexOf(' ');
                //get last space
                if (lastspace !== -1) {
                    input = input.substr(0, lastspace);
                }
                return input + '...';
            }
            return input;
        };
    })
    .filter('words', function() {
        return function(input, words) {
            if (isNaN(words)) return input;
            if (words<=0) return '';
            if(input)
            {
                var inputWords = input.split(/\s+/);
                if (inputWords.length > words) {
                    input = inputWords.slice(0, words).join(' ') + '...';
                }
            }
            return input;
        };
    });
