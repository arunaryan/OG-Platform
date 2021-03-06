/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.bbg;

import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.bloomberglp.blpapi.SessionOptions;
import com.opengamma.bbg.referencedata.statistics.BloombergReferenceDataStatistics;
import com.opengamma.bbg.referencedata.statistics.NullBloombergReferenceDataStatistics;
import com.opengamma.util.spring.SpringFactoryBean;

/**
 * Spring factory bean to create a Bloomberg connector.
 */
@BeanDefinition
public class BloombergConnectorFactoryBean extends SpringFactoryBean<BloombergConnector> {

  /**
   * The configuration name.
   */
  @PropertyDefinition(validate = "notNull")
  private String _name;
  /**
   * The server host name.
   */
  @PropertyDefinition
  private String _host;
  /**
   * The server port number.
   */
  @PropertyDefinition
  private Integer _port;
  /**
   * The pre-populated session options.
   * These options can be left null and they will then be created with default options.
   * If they are non-null, then the server host and port will be added if the
   * server host within this instance is null.
   */
  @PropertyDefinition
  private SessionOptions _sessionOptions;
  /**
   * The pre-populated reference data statistics.
   */
  @PropertyDefinition(validate = "notNull")
  private BloombergReferenceDataStatistics _referenceDataStatistics = NullBloombergReferenceDataStatistics.INSTANCE;

  /**
   * Creates an instance.
   */
  public BloombergConnectorFactoryBean() {
    super(BloombergConnector.class);
  }

  /**
   * Creates an instance, specifying the server.
   * 
   * @param name  the name of the connector, not null
   */
  public BloombergConnectorFactoryBean(String name) {
    super(BloombergConnector.class);
    setName(name);
  }

  /**
   * Creates an instance, specifying the server.
   * 
   * @param name  the name of the connector, not null
   * @param host  the server host name, may be null
   * @param port  the server port, may be null
   */
  public BloombergConnectorFactoryBean(String name, String host, Integer port) {
    super(BloombergConnector.class);
    setName(name);
    setHost(host);
    setPort(port);
  }

  //-------------------------------------------------------------------------
  @Override
  public BloombergConnector createObject() {
    SessionOptions sessionOptions = getSessionOptions();
    sessionOptions = (sessionOptions != null ? sessionOptions : new SessionOptions());
    if (getHost() != null) {
      sessionOptions.setServerHost(getHost());
      sessionOptions.setServerPort(getPort());
    }
    if (sessionOptions.getServerHost() == null || sessionOptions.getServerHost().contains("$")) {
      throw new IllegalStateException("Bloomberg SessionOptions does not have a server host");
    }
    return new BloombergConnector(getName(), sessionOptions, getReferenceDataStatistics());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BloombergConnectorFactoryBean}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static BloombergConnectorFactoryBean.Meta meta() {
    return BloombergConnectorFactoryBean.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(BloombergConnectorFactoryBean.Meta.INSTANCE);
  }

