package com.dulce.backend.notification;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateHtmlSanitizer {

    private static final PolicyFactory POLICY =
            new HtmlPolicyBuilder()
                    .allowElements(
                            "p", "br", "h3", "h4", "strong", "b", "em", "i", "ul", "ol", "li")
                    .toFactory();

    public String sanitize(String html) {
        return html == null ? null : POLICY.sanitize(html);
    }
}
