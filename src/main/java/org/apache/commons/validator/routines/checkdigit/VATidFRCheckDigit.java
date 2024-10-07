/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.validator.routines.checkdigit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.GenericTypeValidator;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.SireneValidator;

/**
 * French VAT identification number (VATIN) Check Digit calculation/validation.
 * <p>
 * le numéro d’identification à la taxe sur la valeur ajoutée (no TVA)
 * </p>
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/VAT_identification_number">Wikipedia - VAT IN</a>
 * for more details.
 * </p>
 *
 * @since 1.10.0
 */
public final class VATidFRCheckDigit extends ModulusCheckDigit {

    private static final long serialVersionUID = -4684483629166798828L;
    private static final Log LOG = LogFactory.getLog(VATidFRCheckDigit.class);

    /** Singleton Check Digit instance */
    private static final VATidFRCheckDigit INSTANCE = new VATidFRCheckDigit();

    /**
     * Gets the singleton instance of this validator.
     * @return A singleton instance of the class.
     */
    public static CheckDigit getInstance() {
        return INSTANCE;
    }

    static final int LEN = 11; // with Check Digit
    static final int CHECKDIGIT_LEN = 2;

    private static final String ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int MODULUS_97 = 97;
    private static final SireneValidator SIRENE_VALIDATOR = SireneValidator.getInstance();

    /**
     * Constructs a Check Digit routine.
     */
    private VATidFRCheckDigit() {
        super(MODULUS_97);
    }

    /**
     * Calculates the <i>weighted</i> value of a character in the
     * code at a specified position.
     *
     * <p>For VATID digits are weighted by their position from right to left.</p>
     *
     * @param charValue The numeric value of the character.
     * @param leftPos The position of the character in the code, counting from left to right
     * @param rightPos The positionof the character in the code, counting from right to left
     * @return The weighted value of the character.
     */
    @Override
    protected int weightedValue(final int charValue, final int leftPos, final int rightPos) {
        if (rightPos > LEN - CHECKDIGIT_LEN) return 0;
        Double pow = Math.pow(10, rightPos + 1 - CHECKDIGIT_LEN); // CHECKSTYLE IGNORE MagicNumber
        return charValue * pow.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String calculate(final String code) throws CheckDigitException {
        if (GenericValidator.isBlankOrNull(code)) {
            throw new CheckDigitException(CheckDigitException.MISSING_CODE);
        }
        Long checkZero = GenericTypeValidator.formatLong(code);
        if (checkZero != null && checkZero == 0) {
            throw new CheckDigitException(CheckDigitException.ZREO_SUM);
        }
        if (!SIRENE_VALIDATOR.isValid(code)) {
            throw new CheckDigitException("Invalid code, " + code + " is not valid SIREN");
        }
/*

How to calculate the intra-Community VAT number

Calculating the intra-Community VAT number requires finding the VAT key from SIREN:
Clé TVA = [12 + 3 × (SIREN modulo 97)] modulo 97
Modulo was the rest of the Euclidean division by 97.

Example: VAT key - [12 - 3 - (404 833 048 modulo 97) - modulo 97 - [12 - 3 - 56 - modulo 97 - 180 modulo 97 - 83
 */
//        Long siren = GenericTypeValidator.formatLong((code));
//        if (siren == null) {
//            throw new CheckDigitException("Invalid code, siren='" + code + "'");
//        }
//        final int cd3 = (int) (3 * (siren % MODULUS_97));
//        final int cd = (12 + cd3) % MODULUS_97;
//        LOG.info(code + " cd=" + cd + ", siren%97=" + siren % MODULUS_97
//            + " [12 + 3 × (SIREN modulo 97)]=" + (12 + cd3));

        Long cde = GenericTypeValidator.formatLong(code + "12");
        if (cde == null) {
            throw new CheckDigitException("Invalid code, '" + code + "'");
        }
        final int cdValue = (int) (cde % MODULUS_97);
        LOG.info(code + "12" + "    SIREN+12 modulo 97 = " + cdValue);
        // There are more than one possible VATIN check digit C(1-2) for a given SIREN,
        // thus, it isn't possible to compute it.

        // convert two-digit numeric values to String
        return Modulus97CheckDigit.toCheckDigit(cdValue);
    }

    public boolean isValidOldStyle(final String code) throws CheckDigitException {
        int cd = GenericTypeValidator.formatInt(code.substring(0, CHECKDIGIT_LEN));
        Long cde = GenericTypeValidator.formatLong((code + "12").substring(CHECKDIGIT_LEN));
        if (cde == null) {
            throw new CheckDigitException("Invalid code " + code);
        }
        return cd == cde % MODULUS_97;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(final String code) {
        if (GenericValidator.isBlankOrNull(code)) {
            return false;
        }
        if (code.length() != LEN) {
            return false;
        }
        if (GenericTypeValidator.formatLong(code.substring(CHECKDIGIT_LEN)) == null) {
            return false;
        }
        try {
        // siehe https://help.sap.com/docs/SUPPORT_CONTENT/crm/3354674613.html
        // cd ist nicht eindeutig - ich mache es in der Reihenfolge
        // 1. c0 isUpperCase && c1 isDigit ==> new style
        // 2. c0 isDigit && c1 isUpperCase ==> new style
        // 3. c0 isDigit && c1 isDigit ==> old style
        // alle anderen sind nicht valide
            int c0 = code.codePointAt(0);
            int c1 = code.codePointAt(1);
            int s0 = ALPHABET.indexOf(c0);
            int s1 = ALPHABET.indexOf(c1);
            if (Character.isUpperCase(c0) && Character.isDigit(c1)) {
                int s = s0 * 34 + s1 - 100; // CHECKSTYLE IGNORE MagicNumber
                int p = (s / 11) + 1; // CHECKSTYLE IGNORE MagicNumber
                int r1 = s % MODULUS_11;
                int r2 = (GenericTypeValidator.formatInt(code.substring(CHECKDIGIT_LEN)) + p) % MODULUS_11;
                return r1 == r2;
            } else if (Character.isDigit(c0) && Character.isUpperCase(c1)) {
                int s = s0 * 24 + s1 - 10; // CHECKSTYLE IGNORE MagicNumber
                int p = (s / 11) + 1; // CHECKSTYLE IGNORE MagicNumber
                int r1 = s % MODULUS_11;
                int r2 = (GenericTypeValidator.formatInt(code.substring(CHECKDIGIT_LEN)) + p) % MODULUS_11;
                return r1 == r2;
            } else if (Character.isDigit(c0) && Character.isDigit(c1)) {
                return isValidOldStyle(code);
            } else {
//                LOG.warn(code + "    invalid check code '" + code.substring(0, CHECKDIGIT_LEN) + "'");
                return false;
            }
        } catch (final CheckDigitException ex) {
            return false;
        }
    }

}