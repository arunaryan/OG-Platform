/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.analytics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opengamma.engine.position.Position;
import com.opengamma.engine.security.Security;

/**
 * An in-memory implementation of {@link AnalyticFunctionRepository}.
 * This can either be used as-is through a factory which scans available functions,
 * or it can be used as a cache on top of a more costly function repository. 
 *
 * @author kirk
 */
public class InMemoryAnalyticFunctionRepository implements AnalyticFunctionRepository {
  private final Map<String, AnalyticFunctionInvoker> _invokersByUniqueIdentifier =
    new HashMap<String, AnalyticFunctionInvoker>();
  private final Set<AnalyticFunctionDefinition> _functions = new HashSet<AnalyticFunctionDefinition>();
  
  public InMemoryAnalyticFunctionRepository() {
  }
  
  public synchronized void addFunction(AbstractAnalyticFunction function, AnalyticFunctionInvoker invoker) {
    validateFunction(function, invoker);
    _functions.add(function);
    function.setUniqueIdentifier(Integer.toString(_functions.size()));
    _invokersByUniqueIdentifier.put(function.getUniqueIdentifier(), invoker);
  }

  /**
   * @param function
   * @param invoker
   */
  private void validateFunction(AbstractAnalyticFunction function,
      AnalyticFunctionInvoker invoker) {
    if(function == null) {
      throw new NullPointerException("Must provide a function.");
    }
    if(invoker == null) {
      throw new NullPointerException("Must provide an invoker.");
    }
    if(function instanceof PrimitiveAnalyticFunctionDefinition) {
      if(!(invoker instanceof PrimitiveAnalyticFunctionInvoker)) {
        throw new IllegalArgumentException("Must provide primitive invoker for primitive definition.");
      }
    } else if(function instanceof SecurityAnalyticFunctionDefinition) {
      if(!(invoker instanceof SecurityAnalyticFunctionInvoker)) {
        throw new IllegalArgumentException("Must provide security invoker for security definition.");
      }
    } else if(function instanceof PositionAnalyticFunctionDefinition) {
      if(!(invoker instanceof PositionAnalyticFunctionInvoker)) {
        throw new IllegalArgumentException("Must provide position invoker for position definition.");
      }
    } else if(function instanceof AggregatePositionAnalyticFunctionDefinition) {
      if(!(invoker instanceof AggregatePositionAnalyticFunctionInvoker)) {
        throw new IllegalArgumentException("Must provide aggregate position invoker for aggregate position definition.");
      }
    } else {
      throw new IllegalArgumentException("Unexpected analytic function definition " + function.getClass());
    }
  }

  @Override
  public Collection<AnalyticFunctionDefinition> getAllFunctions() {
    return Collections.unmodifiableCollection(_functions);
  }

  @Override
  public Collection<AnalyticFunctionDefinition> getFunctionsProducing(
      Collection<AnalyticValueDefinition<?>> outputs) {
    Set<AnalyticFunctionDefinition> result = new HashSet<AnalyticFunctionDefinition>();
    for(AnalyticFunctionDefinition function : _functions) {
      if(!(function instanceof PrimitiveAnalyticFunctionDefinition)) {
        // Can't work with it because it requires execution inside of a
        // security or position context.
        continue;
      }
      if(functionProducesAllValuesInternal(function, null, outputs)) {
        result.add(function);
      }
    }
    return result;
  }
  
  @Override
  public Collection<AnalyticFunctionDefinition> getFunctionsProducing(
      Collection<AnalyticValueDefinition<?>> outputs, Security security) {
    Set<AnalyticFunctionDefinition> result = new HashSet<AnalyticFunctionDefinition>();
    for(AnalyticFunctionDefinition function : _functions) {
      boolean applicable = false;
      if(function instanceof PrimitiveAnalyticFunctionDefinition) {
        // Might be applicable even if we're in a security-specific context.
        applicable = true;
      } else if(function instanceof SecurityAnalyticFunctionDefinition) {
        SecurityAnalyticFunctionDefinition secDefinition = (SecurityAnalyticFunctionDefinition) function;
        if(security != null) {
          applicable = secDefinition.isApplicableTo(security.getSecurityType());
        } else {
          applicable = secDefinition.isApplicableTo((String)null);
        }
      }
      // Neither Position or Aggregate are ever applicable to just a Security.
      if(applicable
          && functionProducesAllValuesInternal(function, security, outputs)) {
        result.add(function);
      }
    }
    return result;
  }
  
