package com.vthr.erp_hrm.infrastructure.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Accepts ZonedDateTime strings with or without zone/offset.
 *
 * Supported examples:
 * - 2026-05-29T08:00
 * - 2026-05-29T08:00:00
 * - 2026-05-29T08:00:00.123
 * - 2026-05-29T08:00+07:00
 * - 2026-05-29T01:00:00Z
 */
public class LenientZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    private static final DateTimeFormatter LOCAL_DATE_TIME_FLEX = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    private final ZoneId defaultZone;

    public LenientZonedDateTimeDeserializer(ZoneId defaultZone) {
        this.defaultZone = defaultZone;
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString();
        if (raw == null)
            return null;

        String text = raw.trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text))
            return null;

        DateTimeParseException last = null;

        try {
            return ZonedDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            last = ex;
        }

        try {
            return OffsetDateTime.parse(text).toZonedDateTime();
        } catch (DateTimeParseException ex) {
            last = ex;
        }

        try {
            LocalDateTime local = LocalDateTime.parse(text, LOCAL_DATE_TIME_FLEX);
            return local.atZone(defaultZone);
        } catch (DateTimeParseException ex) {
            last = ex;
        }

        throw JsonMappingException.from(p, "Invalid date-time value for ZonedDateTime: '" + raw + "'", last);
    }
}
