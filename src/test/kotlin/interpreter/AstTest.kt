package interpreter

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

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