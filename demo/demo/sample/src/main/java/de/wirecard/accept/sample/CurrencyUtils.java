/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class CurrencyUtils {

    public static String format(long units, Currency currency, Locale locale) {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        if (currency != null) {
            numberFormat.setCurrency(currency);
            return numberFormat.format(new BigDecimal(units).scaleByPowerOfTen(-currency.getDefaultFractionDigits()).doubleValue());
        } else {
            return numberFormat.format(units);
        }
    }
}