package interpreter.repl

import interpreter.evaluator.Evaluator
import interpreter.lexer.Lexer
import interpreter.`object`.Environment
import interpreter.parser.Parser
import java.util.*

const val PROMPT = ">> "

class Repl {
    fun start() {
        val keyboard = Scanner(System.`in`)
        val env = Environment.newEnvironment()

        while (true) {
            print(PROMPT)
            val scanned = keyboard.nextLine()
            if (scanned == ":q") {
                return
            }

            val lexer = Lexer.new(scanned)
            val parser = Parser.new(lexer)

            val program = parser.parseProgram()
            if (parser.errors().isNotEmpty()) {
                printParseErrors(parser.errors)
                continue
            }

            val evaluated = Evaluator().eval(program, env)
            println(evaluated.inspect())
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