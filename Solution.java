// ====================================================================
// Solution.java — Shamir's Secret Finder using Lagrange Interpolation
//
// Language : Java  (no Python)
// JSON read: java.nio.file.Files.readString()  — no direct value passing
// Algebra  : Exact rational arithmetic with BigInteger (no float errors)
// Formula  : Lagrange interpolation evaluated at x = 0  →  f(0) = secret
// ====================================================================

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Solution {

    // ── 1.  Decode an arbitrary-base string → BigInteger ─────────────
    //
    //  Processes digit by digit so numbers of any size stay exact.
    //  Supports bases 2–36 (digits 0-9 a-z).
    //  e.g.  decodeBase("111", 2)  → 7
    //        decodeBase("213", 4)  → 39
    //        decodeBase("aed7015a346d635", 15) → ...
    // ─────────────────────────────────────────────────────────────────
    static BigInteger decodeBase(String value, int base) {
        final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";
        BigInteger B      = BigInteger.valueOf(base);
        BigInteger result = BigInteger.ZERO;

        for (char ch : value.toLowerCase().toCharArray()) {
            int d = DIGITS.indexOf(ch);
            if (d < 0 || d >= base)
                throw new IllegalArgumentException(
                    "Invalid digit '" + ch + "' for base " + base);
            result = result.multiply(B).add(BigInteger.valueOf(d));
        }
        return result;
    }

    // ── 2.  Reduce fraction num/den to lowest terms (den > 0) ────────
    static BigInteger[] reduce(BigInteger num, BigInteger den) {
        if (den.signum() < 0) { num = num.negate(); den = den.negate(); }
        BigInteger g = num.abs().gcd(den);          // BigInteger.gcd() built-in
        return new BigInteger[]{ num.divide(g), den.divide(g) };
    }

    // ── 3.  Lagrange Interpolation at x = 0 ──────────────────────────
    //
    //  f(0) = Σᵢ  yᵢ · Lᵢ(0)
    //
    //              Π_{j≠i} (0 − xⱼ)        Π_{j≠i} (−xⱼ)
    //  Lᵢ(0)  =  ─────────────────────  =  ─────────────────────
    //              Π_{j≠i} (xᵢ − xⱼ)        Π_{j≠i} (xᵢ − xⱼ)
    //
    //  All intermediate values kept as exact BigInteger fractions.
    //  The constant term of an integer-coefficient polynomial at
    //  integer nodes is always an integer, so denominator → 1.
    // ─────────────────────────────────────────────────────────────────
    static BigInteger lagrangeAt0(List<BigInteger[]> pts) {
        BigInteger sumN = BigInteger.ZERO;
        BigInteger sumD = BigInteger.ONE;
        int        m    = pts.size();

        for (int i = 0; i < m; i++) {
            BigInteger xi = pts.get(i)[0];
            BigInteger yi = pts.get(i)[1];

            BigInteger liN = BigInteger.ONE;   // numerator   of Lᵢ(0)
            BigInteger liD = BigInteger.ONE;   // denominator of Lᵢ(0)

            for (int j = 0; j < m; j++) {
                if (i == j) continue;
                BigInteger xj = pts.get(j)[0];
                liN = liN.multiply(xj.negate());         // (0 − xⱼ)
                liD = liD.multiply(xi.subtract(xj));     // (xᵢ − xⱼ)
            }

            //  sumN/sumD  +=  yi * liN / liD
            BigInteger newN = sumN.multiply(liD)
                                  .add(yi.multiply(liN).multiply(sumD));
            BigInteger newD = sumD.multiply(liD);

            BigInteger[] r = reduce(newN, newD);
            sumN = r[0];
            sumD = r[1];
        }

        if (!sumD.equals(BigInteger.ONE))
            System.out.println("  ⚠  Denominator = " + sumD + " (unexpected)");

        return sumN.divide(sumD);
    }

    // ── 4.  Read JSON file and solve ──────────────────────────────────
    //
    //  JSON is read via Files.readString() — NO values are hard-coded.
    //  A lightweight regex parser handles the fixed schema:
    //    { "keys": { "n": N, "k": K },
    //      "X":    { "base": "B", "value": "V" }, ... }
    // ─────────────────────────────────────────────────────────────────
    static void solveFile(String filename) throws IOException {

        System.out.println("\n" + "=".repeat(64));
        System.out.println("  File : " + filename);

        // ── JSON CALL ─────────────────────────────────────────────────
        String json = Files.readString(Path.of(filename));

        // Extract n and k from "keys" block
        int n = Integer.parseInt(extractFirst(json, "\"n\"\\s*:\\s*(\\d+)"));
        int k = Integer.parseInt(extractFirst(json, "\"k\"\\s*:\\s*(\\d+)"));
        System.out.printf("  n = %d  |  k = %d  |  polynomial degree = %d%n", n, k, k - 1);
        System.out.println("-".repeat(64));

        // Extract root entries — pattern: "X": { "base": "B", "value": "V" }
        Pattern rootPat = Pattern.compile(
            "\"(\\d+)\"\\s*:\\s*\\{\\s*" +
            "\"base\"\\s*:\\s*\"(\\d+)\"\\s*,\\s*" +
            "\"value\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");

        Matcher        mat    = rootPat.matcher(json);
        List<BigInteger[]> pts = new ArrayList<>();

        while (mat.find()) {
            int    x    = Integer.parseInt(mat.group(1));
            int    base = Integer.parseInt(mat.group(2));
            String raw  = mat.group(3);
            BigInteger y = decodeBase(raw, base);

            System.out.printf("  x = %2d  base = %2d  raw = \"%s\"%n", x, base, raw);
            System.out.printf("                 decoded y = %s%n", y);
            pts.add(new BigInteger[]{ BigInteger.valueOf(x), y });
        }

        // Use exactly k points (minimum required to determine the polynomial)
        List<BigInteger[]> chosen = pts.subList(0, k);
        System.out.printf("%n  Using first %d points → Lagrange interpolation at x = 0 …%n", k);

        BigInteger secret = lagrangeAt0(chosen);

        System.out.println("-".repeat(64));
        System.out.println("  SECRET  (constant term  c = f(0))  :  " + secret);
        System.out.println("=".repeat(64));
    }

    // Helper — return first capture group of a regex match
    static String extractFirst(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) throw new RuntimeException("Pattern not found: " + regex);
        return m.group(1);
    }

    // ── 5.  Entry point ───────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        solveFile("tc1.json");
        solveFile("tc2.json");
    }
}
