/*
 * net/balusc/util/StringUtil.java
 *
 * Copyright (C) 2007 BalusC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package imagedownloader.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Useful String utilities.
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2006/10/stringutil.html
 */
public final class StringUtil {

    // Init ---------------------------------------------------------------------------------------

    /** Argument for <tt>StringUtil#pad()</tt>, set the pad direction to LEFT. */
    public static final int PAD_LEFT = -1;

    /** Argument for <tt>StringUtil#pad()</tt>, set the pad direction to BOTH. */
    public static final int PAD_BOTH = 0;

    /** Argument for <tt>StringUtil#pad()</tt>, set the pad direction to RIGHT. */
    public static final int PAD_RIGHT = 1;

    private StringUtil() {
        // Utility class, hide the constructor.
    }

    // Actions ------------------------------------------------------------------------------------

    /**
     * Pad the given string with the given pad value to the given length in the given direction.
     * Valid directions are <tt>StringUtil.PAD_LEFT</tt>, <tt>StringUtil.PAD_BOTH</tt> and
     * <tt>StringUtil.PAD_RIGHT</tt>. When using <tt>StringUtil.PAD_BOTH</tt>, padding left
     * has precedence over padding right when difference between string's length and the given
     * length is odd.
     * @param string The string to be padded.
     * @param pad The value to pad the given string with.
     * @param length The length to pad the given string to.
     * @param direction The direction to pad the given string to.
     * @return The padded string.
     * @throws IllegalArgumentException If invalid direction is given.
     */
    public static String pad(String string, String pad, int length, int direction)
        throws IllegalArgumentException {
        StringBuilder builder = new StringBuilder(string);

        switch (direction) {
            case PAD_LEFT:
                while (builder.length() < length) {
                    builder.insert(0, pad);
                }
                break;

            case PAD_RIGHT:
                while (builder.length() < length) {
                    builder.append(pad);
                }
                break;

            case PAD_BOTH:
                int right = (length - builder.length()) / 2 + builder.length();
                while (builder.length() < right) {
                    builder.append(pad);
                }
                while (builder.length() < length) {
                    builder.insert(0, pad);
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid direction, must be one of"
                    + " StringUtil.PAD_LEFT, StringUtil.PAD_BOTH or StringUtil.PAD_RIGHT.");
        }

        return builder.toString();
    }

    public static StringBuffer trim(StringBuffer buff, String trim) {
        return new StringBuffer(trim(buff.toString(), trim));
    }

    /**
     * Trim the given string with the given trim value.
     * @param string The string to be trimmed.
     * @param trim The value to trim the given string off.
     * @return The trimmed string.
     */
    public static String trim(String string, String trim) {
        if (string == null) {
            return null;
        }
        
        if (trim.length() == 0) {
            return string;
        }

        int start = 0;
        int end = string.length();
        int length = trim.length();

        while (start + length <= end && string.substring(start, start + length).equals(trim)) {
            start += length;
        }
        while (start + length <= end && string.substring(end - length, end).equals(trim)) {
            end -= length;
        }

        return string.substring(start, end);
    }

    /**
     * Join the given collection with the given join value.
     * @param collection The collection (List, Set) to be joined.
     * @param join The value to be joined between each part.
     * @return The joined collection.
     */
    public static String join(Collection<?> collection, String join) {
        StringBuilder builder = new StringBuilder();

        for (Iterator<?> iter = collection.iterator(); iter.hasNext();) {
            builder.append(iter.next());

            if (iter.hasNext()) {
                builder.append(join);
            }
        }

        return builder.toString();
    }

