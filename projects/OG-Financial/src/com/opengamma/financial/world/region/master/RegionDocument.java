/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.world.region.master;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.time.Instant;

import org.joda.beans.BeanDefinition;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectMetaProperty;

import com.opengamma.core.region.Region;
import com.opengamma.id.Identifier;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.util.ArgumentChecker;

/**
 * A document used to pass into and out of the region master.
 * <p>
 * The region master provides full management of the region database.
 * Each element is stored in a document.
 */
@BeanDefinition
public class RegionDocument extends DirectBean implements Serializable {

  /**
   * The region unique identifier.
   */
  @PropertyDefinition
  private UniqueIdentifier _regionId;
  /**
   * The start of an interval that the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   */
  @PropertyDefinition
  private Instant _versionFromInstant;
  /**
   * The end of an interval that the version of the region is accurate for.
   * Null indicates this is the latest version.
   * This field is populated and managed by the {@code RegionMaster}.
   */
  @PropertyDefinition
  private Instant _versionToInstant;
  /**
   * The start of an interval that the correction of the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   */
  @PropertyDefinition
  private Instant _correctionFromInstant;
  /**
   * The end of an interval that the correction of the version of the region is accurate for.
   * Null indicates this is the latest correction.
   * This field is populated and managed by the {@code RegionMaster}.
   */
  @PropertyDefinition
  private Instant _correctionToInstant;
  /**
   * The identifier of the provider of the data.
   * This optional field can be used to capture the identifier used by the data provider.
   * This can be useful when receiving updates from the same provider.
   */
  @PropertyDefinition
  private Identifier _providerId;
  /**
   * The region.
   */
  @PropertyDefinition
  private ManageableRegion _region;

  /**
   * Creates an instance.
   */
  public RegionDocument() {
  }

