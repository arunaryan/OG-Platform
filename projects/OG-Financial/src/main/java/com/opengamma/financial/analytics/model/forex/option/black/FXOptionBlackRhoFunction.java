/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.forex.option.black;

import static com.opengamma.financial.analytics.model.forex.option.black.FXOptionFunctionUtils.getResultCurrency;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.forex.method.MultipleCurrencyInterestRateCurveSensitivity;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.analytics.model.InterpolatedDataProperties;
import com.opengamma.financial.analytics.model.forex.ForexVisitors;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.currency.CurrencyPairs;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.option.FXBarrierOptionSecurity;
import com.opengamma.financial.security.option.FXDigitalOptionSecurity;
import com.opengamma.financial.security.option.FXOptionSecurity;
import com.opengamma.financial.security.option.NonDeliverableFXDigitalOptionSecurity;
import com.opengamma.financial.security.option.NonDeliverableFXOptionSecurity;
import com.opengamma.util.async.AsynchronousExecution;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.DoublesPair;


/**
 *
 */
public class FXOptionBlackRhoFunction extends AbstractFunction.NonCompiledInvoker {

  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target,
      final Set<ValueRequirement> desiredValues) throws AsynchronousExecution {
    final FinancialSecurity security = (FinancialSecurity) target.getSecurity();
    final ValueRequirement desiredValue = Iterables.getOnlyElement(desiredValues);
    final Object curveSensitivitiesObject = inputs.getValue(ValueRequirementNames.FX_CURVE_SENSITIVITIES);
    if (curveSensitivitiesObject == null) {
      throw new OpenGammaRuntimeException("Could not get curve sensitivities");
    }
    final Currency putCurrency = security.accept(ForexVisitors.getPutCurrencyVisitor());
    final Currency callCurrency = security.accept(ForexVisitors.getCallCurrencyVisitor());
    final Object baseQuotePairsObject = inputs.getValue(ValueRequirementNames.CURRENCY_PAIRS);
    if (baseQuotePairsObject == null) {
      throw new OpenGammaRuntimeException("Could not get base/quote pair data");
    }
    final CurrencyPairs baseQuotePairs = (CurrencyPairs) baseQuotePairsObject;
    final CurrencyPair baseQuotePair = baseQuotePairs.getCurrencyPair(putCurrency, callCurrency);
    if (baseQuotePair == null) {
      throw new OpenGammaRuntimeException("Could not get base/quote pair for currency pair (" + putCurrency + ", " + callCurrency + ")");
    }
    //final String callCurrency = security.accept(ForexVisitors.getCallCurrencyVisitor()).getCode();
    final String callCurrencyCurve = desiredValue.getConstraint(FXOptionBlackFunction.CALL_CURVE);
    final String resultCurrency = getResultCurrency(target, baseQuotePair);
    final String fullCurveName = callCurrencyCurve + "_" + callCurrency.getCode();
    final MultipleCurrencyInterestRateCurveSensitivity curveSensitivities = (MultipleCurrencyInterestRateCurveSensitivity) curveSensitivitiesObject;
    final Map<String, List<DoublesPair>> sensitivitiesForCurrency = curveSensitivities.getSensitivity(Currency.of(resultCurrency)).getSensitivities();
    final ValueSpecification spec = new ValueSpecification(ValueRequirementNames.VALUE_RHO, target.toSpecification(), getResultProperties(target, desiredValue, baseQuotePair).get());
    final double rho = sensitivitiesForCurrency.get(fullCurveName).get(0).second;
    return Collections.singleton(new ComputedValue(spec, rho));
  }

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.SECURITY;
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() != ComputationTargetType.SECURITY) {
      return false;
    }
    return target.getSecurity() instanceof FXOptionSecurity
        || target.getSecurity() instanceof FXBarrierOptionSecurity
        || target.getSecurity() instanceof FXDigitalOptionSecurity
        || target.getSecurity() instanceof NonDeliverableFXOptionSecurity
        || target.getSecurity() instanceof NonDeliverableFXDigitalOptionSecurity;
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
    final ValueProperties.Builder properties = getResultProperties();
    return Collections.singleton(new ValueSpecification(ValueRequirementNames.VALUE_RHO, target.toSpecification(), properties.get()));
  }

  @Override
  public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue) {
    final ValueProperties constraints = desiredValue.getConstraints();
    final Set<String> putCurveNames = constraints.getValues(FXOptionBlackFunction.PUT_CURVE);
    if (putCurveNames == null || putCurveNames.size() != 1) {
      return null;
    }
    final Set<String> callCurveNames = constraints.getValues(FXOptionBlackFunction.CALL_CURVE);
    if (callCurveNames == null || callCurveNames.size() != 1) {
      return null;
    }
    final Set<String> putCurveCalculationConfigs = constraints.getValues(FXOptionBlackFunction.PUT_CURVE_CALC_CONFIG);
    if (putCurveCalculationConfigs == null || putCurveCalculationConfigs.size() != 1) {
      return null;
    }
    final Set<String> callCurveCalculationConfigs = constraints.getValues(FXOptionBlackFunction.CALL_CURVE_CALC_CONFIG);
    if (callCurveCalculationConfigs == null || callCurveCalculationConfigs.size() != 1) {
      return null;
    }
    final Set<String> surfaceNames = constraints.getValues(ValuePropertyNames.SURFACE);
    if (surfaceNames == null || surfaceNames.size() != 1) {
      return null;
    }
    final Set<String> interpolatorNames = constraints.getValues(InterpolatedDataProperties.X_INTERPOLATOR_NAME);
    if (interpolatorNames == null || interpolatorNames.size() != 1) {
      return null;
    }
    final Set<String> leftExtrapolatorNames = constraints.getValues(InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME);
    if (leftExtrapolatorNames == null || leftExtrapolatorNames.size() != 1) {
      return null;
    }
    final Set<String> rightExtrapolatorNames = constraints.getValues(InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME);
    if (rightExtrapolatorNames == null || rightExtrapolatorNames.size() != 1) {
      return null;
    }
    final String putCurveName = putCurveNames.iterator().next();
    final String callCurveName = callCurveNames.iterator().next();
    final String putCurveCalculationConfig = putCurveCalculationConfigs.iterator().next();
    final String callCurveCalculationConfig = callCurveCalculationConfigs.iterator().next();
    final String surfaceName = surfaceNames.iterator().next();
    final String interpolatorName = interpolatorNames.iterator().next();
    final String leftExtrapolatorName = leftExtrapolatorNames.iterator().next();
    final String rightExtrapolatorName = rightExtrapolatorNames.iterator().next();
    final ValueRequirement pairQuoteRequirement = new ValueRequirement(ValueRequirementNames.CURRENCY_PAIRS, target.toSpecification());
    final ValueRequirement sensitivitiesRequirement = getCurveSensitivitiesRequirement(putCurveName, putCurveCalculationConfig,
        callCurveName, callCurveCalculationConfig, surfaceName, interpolatorName, leftExtrapolatorName, rightExtrapolatorName, target);
    return Sets.newHashSet(pairQuoteRequirement, sensitivitiesRequirement);
  }

  private ValueProperties.Builder getResultProperties() {
    return createValueProperties()
        .with(ValuePropertyNames.CALCULATION_METHOD, FXOptionBlackFunction.BLACK_METHOD)
        .withAny(FXOptionBlackFunction.PUT_CURVE)
        .withAny(FXOptionBlackFunction.PUT_CURVE_CALC_CONFIG)
        .withAny(FXOptionBlackFunction.CALL_CURVE)
        .withAny(FXOptionBlackFunction.CALL_CURVE_CALC_CONFIG)
        .withAny(ValuePropertyNames.SURFACE)
        .withAny(InterpolatedDataProperties.X_INTERPOLATOR_NAME)
        .withAny(InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME)
        .withAny(InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME)
        .withAny(ValuePropertyNames.CURRENCY);
  }

  private ValueProperties.Builder getResultProperties(final ComputationTarget target, final ValueRequirement desiredValue, final CurrencyPair baseQuotePair) {
    final String putCurveName = desiredValue.getConstraint(FXOptionBlackFunction.PUT_CURVE);
    final String callCurveName = desiredValue.getConstraint(FXOptionBlackFunction.CALL_CURVE);
    final String putCurveCalculationConfig = desiredValue.getConstraint(FXOptionBlackFunction.PUT_CURVE_CALC_CONFIG);
    final String callCurveCalculationConfig = desiredValue.getConstraint(FXOptionBlackFunction.CALL_CURVE_CALC_CONFIG);
    final String interpolatorName = desiredValue.getConstraint(InterpolatedDataProperties.X_INTERPOLATOR_NAME);
    final String leftExtrapolatorName = desiredValue.getConstraint(InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME);
    final String rightExtrapolatorName = desiredValue.getConstraint(InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME);
    final String surfaceName = desiredValue.getConstraint(ValuePropertyNames.SURFACE);
    return createValueProperties()
        .with(ValuePropertyNames.CALCULATION_METHOD, FXOptionBlackFunction.BLACK_METHOD)
        .with(FXOptionBlackFunction.PUT_CURVE, putCurveName)
        .with(FXOptionBlackFunction.PUT_CURVE_CALC_CONFIG, putCurveCalculationConfig)
        .with(FXOptionBlackFunction.CALL_CURVE, callCurveName)
        .with(FXOptionBlackFunction.CALL_CURVE_CALC_CONFIG, callCurveCalculationConfig)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(InterpolatedDataProperties.X_INTERPOLATOR_NAME, interpolatorName)
        .with(InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME, leftExtrapolatorName)
        .with(InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME, rightExtrapolatorName)
        .with(ValuePropertyNames.CURRENCY, getResultCurrency(target, baseQuotePair));
  }

  private static ValueRequirement getCurveSensitivitiesRequirement(final String putCurveName, final String putCurveCalculationConfig, final String callCurveName,
      final String callCurveCalculationConfig, final String surfaceName, final String interpolatorName, final String leftExtrapolatorName,
      final String rightExtrapolatorName, final ComputationTarget target) {
    final ValueProperties properties = ValueProperties.builder()
        .with(FXOptionBlackFunction.PUT_CURVE, putCurveName)
        .with(FXOptionBlackFunction.CALL_CURVE, callCurveName)
        .with(FXOptionBlackFunction.PUT_CURVE_CALC_CONFIG, putCurveCalculationConfig)
        .with(FXOptionBlackFunction.CALL_CURVE_CALC_CONFIG, callCurveCalculationConfig)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(ValuePropertyNames.CALCULATION_METHOD, FXOptionBlackFunction.BLACK_METHOD)
        .with(InterpolatedDataProperties.X_INTERPOLATOR_NAME, interpolatorName)
        .with(InterpolatedDataProperties.LEFT_X_EXTRAPOLATOR_NAME, leftExtrapolatorName)
        .with(InterpolatedDataProperties.RIGHT_X_EXTRAPOLATOR_NAME, rightExtrapolatorName).get();
    return new ValueRequirement(ValueRequirementNames.FX_CURVE_SENSITIVITIES, target.toSpecification(), properties);
  }
}