  @Override
  public Collection<AnalyticFunctionDefinition> getFunctionsProducing(
      Collection<AnalyticValueDefinition<?>> outputs, Position position) {
    Set<AnalyticFunctionDefinition> result = new HashSet<AnalyticFunctionDefinition>();
    for(AnalyticFunctionDefinition function : _functions) {
      boolean applicable = true;
      if(function instanceof PrimitiveAnalyticFunctionDefinition) {
        // Might be applicable even if we're in a position-specific context.
        applicable = true;
      } else if(function instanceof SecurityAnalyticFunctionDefinition) {
        // Might be applicable as we've got a security resolved.
        SecurityAnalyticFunctionDefinition secDefinition = (SecurityAnalyticFunctionDefinition) function;
        Security security = position.getSecurity();
        if(security != null) {
          applicable = secDefinition.isApplicableTo(security.getSecurityType());
        } else {
          applicable = secDefinition.isApplicableTo((String)null);
        }
      } else if(function instanceof PositionAnalyticFunctionDefinition) {
        PositionAnalyticFunctionDefinition posDefinition = (PositionAnalyticFunctionDefinition) function;
        applicable = posDefinition.isApplicableTo(position);
      }
      // Aggregates are never applicable.
      if(applicable
          && functionProducesAllValuesInternal(function, position, outputs)) {
        result.add(function);
      }
    }
    return result;
  }
  
  @Override
  public Collection<AnalyticFunctionDefinition> getFunctionsProducing(
      Collection<AnalyticValueDefinition<?>> outputs, Collection<Position> positions) {
    Set<AnalyticFunctionDefinition> result = new HashSet<AnalyticFunctionDefinition>();
    for(AnalyticFunctionDefinition function : _functions) {
      boolean applicable = true;
      if(function instanceof PrimitiveAnalyticFunctionDefinition) {
        // Might be applicable even if we're in an aggregate context.
        applicable = true;
      } else if(function instanceof AggregatePositionAnalyticFunctionDefinition) {
        AggregatePositionAnalyticFunctionDefinition aggDefinition = (AggregatePositionAnalyticFunctionDefinition) function;
        applicable = aggDefinition.isApplicableTo(positions);
      }
      // Security and position functions are never applicable to an aggregate
      if(applicable
          && functionProducesAllValuesInternal(function, positions, outputs)) {
        result.add(function);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  protected boolean functionProducesAllValuesInternal(
      AnalyticFunctionDefinition function,
      Object securityOrPositionOrCollection,
      Collection<AnalyticValueDefinition<?>> outputs) {
    Collection<AnalyticValueDefinition<?>> possibleResults = null;
    if(function instanceof PrimitiveAnalyticFunctionDefinition) {
      possibleResults = ((PrimitiveAnalyticFunctionDefinition) function).getPossibleResults();
    } else if(function instanceof SecurityAnalyticFunctionDefinition) {
      assert securityOrPositionOrCollection instanceof Security;
      Security security = (Security)securityOrPositionOrCollection;
      possibleResults = ((SecurityAnalyticFunctionDefinition) function).getPossibleResults(security);
    } else if(function instanceof PositionAnalyticFunctionDefinition) {
      assert securityOrPositionOrCollection instanceof Position;
      Position position = (Position)securityOrPositionOrCollection;
      possibleResults = ((PositionAnalyticFunctionDefinition) function).getPossibleResults(position);
    } else if(function instanceof AggregatePositionAnalyticFunctionDefinition) {
      assert securityOrPositionOrCollection instanceof Collection;
      Collection<Position> positions = (Collection<Position>)securityOrPositionOrCollection;
      possibleResults = ((AggregatePositionAnalyticFunctionDefinition) function).getPossibleResults(positions);
    } else {
      throw new UnsupportedOperationException("Can only handle primitive/security functions now.");
    }
    assert possibleResults != null;
    for(AnalyticValueDefinition<?> output : outputs) {
      boolean foundForOutput = false;
      for(AnalyticValueDefinition<?> possibleResult : possibleResults) {
        if(AnalyticValueDefinitionComparator.matches(output, possibleResult)) {
          foundForOutput = true;
          break;
        }
      }
      if(!foundForOutput) {
        return false;
      }
    }
    return true;
  }

  @Override
  public AnalyticFunctionInvoker getInvoker(String uniqueIdentifier) {
    return _invokersByUniqueIdentifier.get(uniqueIdentifier);
  }

}
