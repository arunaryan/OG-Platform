/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.fudgemsg;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.financial.analytics.volatility.surface.FuturePriceCurveDefinition;
import com.opengamma.util.money.Currency;

/**
 * 
 */
public class FuturePriceCurveDefinitionFudgeEncodingTest extends FinancialTestBase {
  private static final String NAME = "DN";
  private static final Currency UID = Currency.USD;
  private static final Double[] X = new Double[] {.1, 2., 3., 4., 5., 6., 7., 8.};

  @Test
  public void testCycle() {
    final FuturePriceCurveDefinition<Double> definition = new FuturePriceCurveDefinition<Double>(NAME, UID, X);
    assertEquals(definition, cycleObject(FuturePriceCurveDefinition.class, definition));
  }
}
