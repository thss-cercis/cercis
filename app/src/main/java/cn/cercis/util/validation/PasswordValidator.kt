package cn.cercis.util.validation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

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

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 20
private val PASSWORD_ALLOWED_SYMBOLS = """~!@#$%^&*()_-=+'",.;?\[]<>/""".toCharArray()

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
 *  - Symbols [PASSWORD_ALLOWED_SYMBOLS]
 */
fun validatePassword(password: LiveData<String>): MediatorLiveData<PasswordValidationResult> {
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
                    it in PASSWORD_ALLOWED_SYMBOLS -> {
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
        liveData.postValue(
            PasswordValidationResult(
                emptyOrValid = str.isNullOrEmpty() || valid,
                valid = valid,
                ruleLength = ruleLengthVal,
                ruleAllowedCharacters = ruleAllowedCharactersVal,
                ruleUpperCase = ruleUpperCaseVal,
                ruleLowerCase = ruleLowerCaseVal,
                ruleDigit = ruleDigitVal,
                ruleSymbol = ruleSymbolVal,
                invalidCharacter = invalidCharacterVal,
            )
        )
    }
    return liveData
}
