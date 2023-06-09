package interpreter

import interpreter.evaluator.Evaluator
import interpreter.lexer.Lexer
import interpreter.`object`.Environment
import interpreter.parser.Parser
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class Sample {
    @Test
    fun testSampleCode() {
        val env = Environment.newEnvironment()

        val input = """
let name = "M\ton\\k\"ey\nlol";
puts(name);
let age = 1;
let inspirations = ["Scheme", "Lisp", "JavaScript", "Clojure"];
let book = {
  "title": "Writing A Compiler In Go",
  "author": "Thorsten Ball",
  "prequel": "Writing An Interpreter In Go"
};
let printBookName = fn(book) {
  let title = book["title"];
  let author = book["author"];
  puts(author + " - " + title);
};
printBookName(book);

let fibonacci = fn(x) {
  if (x == 0) {
    0
  } else {
    if (x == 1) {
      return 1;
    } else {
      fibonacci(x - 1) + fibonacci(x - 2);
    }
  }
};

let map = fn(arr, f) {
  let iter = fn(arr, accumulated) {
    if (len(arr) == 0) {
      accumulated
    } else {
      iter(rest(arr), push(accumulated, f(first(arr))));
    }
  };

  iter(arr, []);
};

let numbers = [1.0, 1 + 1, 4 - 1, 2 * 2, 2 + 3, 12 / 2, 17.0];
map(numbers, fibonacci);

"""
        val b = ByteArrayOutputStream()
        System.setOut(PrintStream(b))
        val lexer = Lexer.new(input)
        val parser = Parser.new(lexer)

        val program = parser.parseProgram()
        if (parser.errors().isNotEmpty()) {
            println(parser.errors)
        }

        val evaluated = Evaluator().eval(program, env)
        assert(evaluated.inspect() == "[1, 1, 2, 3, 5, 8, 1597]")
        assert(b.toString() == "M\ton\\k\"ey\nlol${System.lineSeparator()}Thorsten Ball - Writing A Compiler In Go${System.lineSeparator()}")
    }
}