  /**
   * Creates an instance from a region.
   * 
   * @param region  the region, not null
   */
  public RegionDocument(final Region region) {
    ArgumentChecker.notNull(region, "region");
    setRegionId(region.getUniqueIdentifier());
    if (region instanceof ManageableRegion) {
      setRegion((ManageableRegion) region);
    } else {
      setRegion(new ManageableRegion(region));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RegionDocument}.
   * @return the meta-bean, not null
   */
  public static RegionDocument.Meta meta() {
    return RegionDocument.Meta.INSTANCE;
  }

  @Override
  public RegionDocument.Meta metaBean() {
    return RegionDocument.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName) {
    switch (propertyName.hashCode()) {
      case -690339025:  // regionId
        return getRegionId();
      case 2006263519:  // versionFromInstant
        return getVersionFromInstant();
      case 1577022702:  // versionToInstant
        return getVersionToInstant();
      case 1808757913:  // correctionFromInstant
        return getCorrectionFromInstant();
      case 973465896:  // correctionToInstant
        return getCorrectionToInstant();
      case 205149932:  // providerId
        return getProviderId();
      case -934795532:  // region
        return getRegion();
    }
    return super.propertyGet(propertyName);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue) {
    switch (propertyName.hashCode()) {
      case -690339025:  // regionId
        setRegionId((UniqueIdentifier) newValue);
        return;
      case 2006263519:  // versionFromInstant
        setVersionFromInstant((Instant) newValue);
        return;
      case 1577022702:  // versionToInstant
        setVersionToInstant((Instant) newValue);
        return;
      case 1808757913:  // correctionFromInstant
        setCorrectionFromInstant((Instant) newValue);
        return;
      case 973465896:  // correctionToInstant
        setCorrectionToInstant((Instant) newValue);
        return;
      case 205149932:  // providerId
        setProviderId((Identifier) newValue);
        return;
      case -934795532:  // region
        setRegion((ManageableRegion) newValue);
        return;
    }
    super.propertySet(propertyName, newValue);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the region unique identifier.
   * @return the value of the property
   */
  public UniqueIdentifier getRegionId() {
    return _regionId;
  }

  /**
   * Sets the region unique identifier.
   * @param regionId  the new value of the property
   */
  public void setRegionId(UniqueIdentifier regionId) {
    this._regionId = regionId;
  }

  /**
   * Gets the the {@code regionId} property.
   * @return the property, not null
   */
  public final Property<UniqueIdentifier> regionId() {
    return metaBean().regionId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start of an interval that the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the value of the property
   */
  public Instant getVersionFromInstant() {
    return _versionFromInstant;
  }

  /**
   * Sets the start of an interval that the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   * @param versionFromInstant  the new value of the property
   */
  public void setVersionFromInstant(Instant versionFromInstant) {
    this._versionFromInstant = versionFromInstant;
  }

  /**
   * Gets the the {@code versionFromInstant} property.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the property, not null
   */
  public final Property<Instant> versionFromInstant() {
    return metaBean().versionFromInstant().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end of an interval that the version of the region is accurate for.
   * Null indicates this is the latest version.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the value of the property
   */
  public Instant getVersionToInstant() {
    return _versionToInstant;
  }

  /**
   * Sets the end of an interval that the version of the region is accurate for.
   * Null indicates this is the latest version.
   * This field is populated and managed by the {@code RegionMaster}.
   * @param versionToInstant  the new value of the property
   */
  public void setVersionToInstant(Instant versionToInstant) {
    this._versionToInstant = versionToInstant;
  }

  /**
   * Gets the the {@code versionToInstant} property.
   * Null indicates this is the latest version.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the property, not null
   */
  public final Property<Instant> versionToInstant() {
    return metaBean().versionToInstant().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start of an interval that the correction of the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the value of the property
   */
  public Instant getCorrectionFromInstant() {
    return _correctionFromInstant;
  }

  /**
   * Sets the start of an interval that the correction of the version of the region is accurate for.
   * This field is populated and managed by the {@code RegionMaster}.
   * @param correctionFromInstant  the new value of the property
   */
  public void setCorrectionFromInstant(Instant correctionFromInstant) {
    this._correctionFromInstant = correctionFromInstant;
  }

  /**
   * Gets the the {@code correctionFromInstant} property.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the property, not null
   */
  public final Property<Instant> correctionFromInstant() {
    return metaBean().correctionFromInstant().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end of an interval that the correction of the version of the region is accurate for.
   * Null indicates this is the latest correction.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the value of the property
   */
  public Instant getCorrectionToInstant() {
    return _correctionToInstant;
  }

  /**
   * Sets the end of an interval that the correction of the version of the region is accurate for.
   * Null indicates this is the latest correction.
   * This field is populated and managed by the {@code RegionMaster}.
   * @param correctionToInstant  the new value of the property
   */
  public void setCorrectionToInstant(Instant correctionToInstant) {
    this._correctionToInstant = correctionToInstant;
  }

  /**
   * Gets the the {@code correctionToInstant} property.
   * Null indicates this is the latest correction.
   * This field is populated and managed by the {@code RegionMaster}.
   * @return the property, not null
   */
  public final Property<Instant> correctionToInstant() {
    return metaBean().correctionToInstant().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the provider of the data.
   * This optional field can be used to capture the identifier used by the data provider.
   * This can be useful when receiving updates from the same provider.
   * @return the value of the property
   */
  public Identifier getProviderId() {
    return _providerId;
  }

  /**
   * Sets the identifier of the provider of the data.
   * This optional field can be used to capture the identifier used by the data provider.
   * This can be useful when receiving updates from the same provider.
   * @param providerId  the new value of the property
   */
  public void setProviderId(Identifier providerId) {
    this._providerId = providerId;
  }

  /**
   * Gets the the {@code providerId} property.
   * This optional field can be used to capture the identifier used by the data provider.
   * This can be useful when receiving updates from the same provider.
   * @return the property, not null
   */
  public final Property<Identifier> providerId() {
    return metaBean().providerId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the region.
   * @return the value of the property
   */
  public ManageableRegion getRegion() {
    return _region;
  }

  /**
   * Sets the region.
   * @param region  the new value of the property
   */
  public void setRegion(ManageableRegion region) {
    this._region = region;
  }

  /**
   * Gets the the {@code region} property.
   * @return the property, not null
   */
  public final Property<ManageableRegion> region() {
    return metaBean().region().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RegionDocument}.
   */
  public static class Meta extends BasicMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code regionId} property.
     */
    private final MetaProperty<UniqueIdentifier> _regionId = DirectMetaProperty.ofReadWrite(this, "regionId", UniqueIdentifier.class);
    /**
     * The meta-property for the {@code versionFromInstant} property.
     */
    private final MetaProperty<Instant> _versionFromInstant = DirectMetaProperty.ofReadWrite(this, "versionFromInstant", Instant.class);
    /**
     * The meta-property for the {@code versionToInstant} property.
     */
    private final MetaProperty<Instant> _versionToInstant = DirectMetaProperty.ofReadWrite(this, "versionToInstant", Instant.class);
    /**
     * The meta-property for the {@code correctionFromInstant} property.
     */
    private final MetaProperty<Instant> _correctionFromInstant = DirectMetaProperty.ofReadWrite(this, "correctionFromInstant", Instant.class);
    /**
     * The meta-property for the {@code correctionToInstant} property.
     */
    private final MetaProperty<Instant> _correctionToInstant = DirectMetaProperty.ofReadWrite(this, "correctionToInstant", Instant.class);
    /**
     * The meta-property for the {@code providerId} property.
     */
    private final MetaProperty<Identifier> _providerId = DirectMetaProperty.ofReadWrite(this, "providerId", Identifier.class);
    /**
     * The meta-property for the {@code region} property.
     */
    private final MetaProperty<ManageableRegion> _region = DirectMetaProperty.ofReadWrite(this, "region", ManageableRegion.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map;

    @SuppressWarnings({"unchecked", "rawtypes" })
    protected Meta() {
      LinkedHashMap temp = new LinkedHashMap();
      temp.put("regionId", _regionId);
      temp.put("versionFromInstant", _versionFromInstant);
      temp.put("versionToInstant", _versionToInstant);
      temp.put("correctionFromInstant", _correctionFromInstant);
      temp.put("correctionToInstant", _correctionToInstant);
      temp.put("providerId", _providerId);
      temp.put("region", _region);
      _map = Collections.unmodifiableMap(temp);
    }

    @Override
    public RegionDocument createBean() {
      return new RegionDocument();
    }

    @Override
    public Class<? extends RegionDocument> beanType() {
      return RegionDocument.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code regionId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<UniqueIdentifier> regionId() {
      return _regionId;
    }

    /**
     * The meta-property for the {@code versionFromInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> versionFromInstant() {
      return _versionFromInstant;
    }

    /**
     * The meta-property for the {@code versionToInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> versionToInstant() {
      return _versionToInstant;
    }

    /**
     * The meta-property for the {@code correctionFromInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> correctionFromInstant() {
      return _correctionFromInstant;
    }

    /**
     * The meta-property for the {@code correctionToInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> correctionToInstant() {
      return _correctionToInstant;
    }

    /**
     * The meta-property for the {@code providerId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Identifier> providerId() {
      return _providerId;
    }

    /**
     * The meta-property for the {@code region} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ManageableRegion> region() {
      return _region;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
