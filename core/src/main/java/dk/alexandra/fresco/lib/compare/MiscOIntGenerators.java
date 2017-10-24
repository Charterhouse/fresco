/*******************************************************************************
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.compare;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Misc computation on OInts -- results are cached
 *
 * @author ttoft
 */
public class MiscOIntGenerators {

  private Map<Integer, BigInteger[]> coefficientsOfPolynomiums;
  private List<BigInteger> twoPowersList;
  private BigInteger modulus;

  public MiscOIntGenerators(BigInteger modulus) {
    coefficientsOfPolynomiums = new HashMap<>();

    this.modulus = modulus;
    twoPowersList = new ArrayList<>(1);
    twoPowersList.add(BigInteger.ONE);
  }


  /**
   * Generate a degree l polynomium P such that P(1) = 1 and P(i) = 0 for i in {2,3,...,l+1}
   *
   * @param l degree of polynomium
   * @return coefficients of P
   */
  public BigInteger[] getPoly(int l) {
    // check that l is positive
    Integer lInt = l;
    BigInteger[] result = coefficientsOfPolynomiums.get(lInt);
    if (result == null) {
      // Generate a new set of OInts and store them...
      result = new BigInteger[l + 1];

      BigInteger[] coefficients = constructPolynomial(l, 1);
      for (int i = 0; i <= l; i++) {
        result[i] = coefficients[coefficients.length - 1 - i];
      }

      coefficientsOfPolynomiums.put(lInt, result);
    }
    return result;
  }

  /**
   * Returns the coefficients of a polynomial of degree <i>l</i> such that
   * <i>f(m) = 1</i> and <i>f(n) = 0</i> for <i>1 &le; n &le; l+1</i> and <i>n
   * &ne; m</i> in <i>Z<sub>p</sub></i>. The first element in the array is the
   * coefficient of the term with the highest degree, eg. degree <i>l</i>.
   *
   * @param l The desired degree of <i>f</i>
   * @param m The only non-zero integer point for <i>f</i> in the range <i>1,2,...,l+1</i>.
   */
  private BigInteger[] constructPolynomial(int l, int m) {

		/*
     * Let f_i be the polynoimial which is the product of the first i of
		 * (x-1), (x-2), ..., (x-(m-1)), (x-(m+1)), ..., (x-(l+1)). Then f_0 = 1
		 * and f_i = (x-k) f_{i-1} where k = i if i < m and k = i+1 if i >= m.
		 * Note that we are interested in calculating f(x) = f_l(x) / f_l(m).
		 *
		 * If we let f_ij denote the j'th coefficient of f_i we have the
		 * recurrence relations:
		 *
		 * f_i0 = 1 for all i (highest degree coefficient)
		 *
		 * f_ij = f_{i-1, j} - f_{i-1, j-1} * k for j = 1,...,i
		 *
		 * f_ij = 0 for j > i
		 */
    BigInteger[] f = new BigInteger[l + 1];

    // Initial value: f_0 = 1
    f[0] = BigInteger.valueOf(1);

		/*
     * We also calculate f_i(m) in order to be able to normalize f such that
		 * f(m) = 1. Note that f_i(m) = f_{i-1}(m)(m - k) with the above notation.
		 */
    BigInteger fm = BigInteger.ONE;

    for (int i = 1; i <= l; i++) {
      int k = i;
      if (i >= m) {
        k++;
      }

      // Apply recurrence relation
      f[i] = f[i - 1].multiply(BigInteger.valueOf(-k)).mod(modulus);
      for (int j = i - 1; j > 0; j--) {
        f[j] = f[j].subtract(BigInteger.valueOf(k).multiply(f[j - 1]).mod(modulus)).mod(modulus);
      }

      fm = fm.multiply(BigInteger.valueOf(m - k)).mod(modulus);
    }

    // Scale all coefficients of f_l by f_l(m)^{-1}.
    fm = fm.modInverse(modulus);
    for (int i = 0; i < f.length; i++) {
      f[i] = f[i].multiply(fm).mod(modulus);
    }

    return f;
  }


  public List<BigInteger> getTwoPowersList(int length) {
    int currentLength = twoPowersList.size();
    if (length > currentLength) {
      ArrayList<BigInteger> newTwoPowersList = new ArrayList<>(length);
      newTwoPowersList.addAll(twoPowersList);
      BigInteger currentValue = newTwoPowersList.get(currentLength - 1);
      while (length > newTwoPowersList.size()) {
        currentValue = currentValue.shiftLeft(1);
        newTwoPowersList.add(currentValue);
      }
      twoPowersList = Collections.unmodifiableList(newTwoPowersList);
    }
    return twoPowersList.subList(0, length);
  }

  public BigInteger[] getExpFromOInt(BigInteger value, int maxBitSize) {
    BigInteger[] Ms = new BigInteger[maxBitSize];
    Ms[0] = value;
    for (int i1 = 1; i1 < Ms.length; i1++) {
      Ms[i1] = Ms[i1 - 1].multiply(value).mod(modulus);
    }
    return Ms;
  }

}