  @Override
  public BloombergConnectorFactoryBean.Meta metaBean() {
    return BloombergConnectorFactoryBean.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3373707:  // name
        return getName();
      case 3208616:  // host
        return getHost();
      case 3446913:  // port
        return getPort();
      case 522757928:  // sessionOptions
        return getSessionOptions();
      case -1225958248:  // referenceDataStatistics
        return getReferenceDataStatistics();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3373707:  // name
        setName((String) newValue);
        return;
      case 3208616:  // host
        setHost((String) newValue);
        return;
      case 3446913:  // port
        setPort((Integer) newValue);
        return;
      case 522757928:  // sessionOptions
        setSessionOptions((SessionOptions) newValue);
        return;
      case -1225958248:  // referenceDataStatistics
        setReferenceDataStatistics((BloombergReferenceDataStatistics) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_name, "name");
    JodaBeanUtils.notNull(_referenceDataStatistics, "referenceDataStatistics");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BloombergConnectorFactoryBean other = (BloombergConnectorFactoryBean) obj;
      return JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getHost(), other.getHost()) &&
          JodaBeanUtils.equal(getPort(), other.getPort()) &&
          JodaBeanUtils.equal(getSessionOptions(), other.getSessionOptions()) &&
          JodaBeanUtils.equal(getReferenceDataStatistics(), other.getReferenceDataStatistics()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getName());
    hash += hash * 31 + JodaBeanUtils.hashCode(getHost());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPort());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSessionOptions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getReferenceDataStatistics());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the configuration name.
   * @return the value of the property, not null
   */
  public String getName() {
    return _name;
  }

  /**
   * Sets the configuration name.
   * @param name  the new value of the property, not null
   */
  public void setName(String name) {
    JodaBeanUtils.notNull(name, "name");
    this._name = name;
  }

  /**
   * Gets the the {@code name} property.
   * @return the property, not null
   */
  public final Property<String> name() {
    return metaBean().name().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the server host name.
   * @return the value of the property
   */
  public String getHost() {
    return _host;
  }

  /**
   * Sets the server host name.
   * @param host  the new value of the property
   */
  public void setHost(String host) {
    this._host = host;
  }

  /**
   * Gets the the {@code host} property.
   * @return the property, not null
   */
  public final Property<String> host() {
    return metaBean().host().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the server port number.
   * @return the value of the property
   */
  public Integer getPort() {
    return _port;
  }

  /**
   * Sets the server port number.
   * @param port  the new value of the property
   */
  public void setPort(Integer port) {
    this._port = port;
  }

  /**
   * Gets the the {@code port} property.
   * @return the property, not null
   */
  public final Property<Integer> port() {
    return metaBean().port().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the pre-populated session options.
   * These options can be left null and they will then be created with default options.
   * If they are non-null, then the server host and port will be added if the
   * server host within this instance is null.
   * @return the value of the property
   */
  public SessionOptions getSessionOptions() {
    return _sessionOptions;
  }

  /**
   * Sets the pre-populated session options.
   * These options can be left null and they will then be created with default options.
   * If they are non-null, then the server host and port will be added if the
   * server host within this instance is null.
   * @param sessionOptions  the new value of the property
   */
  public void setSessionOptions(SessionOptions sessionOptions) {
    this._sessionOptions = sessionOptions;
  }

  /**
   * Gets the the {@code sessionOptions} property.
   * These options can be left null and they will then be created with default options.
   * If they are non-null, then the server host and port will be added if the
   * server host within this instance is null.
   * @return the property, not null
   */
  public final Property<SessionOptions> sessionOptions() {
    return metaBean().sessionOptions().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the pre-populated reference data statistics.
   * @return the value of the property, not null
   */
  public BloombergReferenceDataStatistics getReferenceDataStatistics() {
    return _referenceDataStatistics;
  }

  /**
   * Sets the pre-populated reference data statistics.
   * @param referenceDataStatistics  the new value of the property, not null
   */
  public void setReferenceDataStatistics(BloombergReferenceDataStatistics referenceDataStatistics) {
    JodaBeanUtils.notNull(referenceDataStatistics, "referenceDataStatistics");
    this._referenceDataStatistics = referenceDataStatistics;
  }

  /**
   * Gets the the {@code referenceDataStatistics} property.
   * @return the property, not null
   */
  public final Property<BloombergReferenceDataStatistics> referenceDataStatistics() {
    return metaBean().referenceDataStatistics().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BloombergConnectorFactoryBean}.
   */
  public static class Meta extends SpringFactoryBean.Meta<BloombergConnector> {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> _name = DirectMetaProperty.ofReadWrite(
        this, "name", BloombergConnectorFactoryBean.class, String.class);
    /**
     * The meta-property for the {@code host} property.
     */
    private final MetaProperty<String> _host = DirectMetaProperty.ofReadWrite(
        this, "host", BloombergConnectorFactoryBean.class, String.class);
    /**
     * The meta-property for the {@code port} property.
     */
    private final MetaProperty<Integer> _port = DirectMetaProperty.ofReadWrite(
        this, "port", BloombergConnectorFactoryBean.class, Integer.class);
    /**
     * The meta-property for the {@code sessionOptions} property.
     */
    private final MetaProperty<SessionOptions> _sessionOptions = DirectMetaProperty.ofReadWrite(
        this, "sessionOptions", BloombergConnectorFactoryBean.class, SessionOptions.class);
    /**
     * The meta-property for the {@code referenceDataStatistics} property.
     */
    private final MetaProperty<BloombergReferenceDataStatistics> _referenceDataStatistics = DirectMetaProperty.ofReadWrite(
        this, "referenceDataStatistics", BloombergConnectorFactoryBean.class, BloombergReferenceDataStatistics.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "name",
        "host",
        "port",
        "sessionOptions",
        "referenceDataStatistics");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return _name;
        case 3208616:  // host
          return _host;
        case 3446913:  // port
          return _port;
        case 522757928:  // sessionOptions
          return _sessionOptions;
        case -1225958248:  // referenceDataStatistics
          return _referenceDataStatistics;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BloombergConnectorFactoryBean> builder() {
      return new DirectBeanBuilder<BloombergConnectorFactoryBean>(new BloombergConnectorFactoryBean());
    }

    @Override
    public Class<? extends BloombergConnectorFactoryBean> beanType() {
      return BloombergConnectorFactoryBean.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> name() {
      return _name;
    }

    /**
     * The meta-property for the {@code host} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> host() {
      return _host;
    }

    /**
     * The meta-property for the {@code port} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Integer> port() {
      return _port;
    }

    /**
     * The meta-property for the {@code sessionOptions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<SessionOptions> sessionOptions() {
      return _sessionOptions;
    }

    /**
     * The meta-property for the {@code referenceDataStatistics} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BloombergReferenceDataStatistics> referenceDataStatistics() {
      return _referenceDataStatistics;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
