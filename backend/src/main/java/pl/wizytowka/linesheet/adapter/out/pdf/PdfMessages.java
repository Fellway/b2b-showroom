package pl.wizytowka.linesheet.adapter.out.pdf;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Etykiety dokumentu z pakietów {@code pdf/messages/line-sheet*.properties} (UTF-8). Pakiet bazowy jest polski;
 * nie ma zapasowego przejścia do domyślnego locale JVM, więc wynik nie zależy od maszyny, na której działa serwer.
 */
final class PdfMessages implements IMessageResolver {

    private static final String BUNDLE = "pdf.messages.line-sheet";
    private static final ResourceBundle.Control NO_JVM_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    static String get(Locale locale, String key) {
        return ResourceBundle.getBundle(BUNDLE, locale, PdfMessages.class.getClassLoader(), NO_JVM_FALLBACK).getString(key);
    }

    @Override
    public String getName() {
        return "line-sheet-messages";
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        try {
            String pattern = get(context.getLocale(), key);
            return messageParameters == null || messageParameters.length == 0
                    ? pattern
                    : new MessageFormat(pattern, context.getLocale()).format(messageParameters);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key,
                                                    Object[] messageParameters) {
        return "??" + key + "??";
    }
}
