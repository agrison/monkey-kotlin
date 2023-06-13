package interpreter.parser

import interpreter.ast.*
import interpreter.lexer.Lexer
import interpreter.token.*
import kotlin.Boolean

const val LOWEST = 0
const val EQUALS = 1        // ==
const val RANGE = 2        // ..
const val LESS_GREATER = 3  // > or <
const val BOOL_OPS = 4
const val SUM = 5           // + and -
const val PRODUCT = 6       // * and /
const val MOD = 7
const val PREFIX = 8        // -X or !X
const val CALL = 9          // myFunction(X)
const val INDEX = 10         // array[index]

val precedences = mapOf(
    EQ to EQUALS,
    NOT_EQ to EQUALS,
    DOT_DOT to RANGE,
    LT to LESS_GREATER,
    LTE to LESS_GREATER,
    GT to LESS_GREATER,
    GTE to LESS_GREATER,
    PLUS to SUM,
    MINUS to SUM,
    MODULO to MOD,
    BOOL_AND to BOOL_OPS,
    BOOL_OR to BOOL_OPS,
    SLASH to PRODUCT,
    ASTERISK to PRODUCT,
    LPAREN to CALL,
    LBRACKET to INDEX
)

typealias PrefixParseFn = (parser: Parser) -> Expression?
typealias InfixParseFn = (parser: Parser, exp: Expression) -> Expression?

