package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import org.apache.commons.lang.StringUtils;
import play.data.binding.As;
import play.data.binding.Binder;
import play.i18n.Lang;
import play.libs.I18N;
import play.mvc.Scope;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class ValidDateCheck extends AbstractAnnotationCheck<ValidDate> {

    final static String mes = "validation.date";
    String[] values;
    String[] langs;

    @Override
    public void configure(ValidDate date) {
        setMessage(date.message());
        this.values = date.value();
        this.langs = date.lang();
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        value = Validation.willBeValidated(value);
        //bind(String name, Class<?> clazz, Type type, Annotation[] annotations, Map<String, String[]> params)

        if (value == null || value.toString().length() == 0) {
            return false;
        }
        try {
            getDate(values, langs, value.toString());
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * It can be something like As(lang={"fr,de","*"}, value={"dd-MM-yyyy","MM-dd-yyyy"})
     *
     * @param annotations
     * @param value
     * @return null if it cannot be converted because there is no annotation.
     * @throws java.text.ParseException
     */
    public static java.util.Date getDate(String[] values, String[] langs, String value) throws ParseException {
        // Look up for the BindAs annotation
        Locale locale = Lang.getLocale();
        String format = values[0];
        if (!StringUtils.isEmpty(format)) {
            // This can be comma separated
            Tuple tuple = getLocale(langs);
            if (tuple != null) {
                // Avoid NPE and get the last value if not specified
                format = values[tuple.index < values.length ? tuple.index : values.length - 1];
                locale = tuple.locale;
            }
        }
        if (StringUtils.isEmpty(format)) {
            format = I18N.getDateFormat();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        sdf.setLenient(false);
        return sdf.parse(value);

    }

    public static String getFormatForLangs(String[] langs, String values[]) {
        String format = values[0];
        if (!StringUtils.isEmpty(format)) {
            // This can be comma separated
            Tuple tuple = getLocale(langs);
            if (tuple != null) {
                // Avoid NPE and get the last value if not specified
                return values[tuple.index < values.length ? tuple.index : values.length - 1];
            }
        }
        if (StringUtils.isEmpty(format)) {
            return I18N.getDateFormat();
        }
        return format;
    }

    public static Tuple getLocale(String[] langs) {
        int i = 0;
        for (String l : langs) {
            String[] commaSeparatedLang = l.split(",");
            for (String lang : commaSeparatedLang) {
                if (Lang.get().equals(lang) || "*".equals(lang)) {
                    Locale locale = null;
                    if ("*".equals(lang)) {
                        locale = Lang.getLocale();
                    }
                    if (locale == null) {
                        locale = Lang.getLocale(lang);
                    }
                    if (locale != null) {
                        return new Tuple(i, locale);
                    }
                }
            }
            i++;
        }
        return null;
    }

    /**
     * Contains the index of the locale inside the @As
     */
    private static class Tuple {

        public int index = -1;
        public Locale locale;

        public Tuple(int index, Locale locale) {
            this.locale = locale;
            this.index = index;
        }
    }
}
