/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.var.parametric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.junit.Test;

import com.opengamma.math.function.Function1D;
import com.opengamma.math.matrix.ColtMatrixAlgebra;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.matrix.DoubleMatrix2D;
import com.opengamma.math.matrix.Matrix;
import com.opengamma.math.matrix.MatrixAlgebra;

/**
 * 
 */
public class DeltaMeanCalculatorTest {
  private static final MatrixAlgebra ALGEBRA = new ColtMatrixAlgebra();
  private static final Function1D<ParametricWithMeanVaRDataBundle, Double> F = new DeltaMeanCalculator(ALGEBRA);
  private static final DoubleMatrix1D EMPTY_VECTOR = new DoubleMatrix1D(new double[0]);
  private static final DoubleMatrix2D EMPTY_MATRIX = new DoubleMatrix2D(new double[0][0]);
  private static final DoubleMatrix1D VECTOR = new DoubleMatrix1D(new double[] {3});
  private static final DoubleMatrix2D MATRIX = new DoubleMatrix2D(new double[][] {new double[] {1}});

  @Test(expected = IllegalArgumentException.class)
  public void testNullAlgebra() {
    new DeltaMeanCalculator(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullData() {
    F.evaluate((ParametricWithMeanVaRDataBundle) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyValueDeltaVector() {
    final ParametricWithMeanVaRDataBundle data = new ParametricWithMeanVaRDataBundle(Collections.<Integer, DoubleMatrix1D> singletonMap(1, EMPTY_VECTOR), Collections
        .<Integer, Matrix<?>> singletonMap(1, EMPTY_VECTOR), Collections.<Integer, DoubleMatrix2D> singletonMap(1, EMPTY_MATRIX));
    F.evaluate(data);
  }

  @Test
  public void testEqualsAndHashCode() {
    final Function1D<ParametricWithMeanVaRDataBundle, Double> f1 = new DeltaMeanCalculator(ALGEBRA);
    final Function1D<ParametricWithMeanVaRDataBundle, Double> f2 = new DeltaMeanCalculator(new ColtMatrixAlgebra());
    assertEquals(f1, F);
    assertEquals(f1.hashCode(), F.hashCode());
    assertFalse(f1.equals(f2));
  }

  @Test
  public void test() {
    final ParametricWithMeanVaRDataBundle data = new ParametricWithMeanVaRDataBundle(Collections.<Integer, DoubleMatrix1D> singletonMap(1, VECTOR), Collections.<Integer, Matrix<?>> singletonMap(1,
        VECTOR), Collections.<Integer, DoubleMatrix2D> singletonMap(1, MATRIX));
    assertEquals(F.evaluate(data), 9, 1e-9);
  }
}
