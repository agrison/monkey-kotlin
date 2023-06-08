package interpreter.evaluator

import interpreter.ast.Boolean
import interpreter.`object`.*
import java.lang.Exception

val NULL = MNull()
val TRUE = MBoolean(true)
val FALSE = MBoolean(false)

class Evaluator {
    fun eval(node: interpreter.ast.Node, env: Environment): MonkeyObject {
        return when (node) {
            is interpreter.ast.Program -> evalProgram(node, env)
            is interpreter.ast.BlockStatement -> evalBlockStatement(node, env)
            is interpreter.ast.ExpressionStatement -> eval(node.expression!!, env)
            is interpreter.ast.ReturnStatement -> {
                val value = eval(node.returnValue!!, env)
                if (isError(value)) {
                    return value
                }
                MReturnValue(value)
            }

            is interpreter.ast.LetStatement -> {
                val value = eval(node.value!!, env)
                if (isError(value)) {
                    return value
                }
                env[node.name.value] = value
                return value
            }

            is interpreter.ast.IntegerLiteral -> MInteger(node.value)
            is interpreter.ast.StringLiteral -> MString(node.value)
            is Boolean -> MBoolean(node.value)
            is interpreter.ast.PrefixExpression -> {
                val right = eval(node.right, env)
                if (isError(right)) {
                    return right
                }
                evalPrefixExpression(node.operator, right)
            }

            is interpreter.ast.InfixExpression -> {
                val left = eval(node.left, env)
                if (isError(left)) {
                    return left
                }

                val right = eval(node.right, env)
                if (isError(right)) {
                    return right
                }

                evalInfixExpression(node.operator, left, right)
            }

            is interpreter.ast.IfExpression -> evalIfExpression(node, env)
            is interpreter.ast.Identifier -> evalIdentifier(node, env)
            is interpreter.ast.FunctionLiteral -> MFunction(node.parameters, node.body, env)
            is interpreter.ast.CallExpression -> {
                val function = eval(node.function, env)
                if (isError(function)) {
                    return function
                }

                val args = evalExpressions(node.arguments, env)
                if (args.size == 1 && isError(args[0])) {
                    return args[0]
                }

                applyFunction(function, args)
            }

            is interpreter.ast.ArrayLiteral -> {
                val elements = evalExpressions(node.elements, env)
                if (elements.size == 1 && isError(elements[0])) {
                    return elements[0]
                }

                MArray(elements)
            }

            is interpreter.ast.IndexExpression -> {
                val left = eval(node.left, env)
                if (isError(left)) {
                    return left
                }

                val index = eval(node.index, env)
                if (isError(index)) {
                    return index
                }

                evalIndexExpression(left, index)
            }

            is interpreter.ast.HashLiteral -> evalHashLiteral(node, env)

            else -> MNull()
        }
    }

    fun evalProgram(program: interpreter.ast.Program, env: Environment): MonkeyObject {
        var result: MonkeyObject = MNonInitialized()

        program.statements.forEach { statement ->
            result = eval(statement, env)
            when (result) {
                is MReturnValue -> return (result as MReturnValue).value
                is MError -> return result
            }
        }

        return result
    }

    fun evalBlockStatement(block: interpreter.ast.BlockStatement, env: Environment): MonkeyObject {
        var result: MonkeyObject = MNonInitialized()

        block.statements.forEach { statement ->
            result = eval(statement, env)

            if (result !is MNull) {
                if (result.type() == RETURN_VALUE_OBJ || result.type() == ERROR_OBJ) {
                    return result
                }
            }
        }

        return result
    }

    fun evalPrefixExpression(operator: String, right: MonkeyObject): MonkeyObject {
        return when (operator) {
            "!" -> evalBangOperatorExpression(right)
            "-" -> evalMinusPrefixOperatorExpression(right)
            else -> newError("unknown operator $operator${right.type()}")
        }
    }