class Parser(
    private val lexer: Lexer,
    val errors: MutableList<String>,
    private var curToken: Token,
    private var peekToken: Token,
    private var prefixParseFns: Map<TokenType, PrefixParseFn>,
    private var infixParseFns: Map<TokenType, InfixParseFn>
) {
    companion object {
        fun new(lexer: Lexer): Parser {
            val prefixParseFns = mapOf<TokenType, PrefixParseFn>(
                IDENT to Parser::parseIdentifier,
                INT to Parser::parseIntegerLiteral,
                DOUBLE to Parser::parseDoubleLiteral,
                STRING to Parser::parseStringLiteral,
                BANG to Parser::parsePrefixExpression,
                MINUS to Parser::parsePrefixExpression,
                TRUE to Parser::parseBoolean,
                FALSE to Parser::parseBoolean,
                LPAREN to Parser::parseGroupedExpression,
                IF to Parser::parseIfExpression,
                WHILE to Parser::parseWhileExpression,
                FUNCTION to Parser::parseFunctionLiteral,
                LBRACKET to Parser::parseArrayLiteral,
                LBRACE to Parser::parseHashLiteral,
                DOT_DOT to Parser::parseRangeLiteral,
            )
            val infixParseFns = mapOf<TokenType, InfixParseFn>(
                PLUS to Parser::parseInfixExpression,
                MINUS to Parser::parseInfixExpression,
                SLASH to Parser::parseInfixExpression,
                ASTERISK to Parser::parseInfixExpression,
                MODULO to Parser::parseInfixExpression,
                BOOL_AND to Parser::parseInfixExpression,
                BOOL_OR to Parser::parseInfixExpression,
                EQ to Parser::parseInfixExpression,
                NOT_EQ to Parser::parseInfixExpression,
                LT to Parser::parseInfixExpression,
                GT to Parser::parseInfixExpression,
                LTE to Parser::parseInfixExpression,
                GTE to Parser::parseInfixExpression,
                DOT_DOT to Parser::parseInfixExpression,
                LPAREN to Parser::parseCallExpression,
                LBRACKET to Parser::parseIndexExpression,
            )
            val parser = Parser(lexer, mutableListOf(), Token(EOF, ""), Token(EOF, ""), prefixParseFns, infixParseFns)

            // read 2 tokens so curToken and peekToken are both set
            repeat(2) {
                parser.nextToken()
            }

            return parser
        }
    }

    fun nextToken() {
        curToken = peekToken
        peekToken = lexer.nextToken()
    }

    private fun curTokenIs(t: TokenType) = curToken.type == t

    private fun peekTokenIs(t: TokenType) = peekToken.type == t

    private fun expectPeek(t: TokenType): Boolean {
        return if (peekTokenIs(t)) {
            nextToken()
            true
        } else {
            peekError(t)
            false
        }
    }

    fun errors() = errors

    private fun peekError(t: TokenType) {
        errors.add("expected next token to be ${t}, got ${peekToken.type} instead")
    }

    private fun noPrefixParseFnError(t: TokenType) {
        errors.add("no prefix parse function for $t found")
    }

    fun parseProgram(): Program {
        val statements = mutableListOf<Statement>()

        while (!curTokenIs(EOF)) {
            parseStatement()?.let { statement -> statements.add(statement) }
            nextToken()
        }

        return Program(statements)
    }

    private fun parseStatement(): Statement? {
        return when (curToken.type) {
            LET -> parseLetStatement()
            RETURN -> parseReturnStatement()
            else -> parseExpressionStatement()
        }
    }

    private fun parseLetStatement(): LetStatement? {
        val curToken = curToken

        if (!expectPeek(IDENT)) {
            return null
        }

        val name = Identifier(this.curToken, this.curToken.literal)

        if (!expectPeek(ASSIGN)) {
            return null
        }

        nextToken()

        val value = parseExpression(LOWEST)

        if (peekTokenIs(SEMICOLON)) {
            nextToken()
        }

        return LetStatement(curToken, name, value)
    }

    private fun parseReturnStatement(): ReturnStatement {
        val curToken = curToken

        nextToken()

        val statement = ReturnStatement(curToken, parseExpression(LOWEST))

        if (peekTokenIs(SEMICOLON)) {
            nextToken()
        }

        return statement
    }

    private fun parseExpressionStatement(): ExpressionStatement {
        val statement = ExpressionStatement(curToken, parseExpression(LOWEST))

        if (peekTokenIs(SEMICOLON)) {
            nextToken()
        }

        return statement
    }

    private fun parseExpression(precedence: Int): Expression? {
        val prefix = prefixParseFns[curToken.type]
        if (prefix == null) {
            noPrefixParseFnError(curToken.type)
            return null
        }

        var leftExp: Expression? = prefix(this) ?: return null

        while (!peekTokenIs(SEMICOLON) && precedence < peekPrecedence()) {
            val infix = infixParseFns[peekToken.type] ?: return leftExp

            nextToken()

            leftExp = infix(this, leftExp!!)
        }

        return leftExp
    }

    private fun peekPrecedence(): Int {
        return precedences.getOrDefault(peekToken.type, LOWEST)
    }

    private fun curPrecedence(): Int {
        return precedences.getOrDefault(curToken.type, LOWEST)
    }

    fun parseIdentifier(): Expression {
        return Identifier(curToken, curToken.literal)
    }

    fun parseIntegerLiteral(): Expression? {
        return try {
            val int = curToken.literal.replace("\\D".toRegex(), "").toInt()
            IntegerLiteral(curToken, int)
        } catch (e: Exception) {
            errors.add("Could not parse ${curToken.literal} as integer")
            null
        }
    }

    fun parseRangeLiteral(): Expression? {
        return try {
            val ints = curToken.literal.split("..").map { it.toInt() }
            RangeLiteral(curToken, ints[0] .. ints[1])
        } catch (e: Exception) {
            errors.add("Could not parse ${curToken.literal} as range")
            null
        }
    }

    fun parseDoubleLiteral(): Expression? {
        return try {
            val double = curToken.literal.replace("[^\\d.]".toRegex(), "").toDouble()
            DoubleLiteral(curToken, double)
        } catch (e: Exception) {
            errors.add("Could not parse ${curToken.literal} as double")
            null
        }
    }

    fun parseStringLiteral(): Expression {
        return StringLiteral(curToken, curToken.literal)
    }

    fun parsePrefixExpression(): Expression {
        val curToken = curToken
        nextToken()
        return PrefixExpression(curToken, curToken.literal, parseExpression(PREFIX)!!)
    }

    fun parseInfixExpression(left: Expression): Expression {
        val curToken = curToken
        val precedence = curPrecedence()
        nextToken()
        return InfixExpression(curToken, left, curToken.literal, parseExpression(precedence)!!)
    }

    fun parseBoolean(): BooleanLiteral {
        return BooleanLiteral(curToken, curTokenIs(TRUE))
    }

    fun parseGroupedExpression(): Expression? {
        nextToken()

        val exp = parseExpression(LOWEST)
        if (!expectPeek(RPAREN)) {
            return null
        }

        return exp
    }

    fun parseIfExpression(): Expression? {
        val curToken = curToken
        if (!expectPeek(LPAREN)) {
            return null
        }

        nextToken()
        val condition = parseExpression(LOWEST)

        if (!expectPeek(RPAREN) || !expectPeek(LBRACE)) {
            return null
        }

        val consequence = parseBlockStatement()
        var alternative: BlockStatement? = null
        if (peekTokenIs(ELSE)) {
            nextToken()
            if (!expectPeek(LBRACE)) {
                return null
            }
            alternative = parseBlockStatement()
        }

        return IfExpression(curToken, condition!!, consequence, alternative)
    }

    fun parseWhileExpression(): Expression? {
        val curToken = curToken
        if (!expectPeek(LPAREN)) {
            return null
        }

        nextToken()
        val condition = parseExpression(LOWEST)

        if (!expectPeek(RPAREN) || !expectPeek(LBRACE)) {
            return null
        }

        val consequence = parseBlockStatement()

        return WhileExpression(curToken, condition!!, consequence)
    }

    private fun parseBlockStatement(): BlockStatement {
        val curToken = curToken
        val statements = mutableListOf<Statement>()

        nextToken()

        while (!curTokenIs(RBRACE) && !curTokenIs(EOF)) {
            parseStatement()?.let { statement ->
                statements.add(statement)
            }
            nextToken()
        }

        return BlockStatement(curToken, statements)
    }

    fun parseFunctionLiteral(): Expression? {
        val curToken = curToken

        if (!expectPeek(LPAREN)) {
            return null
        }

        val parameters = parseFunctionParameters()
        if (!expectPeek(LBRACE)) {
            return null
        }

        return FunctionLiteral(curToken, parameters, body = parseBlockStatement())
    }

    private fun parseFunctionParameters(): List<Identifier> {
        val identifiers = mutableListOf<Identifier>()

        // no params
        if (peekTokenIs(RPAREN)) {
            nextToken()
            return identifiers
        }

        nextToken()

        identifiers.add(Identifier(curToken, curToken.literal))

        while (peekTokenIs(COMMA)) {
            nextToken()
            nextToken()
            identifiers.add(Identifier(curToken, curToken.literal))
        }

        if (!expectPeek(RPAREN)) {
            return mutableListOf()
        }

        return identifiers
    }

    fun parseCallExpression(function: Expression): Expression {
        return CallExpression(curToken, function, parseCallArguments())
    }

    private fun parseExpressionList(end: TokenType): List<Expression> {
        val list = mutableListOf<Expression>()

        if (peekTokenIs(end)) {
            nextToken()
            return list
        }

        nextToken()
        list.add(parseLowest())

        while (peekTokenIs(COMMA)) {
            nextToken()
            nextToken()
            list.add(parseLowest())
        }

        if (!expectPeek(end)) {
            return mutableListOf()
        }

        return list
    }

    fun parseArrayLiteral(): Expression {
        return ArrayLiteral(curToken, parseExpressionList(RBRACKET))
    }

    fun parseIndexExpression(left: Expression): Expression? {
        val curToken = curToken
        nextToken()

        val index = parseLowest()

        if (!expectPeek(RBRACKET)) {
            return null
        }

        return IndexExpression(curToken, left, index)
    }

    fun parseHashLiteral(): Expression? {
        val curToken = curToken
        val pairs = mutableMapOf<Expression, Expression>()

        while (!peekTokenIs(RBRACE)) {
            nextToken()
            val key = parseLowest()

            if (!expectPeek(COLON)) {
                return null
            }

            nextToken()
            val value = parseLowest()

            pairs[key] = value

            if (!peekTokenIs(RBRACE) && !expectPeek(COMMA)) {
                return null
            }
        }

        if (!expectPeek(RBRACE)) {
            return null
        }

        return HashLiteral(curToken, pairs)
    }

    private fun parseCallArguments(): List<Expression> {
        val args = mutableListOf<Expression>()

        // no args
        if (peekTokenIs(RPAREN)) {
            nextToken()
            return args
        }

        nextToken()
        args.add(parseLowest())

        while (peekTokenIs(COMMA)) {
            nextToken()
            nextToken()
            args.add(parseLowest())
        }

        if (!expectPeek(RPAREN)) {
            return mutableListOf()
        }

        return args
    }

    private fun parseLowest() = parseExpression(LOWEST)!!

}