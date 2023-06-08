package interpreter

import interpreter.evaluator.Evaluator
import interpreter.lexer.Lexer
import interpreter.`object`.*
import interpreter.parser.Parser
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.Boolean

class EvaluatorTest {
    @ParameterizedTest
    @CsvSource(
        value = [
            "5, 5",
            "10, 10",
            "-5, -5",
            "-10, -10",
            "5 + 5 + 5 + 5 - 10, 10",
            "2 * 2 * 2 * 2 * 2, 32",
            "-50 + 100 + -50, 0",
            "5 * 2 + 10, 20",
            "5 + 2 * 10, 25",
            "20 + 2 * -10, 0",
            "50 / 2 * 2 + 10, 60",
            "2 * (5 + 10), 30",
            "3 * 3 * 3 + 10, 37",
            "3 * (3 * 3) + 10, 37",
            "(5 + 10 * 2 + 15 / 3) * 2 + -10, 50"
        ]
    )
    fun testEvalIntegerExpressions(input: String, expected: Int) {
        val evaluated = testEval(input)
        testIntegerObject(evaluated, expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "true, true",
            "false, false",
            "1 < 2, true",
            "1 > 2, false",
            "1 < 1, false",
            "1 > 1, false",
            "1 == 1, true",
            "1 != 1, false",
            "1 == 2, false",
            "1 != 2, true",
            "true == true, true",
            "false == false, true",
            "true == false, false",
            "true != false, true",
            "false != true, true",
            "(1 < 2) == true, true",
            "(1 < 2) == false, false",
            "(1 > 2) == true, false",
            "(1 > 2) == false, true",
        ]
    )
    fun testEvalBooleanExpression(input: String, expected: Boolean) {
        val evaluated = testEval(input)
        testBooleanObject(evaluated, expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "!true, false",
            "!false, true",
            "!5, false",
            "!!true, true",
            "!!false, false",
            "!!5, true",
        ]
    )
    fun testBangOperator(input: String, expected: Boolean) {
        val evaluated = testEval(input)
        testBooleanObject(evaluated, expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "if (true) { 10 }, 10",
            "if (false) { 10 }, null",
            "if (1) { 10 }, 10",
            "if (1 < 2) { 10 }, 10",
            "if (1 > 2) { 10 }, null",
            "if (1 > 2) { 10 } else { 20 }, 20",
            "if (1 < 2) { 10 } else { 20 }, 10",
        ]
    )
    fun testIfElseExpressions(input: String, expected: String) {
        val evaluated = testEval(input)
        if (expected == "null") {
            testNullObject(evaluated)
        } else {
            testIntegerObject(evaluated, expected.toInt())
        }
    }

    @ParameterizedTest
    @MethodSource("returnStatements")
    fun testReturnStatements(input: String, expected: Int) {
        val evaluated = testEval(input)
        testIntegerObject(evaluated, expected)
    }

