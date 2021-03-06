/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.forex.defaultproperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.financial.analytics.OpenGammaFunctionExclusions;
import com.opengamma.financial.analytics.model.InterpolatedDataProperties;
import com.opengamma.financial.analytics.model.forex.ForexVisitors;
import com.opengamma.financial.property.DefaultPropertyFunction;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.option.FXBarrierOptionSecurity;
import com.opengamma.financial.security.option.FXDigitalOptionSecurity;
import com.opengamma.financial.security.option.FXOptionSecurity;
import com.opengamma.financial.security.option.NonDeliverableFXDigitalOptionSecurity;
import com.opengamma.financial.security.option.NonDeliverableFXOptionSecurity;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 * Default properties for FX options priced using the Black functions.
 */
public class FXOptionBlackSurfaceDefaults extends DefaultPropertyFunction {
  private static final Logger s_logger = LoggerFactory.getLogger(FXOptionBlackSurfaceDefaults.class);
  private static final String[] VALUE_REQUIREMENTS = new String[] {
    ValueRequirementNames.PRESENT_VALUE,
    ValueRequirementNames.FX_PRESENT_VALUE,
    ValueRequirementNames.FX_CURRENCY_EXPOSURE,
    ValueRequirementNames.VALUE_VEGA,
    ValueRequirementNames.VALUE_GAMMA,
    ValueRequirementNames.VALUE_GAMMA_P,
    ValueRequirementNames.VEGA_MATRIX,
    ValueRequirementNames.VEGA_QUOTE_MATRIX,
    ValueRequirementNames.FX_CURVE_SENSITIVITIES,
    ValueRequirementNames.PV01,
    ValueRequirementNames.SECURITY_IMPLIED_VOLATILITY,
    ValueRequirementNames.VALUE_THETA,
    ValueRequirementNames.YIELD_CURVE_NODE_SENSITIVITIES,
    ValueRequirementNames.VALUE_RHO,
    ValueRequirementNames.VALUE_PHI,
    ValueRequirementNames.VALUE_VOMMA,
    ValueRequirementNames.VALUE_VANNA
  };
  private final PriorityClass _priority;
  private final String _interpolatorName;
  private final String _leftExtrapolatorName;
  private final String _rightExtrapolatorName;
  private final Map<Pair<String, String>, String> _surfaceNameByCurrencyPair;

