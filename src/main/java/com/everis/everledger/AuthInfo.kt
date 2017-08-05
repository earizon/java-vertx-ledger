package com.everis.everledger


data class AuthInfo(val id: String, val name: String, val pass: String, val roll: String // TODO:(1) Create ENUM
) {

    val isAdmin: Boolean
        get() = "admin" == roll

    val isConnector: Boolean
        get() = "connector" == roll

    override fun toString() = id

    companion object {
        val ANONYMOUS = AuthInfo("", "", "", "none")
    }
}