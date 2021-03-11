package cn.edu.tsinghua.thss.cercis.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * Checks if a string is strong enough as a password.
 *
 * Length at least 8.
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
    val valid = MutableLiveData<Boolean>()
    val ruleLength = MutableLiveData<Boolean>()
    val ruleAllowedCharacters = MutableLiveData<Boolean>()
    val ruleUpperCase = MutableLiveData<Boolean>()
    val ruleLowerCase = MutableLiveData<Boolean>()
    val ruleDigit = MutableLiveData<Boolean>()
    val ruleSymbol = MutableLiveData<Boolean>()
    val invalidCharacter = MutableLiveData<String>()
    private val observer = Observer<String> { str ->
        var ruleLengthVal = false
        var ruleAllowedCharactersVal = true
        var ruleUpperCaseVal = false
        var ruleLowerCaseVal = false
        var ruleDigitVal = false
        var ruleSymbolVal = false
        var invalidCharacterVal = ""
        if (str != null) {
            ruleLengthVal = str.length >= PASSWORD_MIN_LENGTH
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
        valid.postValue(ruleLengthVal
                && ruleAllowedCharactersVal
                && arrayOf(ruleUpperCaseVal, ruleLowerCaseVal, ruleDigitVal, ruleSymbolVal).count { it } >= 3)
        ruleLength.postValue(ruleLengthVal)
        ruleAllowedCharacters.postValue(ruleAllowedCharactersVal)
        ruleUpperCase.postValue(ruleUpperCaseVal)
        ruleLowerCase.postValue(ruleLowerCaseVal)
        ruleDigit.postValue(ruleDigitVal)
        ruleSymbol.postValue(ruleSymbolVal)
        invalidCharacter.postValue(invalidCharacterVal)
    }

    fun isValid() = valid.value != false

    fun clear() {
        password.removeObserver(observer)
    }

    init {
        password.observeForever(observer)
    }

    companion object {
        const val PASSWORD_MIN_LENGTH = 8
        val ALLOWED_SYMBOLS = arrayOf('!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~')
    }
}