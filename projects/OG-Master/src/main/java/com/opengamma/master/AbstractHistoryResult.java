/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master;

import java.util.Collection;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.util.PublicSPI;

/**
 * Result providing the history of a document.
 * <p>
 * The returned documents may be a mixture of versions and corrections.
 * The document instant fields are used to identify which are which.
 * See {@link AbstractHistoryRequest} for more details.
 * 
 * @param <D>  the type of the document
 */
@PublicSPI
@BeanDefinition
public abstract class AbstractHistoryResult<D extends AbstractDocument> extends AbstractDocumentsResult<D> {

  /**
   * Creates an instance.
   */
  public AbstractHistoryResult() {
  }

  /**
   * Creates an instance.
   * @param coll  the collection of documents to add, not null
   */
  public AbstractHistoryResult(Collection<D> coll) {
    super(coll);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the first document, or null if no documents.
   * @return the first document, null if none
   */
  public D getFirstDocument() {
    return getDocuments().size() > 0 ? getDocuments().get(0) : null;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code AbstractHistoryResult}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static AbstractHistoryResult.Meta meta() {
    return AbstractHistoryResult.Meta.INSTANCE;
  }
  /**
   * The meta-bean for {@code AbstractHistoryResult}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends AbstractDocument> AbstractHistoryResult.Meta<R> metaAbstractHistoryResult(Class<R> cls) {
    return AbstractHistoryResult.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(AbstractHistoryResult.Meta.INSTANCE);
  }

  @SuppressWarnings("unchecked")
  @Override
  public AbstractHistoryResult.Meta<D> metaBean() {
    return AbstractHistoryResult.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code AbstractHistoryResult}.
   */
  public static class Meta<D extends AbstractDocument> extends AbstractDocumentsResult.Meta<D> {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap());

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    public BeanBuilder<? extends AbstractHistoryResult<D>> builder() {
      throw new UnsupportedOperationException("AbstractHistoryResult is an abstract class");
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends AbstractHistoryResult<D>> beanType() {
      return (Class) AbstractHistoryResult.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