    @ParameterizedTest
    @MethodSource("errorHandling")
    fun testErrorHandling(input: String, expected: String) {
        val evaluated = testEval(input)
        assert(evaluated is MError) { "no error object returned. got=${evaluated}" }
        val message = (evaluated as MError).value
        assert(message == expected) { "wrong error message. expected=${expected}, got=${message}" }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "let a = 5; a;, 5",
            "let a = 5 * 5; a;, 25",
            "let a = 5; let b = a; b;, 5",
            "let a = 5; let b = a; let c = a + b + 5; c;, 15",
        ]
    )
    fun testLetExpressions(input: String, expected: Int) {
        val evaluated = testEval(input)
        testIntegerObject(evaluated, expected)
    }

    @Test
    fun testFunctionObject() {
        val input = "fn(x) { x + 2; };"
        val evaluated = testEval(input)

        assert(evaluated is MFunction) { "object is not a function. got=${evaluated.type()}" }
        val fn = (evaluated as MFunction)
        assert(fn.parameters.size == 1) { "function has wrong parameters. Parameters=${fn.parameters}" }
        assert(fn.parameters[0].toString() == "x") { "parameter is not 'x'. got=${fn.parameters[0]}" }
        assert(fn.body.toString() == "(x + 2)") { "body is not (x + 2). got=${fn.body}" }
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "#", value = [
            "let identity = fn(x) { x; }; identity(5);# 5",
            "let identity = fn(x) { return x; }; identity(5);# 5",
            "let double = fn(x) { x * 2; }; double(5);# 10",
            "let add = fn(x, y) { x + y; }; add(5, 5);# 10",
            "let add = fn(x, y) { x + y; }; add(5 + 5, add(5, 5));# 20",
            "fn(x) { x; }(5)# 5",
        ]
    )
    fun testFunctionApplication(input: String, expected: Int) {
        val evaluated = testEval(input)
        testIntegerObject(evaluated, expected)
    }

    @Test
    fun testEnclosingEnvironments() {
        val input = """
let first = 10;
let second = 10;
let third = 10;

let ourFunction = fn(first) {
  let second = 20;

  first + second + third;
};

ourFunction(20) + first + second;"""
        testIntegerObject(testEval(input), 70)
    }

    @Test
    fun testClosures() {
        val input = """
let newAdder = fn(x) {
  fn(y) { x + y };
};

let addTwo = newAdder(2);
addTwo(2);"""
        testIntegerObject(testEval(input), 4)
    }

    @Test
    fun testStringLiteral() {
        val input = "\"Hello World!\""
        val evaluated = testEval(input)
        assert(evaluated is MString)
        assert((evaluated as MString).value == "Hello World!")
    }

    @Test
    fun testStringConcatenation() {
        val input = "\"Hello\" + \" \" + \"World!\""
        val evaluated = testEval(input)
        assert(evaluated is MString)
        assert((evaluated as MString).value == "Hello World!")
    }

    @Test
    fun testStringTimes() {
        val input = "\"Hello\" * 3"
        val evaluated = testEval(input)
        assert(evaluated is MString)
        assert((evaluated as MString).value == "HelloHelloHello")
    }

    @Test
    fun testArrayLiterals() {
        val input = "[1, 2*2, 3+3]"
        val evaluated = testEval(input)

        assert(evaluated is MArray)
        val array = evaluated as MArray

        assert(array.elements.size == 3)
        testIntegerObject(array.elements[0], 1)
        testIntegerObject(array.elements[1], 4)
        testIntegerObject(array.elements[2], 6)
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "#", value = [
            "[1, 2, 3][0]# 1",
            "[1, 2, 3][1]# 2",
            "[1, 2, 3][2]# 3",
            "let i = 0; [1][i];# 1",
            "[1, 2, 3][1 + 1];# 3",
            "let myArray = [1, 2, 3]; myArray[2];# 3",
            "let myArray = [1, 2, 3]; myArray[0] + myArray[1] + myArray[2];# 6",
            "let myArray = [1, 2, 3]; let i = myArray[0]; myArray[i]# 2",
            "[1, 2, 3][3]# null",
            "[1, 2, 3][-1]# null",
        ]
    )
    fun testArrayIndexExpressions(input: String, expected: String) {
        if (expected == "null") {
            testNullObject(testEval(input))
        } else {
            testIntegerObject(testEval(input), expected.toInt())
        }
    }

    @Test
    fun testHashLiterals() {
        val input = """
    let two = "two";
	{
		"one": 10 - 9,
		two: 1 + 1,
		"thr" + "ee": 6 / 2,
		4: 4,
		true: 5,
		false: 6
	}"""

        val evaluated = testEval(input)
        assert(evaluated is Hash) { "Eval didn't return hash." }

        val expected = Hash(
            mutableMapOf(
                MString("one").hashKey() to HashPair(MString("one"), MInteger(1)),
                MString("two").hashKey() to HashPair(MString("two"), MInteger(2)),
                MString("three").hashKey() to HashPair(MString("three"), MInteger(3)),
                MInteger(4).hashKey() to HashPair(MInteger(4), MInteger(4)),
                MBoolean(true).hashKey() to HashPair(MBoolean(true), MInteger(5)),
                MBoolean(false).hashKey() to HashPair(MBoolean(false), MInteger(6))
            )
        )

        assert(expected == evaluated)
    }

    @ParameterizedTest
    @CsvSource(
        delimiterString = "#", value = [
            "{\"foo\": 5}[\"foo\"]# 5",
            "{\"foo\": 5}[\"bar\"]# null",
            "let key = \"foo\"; {\"foo\": 5}[key]# 5",
            "{}[\"foo\"]# null",
            "{5: 5}[5]# 5",
            "{true: 5}[true]# 5",
            "{false: 5}[false]# 5",
        ]
    )
    fun testHashIndexExpressions(input: String, expected: String) {
        if (expected == "null") {
            testNullObject(testEval(input))
        } else {
            testIntegerObject(testEval(input), expected.toInt())
        }
    }

    @ParameterizedTest
    @MethodSource("builtinsSource")
    fun testBuiltinFunctions(input: String, expected: Any?) {
        when (expected) {
            null -> testNullObject(testEval(input))
            is Int -> testIntegerObject(testEval(input), expected.toInt())

            is String -> {
                val evaluated = testEval(input)
                assert(evaluated is MError) { "no error object returned. got=${evaluated}" }
                val message = (evaluated as MError).value
                assert(message == expected) { "wrong error message. expected=${expected}, got=${message}" }
            }

            is List<*> -> {
                val evaluated = testEval(input)
                assert(evaluated is MArray)
                val array = evaluated as MArray
                array.elements.forEachIndexed { i, elem ->
                    testIntegerObject(elem, expected[i] as Int)
                }
            }
        }
    }

    private fun testEval(input: String): MonkeyObject {
        val lexer = Lexer.new(input)
        val parser = Parser.new(lexer)
        val program = parser.parseProgram()
        val env = Environment.newEnvironment()
        return Evaluator().eval(program, env)
    }

    private fun testIntegerObject(obj: MonkeyObject, expected: Int) {
        assert(obj is MInteger) { "object is not Integer. got=${obj.type()}" }
        val value = (obj as MInteger).value
        assert(value == expected) { "integer has wrong value. got=${value}, want=${expected}" }
    }

    private fun testBooleanObject(obj: MonkeyObject, expected: Boolean) {
        assert(obj is MBoolean) { "object is not Boolean. got=${obj.type()}" }
        val value = (obj as MBoolean).value
        assert(value == expected) { "boolean has wrong value. got=${value}, want=${expected}" }
    }

    private fun testNullObject(obj: MonkeyObject) {
        assert(obj is MNull) {
            "object is not interpreter.getNULL. got=${obj}"
        }
    }

    companion object {
        @JvmStatic
        fun returnStatements(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("return 10;", 10),
                Arguments.of("return 10; 9", 10),
                Arguments.of("return 2 * 5; 9;", 10),
                Arguments.of("9; return 2 * 5; 9;", 10),
                Arguments.of("if (10 > 1) { return 10; }", 10),
                Arguments.of(
                    """
if (10 > 1) {
  if (10 > 1) {
    return 10;
  }

  return 1;
}""", 10
                ),
                Arguments.of(
                    """
let f = fn(x) {
  return x;
  x + 10;
};
f(10);""", 10
                ),
                Arguments.of(
                    """
let f = fn(x) {
   let result = x + 10;
   return result;
   return 10;
};
f(10);""", 20
                )
            )
        }

        @JvmStatic
        fun errorHandling(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("5 + true;", "type mismatch: INTEGER + BOOLEAN"),
                Arguments.of("5 + true; 5;", "type mismatch: INTEGER + BOOLEAN"),
                Arguments.of("-true", "unknown operator: -BOOLEAN"),
                Arguments.of("true + false;", "unknown operator: BOOLEAN + BOOLEAN"),
                Arguments.of("true + false + true + false;", "unknown operator: BOOLEAN + BOOLEAN"),
                Arguments.of("5; true + false; 5", "unknown operator: BOOLEAN + BOOLEAN"),
                Arguments.of("\"Hello\" - \"World\"", "unknown operator: STRING - STRING"),
                Arguments.of("if (10 > 1) { true + false; }", "unknown operator: BOOLEAN + BOOLEAN"),
                Arguments.of(
                    """
if (10 > 1) {
  if (10 > 1) {
    return true + false;
  }

  return 1;
}""", "unknown operator: BOOLEAN + BOOLEAN"
                ),
                Arguments.of("foobar", "identifier not found: foobar"),
                Arguments.of("{\"name\": \"Monkey\"}[fn(x) { x }];", "unusable as hash key: FUNCTION"),
                Arguments.of("999[1]", "index operator not supported: INTEGER"),
            )
        }

        @JvmStatic
        fun builtinsSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("len(\"\")", 0),
                Arguments.of("len(\"four\")", 4),
                Arguments.of("len(\"hello world\")", 11),
                Arguments.of("len(1)", "argument to `len` not supported, got INTEGER"),
                Arguments.of("len(\"one\", \"two\")", "wrong number of arguments. got=2, want=1"),
                Arguments.of("len([1, 2, 3])", 3),
                Arguments.of("len([])", 0),
                Arguments.of("puts(\"hello\", \"world!\")", null),
                Arguments.of("first([1, 2, 3])", 1),
                Arguments.of("first([])", null),
                Arguments.of("first(1)", "argument to `first` must be ARRAY or STRING, got INTEGER"),
                Arguments.of("last([1, 2, 3])", 3),
                Arguments.of("last([])", null),
                Arguments.of("last(1)", "argument to `last` must be ARRAY or STRING, got INTEGER"),
                Arguments.of("rest([1, 2, 3])", listOf(2, 3)),
                Arguments.of("rest([])", null),
                Arguments.of("push([], 1)", listOf(1)),
                Arguments.of("push(1, 1)", "argument to `push` must be ARRAY, got INTEGER"),
            )
        }
    }
}