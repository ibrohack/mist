package com.mist.service;

import com.mist.model.Phone;

/**
 * A phone number as typed in a form. Both parts blank means no phone.
 *
 * @param countryCode country calling code, such as {@code +34} or {@code 0034}
 * @param number      the number, spaces and dashes allowed
 */
public record PhoneForm(String countryCode, String number) {

    /**
     * Builds the model value.
     *
     * @return the normalized phone, or {@code null} if both parts are blank
     * @throws IllegalArgumentException with a model key if a value is invalid
     */
    Phone toPhone() {
        if (isBlank(countryCode) && isBlank(number)) {
            return null;
        }
        return Phone.of(countryCode, number);
    }

    /**
     * Builds the model value of an optional phone.
     *
     * @param form the form, or {@code null}
     * @return the phone, or {@code null} if no phone was given
     */
    static Phone toPhone(PhoneForm form) {
        return form == null ? null : form.toPhone();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
