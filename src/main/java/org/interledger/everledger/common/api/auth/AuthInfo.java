package org.interledger.everledger.common.api.auth;


public class AuthInfo { // TODO:(0) Recheck. Split in web users and ledger accounts
    final String id;
    final String name;
    final String pass;
    final String roll;

    public static final AuthInfo ANONYMOUS = new AuthInfo("","","", "none");
    
    public AuthInfo(String id, String name, String pass, String roll) {
        this.id = id;
        this.name = name;
        this.pass = pass;
        this.roll = roll;
    }

    public String getUsername() {
        return name;
    }

    public String getRoll() {
        return roll;
    }

    @Override
    public String toString() {
        return pass;
    }

}