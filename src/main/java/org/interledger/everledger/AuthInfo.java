package org.interledger.everledger;


public class AuthInfo {
    final public String id;
    final public String name;
    final public String pass;
    final public String roll; // TODO:(1) Create ENUM 

    public static final AuthInfo ANONYMOUS = new AuthInfo("","","", "none");
    
    public AuthInfo(String id, String name, String pass, String roll) {
        this.id = id;
        this.name = name;
        this.pass = pass;
        this.roll = roll;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRoll() {
        return roll;
    }

    public boolean isAdmin() {
        return "admin".equals(roll);
    }

    public boolean isConnector() {
        return "connector".equals(roll);
    }

    @Override
    public String toString() {
        return pass;
    }
    
    

}