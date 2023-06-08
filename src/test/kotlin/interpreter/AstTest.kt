package interpreter

import interpreter.ast.Identifier
import interpreter.ast.LetStatement
import interpreter.ast.Program
import interpreter.token.IDENT
import interpreter.token.LET
import interpreter.token.Token
import org.junit.jupiter.api.Test

class AstTest {

    @Test
    fun testString() {
        val program = Program(
            mutableListOf(
                LetStatement(
                    token = Token(LET, "let"),
                    name = Identifier(Token(IDENT, "myVar"), "myVar"),
                    value = Identifier(Token(IDENT, "anotherVar"), "anotherVar")
                )
            )
        )

        assert(program.toString() == "let myVar = anotherVar;") {
            "program.string() wrong. got=${program}"
        }
    }
}