/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.examples.livedata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeField;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.id.ExternalScheme;
import com.opengamma.livedata.server.StandardLiveDataServer;
import com.opengamma.livedata.server.Subscription;
import com.opengamma.livedata.server.SubscriptionListener;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.NamedThreadPoolFactory;
import com.opengamma.util.TerminatableJob;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;

/**
 * An ultra-simple market data simulator, we load the initial values from a CSV file (with a header row)
 * and the format
 * <identification-scheme>, <identifier-value>, <requirement-name>, <value>
 * typically, for last price, you'd use "Market_Value" @see MarketDataRequirementNames
 */
public class ExampleLiveDataServer extends StandardLiveDataServer {

  private static final Logger s_logger = LoggerFactory.getLogger(ExampleLiveDataServer.class);

  private static final FudgeContext s_fudgeConext = OpenGammaFudgeContext.getInstance();
  private static final int NUM_FIELDS = 3;
  private static final double SCALING_FACTOR = 0.005; // i.e. 0.5% * 1SD
  private static final int MAX_MILLIS_BETWEEN_TICKS = 50;

  private final Map<String, FudgeMsg> _marketValues = Maps.newConcurrentMap();
  private final Map<String, FudgeMsg> _baseValues = Maps.newConcurrentMap();
  private volatile double _scalingFactor;
  private volatile int _maxMillisBetweenTicks;
  private final TerminatableJob _marketDataSimulatorJob = new SimulatedMarketDataJob();
  private ExecutorService _executorService;

  public ExampleLiveDataServer(final Resource initialValuesFile) {
    this(initialValuesFile, SCALING_FACTOR, MAX_MILLIS_BETWEEN_TICKS);
  }

  public ExampleLiveDataServer(final Resource initialValuesFile, final double scalingFactor, final int maxMillisBetweenTicks) {
    super(EHCacheUtils.createCacheManager());
    readInitialValues(initialValuesFile);
    _scalingFactor = scalingFactor;
    _maxMillisBetweenTicks = maxMillisBetweenTicks;
  }

