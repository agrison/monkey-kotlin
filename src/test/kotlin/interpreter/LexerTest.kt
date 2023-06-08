package interpreter

import org.junit.jupiter.api.Test

class LexerTest {
    @Test
    fun nextToken() {
        val input = """let five = 5;
let ten = 10;

let add = fn(x, y) {
  x + y;
};

let result = add(five, ten);
!-/*5;
5 < 10 > 5;

if (5 < 10) {
	return true;
} else {
	return false;
}

10 == 10;
10 != 9;
"foobar"
"foo bar"
[1, 2];
{"foo": "bar"}""";

        val lexer = Lexer.new(input)
        val tests = mutableListOf(
            Pair(LET, "let"),
            Pair(IDENT, "five"),
            Pair(ASSIGN, "="),
            Pair(INT, "5"),
            Pair(SEMICOLON, ";"),
            Pair(LET, "let"),
            Pair(IDENT, "ten"),
            Pair(ASSIGN, "="),
            Pair(INT, "10"),
            Pair(SEMICOLON, ";"),
            Pair(LET, "let"),
            Pair(IDENT, "add"),
            Pair(ASSIGN, "="),
            Pair(FUNCTION, "fn"),
            Pair(LPAREN, "("),
            Pair(IDENT, "x"),
            Pair(COMMA, ","),
            Pair(IDENT, "y"),
            Pair(RPAREN, ")"),
            Pair(LBRACE, "{"),
            Pair(IDENT, "x"),
            Pair(PLUS, "+"),
            Pair(IDENT, "y"),
            Pair(SEMICOLON, ";"),
            Pair(RBRACE, "}"),
            Pair(SEMICOLON, ";"),
            Pair(LET, "let"),
            Pair(IDENT, "result"),
            Pair(ASSIGN, "="),
            Pair(IDENT, "add"),
            Pair(LPAREN, "("),
            Pair(IDENT, "five"),
            Pair(COMMA, ","),
            Pair(IDENT, "ten"),
            Pair(RPAREN, ")"),
            Pair(SEMICOLON, ";"),
            Pair(BANG, "!"),
            Pair(MINUS, "-"),
            Pair(SLASH, "/"),
            Pair(ASTERISK, "*"),
            Pair(INT, "5"),
            Pair(SEMICOLON, ";"),
            Pair(INT, "5"),
            Pair(LT, "<"),
            Pair(INT, "10"),
            Pair(GT, ">"),
            Pair(INT, "5"),
            Pair(SEMICOLON, ";"),
            Pair(IF, "if"),
            Pair(LPAREN, "("),
            Pair(INT, "5"),
            Pair(LT, "<"),
            Pair(INT, "10"),
            Pair(RPAREN, ")"),
            Pair(LBRACE, "{"),
            Pair(RETURN, "return"),
            Pair(TRUE, "true"),
            Pair(SEMICOLON, ";"),
            Pair(RBRACE, "}"),
            Pair(ELSE, "else"),
            Pair(LBRACE, "{"),
            Pair(RETURN, "return"),
            Pair(FALSE, "false"),
            Pair(SEMICOLON, ";"),
            Pair(RBRACE, "}"),
            Pair(INT, "10"),
            Pair(EQ, "=="),
            Pair(INT, "10"),
            Pair(SEMICOLON, ";"),
            Pair(INT, "10"),
            Pair(NOT_EQ, "!="),
            Pair(INT, "9"),
            Pair(SEMICOLON, ";"),
            Pair(STRING, "foobar"),
            Pair(STRING, "foo bar"),
            Pair(LBRACKET, "["),
            Pair(INT, "1"),
            Pair(COMMA, ","),
            Pair(INT, "2"),
            Pair(RBRACKET, "]"),
            Pair(SEMICOLON, ";"),
            Pair(LBRACE, "{"),
            Pair(STRING, "foo"),
            Pair(COLON, ":"),
            Pair(STRING, "bar"),
            Pair(RBRACE, "}"),
            Pair(EOF, "")
        )

        tests.forEach { (expectedTokenType, expectedLiteral) ->
            val token = lexer.nextToken()

            assert(token.type == expectedTokenType) {
                "tokentype wrong. expected=${expectedTokenType}, got=${token.type}"
            }
            assert(token.literal == expectedLiteral) {
                "literal wrong. expected=${expectedLiteral}, got=${token.literal}"
            }
        }
    }

}