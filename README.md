## Writing an interpreter in <strike>Go</strike> Kotlin

I recently bought the book *Writing an interpreter in Go* by *Thorsten Ball*, and it was a great read.
I had much fun, rediscovering things I learnt at University but in C with lexx/yacc and OCaml.

So I took a chance at writing along the code in Kotlin while reading the book, so that we have a Monkey interpreter in Kotlin.

I will also make some adaptations to the builtins functions and supporting doubles, Strings with escapement characters, etc.

The code is here in this repository.

## Structure

```
+ interpreter/
  + ast/ 
  + evaluator/
  + lexer/
  + object/
  + parser/
  + repl/
  + token/
  Main.kt <- main program
```

## Tests

Tests are located in `src/test/kotlin`.

```
Tests passed: 131 of 131 tests - 161ms
```
