package controllers;

import com.google.gson.Gson;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
import play.Logger;
import play.data.validation.Error;
import play.exceptions.ActionNotFoundException;
import play.i18n.Messages;
import play.mvc.ActionInvoker;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.utils.Java;

import java.lang.annotation.Annotation;
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
        for (int i = 0; i < paramsNames.length; i++) {
            String arg = paramsNames[i];
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
                        break;
                    }
                }
            } else {
                type.getDeclaredField(f).set(validatedInstance, value);

                final play.data.validation.Validation.ValidationResult result = validation.valid(validatedInstance);
                final List<play.data.validation.Error> errors = validation.errorsMap().get("validatedInstance." + f);
                if (errors != null && errors.size() > 0) {
                    // Use a json array instead...
                    renderText(errors.get(0).message());
                }
            }
        }

        renderText("");
    }

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

        Map<String, List<String>> newMap = new HashMap<String, List<String>>();

        for (int i = 0; i < paramsNames.length; i++) {
            final String arg = paramsNames[i];
            final Class type = actionMethod.getParameterTypes()[i];
            map.putAll(play.data.validation.Validation.getValidators(type, arg));

            for (String key : map.keySet()) {
                List<String> list = new ArrayList<String>();
                for (play.data.validation.Validation.Validator validator : map.get(key)) {
                    String value = validator.annotation.annotationType().getSimpleName().toLowerCase();
                    list.add(value);
                }
                newMap.put(key, list);
            }

        }
        // Same for the messages
        // messages:{required:'Please enter your email address', email:'Please enter a valid email address'}
        Map<String, List<String>> errorMap = new HashMap<String, List<String>>();
        for (int i = 0; i < paramsNames.length; i++) {
            final String arg = paramsNames[i];
            final Class type = actionMethod.getParameterTypes()[i];
            Map<String, List<play.data.validation.Validation.Validator>> validators = play.data.validation.Validation.getValidators(type, arg);


            for (String key : validators.keySet()) {
                List<String> errors = new ArrayList<String>();
                Logger.info(" = " + key);
                for (play.data.validation.Validation.Validator validator : map.get(key)) {
                    errors.add(Messages.get(validator.annotation.annotationType().getDeclaredMethod("message").invoke(validator.annotation) + ""));
                }
                errorMap.put(key, errors);
            }


        }

        rootMap.put("rules", newMap);
        rootMap.put("messages", errorMap);

        Logger.info(" - " + new Gson().toJson(rootMap));

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


}