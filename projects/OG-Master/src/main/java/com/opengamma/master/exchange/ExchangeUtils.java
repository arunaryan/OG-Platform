/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.master.exchange;

import javax.time.calendar.LocalDate;
import javax.time.calendar.LocalTime;
import javax.time.calendar.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.core.exchange.ExchangeSource;
import com.opengamma.id.ExternalId;
import com.opengamma.util.tuple.Pair;

/**
 * Utilities for working with Exchanges.
 */
public class ExchangeUtils {
  private static final Logger s_logger = LoggerFactory.getLogger(ExchangeUtils.class);
  /**
   * THIS IS NOT READY FOR PRIME TIME YET
   * @param exchangeSource a source of exchanges, we assume it provides ManageableExchanges
   * @param isoMic an external id with the ISO MIC code of the exchange
   * @param today the date today (to allow for changes in opening hours over time)
   * @param defaultTime a fallback time to use if a close time could not be established, if set to null, will return null in time field.
   * @return a pair of values, the end of trading period and the time zone or null if no exchange with that code was found.  Time can be null if defaultTime==null.
   */
  public static Pair<LocalTime, TimeZone> getTradingCloseTime(ExchangeSource exchangeSource, ExternalId isoMic, LocalDate today, LocalTime defaultTime) {
    ManageableExchange exchange = (ManageableExchange) exchangeSource.getSingle(isoMic);
    if (exchange != null) {
      for (ManageableExchangeDetail detail : exchange.getDetail()) {
        if (detail.getPhaseName().equals("Trading") && 
            (detail.getCalendarStart() == null || detail.getCalendarStart().equals(today) || detail.getCalendarStart().isBefore(today)) &&
            (detail.getCalendarEnd() == null || detail.getCalendarEnd().equals(today) || detail.getCalendarEnd().isAfter(today))) {
          LocalTime endTime = detail.getPhaseEnd();
          if (endTime != null) {
            return Pair.of(endTime, exchange.getTimeZone());
          }
        }
      }
      s_logger.warn("Couldn't find exchagne close time for {}, defaulting to supplied default", isoMic);
      return Pair.of(defaultTime, exchange.getTimeZone());
    } else {
      return null;       
    }
  }
}
