package controllers;

import play.mvc.*;
import models.Address;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void index2() {
        render();
    }

    public static void submit(Address address) {
        index();
    }

}