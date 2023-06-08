package interpreter.ast

import interpreter.token.Token
import kotlin.Boolean

interface Node {
    fun tokenLiteral(): String
}

interface Statement : Node {
}

interface Expression : Node {
}

class Program(val statements: List<Statement>) : Node {
    override fun tokenLiteral(): String {
        return if (statements.isNotEmpty()) {
            statements[0].tokenLiteral()
        } else {
            ""
        }
    }

    override fun toString(): String {
        val b = StringBuilder()
        statements.forEach { b.append(it) }
        return b.toString()
    }
}

class LetStatement(val token: Token, val name: Identifier, val value: Expression?) : Statement {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()
        out.append(tokenLiteral())
            .append(" ")
            .append(name)
            .append(" = ")

        value?.let {
            out.append(value)
        }

        return out.append(";").toString()
    }
}

class ReturnStatement(val token: Token, val returnValue: Expression?) : Statement {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()
        out.append(tokenLiteral())
            .append(" ")

        returnValue?.let {
            out.append(returnValue)
        }

        return out.append(";").toString()
    }
}

class ExpressionStatement(val token: Token, val expression: Expression?) : Statement {
    override fun tokenLiteral() = token.literal

    override fun toString() = expression?.toString() ?: ""
}

class BlockStatement(val token: Token, val statements: List<Statement>) : Statement {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        statements.forEach { statement ->
            out.append(statement)
        }

        return out.toString()
    }
}

class Identifier(val token: Token, val value: String) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString() = value
}

class Boolean(val token: Token, val value: Boolean) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString() = token.literal
}

class IntegerLiteral(val token: Token, val value: Int) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString() = token.literal
}

class PrefixExpression(val token: Token, val operator: String, val right: Expression) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()
        out.append("(")
            .append(operator)
            .append(right)
            .append(")")

        return out.toString()
    }
}

class InfixExpression(val token: Token, val left: Expression, val operator: String, val right: Expression) :
    Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()
        out.append("(")
            .append(left)
            .append(" ")
            .append(operator)
            .append(" ")
            .append(right)
            .append(")")

        return out.toString()
    }
}

class IfExpression(
    val token: Token,
    val condition: Expression,
    val consequence: BlockStatement,
    val alternative: BlockStatement?
) :
    Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()
        out.append("if")
            .append(condition)
            .append(" ")
            .append(consequence)
            .append(" ")

        alternative?.let {
            out.append("else ")
                .append(alternative)
        }

        return out.toString()
    }
}

class FunctionLiteral(val token: Token, val parameters: List<Identifier>, val body: BlockStatement) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        out.append(tokenLiteral())
            .append("(")
            .append(parameters.joinToString(", ") { it.toString() })
            .append(") ")
            .append(body)

        return out.toString()
    }
}

class CallExpression(val token: Token, val function: Expression, val arguments: List<Expression>) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        out.append(function)
            .append("(")
            .append(arguments.joinToString(", ") { it.toString() })
            .append(") ")

        return out.toString()
    }
}

class StringLiteral(val token: Token, val value: String) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString() = token.literal
}

class ArrayLiteral(val token: Token, val elements: List<Expression>) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        out.append("[")
            .append(elements.joinToString(", ") { it.toString() })
            .append("]")

        return out.toString()
    }
}

class IndexExpression(val token: Token, val left: Expression, val index: Expression) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        out.append("(")
            .append(left)
            .append("[")
            .append(index)
            .append("])")

        return out.toString()
    }
}

class HashLiteral(val token: Token, val pairs: Map<Expression, Expression>) : Expression {
    override fun tokenLiteral() = token.literal

    override fun toString(): String {
        val out = StringBuilder()

        val kv = mutableListOf<String>()
        pairs.forEach { (k, v) -> kv.add("$k:$v")  }

        out.append("{")
            .append(kv.joinToString(", ") { it })
            .append("}")

        return out.toString()
    }
}