    /**
     * Join the given ordinary array with the given join value.
     * @param objects The ordinary array (String[], Integer[], etc) to be joined.
     * @param join The value to be joined between each part.
     * @return The joined array.
     */
    public static String join(Object[] objects, String join) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < objects.length;) {
            builder.append(objects[i]);

            if (++i < objects.length) {
                builder.append(join);
            }
        }

        return builder.toString();
    }

    // Validators ---------------------------------------------------------------------------------

    /**
     * Checks if the given string is null or only contains the words null.
     * @param string
     * @return
     */
    public static boolean isNull(String string) {
        if (string == null || string.trim().length() == 0) {
            return true;
        }

        Pattern pattern = Pattern.compile("^[null]+$",
                Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(string.replaceAll(" ", ""));

        return m.matches();
    }

    /**
     * Check if given string is a number. It should contain digits only.
     * @param string The string to check on.
     * @return True if string is a number.
     */
    public static boolean isNumber(String string) {
        return string != null && string.matches("^\\d+$");
    }

    /**
     * Check if given string is numeric. Positive and negative prefix and dot separators are
     * allowed.
     * @param string The string to check on.
     * @return True if string is numeric.
     */
    public static boolean isNumeric(String string) {
        return string != null && string.matches("^[-+]?\\d+(\\.\\d+)?$");
    }

    /**
     * tests if the spcified regular expression occurs in the specified string.
     * note this method will return true if it finds the expression anywhere in the string
     * regardless of whether the rest of the string does not match!
     * @param regex the rexex to search for
     * @param string the string to search
     * @return true if any matches are found
     */
    public static boolean hasRegularExpression(String regex, String string) {
        if (regex != null && string != null) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string);
            return matcher.find();

        } else {
            return false;
        }
    }

    /**
     * check if the given string contains the expression <tt>[a-zA-Z]+</tt>
     * @param string the string to check
     * @return true if string matches
     */
    public static boolean hasAlphabetCharacters(String string) {
        return hasRegularExpression("[a-zA-Z]+", string);
    }

    /**
     * parses the string as a boolean value. Returns true if and only if the string is
     * not null and equals to 'true', '1', or 'on'.
     * @param string the string to be parsed
     * @return true if conditions are met.
     */
    public static boolean parseBool(String string) {
        if (string == null) {
            string = "false";
        }
        string = string.trim();

        return string.equalsIgnoreCase("true")
                || string.equals("1")
                || string.equalsIgnoreCase("yes")
                || string.equalsIgnoreCase("on");
    }

    /**
     * Check if given string is a valuta. The dot separator and two decimals are required.
     * @param string The string to check on.
     * @return True if string is valuta.
     */
    public static boolean isValuta(String string) {
        return string != null && string.matches("^\\d+\\.\\d{2}$");
    }

    /**
     * Check if given string contains numbers.
     * @param string The string to check on.
     * @return True if string contains numbers.
     */
    public static boolean hasNumbers(String string) {
        return string != null && string.matches("^.*\\d.*$");
    }

    /**
     * Check if given string is a valid email address.
     * @param string The string to check on.
     * @return True if string is an valid email address.
     */
    public static boolean isEmailAddress(String string) {
        return string != null && string.matches("^[\\w-~#&]+(\\.[\\w-~#&]+)*@([\\w-]+\\.)+[a-z]{2,5}$");
    }

    // Converters ---------------------------------------------------------------------------------

    /**
     * Convert the given string to a string with unicode escape sequence for every character.
     * @param string The string to be converted.
     * @return The string with unicode escape sequence for every character.
     */
    public static String toUnicode(String string) {
        StringBuilder builder = new StringBuilder();
        for (char c : string.toCharArray()) {
            builder.append(String.format("\\u%04x", (int) c));
        }
        return builder.toString();
    }

    /**
     * Unescape any unicode escape sequence in the given string.
     * @param string The string to be unescaped.
     * @return The string with unescaped unicode escape sequences.
     */
    public static String unescapeUnicode(String string) {
        Matcher matcher = Pattern.compile("\\\\u((?i)[0-9a-f]{4})").matcher(string);
        while (matcher.find()) {
            int codepoint = Integer.valueOf(matcher.group(1), 16);
            string = string.replaceAll("\\" + matcher.group(0), String.valueOf((char) codepoint));
        }
        return string;
    }

    /**
     * Remove any XSS (Cross Site Scripting) vulrenabilities from the given string.
     * @param string The string to remove XSS from.
     * @return The string with removed XSS, if any.
     */
    public static String removeXss(String string) {
        return string
            .replaceAll("(?i)<script.*?>.*?</script.*?>", "") // Remove all <script> tags.
            .replaceAll("(?i)<.*?javascript:.*?>.*?</.*?>", "") // Remove tags with javascript: call.
            .replaceAll("(?i)<.*?\\s+on.*?>.*?</.*?>", ""); // Remove tags with on* attributes.
    }

    /**
     * Remove any diacritical marks (accents like ç, ñ, é, etc) from the given string.
     * @param string The string to remove diacritical marks from.
     * @return The string with removed diacritical marks, if any.
     */
    public static String removeDiacriticalMarks(String string) {
        return Normalizer.normalize(string, Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Capitalize the first letter of the given string
     * @param string The string to have its first letter capitalized
     * @return The string with its first letter capitalized or a blank string
     * if string was null.
     */
    public static String capitalizeFirst(String string) {
        if (string != null) {
            return (string.substring(0, 1).toUpperCase() +
                    string.substring(1, string.length()));
        } else {
            return "";
        }

    }

    /**
     * Converts a given string to remove all special characters
     * @param string the string to be converted
     * @return the formatted string
     */
    public static String removeSpecialChars(String string) {
        if (string != null) {
            string = string.replaceAll("\\W", "_");
            return string;
        } else {
            return "_";
        }
    }

    /**
     * Converts a given string to SQL compatible in terms of the single quote
     * @param string the string to be converted
     * @param caps should the string be uppercase or not
     * @return the formatted string
     */
    public static String escapeToSQL(String string) {
        if (string != null) {
            string = string.replaceAll("[']", "''");

            string = string.trim();

            return string;
        } else {
            return "";
        }
    }

    /**
     * Converts a given string to Html compatible
     * @param string the string to be converted
     * @return the formatted string
     */
    public static String escapeToHTML(String string) {
        return escapeToHTML(string, false);
    }
    /**
     * Converts a given string to Html compatible
     * @param string the string to be converted
     * @param caps should the string be uppercase or not
     * @return the formatted string
     */
    public static String escapeToHTML(String string, boolean caps) {
        if (string != null) {
            string = string.replaceAll("<", "&lt;");
            string = string.replaceAll(">", "&gt;");
            string = string.replaceAll("\"", "&quot;");
            string = string.replaceAll("'", "&acute;");
            string = string.replaceAll("\n", "<br/>");
            string = string.trim();

            return caps?string.toUpperCase():string;
        } else {
            return "";
        }
    }

    /**
     *  Convenience method to convert a byte array to a hex string.
     *
     * @param  data  the byte[] to convert
     * @return String the converted byte[]
     */
    public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase());
        }
        return (buf.toString());
    }

    /**
     *  Convenience method to convert a byte array to a hex string and seperate with delimeter.
     *
     * @param  data  the byte[] to convert
     * @param  delimeter  the delimeter
     * @return String the formatted converted byte[]
     */
    public static String bytesToHexDelimeter(byte[] data, String delimeter) {
        if (data == null) return "null";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase());
            if (i != data.length-1) {
                buf.append(delimeter);
            }
        }
        return (buf.toString());
    }


    /**
     *  method to convert a byte to a hex string.
     *
     * @param  data  the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }


    /**
     *  Convenience method to convert an int to a hex char.
     *
     * @param  i  the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }
}