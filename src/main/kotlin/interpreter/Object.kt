package interpreter

import kotlin.Boolean

typealias ObjectType = String

const val NULL_OBJ = "interpreter.getNULL"
const val ERROR_OBJ = "ERROR"
const val INTEGER_OBJ = "INTEGER"
const val BOOLEAN_OBJ = "BOOLEAN"
const val RETURN_VALUE_OBJ = "RETURN_VALUE"
const val FUNCTION_OBJ = "FUNCTION"
const val NON_INITALIZED_OBJ = "NON_INITIALIZED"
const val STRING_OBJ = "STRING"
const val ARRAY_OBJ = "ARRAY"
const val HASH_OBJ = "HASH"
const val BUILTIN_OBJ = "BUILTIN"

interface MonkeyObject {
    fun type(): ObjectType
    fun inspect(): String
}

typealias BuiltinFunction = (args: List<MonkeyObject>) -> MonkeyObject

data class MInteger(val value: Int) : MonkeyObject, Hashable {
    override fun type() = INTEGER_OBJ

    override fun inspect() = "$value"

    override fun hashKey() = HashKey(type(), value)
}

data class MBoolean(val value: Boolean) : MonkeyObject, Hashable {
    override fun type() = BOOLEAN_OBJ

    override fun inspect() = "$value"

    override fun hashKey() = HashKey(type(), if (value) 1 else 0)
}

class MNull : MonkeyObject {
    override fun type() = NULL_OBJ

    override fun inspect() = "null"
}

class MReturnValue(val value: MonkeyObject) : MonkeyObject {
    override fun type() = RETURN_VALUE_OBJ

    override fun inspect() = value.inspect()
}

class MError(val value: String) : MonkeyObject {
    override fun type() = ERROR_OBJ

    override fun inspect() = "ERROR: $value"
}

class MNonInitialized() : MonkeyObject {
    override fun type() = NON_INITALIZED_OBJ

    override fun inspect() = "ERROR: Non initialized"
}

class MFunction(val parameters: List<Identifier>, val body: BlockStatement, val env: Environment) : MonkeyObject {
    override fun type() = FUNCTION_OBJ

    override fun inspect(): String {
        val out = StringBuilder()
        val params = mutableListOf<String>()
        parameters.forEach { parameter ->
            params.add(parameter.toString())
        }

        return out.append("fn(")
            .append(params.joinToString(", "))
            .append(") {\n")
            .append(body)
            .append("\n}").toString()
    }
}

data class MString(val value: String) : MonkeyObject, Hashable {
    override fun type() = STRING_OBJ

    override fun inspect() = value

    override fun hashKey() = HashKey(type(), value.hashCode())
}

data class MArray(val elements: List<MonkeyObject>) : MonkeyObject {
    override fun type() = ARRAY_OBJ

    override fun inspect(): String {
        val out = StringBuilder()

        return out.append("[")
            .append(elements.joinToString(", "))
            .append("]").toString()
    }
}

class Builtin(val fn: BuiltinFunction) : MonkeyObject {
    override fun type() = BUILTIN_OBJ

    override fun inspect() = "builtin function"
}

data class HashKey(val type: ObjectType, val value: Int)
data class HashPair(val key: MonkeyObject, val value: MonkeyObject)

data class Hash(val pairs: Map<HashKey, HashPair>) : MonkeyObject {
    override fun type() = HASH_OBJ

    override fun inspect(): String {
        val out = StringBuilder()

        return out.append("{")
            .append(pairs.values.joinToString(", ") { (k, v) -> "${k.inspect()}: ${v.inspect()}" })
            .append("}").toString()
    }
}

interface Hashable {
    fun hashKey(): HashKey
}