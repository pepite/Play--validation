package controllers;

import com.google.gson.Gson;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.data.validation.*;
import play.exceptions.ActionNotFoundException;
import play.i18n.Messages;
import play.mvc.*;
import play.utils.Java;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.data.validation.Error;

public class Validation extends Controller {



    // TODO: manage bla.bli.property
    // TODO: Throws Exception?
    public static void validate(String formAction, String name, String value, String method) throws Exception {
        final Http.Request request = new Http.Request();
        // If name is empty look into the params to see if we have a match
        if (StringUtils.isEmpty(name)) {
            Map<String, String[]> params = Scope.Params.current().all();
            for (String key : params.keySet()) {
                if (!key.equals("formAction") && !key.equals("method") && !key.equals("body")) {
                    name = key;
                    value = params.get(key)[0];
                    break;
                }
            }
        }

        // Make sure we don't have the query string
        if (formAction.startsWith("@")) {
            request.action = formAction.substring(1);
        } else {
            if (formAction.indexOf("?") != -1) {
                formAction = formAction.substring(0, formAction.indexOf("?"));
            }

            request.path = formAction;
            // TODO: Be sure it is a post
            if (method != null) {
                request.method = method.toUpperCase();
            } else {
                request.method = "POST";
            }
            // The route method modify the request object.
            final Router.Route route = Router.route(request);
        }

        final Object[] ca = ActionInvoker.getActionMethod(request.action);
        final Method actionMethod = (Method) ca[1];

        final String[] paramsNames = Java.parameterNames(actionMethod);
        String className = name;
        String f = name;
        // It should be only one parameters
        int pos = -1;
        String arg = "";
        for (int i = 0; i < paramsNames.length; i++) {
            arg = paramsNames[i];
            if (name.indexOf(".") > 0) {
                className = name.substring(0, name.indexOf("."));
            }
            if (name.startsWith(arg + ".") && className.equals(arg)) {
                pos = i;
                f = name.substring(name.lastIndexOf(".") + 1);
                break;
            } else if (className.equals(arg)) {
                pos = i;
                f = className;
                break;
            }

        }

        if (pos != -1) {
            final Class type = actionMethod.getParameterTypes()[pos];
            Object validatedInstance = type.newInstance();

            if (f.equals(className)) {
                validatedInstance = value;
                for (Annotation[] annotations : actionMethod.getParameterAnnotations()) {
                    if (annotations.length > 0) {
                        List<ConstraintViolation> violations = new Validator().validateAction(actionMethod);
                        String[] paramNames = Java.parameterNames(actionMethod);
                        for (ConstraintViolation violation : violations) {
                            // TODO: Use a json array instead...
                            renderText(message(paramNames[((MethodParameterContext) violation.getContext()).getParameterIndex()], violation.getMessage(), violation.getMessageVariables() == null ? new String[0] : violation.getMessageVariables().values().toArray(new String[0])));
                        }

                    }
                }
            } else {
                type.getDeclaredField(f).set(validatedInstance, value);
                //final play.data.validation.Validation.ValidationResult result = validation.valid(validatedInstance);
                final List<play.data.validation.Error> errors = validation.errorsMap().get("validatedInstance." + f);
                if (errors != null && errors.size() > 0) {
                    // Use a json array instead...
                    renderText(errors.get(0).message());
                }
            }
        }

        renderText("true");
    }

