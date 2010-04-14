package play.modules.validation;


import play.PlayPlugin;
import play.mvc.Router;

public class ValidationPlugin extends PlayPlugin {


    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@validation/validate", "Validation.validate");
    }
}
