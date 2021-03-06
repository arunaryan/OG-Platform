/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.bloombergexample.loader;

import static com.google.common.collect.Sets.newHashSet;
import static com.opengamma.util.functional.Functional.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.time.calendar.Clock;
import javax.time.calendar.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.analytics.ircurve.ConfigDBInterpolatedYieldCurveSpecificationBuilder;
import com.opengamma.financial.analytics.ircurve.FixedIncomeStripWithIdentifier;
import com.opengamma.financial.analytics.ircurve.InterpolatedYieldCurveSpecification;
import com.opengamma.financial.analytics.ircurve.InterpolatedYieldCurveSpecificationBuilder;
import com.opengamma.financial.analytics.ircurve.StripInstrumentType;
import com.opengamma.financial.analytics.ircurve.YieldCurveDefinition;
import com.opengamma.financial.convention.ConventionBundle;
import com.opengamma.financial.convention.ConventionBundleMaster;
import com.opengamma.financial.convention.DefaultConventionBundleSource;
import com.opengamma.financial.convention.InMemoryConventionBundleMaster;
import com.opengamma.financial.tool.ToolContext;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.impl.ConfigSearchIterator;
import com.opengamma.util.functional.Function1;
import com.opengamma.util.money.Currency;

/**
 * Finds the historical time series that need to be loaded so that they can be used as instruments in the defined curves.
 */
public class CurveNodeHistoricalDataLoader {

  /**
   * Logger.
   */
  private static Logger s_logger = LoggerFactory.getLogger(CurveNodeHistoricalDataLoader.class);

  private Set<ExternalId> _curveNodesExternalIds;

  private Set<ExternalId> _initialRateExternalIds;

  private Set<ExternalIdBundle> _futuresExternalIds;

  public Set<ExternalId> getCurveNodesExternalIds() {
    return _curveNodesExternalIds;
  }

  public Set<ExternalId> getInitialRateExternalIds() {
    return _initialRateExternalIds;
  }

  public Set<ExternalIdBundle> getFuturesExternalIds() {
    return _futuresExternalIds;
  }

  public void run(final ToolContext tools) {
    final ConfigSource configSource = tools.getConfigSource();
    final ConfigMaster configMaster = tools.getConfigMaster();
    final List<YieldCurveDefinition> curves = getForwardAndFundingCurves(configMaster);

    final Set<Currency> currencies = newHashSet();

    for (final YieldCurveDefinition curve : curves) {
      currencies.add(curve.getCurrency());
    }

    _initialRateExternalIds = getInitialRateExternalIds(currencies);

    final List<LocalDate> dates = buildDates();

    final Set<String> curveNames = map(new HashSet<String>(), curves, new Function1<YieldCurveDefinition, String>() {
      @Override
      public String execute(final YieldCurveDefinition yieldCurveDefinition) {
        return yieldCurveDefinition.getName() + "_" + yieldCurveDefinition.getCurrency().getCode();
      }
    });
    _curveNodesExternalIds = getCurves(configSource, curveNames, dates);

    _futuresExternalIds = getFutures(configSource, curveNames, dates);

  }