  /**
   * @param priority The priority of the functions
   * @param interpolatorName The volatility surface interpolator name
   * @param leftExtrapolatorName The volatility surface left extrapolator name
   * @param rightExtrapolatorName The volatility surface right extrapolator name
   * @param surfaceNamesByCurrencyPair Values for the properties per currency: an array of strings where the <i>i<sup>th</sup></i> currency has properties:
   * <ul>
   * <li><i>i</i> = first currency name,
   * <li><i>i + 1</i> = second currency name,
   * <li><i>i + 2</i> = surface name
   * </ul>
   */
  public FXOptionBlackSurfaceDefaults(final String priority, final String interpolatorName, final String leftExtrapolatorName, final String rightExtrapolatorName,
      final String... surfaceNamesByCurrencyPair) {
    super(ComputationTargetType.SECURITY, true);
    ArgumentChecker.notNull(priority, "priority");
    ArgumentChecker.notNull(interpolatorName, "interpolator name");
    ArgumentChecker.notNull(leftExtrapolatorName, "left extrapolator name");
    ArgumentChecker.notNull(rightExtrapolatorName, "right extrapolator name");
    ArgumentChecker.notNull(surfaceNamesByCurrencyPair, "property values by currency");
    ArgumentChecker.isTrue(surfaceNamesByCurrencyPair.length % 3 == 0, "Must have one surface name per currency pair");
    _priority = PriorityClass.valueOf(priority);
    _interpolatorName = interpolatorName;
    _leftExtrapolatorName = leftExtrapolatorName;
    _rightExtrapolatorName = rightExtrapolatorName;
    _surfaceNameByCurrencyPair = new HashMap<Pair<String, String>, String>();
    for (int i = 0; i < surfaceNamesByCurrencyPair.length; i += 3) {
      final String firstCurrency = surfaceNamesByCurrencyPair[i];
      final String secondCurrency = surfaceNamesByCurrencyPair[i + 1];
      ArgumentChecker.isFalse(firstCurrency.equals(secondCurrency), "The two currencies must not be equal; have {} and {}", firstCurrency, secondCurrency);
      final String surfaceName = surfaceNamesByCurrencyPair[i + 2];
      _surfaceNameByCurrencyPair.put(Pair.of(firstCurrency, secondCurrency), surfaceName);
    }
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() != ComputationTargetType.SECURITY) {
      return false;
    }
    if (!(target.getSecurity() instanceof FinancialSecurity)) {
      return false;
    }
    final FinancialSecurity security = (FinancialSecurity) target.getSecurity();
    final boolean isFXOption = (security instanceof FXOptionSecurity
        || security instanceof FXBarrierOptionSecurity
        || security instanceof FXDigitalOptionSecurity
        || security instanceof NonDeliverableFXOptionSecurity
        || security instanceof NonDeliverableFXDigitalOptionSecurity);
    if (!isFXOption) {
      return false;
    }
    final String putCurrency = security.accept(ForexVisitors.getPutCurrencyVisitor()).getCode();
    final String callCurrency = security.accept(ForexVisitors.getCallCurrencyVisitor()).getCode();
    final Pair<String, String> pair = Pair.of(putCurrency, callCurrency);
    if (_surfaceNameByCurrencyPair.containsKey(pair)) {
      return true;
    }
    return _surfaceNameByCurrencyPair.containsKey(Pair.of(callCurrency, putCurrency));
  }

  @Override
  protected void getDefaults(final PropertyDefaults defaults) {
    for (final String valueRequirement : VALUE_REQUIREMENTS) {
      defaults.addValuePropertyName(valueRequirement, ValuePropertyNames.SURFACE);
      defaults.addValuePropertyName(valueRequirement, InterpolatedDataProperties.X_INTERPOLATOR_NAME);
      defaults.addValuePropertyName(valueRequirement, InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME);
      defaults.addValuePropertyName(valueRequirement, InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME);
    }
  }

  @Override
  protected Set<String> getDefaultValue(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue, final String propertyName) {
    if (InterpolatedDataProperties.X_INTERPOLATOR_NAME.equals(propertyName)) {
      return Collections.singleton(_interpolatorName);
    }
    if (InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME.equals(propertyName)) {
      return Collections.singleton(_leftExtrapolatorName);
    }
    if (InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME.equals(propertyName)) {
      return Collections.singleton(_rightExtrapolatorName);
    }
    final FinancialSecurity security = (FinancialSecurity) target.getSecurity();
    final String putCurrency = security.accept(ForexVisitors.getPutCurrencyVisitor()).getCode();
    final String callCurrency = security.accept(ForexVisitors.getCallCurrencyVisitor()).getCode();
    if (ValuePropertyNames.SURFACE.equals(propertyName)) {
      Pair<String, String> pair = Pair.of(putCurrency, callCurrency);
      if (_surfaceNameByCurrencyPair.containsKey(pair)) {
        return Collections.singleton(_surfaceNameByCurrencyPair.get(pair));
      }
      pair = Pair.of(callCurrency, putCurrency);
      if (_surfaceNameByCurrencyPair.containsKey(pair)) {
        return Collections.singleton(_surfaceNameByCurrencyPair.get(pair));
      }
      s_logger.error("Could not get surface name for currency pair {}, {}; should never happen", putCurrency, callCurrency);
    }
    return null;
  }

  @Override
  public PriorityClass getPriority() {
    return _priority;
  }

  @Override
  public String getMutualExclusionGroup() {
    return OpenGammaFunctionExclusions.FX_OPTION_BLACK_SURFACE_DEFAULTS;
  }
}
