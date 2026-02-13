package fr.iutvannes.dual.model.utils

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * A utility tool for hashing and verifying a password.
 */
object PasswordUtils {

    /**
     * Hash a password
     *
     * @param password password to hash
     * @return hashed password
     */
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    /**
     * Checks if a password matches a hash
     *
     * @param password password to check
     * @param hashed hashed password
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(password: String, hashed: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hashed)
        return result.verified
    }

    /**
     * Checks if a password is valid
     *
     * @param password password to check
     * @return true if the password is valid, false otherwise
     */
    fun isValid(password: String): Boolean {
        if(password.length >= 8 && password.contains(Regex("[a-z]")) && password.contains(Regex("[A-Z]")) && password.contains(Regex("[0-9]"))){
            return true
        } else {
            return false
        }
    }
}