  private Set<ExternalId> getInitialRateExternalIds(final Set<Currency> currencies) {
    final ConventionBundleMaster cbm = new InMemoryConventionBundleMaster();
    final DefaultConventionBundleSource cbs = new DefaultConventionBundleSource(cbm);
    final Set<ExternalId> externalInitialRateId = newHashSet();
    for (final Currency currency : currencies) {
      for (final String swapType : new String[]{"SWAP", "3M_SWAP", "6M_SWAP"}) {
        final String product = currency.getCode() + "_" + swapType;
        final ConventionBundle convention = cbs.getConventionBundle(ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, product));
        if (convention != null) {
          final ExternalId initialRate = convention.getSwapFloatingLegInitialRate();
          final ConventionBundle realIdConvention = cbs.getConventionBundle(initialRate);
          externalInitialRateId.add(realIdConvention.getIdentifiers().getExternalId(ExternalSchemes.BLOOMBERG_TICKER));
        } else {
          s_logger.warn("No convention for {} product", product);
        }
      }
    }
    return externalInitialRateId;
  }

  /**
   * Generate quarterly dates +/- 2 years around today to cover futures from past and near future
   * @return list of dates
   */
  private List<LocalDate> buildDates() {
    final Clock clock = Clock.systemDefaultZone();
    final List<LocalDate> dates = new ArrayList<LocalDate>();
    final LocalDate twoYearsAgo = clock.today().minusYears(2);
    final LocalDate twoYearsTime = clock.today().plusYears(2);
    for (LocalDate next = twoYearsAgo; next.isBefore(twoYearsTime); next = next.plusMonths(3)) {
      dates.add(next);
    }
    return dates;
  }

  /**
   * Get all the curves starting with FUNDING or FORWARD
   * @param configMaster
   * @return list of yield curve definition config object names
   */
  private List<YieldCurveDefinition> getForwardAndFundingCurves(final ConfigMaster configMaster) {
    final List<YieldCurveDefinition> forwardCurves = getCurveDefinitionNames(configMaster, "Forward*");
    final List<YieldCurveDefinition> oldForwardCurves = getCurveDefinitionNames(configMaster, "FORWARD*");
    final List<YieldCurveDefinition> fundingCurves = getCurveDefinitionNames(configMaster, "Funding*");
    final List<YieldCurveDefinition> oldFundingCurves = getCurveDefinitionNames(configMaster, "FUNDING*");
    final List<YieldCurveDefinition> allCurves = Lists.newArrayList();
    allCurves.addAll(forwardCurves);
    allCurves.addAll(fundingCurves);
    allCurves.addAll(oldForwardCurves);
    allCurves.addAll(oldFundingCurves);
    return allCurves;
  }

  /**
   * Get all the curve definition config object names specified by glob expression.
   * @param configMaster
   * @param nameExpr glob type expression - e.g. blah*
   * @return list of names of config objects matching glob expression
   */
  private List<YieldCurveDefinition> getCurveDefinitionNames(final ConfigMaster configMaster, final String nameExpr) {
    final List<YieldCurveDefinition> results = new ArrayList<YieldCurveDefinition>();
    final ConfigSearchRequest<YieldCurveDefinition> request = new ConfigSearchRequest<YieldCurveDefinition>(YieldCurveDefinition.class);
    request.setName(nameExpr);
    for (final ConfigDocument doc : ConfigSearchIterator.iterable(configMaster, request)) {
      results.add((YieldCurveDefinition) doc.getConfig().getValue());
    }
    return results;
  }

  /**
   * For a given list of curve names, on a given list of dates, get the superset of all ids required by those curves.
   * @param configSource
   * @param names
   * @param dates
   * @return list of all ids required by curves
   */
  private Set<ExternalId> getCurves(final ConfigSource configSource, final Collection<String> names, final List<LocalDate> dates) {
    final Set<ExternalId> externalIds = newHashSet();
    for (final String name : names) {
      s_logger.info("Processing curve " + name);
      final YieldCurveDefinition curveDefinition = configSource.getSingle(YieldCurveDefinition.class, name, VersionCorrection.LATEST);
      if (curveDefinition != null) {
        final InterpolatedYieldCurveSpecificationBuilder builder = new ConfigDBInterpolatedYieldCurveSpecificationBuilder(configSource);
        for (final LocalDate date : dates) {
          s_logger.info("Processing curve date " + date);
          final InterpolatedYieldCurveSpecification curveSpec = builder.buildCurve(date, curveDefinition);
          for (final FixedIncomeStripWithIdentifier strip : curveSpec.getStrips()) {
            s_logger.info("Processing strip " + strip.getSecurity());
            externalIds.add(strip.getSecurity());
          }
        }
      } else {
        s_logger.warn("No curve definition with '{}' name", name);
      }
    }
    return externalIds;
  }

  /**
   * For a given list of curve names, on a given list of dates, get the superset of all ids which are futures
   * @param configSource
   * @param names
   * @param dates
   * @return list of all ids required by curves
   */
  private Set<ExternalIdBundle> getFutures(final ConfigSource configSource, final Collection<String> names, final List<LocalDate> dates) {
    final Set<ExternalIdBundle> externalIds = newHashSet();
    for (final String name : names) {
      s_logger.info("Processing curve " + name);
      final YieldCurveDefinition curveDefinition = configSource.getSingle(YieldCurveDefinition.class, name, VersionCorrection.LATEST);
      if (curveDefinition != null) {
        final InterpolatedYieldCurveSpecificationBuilder builder = new ConfigDBInterpolatedYieldCurveSpecificationBuilder(configSource);
        for (final LocalDate date : dates) {
          s_logger.info("Processing curve date " + date);
          final InterpolatedYieldCurveSpecification curveSpec = builder.buildCurve(date, curveDefinition);
          for (final FixedIncomeStripWithIdentifier strip : curveSpec.getStrips()) {
            s_logger.info("Processing strip " + strip.getSecurity());
            if (strip.getStrip().getInstrumentType().equals(StripInstrumentType.FUTURE)) {
              externalIds.add(ExternalIdBundle.of(strip.getSecurity()));
            }
          }
        }
      } else {
        s_logger.warn("No curve definition with '{}' name", name);
      }
    }
    return externalIds;
  }

}
