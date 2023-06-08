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
                    return Token(INT, readNumber())
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

    private fun readIdentifier(): String {
        val initial = position
        while (isLetter(ch)) {
            readChar()
        }
        return input.substring(initial until position)
    }

    private fun readNumber(): String {
        val initial = position
        while (isDigit(ch)) {
            readChar()
        }
        return input.substring(initial until position)
    }

    private fun readString(): String {
        val initial = position + 1
        while (true) {
            readChar()
            if (ch == '"' || ch == '\u0000') {
                break
            }
        }

        return input.substring(initial until position)
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