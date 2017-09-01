package com.everis.everledger

enum class AccessRoll {
    ADMIN,
    CONNECTOR,
    USER,
    NONE

}

// TODO:(1) Change pass by hashOfPass + salt + ...
data class AuthInfo( val id: String, val login: String, val pass: String, val roll: AccessRoll ) {

    val isAdmin: Boolean
        get() = roll == AccessRoll.ADMIN

    val isConnector: Boolean
        get() = roll == AccessRoll.ADMIN

    override fun toString() = id

    companion object {
        val ANONYMOUS = AuthInfo("", "", "", AccessRoll.NONE)
    }
}