import interpreter.evaluator.Evaluator
import interpreter.lexer.Lexer
import interpreter.`object`.Environment
import interpreter.parser.Parser

fun main() {
    val env = Environment.newEnvironment()

    val input = """
let name = "Monkey";
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

let numbers = [1, 1 + 1, 4 - 1, 2 * 2, 2 + 3, 12 / 2];
map(numbers, fibonacci);

"""

    val lexer = Lexer.new(input)
    val parser = Parser.new(lexer)

    val program = parser.parseProgram()
    if (parser.errors().isNotEmpty()) {
        println(parser.errors)
    }

    val evaluated = Evaluator().eval(program, env)
    println(evaluated.inspect())
}

