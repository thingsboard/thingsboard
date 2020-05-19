var fs = require('fs');

var should = require('should');

require('../minify.json.js');

describe('JSON.minify', function() {
    it('define', function() {
        JSON.minify.should.be.ok;
    });
    it('in-memory string', function() {
        var json = '{"key":"value"}';
        var res = JSON.minify(json);
        JSON.parse(res).key.should.equal('value');
    });
    it('comment.json', function() {
        var json = fs.readFileSync(__dirname + '/comment.json', 'utf8');
        var res = JSON.parse(JSON.minify(json));
        res.foo.should.equal('bar');
    });
    it('comment.json', function() {
        var json = fs.readFileSync(__dirname + '/plain.json', 'utf8');
        var res = JSON.parse(JSON.minify(json));
        res.foo.should.equal('bar');
    });
    it('in-memory string json.array plain', function() {
        var json = '[1,2,3]';
        var res = JSON.parse(JSON.minify(json));
        res.length.should.equal(3);
    });
    it('in-memory string json.array start line comment', function() {
        var json = '//[1,2,3]\n[4,5,6,7]';
        var res = JSON.parse(JSON.minify(json));
        res.length.should.equal(4);
        should(res[0]).be.exactly(4).and.be.a.Number();
    });
    it('in-memory string json.array start multi comment', function() {
        var json = '/**[1,2,3]*/[8,9,10,11]';
        var res = JSON.parse(JSON.minify(json));
        res.length.should.equal(4);
        should(res[0]).be.exactly(8).and.be.a.Number();
    });
    it('in-memory string json.array end line comment', function() {
        var json = '[4,5,6,7]//[1,2,3]';
        var res = JSON.parse(JSON.minify(json));
        res.length.should.equal(4);
        should(res[0]).be.exactly(4).and.be.a.Number();
    });
    it('in-memory string json.array end multi comment', function() {
        var json = '[8,9,10,11]/**[1,2,3]*/';
        var res = JSON.parse(JSON.minify(json));
        res.length.should.equal(4);
        should(res[0]).be.exactly(8).and.be.a.Number();
    });
    it('array_comment.json', function() {
        var json = fs.readFileSync(__dirname + '/array_comment.json', 'utf8');
        var res = JSON.parse(JSON.minify(json));
        should(res.length).be.exactly(3).and.be.a.Number();
        should(res[0]['somethi""ng'].length).be.exactly(4).and.be.a.Number();
        should(res[0]['somethi""ng'][3]).be.exactly(23).and.be.a.Number();
    });
});
