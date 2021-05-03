package cn.cercis.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * Checks if a string is strong enough as a password.
 *
 * Length at least 8, at most 20
 * Contains at least 3/4 of the following items:
 *  - Uppercase letters
 *  - Lowercase letters
 *  - Numbers
 *  - Symbols
 * Contains only:
 *  - Alphabetic letters [a-zA-Z]
 *  - Digits [0-9]
 *  - Symbols [PasswordChecker.ALLOWED_SYMBOLS]
 */
class PasswordChecker(val password: LiveData<String>) {
    data class PasswordValidationResult(
            val emptyOrValid: Boolean,
            val valid: Boolean,
            val ruleLength: Boolean,
            val ruleAllowedCharacters: Boolean,
            val ruleUpperCase: Boolean,
            val ruleLowerCase: Boolean,
            val ruleDigit: Boolean,
            val ruleSymbol: Boolean,
            val invalidCharacter: String?,
    )
    val result: MediatorLiveData<PasswordValidationResult> = run {
        val liveData = MediatorLiveData<PasswordValidationResult>()
        liveData.addSource(password) { str: String? ->
            var ruleLengthVal = false
            var ruleAllowedCharactersVal = true
            var ruleUpperCaseVal = false
            var ruleLowerCaseVal = false
            var ruleDigitVal = false
            var ruleSymbolVal = false
            var invalidCharacterVal = ""
            if (str != null) {
                ruleLengthVal = str.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH
                str.forEach {
                    when {
                        it.isUpperCase() -> {
                            ruleUpperCaseVal = true
                        }
                        it.isLowerCase() -> {
                            ruleLowerCaseVal = true
                        }
                        it.isDigit() -> {
                            ruleDigitVal = true
                        }
                        it in ALLOWED_SYMBOLS -> {
                            ruleSymbolVal = true
                        }
                        else -> {
                            ruleAllowedCharactersVal = false
                            invalidCharacterVal = try {
                                String(charArrayOf(it))
                            } catch (t: Throwable) {
                                // todo: deal with surrogate pair
                                ""
                            }
                        }
                    }
                }
            }
            val valid = ruleLengthVal
                    && ruleAllowedCharactersVal
                    && arrayOf(ruleUpperCaseVal, ruleLowerCaseVal, ruleDigitVal, ruleSymbolVal).count { it } >= 3
            liveData.postValue(PasswordValidationResult(
                    emptyOrValid = str.isNullOrEmpty() || valid,
                    valid = valid,
                    ruleLength = ruleLengthVal,
                    ruleAllowedCharacters = ruleAllowedCharactersVal,
                    ruleUpperCase = ruleUpperCaseVal,
                    ruleLowerCase = ruleLowerCaseVal,
                    ruleDigit = ruleDigitVal,
                    ruleSymbol = ruleSymbolVal,
                    invalidCharacter = invalidCharacterVal,
            ))
        }
        liveData
    }

    companion object {
        const val PASSWORD_MIN_LENGTH = 8
        const val PASSWORD_MAX_LENGTH = 20
        val ALLOWED_SYMBOLS = """~!@#$%^&*()_-=+'",.;?\[]<>/""".toCharArray()
    }
}
