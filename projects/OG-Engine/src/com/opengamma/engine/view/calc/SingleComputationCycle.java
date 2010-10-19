/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.view.calc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.depgraph.DependencyGraph;
import com.opengamma.engine.depgraph.DependencyNode;
import com.opengamma.engine.depgraph.DependencyNodeFilter;
import com.opengamma.engine.function.LiveDataSourcingFunction;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.ViewComputationResultModelImpl;
import com.opengamma.engine.view.ViewDefinition;
import com.opengamma.engine.view.ViewInternal;
import com.opengamma.engine.view.ViewProcessingContext;
import com.opengamma.engine.view.cache.CacheSelectHint;
import com.opengamma.engine.view.cache.ViewComputationCache;
import com.opengamma.engine.view.calc.stats.GraphExecutorStatisticsGatherer;
import com.opengamma.engine.view.compilation.ViewEvaluationModel;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 * Holds all data and actions for a single pass through a computation cycle.
 * In general, each invocation of {@link ViewRecalculationJob#runOneCycle()}
 * will create an instance of this class.
 * <p/>
 * At the moment, the concurrency metaphor is:
 * <ul>
 *   <li>Each distinct security has its own execution plan</li>
 *   <li>The cycle will schedule each node in the execution plan sequentially</li>
 *   <li>If there are shared sub-graphs that aren't security specific, they'll be captured at execution time.</li>
 * </ul>
 * This is, of course, not optimal, and later on we can fix that.
 *
 * @author kirk
 */
public class SingleComputationCycle {
  private static final Logger s_logger = LoggerFactory.getLogger(SingleComputationCycle.class);
  // Injected Inputs:
  private final ViewInternal _view;
  private final Instant _valuationTime;

  private final DependencyGraphExecutor<?> _dependencyGraphExecutor;
  private final GraphExecutorStatisticsGatherer _statisticsGatherer;

  // State:

  /** Current state of the cycle */
  private enum State {
    CREATED, INPUTS_PREPARED, EXECUTING, EXECUTION_INTERRUPTED, FINISHED, CLEANED
  }

  private State _state;

  /**
   * Nanoseconds, see System.nanoTime()
   */
  private long _startTime;

  /**
   * Nanoseconds, see System.nanoTime()
   */
  private long _endTime;

  private final ReentrantReadWriteLock _nodeExecutionLock = new ReentrantReadWriteLock();
  private final Set<DependencyNode> _executedNodes = new HashSet<DependencyNode>();
  private final Set<DependencyNode> _failedNodes = new HashSet<DependencyNode>();
  private final Map<String, ViewComputationCache> _cachesByCalculationConfiguration = new HashMap<String, ViewComputationCache>();

  // Outputs:
  private final ViewComputationResultModelImpl _resultModel;

  public SingleComputationCycle(ViewInternal view, long valuationTime) {
    ArgumentChecker.notNull(view, "view");

    _view = view;
    _valuationTime = Instant.ofEpochMillis(valuationTime);

    _resultModel = new ViewComputationResultModelImpl();
    _resultModel.setCalculationConfigurationNames(getViewEvaluationModel().getDependencyGraphsByConfiguration().keySet());

    if (getViewEvaluationModel().getPortfolio() != null) {
      _resultModel.setPortfolio(getViewEvaluationModel().getPortfolio());
    }

    _dependencyGraphExecutor = getProcessingContext().getDependencyGraphExecutorFactory().createExecutor(this);
    _statisticsGatherer = getProcessingContext().getGraphExecutorStatisticsGathererProvider().getStatisticsGatherer(view);

    _state = State.CREATED;
  }

  public ViewInternal getView() {
    return _view;
  }

  public Instant getValuationTime() {
    return _valuationTime;
  }

  /**
   * @return the viewName
   */
  public String getViewName() {
    return getView().getName();
  }

  /**
   * @return the processingContext
   */
  public ViewProcessingContext getProcessingContext() {
    return getView().getProcessingContext();
  }

  /**
   * @return the start time. Nanoseconds, see {@link System#nanoTime()}. 
   */
  public long getStartTime() {
    return _startTime;
  }

  /**
   * @return the end time. Nanoseconds, see {@link System#nanoTime()}. 
   */
  public long getEndTime() {
    return _endTime;
  }

  /**
   * @return How many nanoseconds the cycle took
   */
  public long getDurationNanos() {
    return getEndTime() - getStartTime();
  }

  /**
   * @return the resultModel
   */
  public ViewComputationResultModelImpl getResultModel() {
    return _resultModel;
  }

  public ViewComputationCache getComputationCache(String calcConfigName) {
    return _cachesByCalculationConfiguration.get(calcConfigName);
  }

  /**
   * @return the viewDefinition
   */
  public ViewDefinition getViewDefinition() {
    return getView().getDefinition();
  }

  public DependencyGraphExecutor<?> getDependencyGraphExecutor() {
    return _dependencyGraphExecutor;
  }

  public GraphExecutorStatisticsGatherer getStatisticsGatherer() {
    return _statisticsGatherer;
  }

  public Map<String, ViewComputationCache> getCachesByCalculationConfiguration() {
    return Collections.unmodifiableMap(_cachesByCalculationConfiguration);
  }

  public ViewEvaluationModel getViewEvaluationModel() {
    // REVIEW jonathan 2010-08-17 -- when we support re-compilation of views, we need to be more careful about how we
    // handle the view evaluation model to ensure that a computation cycle works entirely with the output from a single
    // compilation.
    return getView().getViewEvaluationModel();
  }

  public Set<String> getAllCalculationConfigurationNames() {
    return getViewEvaluationModel().getDependencyGraphsByConfiguration().keySet();
  }

  // --------------------------------------------------------------------------

  public void prepareInputs() {
    if (_state != State.CREATED) {
      throw new IllegalStateException("State must be " + State.CREATED);
    }

    _startTime = System.nanoTime();

    getResultModel().setViewName(getViewName());
    getResultModel().setValuationTime(getValuationTime());

    createAllCaches();

    Set<ValueSpecification> allLiveDataRequirements = getViewEvaluationModel().getAllLiveDataRequirements();
    s_logger.debug("Populating {} market data items for snapshot {}", allLiveDataRequirements.size(), getValuationTime());

    Set<ValueSpecification> missingLiveData = new HashSet<ValueSpecification>();
    for (ValueSpecification liveDataRequirement : allLiveDataRequirements) {
      Object data = getProcessingContext().getLiveDataSnapshotProvider().querySnapshot(getValuationTime().toEpochMillisLong(), liveDataRequirement.getRequirementSpecification());
      if (data == null) {
        s_logger.debug("Unable to load live data value for {} at snapshot {}.", liveDataRequirement, getValuationTime());
        missingLiveData.add(liveDataRequirement);
      } else {
        ComputedValue dataAsValue = new ComputedValue(liveDataRequirement, data);
        // s_logger.warn("Live Data Requirement: {}", dataAsValue);
        addToAllCaches(dataAsValue);
      }
    }
    if (!missingLiveData.isEmpty()) {
      s_logger.warn("Missing {} live data elements: {}", missingLiveData.size(), formatMissingLiveData(missingLiveData));
    }

    _state = State.INPUTS_PREPARED;
  }

  protected static String formatMissingLiveData(Set<ValueSpecification> missingLiveData) {
    StringBuilder sb = new StringBuilder();
    for (ValueSpecification spec : missingLiveData) {
      ValueRequirement req = spec.getRequirementSpecification();
      sb.append("[").append(req.getValueName()).append(" on ");
      sb.append(req.getTargetSpecification().getType());
      if (req.getTargetSpecification().getType() == ComputationTargetType.PRIMITIVE) {
        sb.append("-").append(req.getTargetSpecification().getIdentifier().getScheme().getName());
      }
      sb.append(":").append(req.getTargetSpecification().getIdentifier().getValue()).append("] ");
    }
    return sb.toString();
  }

  /**
   * 
   */
  private void createAllCaches() {
    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      ViewComputationCache cache = getProcessingContext().getComputationCacheSource().getCache(getViewName(), calcConfigurationName, getValuationTime().toEpochMillisLong());
      _cachesByCalculationConfiguration.put(calcConfigurationName, cache);
    }
  }

  /**
   * @param dataAsValue
   */
  private void addToAllCaches(ComputedValue dataAsValue) {
    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      getComputationCache(calcConfigurationName).putSharedValue(dataAsValue);
    }
  }

  // --------------------------------------------------------------------------

  /**
   * Determine which live data inputs have changed between iterations, and:
   * <ul>
   * <li>Copy over all values that can be demonstrated to be the same from the previous iteration (because no input has changed)
   * <li>Only recompute the values that could have changed based on live data inputs
   * </ul> 
   * 
   * @param previousCycle Previous iteration. It must not have been cleaned yet ({@link #releaseResources()}).
   */
  public void computeDelta(SingleComputationCycle previousCycle) {
    if (_state != State.INPUTS_PREPARED) {
      throw new IllegalStateException("State must be " + State.INPUTS_PREPARED);
    }
    if (previousCycle._state != State.FINISHED) {
      throw new IllegalArgumentException("State of previous cycle must be " + State.FINISHED);
    }

    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      DependencyGraph depGraph = getViewEvaluationModel().getDependencyGraph(calcConfigurationName);

      ViewComputationCache cache = getComputationCache(calcConfigurationName);
      ViewComputationCache previousCache = previousCycle.getComputationCache(calcConfigurationName);

      LiveDataDeltaCalculator deltaCalculator = new LiveDataDeltaCalculator(depGraph, cache, previousCache);
      deltaCalculator.computeDelta();

      s_logger.info("Computed delta for calc conf {}. Of {} nodes, {} require recomputation.", new Object[] {calcConfigurationName, depGraph.getSize(), deltaCalculator.getChangedNodes().size()});

      for (DependencyNode unchangedNode : deltaCalculator.getUnchangedNodes()) {
        markExecuted(unchangedNode);

        for (ValueSpecification spec : unchangedNode.getOutputValues()) {
          Object previousValue = previousCache.getValue(spec);
          if (previousValue != null) {
            cache.putSharedValue(new ComputedValue(spec, previousValue));
          }
        }
      }
    }
  }

  // REVIEW kirk 2009-11-03 -- This is a database kernel. Act accordingly.
  /**
   * Synchronously runs the computation cycle.
   * 
   * @throws InterruptedException  if the thread is interrupted while waiting for the computation cycle to complete.
   *                               Execution of any outstanding jobs will be cancelled, but {@link #releaseResources()}
   *                               still must be called. 
   */
  public void executePlans() throws InterruptedException {
    if (_state != State.INPUTS_PREPARED) {
      throw new IllegalStateException("State must be " + State.INPUTS_PREPARED);
    }
    _state = State.EXECUTING;

    LinkedList<Future<?>> futures = new LinkedList<Future<?>>();

    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      s_logger.info("Executing plans for calculation configuration {}", calcConfigurationName);
      DependencyGraph depGraph = getExecutableDependencyGraph(calcConfigurationName);

      s_logger.info("Submitting {} for execution by {}", depGraph, getDependencyGraphExecutor());

      Future<?> future = getDependencyGraphExecutor().execute(depGraph, _statisticsGatherer);
      futures.add(future);
    }

    while (!futures.isEmpty()) {
      Future<?> future = futures.poll();
      try {
        future.get(5, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        s_logger.info("Waiting for " + future);
        futures.add(future);
      } catch (InterruptedException e) {
        Thread.interrupted();
        // Cancel all outstanding jobs to free up resources
        future.cancel(true);
        for (Future<?> incompleteFuture : futures) {
          incompleteFuture.cancel(true);
        }
        _state = State.EXECUTION_INTERRUPTED;
        s_logger.info("Execution interrupted before completion.");
        throw e;
      } catch (ExecutionException e) {
        s_logger.error("Unable to execute dependency graph", e);
        // Should we be swallowing this or not?
        throw new OpenGammaRuntimeException("Unable to execute dependency graph", e);
      }
    }

    _state = State.FINISHED;
  }

  private DependencyGraph getDependencyGraph(String calcConfName) {
    DependencyGraph depGraph = getViewEvaluationModel().getDependencyGraph(calcConfName);
    return depGraph;
  }

  /**
   * @param calcConfName configuration name
   * @return A dependency graph with nodes already executed stripped out.
   * See {@link #computeDelta} and how it calls {@link #markExecuted}.
   */
  protected DependencyGraph getExecutableDependencyGraph(String calcConfName) {
    DependencyGraph originalDepGraph = getDependencyGraph(calcConfName);

    DependencyGraph dependencyGraph = originalDepGraph.subGraph(new DependencyNodeFilter() {
      public boolean accept(DependencyNode node) {
        // LiveData functions do not need to be computed.
        if (node.getFunction().getFunction() instanceof LiveDataSourcingFunction) {
          markExecuted(node);
        }

        return !isExecuted(node);
      }
    });
    return dependencyGraph;
  }

  // --------------------------------------------------------------------------

  public void populateResultModel() {
    Instant resultTimestamp = Instant.nowSystemClock();
    getResultModel().setResultTimestamp(resultTimestamp);

    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      DependencyGraph depGraph = getViewEvaluationModel().getDependencyGraph(calcConfigurationName);
      populateResultModel(calcConfigurationName, depGraph);
    }

    _endTime = System.nanoTime();
  }

  protected void populateResultModel(String calcConfigurationName, DependencyGraph depGraph) {
    ViewComputationCache computationCache = getComputationCache(calcConfigurationName);
    for (Pair<ValueSpecification, Object> value : computationCache.getValues(depGraph.getOutputValues(), CacheSelectHint.allShared())) {
      if (value.getValue() == null) {
        continue;
      }
      if (!getViewDefinition().getResultModelDefinition().shouldOutputResult(value.getFirst(), depGraph)) {
        continue;
      }
      getResultModel().addValue(calcConfigurationName, new ComputedValue(value.getFirst(), value.getSecond()));
    }
  }

  public void releaseResources() {
    if (_state != State.FINISHED && _state != State.EXECUTION_INTERRUPTED) {
      throw new IllegalStateException("State must be " + State.FINISHED + " or " + State.EXECUTION_INTERRUPTED);
    }

    if (getViewDefinition().isDumpComputationCacheToDisk()) {
      dumpComputationCachesToDisk();
    }

    getProcessingContext().getLiveDataSnapshotProvider().releaseSnapshot(getValuationTime().toEpochMillisLong());
    getProcessingContext().getComputationCacheSource().releaseCaches(getViewName(), getValuationTime().toEpochMillisLong());

    _state = State.CLEANED;
  }

  public void dumpComputationCachesToDisk() {
    for (String calcConfigurationName : getAllCalculationConfigurationNames()) {
      DependencyGraph depGraph = getDependencyGraph(calcConfigurationName);
      ViewComputationCache computationCache = getComputationCache(calcConfigurationName);

      TreeMap<String, Object> key2Value = new TreeMap<String, Object>();
      for (ValueSpecification outputSpec : depGraph.getOutputValues()) {
        Object value = computationCache.getValue(outputSpec);
        key2Value.put(outputSpec.toString(), value);
      }

      try {
        File file = File.createTempFile("computation-cache-" + calcConfigurationName + "-", ".txt");
        s_logger.info("Dumping cache for calc conf " + calcConfigurationName + " to " + file.getAbsolutePath());
        FileWriter writer = new FileWriter(file);
        writer.write(key2Value.toString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException("Writing cache to file failed", e);
      }
    }
  }

  // --------------------------------------------------------------------------

  public boolean isExecuted(DependencyNode node) {
    if (node == null) {
      return true;
    }
    _nodeExecutionLock.readLock().lock();
    try {
      return _executedNodes.contains(node);
    } finally {
      _nodeExecutionLock.readLock().unlock();
    }
  }

  public void markExecuted(DependencyNode node) {
    if (node == null) {
      return;
    }
    _nodeExecutionLock.writeLock().lock();
    try {
      _executedNodes.add(node);
    } finally {
      _nodeExecutionLock.writeLock().unlock();
    }
  }

  public boolean isFailed(DependencyNode node) {
    if (node == null) {
      return true;
    }
    _nodeExecutionLock.readLock().lock();
    try {
      return _failedNodes.contains(node);
    } finally {
      _nodeExecutionLock.readLock().unlock();
    }
  }

  public void markFailed(DependencyNode node) {
    if (node == null) {
      return;
    }
    _nodeExecutionLock.writeLock().lock();
    try {
      _failedNodes.add(node);
    } finally {
      _nodeExecutionLock.writeLock().unlock();
    }
  }
}
