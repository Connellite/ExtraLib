package io.github.connellite.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimeParseTest {

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("dateCases")
    void parsesDates(String text, String expected) {
        assertEquals(LocalDate.parse(expected), DateTimeUtil.parseLocalDate(text));
        assertEquals(LocalDate.parse(expected), DateTimeUtil.tryParseLocalDate(text));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("dateTimeCases")
    void parsesDateTimes(String text, String expected) {
        assertEquals(LocalDateTime.parse(expected), DateTimeUtil.parseLocalDateTime(text));
        assertEquals(LocalDateTime.parse(expected), DateTimeUtil.tryParseLocalDateTime(text));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("timeCases")
    void parsesTimes(String text, String expected) {
        assertEquals(LocalTime.parse(expected), DateTimeUtil.parseLocalTime(text));
        assertEquals(LocalTime.parse(expected), DateTimeUtil.tryParseLocalTime(text));
    }

    @ParameterizedTest(name = "[{index}] reject {0}")
    @MethodSource("rejectCases")
    void rejectsUnparseable(String text) {
        assertNull(DateTimeUtil.tryParseLocalDate(text));
        assertNull(DateTimeUtil.tryParseLocalDateTime(text));
        assertNull(DateTimeUtil.tryParseLocalTime(text));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDate(text));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDateTime(text));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.parseLocalTime(text));
    }

    static Stream<Arguments> dateCases() {
        return Stream.of(
                // ISO / basic / year-first numeric
                Arguments.of("2011-12-03", "2011-12-03"),
                Arguments.of("2011-12-3", "2011-12-03"),
                Arguments.of("2011-1-03", "2011-01-03"),
                Arguments.of("2011-1-3", "2011-01-03"),
                Arguments.of("2024-03-21", "2024-03-21"),
                Arguments.of("2024-3-2", "2024-03-02"),
                Arguments.of("0001-01-01", "0001-01-01"),
                Arguments.of("0000-01-01", "0000-01-01"),
                Arguments.of("9999-12-31", "9999-12-31"),
                Arguments.of("2000-02-29", "2000-02-29"),
                Arguments.of("1900-02-28", "1900-02-28"),
                Arguments.of("20240321", "2024-03-21"),
                Arguments.of("20140426", "2014-04-26"),
                Arguments.of("2024/03/21", "2024-03-21"),
                Arguments.of("2024/3/21", "2024-03-21"),
                Arguments.of("2014/4/26", "2014-04-26"),
                Arguments.of("2024.03.21", "2024-03-21"),
                Arguments.of("2014.4.26", "2014-04-26"),
                Arguments.of("2024年03月21日", "2024-03-21"),
                Arguments.of("2024年1月2日", "2024-01-02"),
                Arguments.of("2024年12月31日", "2024-12-31"),
                Arguments.of("+12024-03-21", "+12024-03-21"),
                Arguments.of("+02024-03-21", "2024-03-21"),
                Arguments.of("-0001-03-21", "-0001-03-21"),
                Arguments.of("-0001-12-31", "-0001-12-31"),

                // day-first numeric
                Arguments.of("21.03.2024", "2024-03-21"),
                Arguments.of("21-03-2024", "2024-03-21"),
                Arguments.of("21/03/2024", "2024-03-21"),
                Arguments.of("26.4.2014", "2014-04-26"),
                Arguments.of("26-4-2014", "2014-04-26"),
                Arguments.of("26/4/2014", "2014-04-26"),
                Arguments.of("1.1.2000", "2000-01-01"),
                Arguments.of("01.01.00", "2000-01-01"),
                Arguments.of("31.12.99", "1999-12-31"),
                Arguments.of("21.03.24", "2024-03-21"),
                Arguments.of("12.02.1999", "1999-02-12"),
                Arguments.of("29.02.2000", "2000-02-29"),
                Arguments.of("28.02.2100", "2100-02-28"),

                // English month names
                Arguments.of("21 Jan 2024", "2024-01-21"),
                Arguments.of("21 January 2024", "2024-01-21"),
                Arguments.of("21 Feb 2024", "2024-02-21"),
                Arguments.of("21 February 2024", "2024-02-21"),
                Arguments.of("21 Mar 2024", "2024-03-21"),
                Arguments.of("21 March 2024", "2024-03-21"),
                Arguments.of("21 Apr 2024", "2024-04-21"),
                Arguments.of("21 April 2024", "2024-04-21"),
                Arguments.of("21 May 2024", "2024-05-21"),
                Arguments.of("21 Jun 2024", "2024-06-21"),
                Arguments.of("21 June 2024", "2024-06-21"),
                Arguments.of("21 Jul 2024", "2024-07-21"),
                Arguments.of("21 July 2024", "2024-07-21"),
                Arguments.of("21 Aug 2024", "2024-08-21"),
                Arguments.of("21 August 2024", "2024-08-21"),
                Arguments.of("21 Sep 2024", "2024-09-21"),
                Arguments.of("21 Sept 2024", "2024-09-21"),
                Arguments.of("21 Sept. 2024", "2024-09-21"),
                Arguments.of("21 September 2024", "2024-09-21"),
                Arguments.of("21 Oct 2024", "2024-10-21"),
                Arguments.of("21 October 2024", "2024-10-21"),
                Arguments.of("21 Nov 2024", "2024-11-21"),
                Arguments.of("21 November 2024", "2024-11-21"),
                Arguments.of("21 Dec 2024", "2024-12-21"),
                Arguments.of("21 December 2024", "2024-12-21"),
                Arguments.of("21-Mar-2024", "2024-03-21"),
                Arguments.of("08/May/2005", "2005-05-08"),
                Arguments.of("08-May-2005", "2005-05-08"),
                Arguments.of("Mar 21, 2024", "2024-03-21"),
                Arguments.of("March 21, 2024", "2024-03-21"),
                Arguments.of("May 8, 2009", "2009-05-08"),
                Arguments.of("May 8 2009", "2009-05-08"),
                Arguments.of("may 8, 2009", "2009-05-08"),
                Arguments.of("MAY 8, 2009", "2009-05-08"),
                Arguments.of("Jan 2, 2006", "2006-01-02"),
                Arguments.of("January 2, 2006", "2006-01-02"),
                Arguments.of("2 January 2006", "2006-01-02"),
                Arguments.of("2006 January 2", "2006-01-02"),
                Arguments.of("2006 Jan 2", "2006-01-02"),
                Arguments.of("2024 Mar 21", "2024-03-21"),
                Arguments.of("Feb 29, 2024", "2024-02-29"),
                Arguments.of("Sept 21, 2024", "2024-09-21"),
                Arguments.of("oct 7, 1970", "1970-10-07"),
                Arguments.of("Oct 7, 1970", "1970-10-07"),
                Arguments.of("OCT 7, 1970", "1970-10-07"),
                Arguments.of("7 oct 1970", "1970-10-07"),

                // with weekday prefix
                Arguments.of("Mon, 02 Jan 2006", "2006-01-02"),
                Arguments.of("Monday, 02 Jan 2006", "2006-01-02"),
                Arguments.of("Tue 21 Mar 2024", "2024-03-21"),
                Arguments.of("Wed, 21-Mar-2024", "2024-03-21"),
                Arguments.of("Thu, 21 Mar 2024", "2024-03-21"),

                // Russian
                Arguments.of("21 янв. 2024", "2024-01-21"),
                Arguments.of("21 января 2024", "2024-01-21"),
                Arguments.of("21 фев. 2024", "2024-02-21"),
                Arguments.of("21 февраля 2024", "2024-02-21"),
                Arguments.of("21 мар. 2024", "2024-03-21"),
                Arguments.of("21 марта 2024", "2024-03-21"),
                Arguments.of("21 апр. 2024", "2024-04-21"),
                Arguments.of("21 апреля 2024", "2024-04-21"),
                Arguments.of("21 мая 2024", "2024-05-21"),
                Arguments.of("21 июн. 2024", "2024-06-21"),
                Arguments.of("21 июня 2024", "2024-06-21"),
                Arguments.of("21 июл. 2024", "2024-07-21"),
                Arguments.of("21 июля 2024", "2024-07-21"),
                Arguments.of("21 авг. 2024", "2024-08-21"),
                Arguments.of("21 августа 2024", "2024-08-21"),
                Arguments.of("21 сент. 2024", "2024-09-21"),
                Arguments.of("21 сентября 2024", "2024-09-21"),
                Arguments.of("21 окт. 2024", "2024-10-21"),
                Arguments.of("21 октября 2024", "2024-10-21"),
                Arguments.of("21 нояб. 2024", "2024-11-21"),
                Arguments.of("21 ноября 2024", "2024-11-21"),
                Arguments.of("21 дек. 2024", "2024-12-21"),
                Arguments.of("21 декабря 2024", "2024-12-21"),

                // German
                Arguments.of("21 Januar 2024", "2024-01-21"),
                Arguments.of("21 Februar 2024", "2024-02-21"),
                Arguments.of("21 März 2024", "2024-03-21"),
                Arguments.of("21 April 2024", "2024-04-21"),
                Arguments.of("21 Mai 2024", "2024-05-21"),
                Arguments.of("21 Juni 2024", "2024-06-21"),
                Arguments.of("21 Juli 2024", "2024-07-21"),
                Arguments.of("21 August 2024", "2024-08-21"),
                Arguments.of("21 September 2024", "2024-09-21"),
                Arguments.of("21 Oktober 2024", "2024-10-21"),
                Arguments.of("21 November 2024", "2024-11-21"),
                Arguments.of("21 Dezember 2024", "2024-12-21"),

                // French
                Arguments.of("21 janvier 2024", "2024-01-21"),
                Arguments.of("21 février 2024", "2024-02-21"),
                Arguments.of("21 mars 2024", "2024-03-21"),
                Arguments.of("21 avril 2024", "2024-04-21"),
                Arguments.of("21 mai 2024", "2024-05-21"),
                Arguments.of("21 juin 2024", "2024-06-21"),
                Arguments.of("21 juillet 2024", "2024-07-21"),
                Arguments.of("21 août 2024", "2024-08-21"),
                Arguments.of("21 septembre 2024", "2024-09-21"),
                Arguments.of("21 octobre 2024", "2024-10-21"),
                Arguments.of("21 novembre 2024", "2024-11-21"),
                Arguments.of("21 décembre 2024", "2024-12-21"),

                // Italian
                Arguments.of("21 gennaio 2024", "2024-01-21"),
                Arguments.of("21 febbraio 2024", "2024-02-21"),
                Arguments.of("21 marzo 2024", "2024-03-21"),
                Arguments.of("21 aprile 2024", "2024-04-21"),
                Arguments.of("21 maggio 2024", "2024-05-21"),
                Arguments.of("21 giugno 2024", "2024-06-21"),
                Arguments.of("21 luglio 2024", "2024-07-21"),
                Arguments.of("21 agosto 2024", "2024-08-21"),
                Arguments.of("21 settembre 2024", "2024-09-21"),
                Arguments.of("21 ottobre 2024", "2024-10-21"),
                Arguments.of("21 novembre 2024", "2024-11-21"),
                Arguments.of("21 dicembre 2024", "2024-12-21"),

                // Spanish
                Arguments.of("21 enero 2024", "2024-01-21"),
                Arguments.of("21 febrero 2024", "2024-02-21"),
                Arguments.of("21 abril 2024", "2024-04-21"),
                Arguments.of("21 mayo 2024", "2024-05-21"),
                Arguments.of("21 junio 2024", "2024-06-21"),
                Arguments.of("21 julio 2024", "2024-07-21"),
                Arguments.of("21 septiembre 2024", "2024-09-21"),
                Arguments.of("21 octubre 2024", "2024-10-21"),
                Arguments.of("21 noviembre 2024", "2024-11-21"),
                Arguments.of("21 diciembre 2024", "2024-12-21"),

                // whitespace / date-time sources for date extraction
                Arguments.of("  2024-03-21  ", "2024-03-21"),
                Arguments.of("\t2024-03-21\n", "2024-03-21"),
                Arguments.of("2024-03-21T10:15:30", "2024-03-21"),
                Arguments.of("2024-03-21 10:15:30", "2024-03-21"),
                Arguments.of("2024-03-21T10:15:30Z", "2024-03-21"),
                Arguments.of("2024-03-21T00:00:00", "2024-03-21"),
                Arguments.of("2024-03-21 00:00:00.000000000", "2024-03-21"),
                Arguments.of("2024-03-21T", "2024-03-21"),

                // year and month only, resolved to the first of the month
                Arguments.of("2024-03", "2024-03-01"),
                Arguments.of("2024-3", "2024-03-01"),
                Arguments.of("2006-01", "2006-01-01"),
                Arguments.of("2024.03", "2024-03-01"),
                Arguments.of("2014.05", "2014-05-01"),
                Arguments.of("2024/03", "2024-03-01"),

                // year only, resolved to the first of January
                Arguments.of("2024", "2024-01-01"),
                Arguments.of("2014", "2014-01-01"),
                Arguments.of("0001", "0001-01-01"),
                Arguments.of("  2024  ", "2024-01-01"),

                // ordinal day suffix
                Arguments.of("April 8th 2009", "2009-04-08"),
                Arguments.of("April 8th, 2009", "2009-04-08"),
                Arguments.of("April 1st, 2009", "2009-04-01"),
                Arguments.of("April 2nd, 2009", "2009-04-02"),
                Arguments.of("April 3rd, 2009", "2009-04-03"),
                Arguments.of("April 23RD, 2009", "2009-04-23"),
                Arguments.of("8th May 2009", "2009-05-08"),
                Arguments.of("21st March 2024", "2024-03-21"),
                Arguments.of("Mar 21st, 2024", "2024-03-21"),

                // two-digit year written with an apostrophe
                Arguments.of("oct 7, '70", "1970-10-07"),
                Arguments.of("oct. 7, '70", "1970-10-07"),
                Arguments.of("Mar 21, '24", "2024-03-21"),

                // colon as the date separator
                Arguments.of("2014:10:13", "2014-10-13"),
                Arguments.of("2024:03:21", "2024-03-21"),
                Arguments.of("03:19:2012", "2012-03-19"),
                Arguments.of("21:03:2024", "2024-03-21"),
                Arguments.of("13:10:2014", "2014-10-13"),

                // compact two-digit year followed by a time
                Arguments.of("171113 14:14:20", "2017-11-13"),
                Arguments.of("240321 10:15", "2024-03-21")
        );
    }

    static Stream<Arguments> dateTimeCases() {
        return Stream.of(
                // ISO / SQL / separators
                Arguments.of("2024-03-21T10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21t10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21_10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21 10:15", "2024-03-21T10:15"),
                Arguments.of("2024-03-21T10:15", "2024-03-21T10:15"),
                Arguments.of("2014-04-26 17:24:37", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37", "2014-04-26T17:24:37"),
                Arguments.of("2013-04-01 22:43:01", "2013-04-01T22:43:01"),
                Arguments.of("2013-04-01 22:43", "2013-04-01T22:43"),
                Arguments.of("2014-04-26_17:24:37", "2014-04-26T17:24:37"),

                // fractions
                Arguments.of("2024-03-21 10:15:30.123", "2024-03-21T10:15:30.123"),
                Arguments.of("2024-03-21 10:15:30.123456789", "2024-03-21T10:15:30.123456789"),
                Arguments.of("2024-03-21 10:15:30,5", "2024-03-21T10:15:30.500"),
                Arguments.of("2014-04-26 17:24:37.123", "2014-04-26T17:24:37.123"),
                Arguments.of("2014-04-26T17:24:37.123", "2014-04-26T17:24:37.123"),
                Arguments.of("2014-04-26T17:24:37.123456789", "2014-04-26T17:24:37.123456789"),
                Arguments.of("2014-04-26T17:24:37,123", "2014-04-26T17:24:37.123"),
                Arguments.of("2014-04-26T17:24:37.1", "2014-04-26T17:24:37.100"),
                Arguments.of("2014-04-26T17:24:37.12", "2014-04-26T17:24:37.120"),
                Arguments.of("2014-04-26T17:24:37.1234", "2014-04-26T17:24:37.123400"),
                Arguments.of("2014-04-26T17:24:37.1234567890", "2014-04-26T17:24:37.123456789"),
                Arguments.of("2014-04-26T17:24:37.000000001", "2014-04-26T17:24:37.000000001"),
                Arguments.of("2014-04-26T17:24:37.999999999", "2014-04-26T17:24:37.999999999"),
                Arguments.of("2006-01-02T15:04:05.999999999Z", "2006-01-02T15:04:05.999999999"),
                Arguments.of("2009-08-12T22:15:09.988+01:00", "2009-08-12T22:15:09.988"),
                Arguments.of("2024-03-21T10:15:30.5Z", "2024-03-21T10:15:30.500"),
                Arguments.of("2024-03-21 10:15:30.5 UTC", "2024-03-21T10:15:30.500"),
                Arguments.of("2024-06-21T12:00:00.000Z", "2024-06-21T12:00"),
                Arguments.of("2024-06-21T12:00:00.000000Z", "2024-06-21T12:00"),

                // offsets / zones
                Arguments.of("2024-03-21T10:15:30Z", "2024-03-21T10:15:30"),
                Arguments.of("2014-04-26T17:24:37Z", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37z", "2014-04-26T17:24:37"),
                Arguments.of("2024-03-21T10:15:30+03:00", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21T10:15:30-0500", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21T08:41-04", "2024-03-21T08:41"),
                Arguments.of("2019-05-29T08:41-04:00", "2019-05-29T08:41"),
                Arguments.of("2019-05-29T08:41-04", "2019-05-29T08:41"),
                Arguments.of("2014-04-26T17:24:37+00:00", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37+0000", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37+00", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37-00:00", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37+05:30", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37+0530", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37-07:00", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37-0700", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37+08:00:00", "2014-04-26T17:24:37"),
                Arguments.of("2009-08-12T22:15:09+01:00", "2009-08-12T22:15:09"),
                Arguments.of("2024-03-21T10:15:30+03:00[Europe/Moscow]", "2024-03-21T10:15:30"),
                Arguments.of("2014-04-26T17:24:37+03:00[Europe/Moscow]", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37[Europe/Moscow]", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26T17:24:37[UTC]", "2014-04-26T17:24:37"),
                Arguments.of("2006-01-02 15:04:05 -0700", "2006-01-02T15:04:05"),
                Arguments.of("2006-01-02 15:04:05 -07:00", "2006-01-02T15:04:05"),
                Arguments.of("2006-01-02 15:04:05.999999999 -07:00", "2006-01-02T15:04:05.999999999"),
                Arguments.of("2014-04-26 17:24:37 +0800", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 +08:00", "2014-04-26T17:24:37"),

                // compact
                Arguments.of("20240321T101530Z", "2024-03-21T10:15:30"),
                Arguments.of("20240321101530", "2024-03-21T10:15:30"),
                Arguments.of("202403211015", "2024-03-21T10:15"),
                Arguments.of("20140426T172437", "2014-04-26T17:24:37"),
                Arguments.of("20140426T172437Z", "2014-04-26T17:24:37"),
                Arguments.of("20140426172437", "2014-04-26T17:24:37"),
                Arguments.of("201404261724", "2014-04-26T17:24"),

                // zone abbreviations
                Arguments.of("2024-03-21 10:15:30 UTC", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21 10:15:30 MSK", "2024-03-21T10:15:30"),
                Arguments.of("2024-03-21 10:15:30 GMT+03:00", "2024-03-21T10:15:30"),
                Arguments.of("2014-04-26 17:24:37 UTC", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 GMT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 UT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 Z", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 Zulu", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 MSK", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 CET", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 CEST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 EST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 EDT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 PST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 PDT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 CST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 CDT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 MST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 MDT", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 JST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 IST", "2014-04-26T17:24:37"),
                Arguments.of("2014-04-26 17:24:37 GMT+3", "2014-04-26T17:24:37"),
                Arguments.of("2014-12-16 06:20:00 UTC", "2014-12-16T06:20"),

                // AM/PM
                Arguments.of("Mar 21, 2024 5:57:51 PM", "2024-03-21T17:57:51"),
                Arguments.of("Mar 21, 2024 5:57:51 AM", "2024-03-21T05:57:51"),
                Arguments.of("Mar 21, 2024 12:30 AM", "2024-03-21T00:30"),
                Arguments.of("Mar 21, 2024 12:30 PM", "2024-03-21T12:30"),
                Arguments.of("May 8, 2009 5:57:51 PM", "2009-05-08T17:57:51"),
                Arguments.of("May 8, 2009 5:57:51AM", "2009-05-08T05:57:51"),
                Arguments.of("May 8, 2009 5:57:51 pm", "2009-05-08T17:57:51"),
                Arguments.of("May 8, 2009 5:57:51 am", "2009-05-08T05:57:51"),
                Arguments.of("May 8, 2009 12:00:00 AM", "2009-05-08T00:00"),
                Arguments.of("May 8, 2009 12:00:00 PM", "2009-05-08T12:00"),
                Arguments.of("May 8, 2009 12:00 AM", "2009-05-08T00:00"),
                Arguments.of("May 8, 2009 12:00 PM", "2009-05-08T12:00"),
                Arguments.of("May 8, 2009 1:00 AM", "2009-05-08T01:00"),
                Arguments.of("May 8, 2009 1:00 PM", "2009-05-08T13:00"),
                Arguments.of("May 8, 2009 0:00:00 AM", "2009-05-08T00:00"),
                Arguments.of("7 Oct 1970 5:57:51 PM", "1970-10-07T17:57:51"),

                // RFC 1123 / asctime / weekday
                Arguments.of("Thu, 21 Mar 2024 10:15:30 GMT", "2024-03-21T10:15:30"),
                Arguments.of("Thu Mar 21 10:15:30 MSK 2024", "2024-03-21T10:15:30"),
                Arguments.of("Mon Jan  2 15:04:05 2006", "2006-01-02T15:04:05"),
                Arguments.of("Mon Jan 02 15:04:05 2006", "2006-01-02T15:04:05"),
                Arguments.of("Mon Jan 02 15:04:05 -0700 2006", "2006-01-02T15:04:05"),
                Arguments.of("Mon Jan 02 15:04:05 MST 2006", "2006-01-02T15:04:05"),
                Arguments.of("Mon, 02 Jan 2006 15:04:05 MST", "2006-01-02T15:04:05"),
                Arguments.of("Mon, 02 Jan 2006 15:04:05 -0700", "2006-01-02T15:04:05"),
                Arguments.of("Mon, 02 Jan 2006 15:04:05 +0000", "2006-01-02T15:04:05"),
                Arguments.of("Mon, 02-Jan-06 15:04:05 MST", "2006-01-02T15:04:05"),
                Arguments.of("Thursday, 02-Jan-06 15:04:05 MST", "2006-01-02T15:04:05"),
                Arguments.of("Fri, 21 Jun 2024 12:00:00 +0000", "2024-06-21T12:00"),

                // day-first / named / CJK with time
                Arguments.of("21.03.2024 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("21/03/2024 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("21-03-2024 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("21.03.2024 10:15:30.123", "2024-03-21T10:15:30.123"),
                Arguments.of("21 Mar 2024 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("21 марта 2024 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024/03/21 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024.03.21 10:15:30", "2024-03-21T10:15:30"),
                Arguments.of("2024年03月21日 10:15:30", "2024-03-21T10:15:30"),

                // date-only becomes start of day
                Arguments.of("21.03.2024", "2024-03-21T00:00"),
                Arguments.of("12.02.1999", "1999-02-12T00:00"),
                Arguments.of("20240321", "2024-03-21T00:00"),
                Arguments.of("oct 7, 1970", "1970-10-07T00:00"),
                Arguments.of("Oct 7, 1970", "1970-10-07T00:00"),
                Arguments.of("OCT 7, 1970", "1970-10-07T00:00"),
                Arguments.of("7 oct 1970", "1970-10-07T00:00"),

                // leap second
                Arguments.of("2016-12-31T23:59:60Z", "2016-12-31T23:59:59"),

                // "at" announcing the time
                Arguments.of("Mar 21, 2024 at 10:15", "2024-03-21T10:15"),
                Arguments.of("oct 1, 1970 at 10:15", "1970-10-01T10:15"),
                Arguments.of("January 02, 2006 at 3:04pm MST-07", "2006-01-02T15:04"),
                Arguments.of("September 17, 2012 at 5:00pm UTC-05", "2012-09-17T17:00"),
                Arguments.of("2024-03-21 AT 10:15:30", "2024-03-21T10:15:30"),

                // zone name in parentheses closing the input
                Arguments.of("Tue, 11 Jul 2017 16:28:13 +0200 (CEST)", "2017-07-11T16:28:13"),
                Arguments.of(
                        "Fri Jul 03 2015 18:04:07 GMT+0100 (GMT Daylight Time)",
                        "2015-07-03T18:04:07"),
                Arguments.of(
                        "Jul 03 2015 18:04:07 GMT+0100 (GMT Daylight Time)", "2015-07-03T18:04:07"),

                // fraction of a second introduced by a fourth colon
                Arguments.of("2020-08-17T17:00:00:000+0100", "2020-08-17T17:00"),
                Arguments.of("2024-03-21 10:15:30:123", "2024-03-21T10:15:30.123"),

                // Go's monotonic clock reading
                Arguments.of(
                        "2017-04-03 22:32:14.009 +0300 MSK m=+0.000000001",
                        "2017-04-03T22:32:14.009"),
                Arguments.of(
                        "2024-03-21 10:15:30 +0000 UTC m=+0.000000001", "2024-03-21T10:15:30"),

                // a year beside a time, with no month or day
                Arguments.of("15:04:05 2008", "2008-01-01T15:04:05"),
                Arguments.of("17:57:51 -0700 2009", "2009-01-01T17:57:51"),
                Arguments.of("17:57:51 MST 2009", "2009-01-01T17:57:51"),
                Arguments.of("2024", "2024-01-01T00:00"),

                // colon as the date separator, with a time
                Arguments.of("2014:07:10 06:55:38.156283", "2014-07-10T06:55:38.156283"),
                Arguments.of("03:19:2012 10:11:59", "2012-03-19T10:11:59"),
                Arguments.of("171113 14:14:20", "2017-11-13T14:14:20"),
                Arguments.of("240321 10:15", "2024-03-21T10:15"),
                Arguments.of("2024年03月21日 10:15", "2024-03-21T10:15")
        );
    }

    static Stream<Arguments> timeCases() {
        return Stream.of(
                Arguments.of("0:0", "00:00"),
                Arguments.of("0:0:0", "00:00"),
                Arguments.of("00:00", "00:00"),
                Arguments.of("00:00:00", "00:00"),
                Arguments.of("00:00:00.0", "00:00"),
                Arguments.of("00:00:00.000000000", "00:00"),
                Arguments.of("10:15", "10:15"),
                Arguments.of("10:15:30", "10:15:30"),
                Arguments.of("10:15:30.123", "10:15:30.123"),
                Arguments.of("10:15:30,5", "10:15:30.500"),
                Arguments.of("9:5:3", "09:05:03"),
                Arguments.of("1:2", "01:02"),
                Arguments.of("1:2:3", "01:02:03"),
                Arguments.of("1:02:03.4", "01:02:03.400"),
                Arguments.of("23:59", "23:59"),
                Arguments.of("23:59:59", "23:59:59"),
                Arguments.of("23:59:59.999999999", "23:59:59.999999999"),
                Arguments.of("10:15:60", "10:15:59"),
                Arguments.of("2024-03-21T10:15:30", "10:15:30"),
                Arguments.of("2011-12-03T10:15:30.5", "10:15:30.500"),
                Arguments.of("21.03.2024", "00:00"),
                Arguments.of("21.03.2024 10:15", "10:15"),
                Arguments.of("May 8, 2009 5:57:51 PM", "17:57:51"),
                Arguments.of("T10:15:30", "10:15:30"),
                Arguments.of("18:31:59:257", "18:31:59.257"),
                Arguments.of("19:55:00+01", "19:55"),
                Arguments.of("19:55:00+0100", "19:55"),
                Arguments.of("19:55:00.799+0100", "19:55:00.799"),
                Arguments.of("22:18+0530", "22:18"),
                Arguments.of("15:04:05.99Z", "15:04:05.990"),
                Arguments.of("05:24:37 PM", "17:24:37"),
                Arguments.of("17:57:51 -07:00", "17:57:51"),
                Arguments.of("17:57:51 -0700 -07", "17:57:51"),
                Arguments.of("17:57:51 -0700 2009", "17:57:51"),
                Arguments.of("15:04:05 2008", "15:04:05")
        );
    }

    static Stream<Arguments> rejectCases() {
        return Stream.of(
                "not-a-date",
                "date",
                "hello world",
                "abc",
                "null",
                "NaN",
                "Infinity",
                "++++",
                "...",
                "////",
                "P1D",
                "PT1H",
                "2024-W12-3",
                "2024-085",

                // out-of-range calendar
                "2024-13-01",
                "2024-00-01",
                "2024-01-00",
                "2024-01-32",
                "2024-04-31",
                "2024-02-30",
                "2023-02-29",
                "2100-02-29",
                "1900-02-29",
                "21.13.2024",
                "21.00.2024",
                "32.01.2024",
                "00.01.2024",
                "99/99/9999",
                "00/00/0000",
                "13/13/2024",
                "May 32, 2009",
                "Feb 30, 2024",
                "Feb 29, 2023",
                "2024年13月01日",
                "2024年02月30日",

                // out-of-range time
                "25:00",
                "24:00",
                "24:00:00",
                "10:60",
                "10:61",
                "10:61:00",
                "10:15:61",
                "-1:00",
                "2024-03-21 25:00:00",
                "2024-03-21 10:60:00",
                "May 8, 2009 13:00:00 PM",

                // incomplete / mixed separators / garbage
                "03/2024",
                "171113",
                "171113 14",
                "1 2 34",
                "1 2 2024",
                "2024 3 21",
                "2024-03/21",
                "2024/03-21",
                "2024.03/21",
                "12345678",
                "1234567",
                "123456789",
                "0000-00-00",
                "2024-03-21 banana",
                "banana 2024-03-21",
                "2024-03-21T10:15:30+25:00",
                "2024-03-21T10:15:30+99:00",
                "2024-03-21T10:15:30[Nowhere/Land]",
                "2024-03-21T10:15:30[Not/AZone]"
        ).map(Arguments::of);
    }

    @Test
    void blankAndNullYieldNull() {
        assertNull(DateTimeUtil.tryParseLocalDate(null));
        assertNull(DateTimeUtil.tryParseLocalDate("   "));
        assertNull(DateTimeUtil.tryParseLocalDate("\t\n"));
        assertNull(DateTimeUtil.parseLocalDate(null));
        assertNull(DateTimeUtil.parseLocalDateTime(""));
        assertNull(DateTimeUtil.parseLocalDateTime("  "));
        assertNull(DateTimeUtil.parseLocalTime(" "));
        assertNull(DateTimeUtil.parseLocalTime("\t"));
        assertNull(DateTimeUtil.tryParseLocalDateTime(null));
        assertNull(DateTimeUtil.tryParseLocalTime(null));
    }

    @Test
    void dateOnlyInputHasNoTime() {
        assertNull(DateTimeUtil.tryParseLocalDate("10:15:30"));
        assertNull(DateTimeUtil.tryParseLocalDate("23:59:59.999999999"));
        assertNull(DateTimeUtil.tryParseLocalDateTime("10:15:30"));
        assertNull(DateTimeUtil.tryParseLocalDateTime("00:00"));
    }

    @Test
    void valueAboveTwelveIsAlwaysTheDay() {
        withLocale(Locale.US, () -> {
            assertEquals(LocalDate.of(2024, 12, 25), DateTimeUtil.parseLocalDate("25/12/2024"));
            assertEquals(LocalDate.of(2024, 12, 25), DateTimeUtil.parseLocalDate("12/25/2024"));
            assertEquals(LocalDate.of(2024, 3, 15), DateTimeUtil.parseLocalDate("3/15/2024"));
            assertEquals(LocalDate.of(2024, 1, 31), DateTimeUtil.parseLocalDate("1/31/2024"));
            assertEquals(LocalDate.of(2024, 1, 31), DateTimeUtil.parseLocalDate("31/1/2024"));
        });
        withLocale(Locale.UK, () -> {
            assertEquals(LocalDate.of(2024, 12, 25), DateTimeUtil.parseLocalDate("25/12/2024"));
            assertEquals(LocalDate.of(2024, 12, 25), DateTimeUtil.parseLocalDate("12/25/2024"));
            assertEquals(LocalDate.of(2024, 3, 15), DateTimeUtil.parseLocalDate("15/3/2024"));
        });
    }

    @Test
    void ambiguousNumericDateFollowsLocale() {
        withLocale(Locale.US, () -> {
            assertEquals(LocalDate.of(2024, 3, 4), DateTimeUtil.parseLocalDate("03/04/2024"));
            assertEquals(LocalDate.of(2024, 3, 4), DateTimeUtil.parseLocalDate("3/4/2024"));
            assertEquals(LocalDate.of(2003, 1, 2), DateTimeUtil.parseLocalDate("01/02/03"));
        });
        withLocale(Locale.UK, () -> {
            assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03/04/2024"));
            assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("3/4/2024"));
            assertEquals(LocalDate.of(2003, 2, 1), DateTimeUtil.parseLocalDate("01/02/03"));
        });
        withLocale(new Locale("ru"), () ->
                assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03/04/2024")));
        withLocale(Locale.GERMANY, () ->
                assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03/04/2024")));
        withLocale(Locale.FRANCE, () ->
                assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03/04/2024")));
    }

    @Test
    void dashSeparatedAmbiguousDateFollowsLocale() {
        withLocale(Locale.US, () ->
                assertEquals(LocalDate.of(2024, 3, 4), DateTimeUtil.parseLocalDate("03-04-2024")));
        withLocale(Locale.UK, () ->
                assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03-04-2024")));
    }

    @Test
    void colonSeparatedAmbiguousDateFollowsLocale() {
        withLocale(Locale.US, () -> {
            assertEquals(LocalDate.of(2006, 1, 2), DateTimeUtil.parseLocalDate("01:02:2006"));
            assertEquals(LocalDate.of(2014, 3, 1), DateTimeUtil.parseLocalDate("3:1:2014"));
            assertEquals(LocalDate.of(2014, 4, 8), DateTimeUtil.parseLocalDate("4:8:2014"));
        });
        withLocale(Locale.UK, () -> {
            assertEquals(LocalDate.of(2006, 2, 1), DateTimeUtil.parseLocalDate("01:02:2006"));
            assertEquals(LocalDate.of(2014, 1, 3), DateTimeUtil.parseLocalDate("3:1:2014"));
        });
    }

    /**
     * A colon-separated run of numbers is a date only when a four-digit year sits at one of its
     * ends; the shapes a time could also have stay a time.
     */
    @Test
    void colonSeparatedDateNeedsAFourDigitYear() {
        assertEquals(LocalTime.of(22, 43, 22), DateTimeUtil.parseLocalTime("22:43:22"));
        assertEquals(LocalTime.of(1, 2, 6), DateTimeUtil.parseLocalTime("1:2:06"));
        assertEquals(LocalTime.of(10, 13), DateTimeUtil.parseLocalTime("10:13"));
        assertNull(DateTimeUtil.tryParseLocalDate("22:43:22"));
        assertNull(DateTimeUtil.tryParseLocalDate("1:2:06"));
    }

    @Test
    void dotSeparatedDateStaysDayFirstInEveryLocale() {
        withLocale(Locale.US, () -> {
            assertEquals(LocalDate.of(1999, 2, 12), DateTimeUtil.parseLocalDate("12.02.1999"));
            assertEquals(LocalDate.of(2024, 4, 3), DateTimeUtil.parseLocalDate("03.04.2024"));
        });
        withLocale(Locale.UK, () ->
                assertEquals(LocalDate.of(1999, 2, 12), DateTimeUtil.parseLocalDate("12.02.1999")));
        withLocale(new Locale("ru"), () ->
                assertEquals(LocalDate.of(1999, 2, 12), DateTimeUtil.parseLocalDate("12.02.1999")));
        withLocale(Locale.GERMANY, () ->
                assertEquals(LocalDate.of(1999, 2, 12), DateTimeUtil.parseLocalDate("12.02.1999")));
    }

    /**
     * Two-digit years fall into the window that starts 80 years back, so the year just before that
     * window belongs to the next century.
     */
    @Test
    void twoDigitYearUsesSlidingWindow() {
        int thisYear = LocalDate.now().getYear();
        assertEquals(thisYear - 1, parseTwoDigitYear(thisYear - 1));
        assertEquals(thisYear - 80, parseTwoDigitYear(thisYear - 80));
        assertEquals(thisYear + 19, parseTwoDigitYear(thisYear - 81));
        assertEquals(thisYear, parseTwoDigitYear(thisYear));
        assertEquals(thisYear + 1, parseTwoDigitYear(thisYear + 1));
    }

    private static int parseTwoDigitYear(int year) {
        return DateTimeUtil.parseLocalDate("21.03." + String.format("%02d", Math.floorMod(year, 100))).getYear();
    }

    @Test
    void monthNamesOfOtherLanguagesAreRecognised() {
        assertEquals(LocalDate.of(2024, 1, 21), DateTimeUtil.parseLocalDate("21 Januar 2024"));
        assertEquals(LocalDate.of(2024, 2, 21), DateTimeUtil.parseLocalDate("21 février 2024"));
        assertEquals(LocalDate.of(2024, 3, 21), DateTimeUtil.parseLocalDate("21 März 2024"));
        assertEquals(LocalDate.of(2024, 8, 21), DateTimeUtil.parseLocalDate("21 août 2024"));
        assertEquals(LocalDate.of(2024, 1, 21), DateTimeUtil.parseLocalDate("21 enero 2024"));
        assertEquals(LocalDate.of(2024, 5, 21), DateTimeUtil.parseLocalDate("21 maggio 2024"));
    }

    @Test
    void parseMethodsThrowWithTrimmedMessage() {
        IllegalArgumentException date = assertThrows(
                IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDate("  bad  "));
        assertEquals("Unparseable date: 'bad'", date.getMessage());

        IllegalArgumentException dateTime = assertThrows(
                IllegalArgumentException.class, () -> DateTimeUtil.parseLocalDateTime("  bad  "));
        assertEquals("Unparseable date-time: 'bad'", dateTime.getMessage());

        IllegalArgumentException time = assertThrows(
                IllegalArgumentException.class, () -> DateTimeUtil.parseLocalTime("  bad  "));
        assertEquals("Unparseable time: 'bad'", time.getMessage());
    }

    @Test
    void tryParseMatchesParseOnSuccess() {
        String[] samples = {
                "2024-03-21",
                "21.03.2024 10:15:30",
                "May 8, 2009 5:57:51 PM",
                "21 марта 2024",
                "20240321T101530Z"
        };
        for (String sample : samples) {
            assertEquals(DateTimeUtil.parseLocalDate(sample), DateTimeUtil.tryParseLocalDate(sample));
            assertEquals(DateTimeUtil.parseLocalDateTime(sample), DateTimeUtil.tryParseLocalDateTime(sample));
            assertEquals(DateTimeUtil.parseLocalTime(sample), DateTimeUtil.tryParseLocalTime(sample));
        }
    }

    @Test
    void epochUnitComesFromTheNumberOfDigits() {
        assertEquals(Instant.ofEpochSecond(1332151919), DateTimeUtil.tryParseEpoch("1332151919"));
        assertEquals(Instant.ofEpochMilli(1499979795437L), DateTimeUtil.tryParseEpoch("1499979795437"));
        assertEquals(
                Instant.ofEpochSecond(1499979795, 437_000_000),
                DateTimeUtil.tryParseEpoch("1499979795437000"));
        assertEquals(
                Instant.ofEpochSecond(1499979655, 583_057_426),
                DateTimeUtil.tryParseEpoch("1499979655583057426"));
        assertEquals(Instant.EPOCH, DateTimeUtil.tryParseEpoch("0000000000"));
        assertEquals(Instant.ofEpochSecond(1332151919), DateTimeUtil.tryParseEpoch("  1332151919 "));
    }

    /**
     * Only the four lengths a real epoch has are accepted; everything else, including the lengths
     * that are calendar dates and the lengths dateparse silently turns into an epoch of zero, is
     * rejected.
     */
    @Test
    void onlyEpochLengthsAreEpochs() {
        String[] rejected = {
                null, "", "   ", "2024", "20140601", "20180722105203",
                "13321519", "133215191", "13321519190", "133215191900",
                "14999797954370", "149997979543700", "14999796555830574", "149997965558305742",
                "14999796555830574260", "9999999999999999999",
                "-1332151919", "13321519.19", "1332151919x"
        };
        for (String text : rejected) {
            assertNull(DateTimeUtil.tryParseEpoch(text), text);
        }
    }

    @Test
    void epochIsReadOnlyByTheZoneAwareOverload() {
        assertNull(DateTimeUtil.tryParseLocalDate("1332151919"));
        assertNull(DateTimeUtil.tryParseLocalDateTime("1332151919"));
        assertNull(DateTimeUtil.tryParseLocalTime("1332151919"));

        assertEquals(
                LocalDateTime.of(2012, 3, 19, 10, 11, 59),
                DateTimeUtil.tryParseLocalDateTime("1332151919", ZoneOffset.UTC));
        // Moscow kept UTC+4 all year between 2011 and 2014
        assertEquals(
                LocalDateTime.of(2012, 3, 19, 14, 11, 59),
                DateTimeUtil.tryParseLocalDateTime("1332151919", ZoneId.of("Europe/Moscow")));
    }

    /**
     * The zone argument applies to an epoch only; every other shape is still returned exactly as
     * written, so the overload agrees with {@link DateTimeUtil#tryParseLocalDateTime(String)}.
     */
    @Test
    void zoneAwareOverloadDoesNotShiftOrdinaryInput() {
        String[] samples = {
                "2024-03-21T10:15:30",
                "2024-03-21T10:15:30+03:00",
                "2024-03-21T10:15:30Z",
                "21.03.2024 10:15:30",
                "20140601",
                "20180722105203",
                "not-a-date"
        };
        for (String sample : samples) {
            assertEquals(
                    DateTimeUtil.tryParseLocalDateTime(sample),
                    DateTimeUtil.tryParseLocalDateTime(sample, ZoneId.of("Asia/Tokyo")),
                    sample);
        }
    }

    private static void withLocale(Locale locale, Runnable action) {
        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, locale);
        try {
            action.run();
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }
}
