package com.telemetria.integration.sefaz.cte.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Valor para cálculo do SHA-256 não pode ser nulo."
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result =
                    new StringBuilder(64);

            for (byte b : hash) {

                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 não está disponível na JVM.",
                    e
            );
        }
    }
}
