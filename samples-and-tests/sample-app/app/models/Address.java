import play.data.validation.Required;

public class Address {
   
    @Required
    public String fullName;
    @Required
    public String address1;
    public String address2;
    @Required
    public String city;
    @Required
    public String postcode;
    @Required
    public String country;
    
}