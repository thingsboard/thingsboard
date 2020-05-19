/* eslint-env jasmine */
/* global Split */

function calcParts (expr) {
    var re = /calc\(([\d]*\.?[\d]*?)%\s?-\s?([\d]+)px\)/
    var m = re.exec(expr)

    return {
        percentage: parseFloat(m[1]),
        pixels: parseInt(m[2], 10),
    }
}

describe('Split', function() {
    beforeEach(function() {
        document.body.style.width = '800px'
        document.body.style.height = '600px'

        this.a = document.createElement('div')
        this.b = document.createElement('div')
        this.c = document.createElement('div')

        this.a.id = 'a'
        this.b.id = 'b'
        this.c.id = 'c'

        document.body.appendChild(this.a)
        document.body.appendChild(this.b)
        document.body.appendChild(this.c)
    })

    afterEach(function() {
        document.body.removeChild(this.a)
        document.body.removeChild(this.b)
        document.body.removeChild(this.c)
    })

    it('splits in two when given two elements', function() {
        Split(['#a', '#b'])

        expect(this.a.style.width).toBe('50%')
        expect(this.b.style.width).toBe('50%')
    })

    it('splits in three when given three elements', function() {
        Split(['#a', '#b', '#c'])

        expect(this.a.style.width).toBe('33.33%')
        expect(this.b.style.width).toBe('33.33%')
        expect(this.c.style.width).toBe('33.33%')
    })

    it('splits vertically when direction is vertical', function() {
        Split(['#a', '#b'], {
            direction: 'vertical',
        })

        expect(this.a.style.height).toBe('50%')
        expect(this.b.style.height).toBe('50%')
    })

    it('splits in percentages when given sizes', function() {
        Split(['#a', '#b'], {
            sizes: [25, 75],
        })

        expect(this.a.style.width).toBe('25%')
        expect(this.b.style.width).toBe('75%')
    })

    it('splits in percentages when given sizes', function() {
        Split(['#a', '#b'], {
            sizes: [25, 75],
        })

        expect(this.a.style.width).toBe('25%')
        expect(this.b.style.width).toBe('75%')
    })

    it('accounts for gutter size', function() {
        Split(['#a', '#b'], {
            gutterSize: 20,
        })

        expect(this.a.style.width).toBe('50%')
        expect(this.b.style.width).toBe('50%')
    })

    it('accounts for gutter size with more than two elements', function() {
        Split(['#a', '#b', '#c'], {
            gutterSize: 20,
        })

        expect(this.a.style.width).toBe('33.33%')
        expect(this.b.style.width).toBe('33.33%')
        expect(this.c.style.width).toBe('33.33%')
    })

    it('accounts for gutter size when direction is vertical', function() {
        Split(['#a', '#b'], {
            direction: 'vertical',
            gutterSize: 20,
        })

        expect(this.a.style.height).toBe('50%')
        expect(this.b.style.height).toBe('50%')
    })

    it('accounts for gutter size with more than two elements when direction is vertical', function() {
        Split(['#a', '#b', '#c'], {
            direction: 'vertical',
            gutterSize: 20,
        })

        expect(this.a.style.height).toBe('33.33%')
        expect(this.b.style.height).toBe('33.33%')
        expect(this.c.style.height).toBe('33.33%')
    })

    it('set size directly when given css values', function() {
        Split(['#a', '#b'], {
            sizes: ['150px', '640px'],
        })

        expect(this.a.style.width).toBe('150px')
        expect(this.b.style.width).toBe('640px')
    })

    it('adjusts sizes using setSizes', function() {
        var split = Split(['#a', '#b'])

        split.setSizes([70, 30])

        expect(this.a.style.width).toBe('70%')
        expect(this.b.style.width).toBe('30%')
    })

    it('sets element styles using the elementStyle function', function() {
        Split(['#a', '#b'], {
            elementStyle: function (dimension, size, gutterSize) {
                return {
                    'width': size + '%',
                }
            },
        })

        expect(this.a.style.width).toBe('50%')
        expect(this.b.style.width).toBe('50%')
    })
})
