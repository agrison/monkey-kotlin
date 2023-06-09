package interpreter.lexer

import interpreter.token.*

class Lexer(private val input: String, private var position: Int, private var readPosition: Int, private var ch: Char) {
    companion object {
        fun new(input: String): Lexer {
            Lexer(input, 0, 0, '\u0000').let { l ->
                l.readChar()
                return l
            }
        }

        val whitespaces = charArrayOf(' ', '\t', '\n', '\r')
    }

    fun nextToken(): Token {
        val tok: Token

        skipWhitespace()

        when (ch) {
            '=' -> {
                tok = if (peekChar() == '=') {
                    val current = ch
                    readChar()
                    val literal = current.toString() + ch.toString()
                    Token(EQ, literal)
                } else {
                    Token(ASSIGN, ch)
                }
            }

            '+' -> tok = Token(PLUS, ch)
            '-' -> tok = Token(MINUS, ch)
            '!' -> {
                tok = if (peekChar() == '=') {
                    val current = ch
                    readChar()
                    val literal = current.toString() + ch.toString()
                    Token(NOT_EQ, literal)
                } else {
                    Token(BANG, ch)
                }
            }

            '/' -> tok = Token(SLASH, ch)
            '*' -> tok = Token(ASTERISK, ch)
            '<' -> tok = Token(LT, ch)
            '>' -> tok = Token(GT, ch)
            ';' -> tok = Token(SEMICOLON, ch)
            ':' -> tok = Token(COLON, ch)
            ',' -> tok = Token(COMMA, ch)
            '{' -> tok = Token(LBRACE, ch)
            '}' -> tok = Token(RBRACE, ch)
            '(' -> tok = Token(LPAREN, ch)
            ')' -> tok = Token(RPAREN, ch)
            '"' -> tok = Token(STRING, readString())
            '[' -> tok = Token(LBRACKET, ch)
            ']' -> tok = Token(RBRACKET, ch)
            '\u0000' -> tok = Token(EOF, "")
            else -> {
                if (isLetter(ch)) {
                    val ident = readIdentifier()
                    return Token(lookupIdent(ident), ident)
                } else if (isDigit(ch)) {
                    return readNumber()
                } else {
                    tok = Token(ILLEGAL, ch)
                }
            }
        }

        readChar()
        return tok
    }

    private fun skipWhitespace() {
        while (ch in whitespaces) {
            readChar()
        }
    }

    fun readChar() {
        ch = peekChar()
        position = readPosition
        readPosition += 1
    }

    private fun peekChar(): Char {
        return if (readPosition >= input.length) {
            '\u0000'
        } else {
            input[readPosition]
        }
    }

    private fun previousChar(): Char {
        return if (readPosition - 2 >= input.length || readPosition - 2 < 0) {
            '\u0000'
        } else {
            input[readPosition - 2]
        }
    }

    private fun readIdentifier(): String {
        val initial = position
        while (isLetter(ch)) {
            readChar()
        }
        return input.substring(initial until position)
    }

    private fun readNumber(): Token {
        val initial = position
        var isInt = true
        while (isDigit(ch)) {
            readChar()
        }
        if (ch == '.') {
            isInt = false
            readChar()
            while (isDigit(ch)) {
                readChar()
            }
        }

        return if (isInt) Token(INT, input.substring(initial until position))
        else Token(DOUBLE, input.substring(initial until position))
    }

    private fun readString(): String {
        val initial = position + 1
        var hasEscapements = false
        while (true) {
            readChar()
            if (ch == '\\') {
                hasEscapements = true
            }
            if (ch == '"' || ch == '\u0000') {
                if (previousChar() != '\\') {
                    break
                }
            }
        }

        return if (hasEscapements) input.substring(initial until position)
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\t", "\t")
            .replace("\\n", "\n")
            .replace("\\r", "\\r")
        else input.substring(initial until position)
    }

    private fun isLetter(ch: Char): Boolean {
        return ch in 'a'..'z' || ch in 'A'..'Z' || ch == '_'
    }

    private fun isDigit(ch: Char): Boolean {
        return ch in '0'..'9'
    }

    fun newToken(tokenType: TokenType, ch: Char): Token {
        return Token(tokenType, ch.toString())
    }
}