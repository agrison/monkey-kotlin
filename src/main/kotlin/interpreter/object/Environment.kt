package interpreter.`object`

class Environment(private val store: MutableMap<String, MonkeyObject>, private val outer: Environment?) {
    companion object {
        fun newEnvironment(): Environment {
            return Environment(mutableMapOf(), null)
        }

        fun newEnclosedEnvironment(outer: Environment): Environment {
            return Environment(mutableMapOf(), outer)
        }
    }

    operator fun get(name: String): MonkeyObject? {
        return store[name] ?: if (outer != null) outer[name] else null;
    }

    operator fun set(name: String, value: MonkeyObject): MonkeyObject {
        store[name] = value
        return value
    }
}