package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class NumberCheck extends AbstractAnnotationCheck<Number> {

    final static String mes = "validation.number";

    @Override
    public void configure(Number email) {
        setMessage(email.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        value = play.data.validation.Validation.willBeValidated(value);
        if (value == null || value.toString().length() == 0) {
            return false;
        }
        try {
            Integer.valueOf(value.toString());
        } catch(Throwable e) {
            return false;
        }
        return true;
    }
   
}
