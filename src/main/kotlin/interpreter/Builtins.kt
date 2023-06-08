import interpreter.`object`.*

val builtins = mapOf<String, Builtin>(
    "len" to Builtin(Builtins::len),
    "puts" to Builtin(Builtins::puts),
    "first" to Builtin(Builtins::first),
    "last" to Builtin(Builtins::last),
    "rest" to Builtin(Builtins::rest),
    "push" to Builtin(Builtins::push),
)

class Builtins {
    companion object {
        fun len(args: List<MonkeyObject>): MonkeyObject {
            if (args.size != 1) {
                return MError("wrong number of arguments. got=${args.size}, want=1")
            }

            return when (args[0]) {
                is MArray -> MInteger((args[0] as MArray).elements.size)
                is MString -> MInteger((args[0] as MString).value.length)
                else -> MError("argument to `len` not supported, got ${args[0].type()}")
            }
        }

        fun puts(args: List<MonkeyObject>): MonkeyObject {
            args.forEach { println(it.inspect()) }
            return NULL
        }

        fun first(args: List<MonkeyObject>): MonkeyObject {
            if (args.size != 1) {
                return MError("wrong number of arguments. got=${args.size}, want=1")
            }

            return when (args[0]) {
                is MArray -> {
                    val array = (args[0] as MArray).elements
                    return if (array.isEmpty()) NULL else array.first()
                }
                is MString -> MString((args[0] as MString).value[0].toString())
                else -> MError("argument to `first` must be ARRAY or STRING, got ${args[0].type()}")
            }
        }

        fun last(args: List<MonkeyObject>): MonkeyObject {
            if (args.size != 1) {
                return MError("wrong number of arguments. got=${args.size}, want=1")
            }

            return when (args[0]) {
                is MArray -> {
                    val array = (args[0] as MArray).elements
                    return if (array.isEmpty()) NULL else array.last()
                }
                is MString -> MString((args[0] as MString).value.last().toString())
                else -> MError("argument to `last` must be ARRAY or STRING, got ${args[0].type()}")
            }
        }

        fun rest(args: List<MonkeyObject>): MonkeyObject {
            if (args.size != 1) {
                return MError("wrong number of arguments. got=${args.size}, want=1")
            }

            return when (args[0]) {
                is MArray -> {
                    val arr = args[0] as MArray
                    val length = arr.elements.size
                    if (length > 0) {
                        val newElements = arr.elements.subList(1, length - 1).toList()
                        return MArray(newElements)
                    }
                    return NULL
                }

                else -> MError("argument to `last` must be ARRAY, got ${args[0].type()}")
            }
        }

        fun push(args: List<MonkeyObject>): MonkeyObject {
            if (args.size != 2) {
                return MError("wrong number of arguments. got=${args.size}, want=2")
            }

            return when (args[0]) {
                is MArray -> {
                    val arr = args[0] as MArray
                    val newElements = arr.elements.toMutableList()
                    newElements.add(args[1])
                    return MArray(newElements)
                }

                else -> MError("argument to `push` must be ARRAY, got ${args[0].type()}")
            }
        }
    }
}