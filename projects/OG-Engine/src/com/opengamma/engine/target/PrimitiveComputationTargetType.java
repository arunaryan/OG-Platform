/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.target;

import com.opengamma.engine.ComputationTargetResolver;
import com.opengamma.engine.target.resolver.Resolver;
import com.opengamma.id.UniqueId;
import com.opengamma.id.UniqueIdentifiable;

/**
 * Specialized form of {@link ObjectComputationTargetType} for primitive objects that can be converted directly to/from unique identifiers without an external resolver service. Instances also serve as
 * a {@link Resolver} so that they can be added to a {@link ComputationTargetResolver} to handle the type.
 * 
 * @param <T> the target object type
 */
public final class PrimitiveComputationTargetType<T extends UniqueIdentifiable> extends ObjectComputationTargetType<T> implements Resolver<T> {

  private static final long serialVersionUID = 1L;

  private final Resolver<T> _resolver;

  private PrimitiveComputationTargetType(final ComputationTargetType type, final Class<T> clazz, final Resolver<T> resolver) {
    super(type, clazz);
    _resolver = resolver;
  }

  public static <T extends UniqueIdentifiable> PrimitiveComputationTargetType<T> of(final ComputationTargetType type, final Class<T> clazz, final Resolver<T> resolver) {
    assert type.isTargetType(clazz);
    assert resolver != null;
    return new PrimitiveComputationTargetType<T>(type, clazz, resolver);
  }

  public T resolve(final UniqueId identifier) {
    return _resolver.resolve(identifier);
  }

}
