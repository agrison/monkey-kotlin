## Writing an interpreter in <strike>Go</strike> Kotlin

I recently bought the bundle of books [*Writing an interpreter in Go* and *Writing a compiler in Go*](https://compilerbook.com/) by *Thorsten Ball*, and it was a great read.

I had much fun, rediscovering things I learnt at University but in C with lex/yacc and OCaml (ocamllex, ocamlyacc).

So I took a chance at writing along the code in Kotlin while reading the book, so that we have a Monkey interpreter in Kotlin.

I have also made some adaptations to the builtins functions and supporting Doubles, Strings with escapement characters, String repeat, Array and Hash concatenations, etc.

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
puts(map(numbers, fibonacci));
```

#### Output

```text
M	on\k"ey
    is kool
Thorsten Ball - Writing A Compiler In Go
[1, 1, 2, 3, 5, 8, 1597]
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
