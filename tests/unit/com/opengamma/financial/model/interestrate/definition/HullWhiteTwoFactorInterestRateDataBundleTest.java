/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.model.interestrate.definition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.time.calendar.ZonedDateTime;

import org.junit.Test;

import com.opengamma.financial.model.interestrate.curve.ConstantYieldCurve;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.model.volatility.curve.ConstantVolatilityCurve;
import com.opengamma.financial.model.volatility.curve.VolatilityCurve;
import com.opengamma.util.time.DateUtil;

/**
 * 
 */
public class HullWhiteTwoFactorInterestRateDataBundleTest {
  private static final double R = 0.04;
  private static final double SIGMA = 0.2;
  private static final double SPEED_1 = 0.1;
  private static final double SPEED_2 = 0.15;
  private static final double U = 0.1;
  private static final double F = 0.3;
  private static final double RHO = 0.2;
  private static final YieldAndDiscountCurve R_CURVE = new ConstantYieldCurve(R);
  private static final VolatilityCurve SIGMA_CURVE_1 = new ConstantVolatilityCurve(SIGMA);
  private static final VolatilityCurve SIGMA_CURVE_2 = new ConstantVolatilityCurve(2 * SIGMA);
  private static final ZonedDateTime DATE = DateUtil.getUTCDate(2010, 7, 1);
  private static final HullWhiteTwoFactorInterestRateDataBundle DATA = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);

  @Test(expected = IllegalArgumentException.class)
  public void testNullYieldCurve() {
    new HullWhiteTwoFactorInterestRateDataBundle(null, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullVolatilityCurve1() {
    new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, null, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullVolatilityCurve2() {
    new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, null, DATE, SPEED_1, SPEED_2, U, F, RHO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullDate() {
    new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, null, SPEED_1, SPEED_2, U, F, RHO);
  }

  @Test
  public void testGetters() {
    assertEquals(DATA.getYieldCurve(), R_CURVE);
    assertEquals(DATA.getFirstVolatilityCurve(), SIGMA_CURVE_1);
    assertEquals(DATA.getSecondVolatilityCurve(), SIGMA_CURVE_2);
    assertEquals(DATA.getDate(), DATE);
    assertEquals(DATA.getFirstSpeed(), SPEED_1, 0);
    assertEquals(DATA.getSecondSpeed(), SPEED_2, 0);
    assertEquals(DATA.getU(), U, 0);
    assertEquals(DATA.getF(), F, 0);
    assertEquals(DATA.getRho(), RHO, 0);
    double t = 0.2;
    assertEquals(DATA.getInterestRate(t), R_CURVE.getInterestRate(t), 1e-15);
    assertEquals(DATA.getFirstVolatility(t), SIGMA_CURVE_1.getVolatility(t), 1e-15);
    assertEquals(DATA.getSecondVolatility(t), SIGMA_CURVE_2.getVolatility(t), 1e-15);
  }

  @Test
  public void testHashCodeAndEquals() {
    HullWhiteTwoFactorInterestRateDataBundle other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);
    assertEquals(other, DATA);
    assertEquals(other.hashCode(), DATA.hashCode());
    other = new HullWhiteTwoFactorInterestRateDataBundle(new ConstantYieldCurve(R - 0.01), SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, new ConstantVolatilityCurve(SIGMA + 0.3), SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, new ConstantVolatilityCurve(SIGMA + 0.1), DATE, SPEED_1, SPEED_2, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE.minusDays(4), SPEED_1, SPEED_2, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1 + 0.1, SPEED_2, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2 + 0.1, U, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U + 0.1, F, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F + 0.1, RHO);
    assertFalse(other.equals(DATA));
    other = new HullWhiteTwoFactorInterestRateDataBundle(R_CURVE, SIGMA_CURVE_1, SIGMA_CURVE_2, DATE, SPEED_1, SPEED_2, U, F, RHO + 0.1);
    assertFalse(other.equals(DATA));
  }
}
