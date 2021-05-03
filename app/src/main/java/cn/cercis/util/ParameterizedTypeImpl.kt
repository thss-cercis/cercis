package cn.cercis.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ParameterizedTypeImpl(private val internalRawType: Type, vararg parameters: Type) : ParameterizedType {
    private val parameters: Array<Type> = arrayOf(*parameters)

    override fun getActualTypeArguments(): Array<Type> {
        return parameters
    }

    override fun getRawType(): Type {
        return internalRawType
    }

    override fun getOwnerType(): Type? {
        return null
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            is ParameterizedType -> other.rawType == internalRawType && other.actualTypeArguments.contentEquals(parameters)
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = internalRawType.hashCode()
        result = 31 * result + parameters.contentHashCode()
        return result
    }
}
