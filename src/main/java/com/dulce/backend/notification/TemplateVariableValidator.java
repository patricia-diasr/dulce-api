package com.dulce.backend.notification;

import com.dulce.backend.common.exception.BadRequestException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplateVariableValidator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private static final Set<String> KNOWN_VARIABLES =
            Set.of(
                    "customerName",
                    "orderId",
                    "orderStatus",
                    "pickupDate",
                    "pickupTime",
                    "orderTotal",
                    "orderItems",
                    "discountNotice",
                    "orderNotesNotice",
                    "refundNotice",
                    "paymentStatus",
                    "amountPaid");

    public void validate(String text) {
        if (text == null) {
            return;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (!KNOWN_VARIABLES.contains(variable)) {
                throw new BadRequestException("Variável desconhecida: {{" + variable + "}}.");
            }
        }
    }
}
