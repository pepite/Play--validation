package models;

import play.data.validation.Required;
import play.data.validation.Email;
import play.data.validation.Number;
    
public class Address {
   
    @Required( message =  "Please enter your name!")
    public String fullName;
    @Email
    public String email;
    @Required ( message =  "Please enter your address!")
    public String address1;
    public String address2;
    @Required
    public String city;
    @Required
    public String postcode;
    @Required
    public String country;
    @Number
    public String number;
    
}