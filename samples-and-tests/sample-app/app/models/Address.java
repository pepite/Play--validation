package models;

import play.data.validation.Required;
import play.data.validation.Email;
import play.data.validation.ValidNumber;
import play.data.validation.Match;

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
    @Match(value = "^\\d{4}$", message = "Invalid postcode")
    public String postcode;
    @Required
    public String country;
    @ValidNumber @Required
    public String number;
    
}