    fun evalInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
        return when {
            left.type() == INTEGER_OBJ && right.type() == INTEGER_OBJ -> evalIntegerInfixExpression(
                operator,
                left,
                right
            )
            left.type() == STRING_OBJ && (right.type() == STRING_OBJ || right.type() == INTEGER_OBJ) -> evalStringInfixExpression(
                operator,
                left,
                right
            )
            operator == "==" -> MBoolean(left == right)
            operator == "!=" -> MBoolean(left != right)
            left.type() != right.type() -> newError("type mismatch: ${left.type()} $operator ${right.type()}")
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    fun evalBangOperatorExpression(right: MonkeyObject): MonkeyObject {
        return when (right) {
            TRUE -> FALSE
            FALSE -> TRUE
            NULL -> TRUE
            else -> FALSE
        }
    }

    fun evalMinusPrefixOperatorExpression(right: MonkeyObject): MonkeyObject {
        if (right.type() != INTEGER_OBJ) {
            return newError("unknown operator: -${right.type()}")
        }

        return MInteger(0 - (right as MInteger).value)
    }

    fun evalIntegerInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
        val leftVal = (left as MInteger).value
        val rightVal = (right as MInteger).value

        return when (operator) {
            "+" -> MInteger(leftVal + rightVal)
            "-" -> MInteger(leftVal - rightVal)
            "*" -> MInteger(leftVal * rightVal)
            "/" -> MInteger(leftVal / rightVal)
            "<" -> MBoolean(leftVal < rightVal)
            ">" -> MBoolean(leftVal > rightVal)
            "==" -> MBoolean(leftVal == rightVal)
            "!=" -> MBoolean(leftVal != rightVal)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    fun evalStringInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
        val leftVal = (left as MString).value
        val intRightVal = if (right is MInteger) right.value else null
        val rightVal = if (right is MString) right.value else ""

        return when (operator) {
            "+" -> if (intRightVal != null) MString(leftVal + intRightVal.toString()) else MString(leftVal + rightVal)
            "*" -> if (intRightVal != null) MString(leftVal.repeat(intRightVal)) else newError("cannot multiply two strings")
            "<" -> MBoolean(leftVal < rightVal)
            ">" -> MBoolean(leftVal > rightVal)
            "==" -> MBoolean(leftVal == rightVal)
            "!=" -> MBoolean(leftVal != rightVal)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    fun evalIfExpression(ie: interpreter.ast.IfExpression, env: Environment): MonkeyObject {
        val condition = eval(ie.condition, env)
        if (isError(condition)) {
            return condition
        }

        return if (isTruthy(condition)) {
            eval(ie.consequence, env)
        } else if (ie.alternative != null) {
            eval(ie.alternative, env)
        } else {
            NULL
        }
    }

    fun evalIdentifier(node: interpreter.ast.Identifier, env: Environment): MonkeyObject {
        val value = env[node.value]
        if (value != null) {
            return value
        }

        val builtin = builtins[node.value]
        if (builtin != null) {
            return builtin
        }

        return newError("identifier not found: ${node.value}")
    }

    private fun isTruthy(obj: MonkeyObject): kotlin.Boolean {
        return when (obj) {
            NULL -> false
            TRUE -> true
            FALSE -> false
            else -> true
        }
    }

    fun newError(s: String): MonkeyObject {
        return MError(s)
    }

    fun isError(obj: MonkeyObject): kotlin.Boolean {
        return obj.type() == ERROR_OBJ
    }

    fun evalExpressions(exps: List<interpreter.ast.Expression>, env: Environment): List<MonkeyObject> {
        val result = mutableListOf<MonkeyObject>()

        exps.forEach { exp ->
            val evaluated = eval(exp, env)
            if (isError(evaluated)) {
                return mutableListOf(evaluated)
            }
            result.add(evaluated)
        }

        return result
    }

    fun applyFunction(fn: MonkeyObject, args: List<MonkeyObject>): MonkeyObject {
        return when (fn) {
            is MFunction -> {
                val extendedEnv = extendFunctionEnv(fn, args)
                val evaluated = eval(fn.body, extendedEnv)
                unwrapReturnValue(evaluated)
            }
            is Builtin -> fn.fn(args)
            else -> newError("not a function: ${fn.type()}")
        }
    }

    fun extendFunctionEnv(fn: MFunction, args: List<MonkeyObject>): Environment {
        val env = Environment.newEnclosedEnvironment(fn.env)

        fn.parameters.forEachIndexed { idx, parameter ->
            env[parameter.value] = args[idx]
        }

        return env
    }

    fun unwrapReturnValue(obj: MonkeyObject): MonkeyObject {
        if (obj is MReturnValue) {
            return obj.value
        }

        return obj
    }

    fun evalIndexExpression(left: MonkeyObject, index: MonkeyObject): MonkeyObject {
        return when {
            left.type() == ARRAY_OBJ && index.type() == INTEGER_OBJ -> evalArrayIndexExpression(
                left as MArray,
                index as MInteger
            )

            left.type() == HASH_OBJ -> evalHashIndexExpression(left as Hash, index)
            else -> newError("index operator not supported: ${left.type()}")
        }
    }

    fun evalArrayIndexExpression(array: MArray, index: MInteger): MonkeyObject {
        if (index.value < 0 || index.value > array.elements.size - 1) {
            return NULL
        }

        return array.elements[index.value]
    }

    fun evalHashLiteral(node: interpreter.ast.HashLiteral, env: Environment): MonkeyObject {
        val pairs = mutableMapOf<HashKey, HashPair>()

        node.pairs.entries.forEach { (k, v) ->
            val key = eval(k, env)
            if (isError(key)) {
                return key
            }

            if (key !is Hashable) {
                return newError("unusable as hash key: ${key.type()}")
            }

            val value = eval(v, env)

            val hashed = key.hashKey()
            pairs[hashed] = HashPair(key, value)
        }

        return Hash(pairs)
    }

    fun evalHashIndexExpression(hash: Hash, index: MonkeyObject): MonkeyObject {
        if (index !is Hashable) {
            return newError("unusable as hash key: ${index.type()}")
        }

        return try {
            hash.pairs[index.hashKey()]?.value ?: NULL
        } catch (e: Exception) {
            NULL
        }
    }
}