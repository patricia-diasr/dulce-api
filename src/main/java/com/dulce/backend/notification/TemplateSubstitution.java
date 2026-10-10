package com.dulce.backend.notification;

import java.util.Map;

public final class TemplateSubstitution {

    private TemplateSubstitution() {}

    public static String apply(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
