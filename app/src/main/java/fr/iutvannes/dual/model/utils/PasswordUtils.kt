package fr.iutvannes.dual.model.utils

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordUtils {

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hashed: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hashed)
        return result.verified
    }

    fun isValid(password: String): Boolean {
        if(password.length >= 8 && password.contains(Regex("[a-z]")) && password.contains(Regex("[A-Z]")) && password.contains(Regex("[0-9]"))){
            return true
        } else {
            return false
        }
    }
}