    // TODO: manage bla.bli.property
    // TODO: Throws Exception?
    public static String getValidators(String formAction, String method) throws Exception {
        final Http.Request request = new Http.Request();
        // Make sure we don't have the query string
        if (formAction.startsWith("@")) {
            request.action = formAction.substring(1);
        } else {
            if (formAction.indexOf("?") != -1) {
                formAction = formAction.substring(0, formAction.indexOf("?"));
            }

            request.path = formAction;
            // TODO: Be sure it is a post
            request.method = method.toUpperCase();
            // The route method modify the request object.
            Router.route(request);
        }

        final Object[] ca = ActionInvoker.getActionMethod(request.action);
        final Method actionMethod = (Method) ca[1];

        final String[] paramsNames = Java.parameterNames(actionMethod);
        // It should be only one parameters
        Map<String, List<play.data.validation.Validation.Validator>> map = new HashMap<String, List<play.data.validation.Validation.Validator>>();
        Map<String, Object> rootMap = new HashMap<String, Object>();

        Map<String, Map<String, Object>> newMap = new HashMap<String, Map<String, Object>>();

        for (int i = 0; i < paramsNames.length; i++) {
            final String arg = paramsNames[i];
            final Class type = actionMethod.getParameterTypes()[i];
            map.putAll(play.data.validation.Validation.getValidators(type, arg));

            for (String key : map.keySet()) {
                Map<String, Object> values = new HashMap<String, Object>();
                for (play.data.validation.Validation.Validator validator : map.get(key)) {
                    String name = validator.annotation.annotationType().getSimpleName().toLowerCase();
                    Object value = Boolean.TRUE;
                    try {
                        value = validator.annotation.annotationType().getDeclaredMethod("value").invoke(validator.annotation).toString();
                        // If we are on an object
                        // This is for equals(). TODO: Check what can be done for others
                        if (name.equals("equals")) {
                            name = "equalTo";
                            value = "[name='" + key.substring(0, key.lastIndexOf(".")) + "." + value + "']";
                        } else if (name.equals("checkwith")) {
                            // We use the remote 
                            // remote: "/"
                            name = "remote";
                            // Get the name
                            value = "/@validation/validate?formAction=" + formAction + "&method=" + method;
                        } else if (name.equals("minsize")) {
                            name = "min";
                            value = ((MinSize) validator.annotation).value();
                        } else if (name.equals("maxsize")) {
                            value = ((MaxSize) validator.annotation).value();
                        } else if (name.equals("range")) {
                            value = "[" + ((Range) validator.annotation).min() + ", " + ((Range) validator.annotation).max() + "]";
                        } else if (name.equals("required")) {
                            value = "true";
                        } else if (name.equals("number")) {
                            // TODO: add support for float, digit, currency, etc by adding format
                            name = "digits";
                            value = "true";
                        } else if (name.equals("url")) {
                            value = "true";
                        } else if (name.equals("date")) {
                            // TODO: add format to the date on the js side
                            String format = ValidDateCheck.getFormatForLangs(((ValidDate) validator.annotation).lang(), ((ValidDate) validator.annotation).value());
                            name = "date";
                            value = "true";
                        } else if (name.equals("match")) {
                            value = ((Match) validator.annotation).value();
                        }
                    } catch (Exception e) {
                    }
                    values.put(name, value);

                }
                newMap.put(key, values);
            }

        }
        // Same for the messages
        // messages:{required:'Please enter your email address', email:'Please enter a valid email address'}
        Map<String, Map<String, String>> errorMap = new HashMap<String, Map<String, String>>();
        for (int i = 0; i < paramsNames.length; i++) {
            final String arg = paramsNames[i];
            final Class type = actionMethod.getParameterTypes()[i];
            Map<String, List<play.data.validation.Validation.Validator>> validators = play.data.validation.Validation.getValidators(type, arg);


            for (String key : validators.keySet()) {
                Map<String, String> errors = new HashMap<String, String>();
                for (play.data.validation.Validation.Validator validator : map.get(key)) {
                    String name = validator.annotation.annotationType().getSimpleName().toLowerCase();
                    if (name.equals("equals")) {
                        name = "equalTo";
                    } else if (name.equals("checkwith")) {
                        // We use the remote
                        // remote: "/"
                        name = "remote";
                    } else if (name.equals("minsize")) {
                        name = "min";
                    } else if (name.equals("maxsize")) {
                        name = "max";
                    } else if (name.equals("number")) {
                        // TODO: add support for float, digit, currency, etc by adding format
                        name = "digits";
                    }
                    errors.put(name, Messages.get(validator.annotation.annotationType().getDeclaredMethod("message").invoke(validator.annotation).toString()));
                }
                errorMap.put(key, errors);
            }


        }

        rootMap.put("rules", newMap);
        rootMap.put("messages", errorMap);

        return new Gson().toJson(rootMap);
    }

