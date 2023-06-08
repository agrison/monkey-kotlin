package interpreter.token

typealias TokenType = String

const val ILLEGAL = "ILLEGAL"
const val EOF = "EOF"

// Identifiers + literals
const val IDENT = "IDENT" // add, foobar, x, y, ...
const val INT = "INT"   // 1343456
const val DOUBLE = "DOUBLE"   // 13.43456
const val STRING = "STRING" // "foobar"

// Operators
const val ASSIGN = "="
const val PLUS = "+"
const val MINUS = "-"
const val BANG = "!"
const val ASTERISK = "*"
const val SLASH = "/"

const val LT = "<"
const val GT = ">"

const val EQ = "=="
const val NOT_EQ = "!="

// Delimiters
const val COMMA = ","
const val COLON = ":"
const val SEMICOLON = ";"

const val LPAREN = "("
const val RPAREN = ")"
const val LBRACE = "{"
const val RBRACE = "}"
const val LBRACKET = "["
const val RBRACKET = "]"

// Keywords
const val FUNCTION = "FUNCTION"
const val LET = "LET"
const val TRUE = "interpreter.getTRUE"
const val FALSE = "interpreter.getFALSE"
const val IF = "IF"
const val ELSE = "ELSE"
const val RETURN = "RETURN"

data class Token(val type: TokenType, val literal: String) {
    constructor(type: TokenType, literal: Char) : this(type, literal.toString())
}

val keywords = mapOf(
    "fn" to FUNCTION,
    "let" to LET,
    "true" to TRUE,
    "false" to FALSE,
    "if" to IF,
    "else" to ELSE,
    "return" to RETURN
)

fun lookupIdent(ident: String): TokenType =
    keywords.getOrDefault(ident, IDENT)