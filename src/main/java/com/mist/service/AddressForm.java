package com.mist.service;

import com.mist.model.Address;

/**
 * A postal address as typed in a form. The service checks and normalizes it.
 *
 * @param street     street and number
 * @param details    floor, door and so on, or {@code null}
 * @param postalCode postal code
 * @param city       city
 * @param province   province, or {@code null}
 * @param country    ISO 3166-1 alpha-2 country code, such as {@code ES}
 */
public record AddressForm(String street, String details, String postalCode, String city, String province,
                          String country) {

    /**
     * Builds the model value.
     *
     * @return the normalized address
     * @throws IllegalArgumentException with a model key if a value is invalid
     */
    Address toAddress() {
        return Address.of(street, details, postalCode, city, province, country);
    }
}