    private static String message(String key, String message, String[] variables) {
        key = Messages.get(key);
        Object[] args = new Object[variables.length + 1];
        System.arraycopy(variables, 0, args, 1, variables.length);
        args[0] = key;
        return Messages.get(message, (Object[]) args);
    }

    static class Validator extends Guard {

        public List<ConstraintViolation> validateAction(Method actionMethod) throws Exception {
            List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
            Object instance = null;
            // Patch for scala defaults
            if (!Modifier.isStatic(actionMethod.getModifiers()) && actionMethod.getDeclaringClass().getSimpleName().endsWith("$")) {
                try {
                    instance = actionMethod.getDeclaringClass().getDeclaredField("MODULE$").get(null);
                } catch (Exception e) {
                    throw new ActionNotFoundException(Http.Request.current().action, e);
                }
            }
            Object[] rArgs = ActionInvoker.getActionMethodArgs(actionMethod, instance);
            validateMethodParameters(null, actionMethod, rArgs, violations);
            validateMethodPre(null, actionMethod, rArgs, violations);
            return violations;
        }
    }



 // ~~~~ Integration helper
    public static Map<String, List< play.data.validation.Validation.Validator>> getValidators(Class<?> clazz, String name) {
        Map<String, List< play.data.validation.Validation.Validator>> result = new HashMap<String, List< play.data.validation.Validation.Validator>>();
        searchValidator(clazz, name, result);
        return result;
    }

    public static List< play.data.validation.Validation.Validator> getValidators(Class<?> clazz, String property, String name) {
        try {
            List< play.data.validation.Validation.Validator> validators = new ArrayList< play.data.validation.Validation.Validator>();
            while (!clazz.equals(Object.class)) {
                try {
                    Field field = clazz.getDeclaredField(property);
                    for (Annotation annotation : field.getDeclaredAnnotations()) {
                        if (annotation.annotationType().getName().startsWith("play.data.validation")) {
                            play.data.validation.Validation.Validator validator = new play.data.validation.Validation.Validator(annotation);
                            validators.add(validator);
                            if (annotation.annotationType().equals(Equals.class)) {
                                validator.params.put("equalsTo", name + "." + ((Equals) annotation).value());
                            }
                            if (annotation.annotationType().equals(InFuture.class)) {
                                validator.params.put("reference", ((InFuture) annotation).value());
                            }
                            if (annotation.annotationType().equals(InPast.class)) {
                                validator.params.put("reference", ((InPast) annotation).value());
                            }
                        }
                    }
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            return validators;
        } catch (Exception e) {
            return new ArrayList< play.data.validation.Validation.Validator>();
        }
    }

    static void searchValidator(Class<?> clazz, String name, Map<String, List< play.data.validation.Validation.Validator>> result) {
        while (!clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {

                List< play.data.validation.Validation.Validator> validators = new ArrayList< play.data.validation.Validation.Validator>();
                String key = name + "." + field.getName();
                boolean containsAtValid = false;
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().getName().startsWith("play.data.validation")) {
                        play.data.validation.Validation.Validator validator = new play.data.validation.Validation.Validator(annotation);
                        validators.add(validator);
                        if (annotation.annotationType().equals(Equals.class)) {
                            validator.params.put("equalsTo", name + "." + ((Equals) annotation).value());
                        }
                        if (annotation.annotationType().equals(InFuture.class)) {
                            validator.params.put("reference", ((InFuture) annotation).value());
                        }
                        if (annotation.annotationType().equals(InPast.class)) {
                            validator.params.put("reference", ((InPast) annotation).value());
                        }

                    }
                    if (annotation.annotationType().equals(Valid.class)) {
                        containsAtValid = true;
                    }
                }
                if (!validators.isEmpty()) {
                    result.put(key, validators);
                }
                if (containsAtValid) {
                    searchValidator(field.getType(), key, result);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

}