  //-------------------------------------------------------------------------
  private void readInitialValues(final Resource initialValuesFile) {
    CSVReader reader = null;
    try {
      reader = new CSVReader(new BufferedReader(new InputStreamReader(initialValuesFile.getInputStream())));
      // Read header row
      @SuppressWarnings("unused")
      final
      String[] headers = reader.readNext();
      String[] line;
      int lineNum = 1;
      while ((line = reader.readNext()) != null) {
        lineNum++;
        if (line.length > 0 && line[0].startsWith("#")) {
          continue;
        }
        if (line.length != NUM_FIELDS) {
          s_logger.error("Not enough fields in CSV on line " + lineNum);
        } else {
          final String identifier = line[0];
          final String fieldName = line[1];
          final String valueStr = line[2];
          final Double value = Double.parseDouble(valueStr);
          addTicks(identifier, fieldName, value);
        }
      }
    } catch (final FileNotFoundException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  /**
   * Gets the marketValues.
   * @return the marketValues
   */
  public Map<String, FudgeMsg> getMarketValues() {
    return Collections.unmodifiableMap(_marketValues);
  }

  /**
   * Gets the scalingFactor.
   * @return the scalingFactor
   */
  public double getScalingFactor() {
    return _scalingFactor;
  }

  /**
   * Sets the scalingFactor.
   * @param scalingFactor  the scalingFactor
   */
  public void setScalingFactor(final double scalingFactor) {
    _scalingFactor = scalingFactor;
  }

  /**
   * Gets the maxMillisBetweenTicks.
   * @return the maxMillisBetweenTicks
   */
  public int getMaxMillisBetweenTicks() {
    return _maxMillisBetweenTicks;
  }

  /**
   * Sets the maxMillisBetweenTicks.
   * @param maxMillisBetweenTicks  the maxMillisBetweenTicks
   */
  public void setMaxMillisBetweenTicks(final int maxMillisBetweenTicks) {
    _maxMillisBetweenTicks = maxMillisBetweenTicks;
  }

  /**
   * @param uniqueId the uniqueId, not null
   * @param fieldName the field name, not null
   * @param value the market value, not null
   */
  public void addTicks(final String uniqueId, final String fieldName, final Double value) {
    ArgumentChecker.notNull(uniqueId, "unique identifier");
    ArgumentChecker.notNull(fieldName, "field name");
    ArgumentChecker.notNull(value, "market value");

    final FudgeMsg previousTicks = _marketValues.get(uniqueId);
    MutableFudgeMsg ticks = null;
    if (previousTicks == null) {
      ticks = s_fudgeConext.newMessage();
    } else {
      ticks = s_fudgeConext.newMessage(previousTicks);
      if (ticks.hasField(fieldName)) {
        ticks.remove(fieldName);
      }
    }
    ticks.add(fieldName, value);
    _marketValues.put(uniqueId, ticks);
    _baseValues.put(uniqueId, ticks);
  }

  @Override
  protected Map<String, Object> doSubscribe(final Collection<String> uniqueIds) {
    ArgumentChecker.notNull(uniqueIds, "Subscriptions");
    final Map<String, Object> result = Maps.newHashMap();
    final List<String> missingSubscriptions = Lists.newArrayList();
    for (final String uniqueId : uniqueIds) {
      if (!_marketValues.containsKey(uniqueId)) {
        missingSubscriptions.add(uniqueId);
      }
      result.put(uniqueId, new AtomicReference<String>(uniqueId));
    }
    if (!missingSubscriptions.isEmpty()) {
      s_logger.error("Could not subscribe to {}", missingSubscriptions);
    }
    return result;
  }

  @Override
  protected void doUnsubscribe(final Collection<Object> subscriptionHandles) {
    // No-op; don't maintain or forward any subscription state
  }

  @Override
  protected Map<String, FudgeMsg> doSnapshot(final Collection<String> uniqueIds) {
    ArgumentChecker.notNull(uniqueIds, "Unique IDs");
    if (uniqueIds.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<String, FudgeMsg> returnValue = new HashMap<String, FudgeMsg>();
    for (final String securityUniqueId : uniqueIds) {
      final FudgeMsg fieldData = _marketValues.get(securityUniqueId);
      returnValue.put(securityUniqueId, fieldData);
    }
    return returnValue;
  }

  @Override
  protected ExternalScheme getUniqueIdDomain() {
    return ExternalSchemes.OG_SYNTHETIC_TICKER;
  }

  @Override
  protected void doConnect() {
    addSubscriptionListener(new SubscriptionListener() {

      @Override
      public void unsubscribed(final Subscription subscription) {
        final Set<String> activeSubscriptionIds = getActiveSubscriptionIds();
        if (activeSubscriptionIds.isEmpty()) {
          _marketDataSimulatorJob.terminate();
          if (_executorService != null) {
            _executorService.shutdown();
          }
        }
      }

      @Override
      public void subscribed(final Subscription subscription) {
        if (!_marketDataSimulatorJob.isStarted()) {
          _executorService = Executors.newCachedThreadPool(new NamedThreadPoolFactory("ExampleLiveDataServer"));
          _executorService.submit(_marketDataSimulatorJob);
        }
      }
    });

  }

  @Override
  protected void doDisconnect() {
    if (_executorService != null) {
      _executorService.shutdown();
    }
    _marketValues.clear();
  }

  @Override
  protected boolean snapshotOnSubscriptionStartRequired(final Subscription subscription) {
    return false;
  }

  private class SimulatedMarketDataJob extends TerminatableJob {

    private final Random _random = new Random();
    private final List<String> _doneSnapShots = Lists.newArrayList();

    private void doSnapShotOnStart(final String[] subscriptions) {
      for (final String identifier : subscriptions) {
        if (!_doneSnapShots.contains(identifier)) {
          final FudgeMsg lastestValue = _marketValues.get(identifier);
          liveDataReceived(identifier, lastestValue);
          _doneSnapShots.add(identifier);
        }
      }
    }

    private String[] getAvailableMarketDataSubscriptions() {
      final List<String> result = Lists.newArrayList();
      final Set<Subscription> subscriptions = getSubscriptions();
      for (final Subscription subscription : subscriptions) {
        final String securityUniqueId = subscription.getSecurityUniqueId();
        if (_marketValues.containsKey(securityUniqueId)) {
          result.add(securityUniqueId);
        }
      }
      return result.toArray(new String[0]);
    }

    private double wiggleValue(final double value, final double centre) {
      return (9 * value + centre) / 10 + (_random.nextGaussian() * (value * _scalingFactor));
    }

    @Override
    protected void runOneCycle() {
      final String[] subscriptions = getAvailableMarketDataSubscriptions();
      if (subscriptions.length > 0) {
        doSnapShotOnStart(subscriptions);
        final String identifier = subscriptions[_random.nextInt(subscriptions.length)];
        final FudgeMsg lastValues = _marketValues.get(identifier);
        final FudgeMsg baseValues = _baseValues.get(identifier);
        final MutableFudgeMsg nextValues = s_fudgeConext.newMessage();
        for (final FudgeField field : lastValues) {
          final double lastValue = (Double) field.getValue();
          final double baseValue = baseValues.getDouble(field.getName());
          final double value = wiggleValue(lastValue, baseValue);
          nextValues.add(field.getName(), value);
        }
        _marketValues.put(identifier, nextValues);
        liveDataReceived(identifier, nextValues);
        try {
          Thread.sleep(_random.nextInt(_maxMillisBetweenTicks));
        } catch (final InterruptedException e) {
          s_logger.error("Sleep interrupted, finishing");
          Thread.interrupted();
        }
      }
    }
  }

}
