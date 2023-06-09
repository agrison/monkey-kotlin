## Writing an interpreter in <strike>Go</strike> Kotlin: Monktey

I recently bought the bundle of books [*Writing an interpreter in Go*](https://interpreterbook.com/) and [*Writing a compiler in Go*](https://compilerbook.com/) by *Thorsten Ball*, and it was a great read.

I had much fun, rediscovering things I learnt at University but in C with lex/yacc and OCaml (ocamllex, ocamlyacc).

So I took a chance at writing along the code in Kotlin while reading the book, so that we have a Monkey interpreter in Kotlin.

I have also made some adaptations to the builtins functions and supporting:
- Doubles (`3.14`)
- Strings with escapement characters (`\n`, `\t`, `\r`, `\\`)
- String repeat (`"a" * 3`)
- Array and Hash concatenations (`[0] + [1]`, `{a: 1} + {b: 2}`)
- Ranges (`0..4`, `[0, 1, 2, 3, 4][1..2] == [1, 2]`)
- Negative indexing (`[0, 1, 2, 3, 4][-1] == 4`)

I'd like to implement:
- `for`/`while` loops (including `break`/`continue`)
- infix `if` called when / or similarly unless
- more operators (in (contains), mod, <=, >=, etc)
- macro system from the book
- ...

The code is here in this repository.

## Sample

```js
let name = "M\ton\\k\"ey\n\tis kool";
puts(name);
let age = 1;
let inspirations = ["Scheme", "Lisp", "JavaScript", "Clojure"];
let prequel = {
  "prequel": "Writing An Interpreter in Go"
};
let newBook = {
  "title": "Writing A Compiler In Go",
  "author": "Thorsten Ball",
};
let book = prequel + newBook;

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

let numbers = [1.0, 1 + 1, 4 - 1, 2 * 2, 2 + 3] + [12 / 2, 17.0];
let fib = map(numbers, fibonacci);
puts(fib);
puts(fib[2..4]);
```

#### Output

```text
M	on\k"ey
    is kool
Thorsten Ball - Writing A Compiler In Go
[1, 1, 2, 3, 5, 8, 1597]
[2, 3, 5]
```

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
