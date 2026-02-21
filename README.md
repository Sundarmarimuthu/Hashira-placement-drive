# Secret Finder — Lagrange Interpolation

> Placements Assignment — finds the **constant term `c = f(0)`** of a polynomial
> whose roots are given as base-encoded `(x, y)` shares (Shamir's Secret Sharing model).

---

## Approach

| Step | What happens |
|------|-------------|
| **JSON Read** | `Files.readString()` loads the test-case file — no values are hard-coded |
| **Base Decode** | Each y-value is decoded from its arbitrary base (2–36) using `BigInteger` digit-by-digit accumulation — handles 50+ digit numbers exactly |
| **Lagrange at x=0** | The first `k` points are used to evaluate the unique degree-(k−1) polynomial at x = 0, giving `f(0) = c` (the secret) |
| **Exact Arithmetic** | All fractions are stored as BigInteger pairs `(num, den)` reduced via GCD — zero floating-point error |

**Formula:**

```
f(0) = Σᵢ  yᵢ · Π_{j≠i}  (−xⱼ) / (xᵢ − xⱼ)
```

---

## Files

```
hashira/
├── Solution.java   ← main Java solution (no external libraries)
├── tc1.json        ← test case 1  (n=4, k=3)
├── tc2.json        ← test case 2  (n=10, k=7)
└── README.md
```

---

## Compile & Run

```bash
# Compile
javac Solution.java

# Run (tc1.json and tc2.json must be in the same directory)
java Solution
```

**Requirements:** Java 11+ (for `Files.readString`), no external dependencies.

---

## Expected Output

```
================================================================
  File : tc1.json
  n = 4  |  k = 3  |  polynomial degree = 2
  ...
  SECRET  (constant term  c = f(0))  :  3
================================================================

================================================================
  File : tc2.json
  n = 10  |  k = 7  |  polynomial degree = 6
  ...
  SECRET  (constant term  c = f(0))  :  <large number>
================================================================
```
