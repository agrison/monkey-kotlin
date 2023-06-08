import interpreter.repl.Repl

fun main() {
    println("Hello ${System.getProperty("user.name")}! This is the Monkey programming language!")
    println("Feel Free to type in commands\n")

    val repl = Repl()
    repl.start()
}