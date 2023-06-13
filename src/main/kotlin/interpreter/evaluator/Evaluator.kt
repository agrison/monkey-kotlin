package interpreter.evaluator

import interpreter.ast.BooleanLiteral
import interpreter.`object`.*
import interpreter.token.DOT_DOT
import kotlin.math.absoluteValue

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
            is interpreter.ast.DoubleLiteral -> MDouble(node.value)
            is interpreter.ast.StringLiteral -> MString(node.value)
            is interpreter.ast.RangeLiteral -> MRange(node.value)
            is BooleanLiteral -> MBoolean(node.value)
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
            is interpreter.ast.WhileExpression -> evalWhileExpression(node, env)
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

    private fun evalProgram(program: interpreter.ast.Program, env: Environment): MonkeyObject {
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

    private fun evalBlockStatement(block: interpreter.ast.BlockStatement, env: Environment): MonkeyObject {
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

    private fun evalPrefixExpression(operator: String, right: MonkeyObject): MonkeyObject {
        return when (operator) {
            "!" -> evalBangOperatorExpression(right)
            "-" -> evalMinusPrefixOperatorExpression(right)
            else -> newError("unknown operator $operator${right.type()}")
        }
    }

    private fun evalInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
        return when {
            left.type().isNumber() && right.type().isNumber() -> evalNumberInfixExpression(
                operator,
                left,
                right
            )

            left.type() == STRING_OBJ && (right.type() == STRING_OBJ || right.type() == INTEGER_OBJ) -> evalStringInfixExpression(
                operator,
                left,
                right
            )

            (operator == "+" || operator == "-") && left.type() == ARRAY_OBJ && right.type() == ARRAY_OBJ -> evalArrayInfixExpression(
                operator, left as MArray, right as MArray
            )

            (operator == "+" || operator == "-") && left.type() == HASH_OBJ && right.type() == HASH_OBJ -> evalHashInfixExpression(
                operator, left as MHash, right as MHash
            )

            operator == DOT_DOT && left.type() == INTEGER_OBJ && right.type() == INTEGER_OBJ -> {
                MRange((left as MInteger).value..(right as MInteger).value)
            }

            operator == "==" -> MBoolean(left == right)
            operator == "!=" -> MBoolean(left != right)
            left.type() != right.type() -> newError("type mismatch: ${left.type()} $operator ${right.type()}")
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    private fun evalBangOperatorExpression(right: MonkeyObject): MonkeyObject {
        return when (right) {
            TRUE -> FALSE
            FALSE -> TRUE
            NULL -> TRUE
            else -> FALSE
        }
    }

    private fun evalMinusPrefixOperatorExpression(right: MonkeyObject): MonkeyObject {
        if (right.type() == INTEGER_OBJ) {
            return MInteger(0 - (right as MInteger).value)
        } else if (right.type() == DOUBLE_OBJ) {
            return MDouble(0 - (right as MDouble).value)
        }

        return newError("unknown operator: -${right.type()}")
    }

    private fun evalNumberInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
        if (left is MInteger && right is MInteger) {
            return evalIntegerInfixExpression(operator, left, right)
        }

        val leftVal =
            (when (left) {
                is MInteger -> left.value
                is MDouble -> left.value
                else -> return newError("not an INTEGER nor a DOUBLE")
            }).toDouble()
        val rightVal =
            (when (right) {
                is MInteger -> right.value
                is MDouble -> right.value
                else -> return newError("not an INTEGER nor a DOUBLE")
            }).toDouble()

        return when (operator) {
            "+" -> MDouble(leftVal + rightVal)
            "-" -> MDouble(leftVal - rightVal)
            "*" -> MDouble(leftVal * rightVal)
            "/" -> MDouble(leftVal / rightVal)
            "<" -> MBoolean(leftVal < rightVal)
            ">" -> MBoolean(leftVal > rightVal)
            "==" -> MBoolean(leftVal == rightVal)
            "!=" -> MBoolean(leftVal != rightVal)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    private fun evalIntegerInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
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
            ".." -> MRange(leftVal..rightVal)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    private fun evalStringInfixExpression(operator: String, left: MonkeyObject, right: MonkeyObject): MonkeyObject {
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

    private fun evalArrayInfixExpression(operator: String, left: MArray, right: MArray): MonkeyObject {
        return when (operator) {
            "+" -> MArray(left.elements + right.elements)
            "-" -> MArray(left.elements - right.elements)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    private fun evalHashInfixExpression(operator: String, left: MHash, right: MHash): MonkeyObject {
        return when (operator) {
            "+" -> MHash(left.pairs + right.pairs)
            "-" -> MHash(left.pairs - right.pairs.keys)
            else -> newError("unknown operator: ${left.type()} $operator ${right.type()}")
        }
    }

    private fun evalIfExpression(ie: interpreter.ast.IfExpression, env: Environment): MonkeyObject {
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

    private fun evalWhileExpression(we: interpreter.ast.WhileExpression, env: Environment): MonkeyObject {
        var condition = eval(we.condition, env)
        if (isError(condition)) {
            return condition
        }

        while (isTruthy(condition)) {
            eval(we.consequence, env)
            condition = eval(we.condition, env)
            if (isError(condition)) {
                return condition
            }
        }

        return NULL
    }

    private fun evalIdentifier(node: interpreter.ast.Identifier, env: Environment): MonkeyObject {
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

    private fun isTruthy(obj: MonkeyObject): Boolean {
        return when (obj) {
            NULL -> false
            TRUE -> true
            FALSE -> false
            else -> true
        }
    }

    private fun newError(s: String): MonkeyObject {
        return MError(s)
    }

    private fun isError(obj: MonkeyObject): Boolean {
        return obj.type() == ERROR_OBJ
    }

    private fun evalExpressions(exps: List<interpreter.ast.Expression>, env: Environment): List<MonkeyObject> {
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

    private fun applyFunction(fn: MonkeyObject, args: List<MonkeyObject>): MonkeyObject {
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

    private fun extendFunctionEnv(fn: MFunction, args: List<MonkeyObject>): Environment {
        val env = Environment.newEnclosedEnvironment(fn.env)

        fn.parameters.forEachIndexed { idx, parameter ->
            env[parameter.value] = args[idx]
        }

        return env
    }

    private fun unwrapReturnValue(obj: MonkeyObject): MonkeyObject {
        if (obj is MReturnValue) {
            return obj.value
        }

        return obj
    }

    private fun evalIndexExpression(left: MonkeyObject, index: MonkeyObject): MonkeyObject {
        return when {
            left.type() == ARRAY_OBJ && index.type() == INTEGER_OBJ -> evalArrayIndexExpression(
                left as MArray,
                index as MInteger
            )
            left.type() == ARRAY_OBJ && index.type() == RANGE_OBJ -> evalArrayIndexExpression(
                left as MArray,
                index as MRange
            )

            left.type() == HASH_OBJ -> evalHashIndexExpression(left as MHash, index)
            else -> newError("index operator not supported: ${left.type()}")
        }
    }

    private fun evalArrayIndexExpression(array: MArray, index: MInteger): MonkeyObject {
        if (index.value > array.elements.size - 1 || (index.value < 0 && index.value.absoluteValue > array.elements.size)) {
            return NULL
        }

        if (index.value < 0) {
            return array.elements[array.elements.size + index.value]
        }

        return array.elements[index.value]
    }

    private fun evalArrayIndexExpression(array: MArray, index: MRange): MonkeyObject {
        if (index.value.first < 0 || index.value.last + 1 > array.elements.size - 1) {
            return NULL
        }

        return MArray(array.elements.subList(index.value.first, index.value.last + 1))
    }

    private fun evalHashLiteral(node: interpreter.ast.HashLiteral, env: Environment): MonkeyObject {
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

        return MHash(pairs)
    }

    private fun evalHashIndexExpression(hash: MHash, index: MonkeyObject): MonkeyObject {
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