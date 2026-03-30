package com.app.telemetria;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class TelemetriaApplicationTests {

    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> {
            Class.forName("com.app.telemetria.TelemetriaApplication");
        });
    }
}