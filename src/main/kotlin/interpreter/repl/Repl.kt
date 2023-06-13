package interpreter.repl

import interpreter.evaluator.Evaluator
import interpreter.lexer.Lexer
import interpreter.`object`.Environment
import interpreter.parser.Parser

const val PROMPT = ">> "

class Repl {
    fun start() {
        val env = Environment.newEnvironment()
        val buffer = StringBuilder()

        while (true) {
            print(if (!buffer.contains("\n")) PROMPT else "...\t")
            val scanned = readln()
            buffer.append(scanned + "\n")

            // this is not the proper way of doing, but for the time being it's probably sufficient :)
            if (buffer.count { it in charArrayOf('(', '{', '[') } != buffer.count { it in charArrayOf(')', '}', ']') }) {
                continue
            }

            val lexer = Lexer.new(buffer.toString())
            val parser = Parser.new(lexer)

            val program = parser.parseProgram()
            if (parser.errors().isNotEmpty()) {
                printParseErrors(parser.errors)
                continue
            }

            val evaluated = Evaluator().eval(program, env)
            println(evaluated.inspect())
            buffer.clear()
        }
    }

    private fun printParseErrors(errors: List<String>) {
        println(MONKEY_FACE)
        println("Woops! We ran into some monkey business here!")
        println(" parser errors: ")
        errors.forEach { error -> println("\t${error}") }
    }
}

const val MONKEY_FACE = """            __,__
   .--.  .-"     "-.  .--.
  / .. \/  .-. .-.  \/ .. \
 | |  '|  /   Y   \  |'  | |
 | \   \  \ 0 | 0 /  /   / |
  \ '- ,\.-“““““““-./, -' /
   ''-' /_   ^ ^   _\ '-''
       |  \._   _./  |
       \   \ '~' /   /
        '._ '-=-' _.'
           '-----'
"""