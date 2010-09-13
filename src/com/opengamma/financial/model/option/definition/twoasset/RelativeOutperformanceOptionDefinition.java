/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.model.option.definition.twoasset;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.model.option.definition.EuropeanExerciseFunction;
import com.opengamma.financial.model.option.definition.OptionDefinition;
import com.opengamma.financial.model.option.definition.OptionExerciseFunction;
import com.opengamma.financial.model.option.definition.OptionPayoffFunction;
import com.opengamma.util.time.Expiry;

/**
 * Defines a European-style relative outperformance option (also known as quotient options).
 * <p>
 * The payoff of a relative outperformance call option is  
 * {@latex.ilb %preamble{\\usepackage{amsmath}}
 * \\begin{eqnarray*}
 * max\\left(\\frac{S_1}{S_2} - K, 0\\right)
 * \\end{eqnarray*}}
 * and that of a put is
 * {@latex.ilb %preamble{\\usepackage{amsmath}}
 * \\begin{eqnarray*}
 * max\\left(K - \\frac{S_1}{S_2}, 0\\right)
 * \\end{eqnarray*}}
 * where {@latex.inline $K$} is the strike, {@latex.inline $S_1$} is the spot price of the first underlying and {@latex.inline $S_2$} is the spot price of 
 * the second underlying.
 */
public class RelativeOutperformanceOptionDefinition extends OptionDefinition {
  private final OptionExerciseFunction<StandardTwoAssetOptionDataBundle> _exerciseFunction = new EuropeanExerciseFunction<StandardTwoAssetOptionDataBundle>();
  private final OptionPayoffFunction<StandardTwoAssetOptionDataBundle> _payoffFunction = new OptionPayoffFunction<StandardTwoAssetOptionDataBundle>() {

    @Override
    public double getPayoff(final StandardTwoAssetOptionDataBundle data, final Double optionPrice) {
      Validate.notNull(data, "data");
      final double s1 = data.getFirstSpot();
      final double s2 = data.getSecondSpot();
      return isCall() ? Math.max(s1 / s2 - getStrike(), 0) : Math.max(getStrike() - s1 / s2, 0);
    }
  };

  /**
   * 
   * @param strike The strike of the option
   * @param expiry The expiry of the option
   * @param isCall Is the option a call
   */
  public RelativeOutperformanceOptionDefinition(final double strike, final Expiry expiry, final boolean isCall) {
    super(strike, expiry, isCall);
  }

  /**
   * The exercise function of this option is European (see {@link EuropeanExerciseFunction})
   * @return The exercise function
   */
  @SuppressWarnings("unchecked")
  @Override
  public OptionExerciseFunction<StandardTwoAssetOptionDataBundle> getExerciseFunction() {
    return _exerciseFunction;
  }

  /**
   * 
   * @return The payoff function
   */
  @SuppressWarnings("unchecked")
  @Override
  public OptionPayoffFunction<StandardTwoAssetOptionDataBundle> getPayoffFunction() {
    return _payoffFunction;
  }

}
