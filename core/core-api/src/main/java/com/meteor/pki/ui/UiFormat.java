package com.meteor.pki.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component("uiFormat")
public class UiFormat {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    public String instant(Instant instant) {
        return instant == null ? "-" : FORMAT.format(instant);
    }

}
