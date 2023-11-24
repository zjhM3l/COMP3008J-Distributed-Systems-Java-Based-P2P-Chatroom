package model;

/**
 *
 * @author Jiahe_Zhang
 *  The user's model definition
 */

public class User {
    private String name;    //User name

    public User(String userDescription) {
        String items[] = userDescription.split("%");  //Split the string with a %.
        this.name = items[0];    //The first part is assigned to the user name
    }

    public String getName() {
        return name;
    }

    public String description() {
        return name;
    }
}
