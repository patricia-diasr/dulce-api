package com.dulce.backend.ticket;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

final class PdfText {

    private static final Charset WIN_1252 = Charset.forName("windows-1252");

    private PdfText() {}

    static String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        CharsetEncoder encoder = WIN_1252.newEncoder();
        StringBuilder result = new StringBuilder();
        raw.strip()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .codePoints()
                .forEach(
                        codePoint -> {
                            if (codePoint == '\n') {
                                result.append('\n');
                            } else if (Character.isISOControl(codePoint)) {
                                result.append(' ');
                            } else if (Character.isBmpCodePoint(codePoint)
                                    && encoder.canEncode((char) codePoint)) {
                                result.appendCodePoint(codePoint);
                            } else {
                                result.append('?');
                            }
                        });
        return result.toString();
    }

    static String orEmpty(String raw) {
        String cleaned = clean(raw);
        return cleaned == null ? "" : cleaned;
    }
}
