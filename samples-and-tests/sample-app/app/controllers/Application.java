package controllers;

import play.mvc.*;
import models.Address;
import play.data.validation.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void index2() {
        render();
    }

    public static void submit(@Valid Address address) {
         if(validation.hasErrors()) {
           params.flash(); // add http parameters to the flash scope
           validation.keep(); // keep the errors for the next request
           index2();
        }
        index();
    }

}