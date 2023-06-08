import interpreter.Repl

fun main(args: Array<String>) {
    println("Hello ! This is the Monkey programming language!")
    println("Feel Free to type in commands\n")

    val repl = Repl()
    repl.start()
}