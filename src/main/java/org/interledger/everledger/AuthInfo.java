package org.interledger.everledger.common.api.auth;


public class AuthInfo {
    final String id;
    final String name;
    final String pass;
    final String roll; // TODO:(0) Create ENUM 

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