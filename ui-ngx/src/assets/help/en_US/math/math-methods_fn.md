## Built-in mathematical functions
The following expression field provides support for built-in mathematical functions that you can utilize for various calculations.

| Function      | Description                                                         | Example           |
|---------------|---------------------------------------------------------------------|-------------------|
| `abs(x)`      | Absolute value of `x`.                                              | `abs(-7) = 7`     |
| `acos(x)`     | Arc cosine of `x`, result in radians. Requires `-1 ≤ x ≤ 1`.        | `acos(1) = 0`     |
| `asin(x)`     | Arc sine of `x`, result in radians. Requires `-1 ≤ x ≤ 1`.          | `asin(0) = 0`     |
| `atan(x)`     | Arc tangent of `x`, result in radians.                              | `atan(0) = 0`     |
| `cbrt(x)`     | Cube root of `x`.                                                   | `cbrt(8) = 2`     |
| `ceil(x)`     | Rounds `x` up to the nearest integer.                               | `ceil(3.1) = 4`   |
| `cos(x)`      | Cosine of `x`, where `x` is in radians.                             | `cos(0) = 1`      |
| `cosh(x)`     | Hyperbolic cosine of `x`.                                           | `cosh(0) = 1`     |
| `cot(x)`      | Cotangent of `x` (1 / tan(`x`)), where `x` is in radians.           | `cot(0.7854) ≈ 1` |
| `exp(x)`      | Computes `e^x`.                                                     | `exp(0) = 1`      |
| `expm1(x)`    | Computes `e^x - 1` accurately for small `x`.                        | `expm1(0) = 0`    |
| `floor(x)`    | Rounds `x` down to the nearest integer.                             | `floor(3.9) = 3`  |
| `ln(x)`       | Natural logarithm (base *e*) of `x`. Requires `x > 0`.              | `ln(1) = 0`       |
| `log(x)`      | Natural logarithm (base *e*) of `x`. Requires `x > 0`.              | `log(1) = 0`      |
| `lg(x)`       | Natural logarithm (base 10) of `x`. Requires `x > 0`.               | `lg(10) = 1`      |
| `log10(x)`    | Logarithm base 10 of `x`. Requires `x > 0`.                         | `log10(100) = 2`  |
| `log2(x)`     | Logarithm base 2 of `x`. Requires `x > 0`.                          | `log2(8) = 3`     |
| `logab(a, b)` | Logarithm of `b` with base `a`. Requires `a > 0`, `b > 0`, `a ≠ 1`. | `logab(2, 8) = 3` |
| `log1p(x)`    | Computes `ln(1 + x)` accurately for small `x`. Requires `x > -1`.   | `log1p(0) = 0`    |
| `pow(x, y)`   | Raises `x` to the power of `y` (`x^y`).                             | `pow(2, 3) = 8`   |
| `signum(x)`   | Returns the sign of `x`: -1 if `x < 0`, 0 if `x = 0`, 1 if `x > 0`. | `signum(-5) = -1` |
| `sin(x)`      | Sine of `x`, where `x` is in radians.                               | `sin(0) = 0`      |
| `sinh(x)`     | Hyperbolic sine of `x`.                                             | `sinh(0) = 0`     |
| `sqrt(x)`     | Square root of `x`. Requires `x ≥ 0`.                               | `sqrt(4) = 2`     |
| `tan(x)`      | Tangent of `x`, where `x` is in radians.                            | `tan(0) = 0`      |
| `tanh(x)`     | Hyperbolic tangent of `x`.                                          | `tanh(0) = 0`     |
