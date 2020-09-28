Over the weekend I decided to take a moment to play a bit with the exciting new features of Java Records and Sealed classes 
as previewed in Java 14. I already had an idea of how I could use them: I remembered that I had answered a question in Stackoverflow 
about [good examples of inheritance][1]. The question was more about class inheritance, and I failed to answer it properly because 
my answer was about interface inheritance. Nonetheless, it had a lot of familiarity with examples provided by Brian Goetz in 
[his seminal article][2] about these new Java features, so I decided it was a good place to start and fortunately for me, 
IntelliJ already has [support for these new features][3].

## The Old Way

In my original example, I present an interface Expression to represent a mathematical expression that evaluates to an integer:

```java
public interface Expression {
    int evaluate();

}
```

And then I provide a number of classes that implement different kinds of expressions: a constant, a negation, an addition, a subtraction, a multiplication, and exponentiation expressions. Each one provides an implementation of how to evaluate the expression to an integer and a toString expression of how to present it as a string.

```java
public interface Expression {

    int evaluate();

    public class Constant implements Expression {

        private final int value;

        public Constant(int value) {
            this.value = value;
        }

        @Override
        public int evaluate() {
            return this.value;
        }

        @Override
        public String toString() {
            return String.format(" %d ", this.value);
        }

    }

    public class Negate implements Expression {

        private final Expression expression;

        public Negate(Expression expression) {
            this.expression = expression;
        }

        @Override
        public int evaluate() {
            return -(this.expression.evaluate());
        }

        @Override
        public String toString() {
            return String.format(" -%s ", this.expression);
        }
    }

    public class Exponent implements Expression {

        private final Expression expression;
        private final int exponent;

        public Exponent(Expression expression, int exponent) {
            this.expression = expression;
            this.exponent = exponent;
        }

        @Override
        public int evaluate() {
            return (int) Math.pow(
               this.expression.evaluate(), this.exponent
            );
        }

        @Override
        public String toString() {
            return String.format(" %s ^ %d", 
              this.expression, this.exponent);
        }

    }

    public class Addition implements Expression {

        private final Expression left;
        private final Expression right;

        public Addition(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int evaluate() {
            return this.left.evaluate() + this.right.evaluate();
        }

        @Override
        public String toString() {
            return String.format(" (%s + %s) ", this.left, this.right);
        }
    }

    public class Multiplication implements Expression {

        private final Expression left;
        private final Expression right;

        public Multiplication(Expression left, Expression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int evaluate() {
            return this.left.evaluate() *  this.right.evaluate();
        }

        @Override
        public String toString() {
            return String.format(" (%s * %s) ", this.left, this.right);
        }
    }

}
```

With this simple set of implementations we could easily represent and evaluate integer mathematical expressions, like the following:

```java
Expression two = new Constant(2);
Expression four = new Constant(4);
Expression negOne = new Negate(new Constant(1));
Expression sumTwoFour = new Addition(two, four);
Expression mult = new Multiplication(sumTwoFour, negOne);
Expression exp = new Exponent(mult, 2);
Expression res = new Addition(exp, new Constant(1));

System.out.println(res + " = " + res.evaluate());
```

Which yields the following to the main output: ```( ( ( 2 + 4 ) * - 1 ) ^ 2 + 1 ) = 37.```

# The New Way

Now, we could reconsider our perspective of the mathematical expressions, perhaps consider it just a vessel to transport data, 
but not logic. This is more compatible with the traditional way of thinking in functional programming languages that use 
[algebraic data types][4] to hold the data and functions that act on those types to provide the behavior.

If we do it that way we could reconsider the definition of our expression interface as follows:

```java
public sealed interface Expression
        permits Constant, Negate, Exponent, Addition, Multiplication {}
```

By this, we mean that there is a finite set of implementations for the ```Expression``` interface. 
Such implementations would be the equivalent of an algebraic data type in statically-typed functional languages like Haskell or SML. 
The records are just vessels to carry the data, or as Brian Goetz says: 'the state, the whole state, and nothing but the state'. 
As such, they offer no implementation of any particular behavior other than what the record keyword guarantees: 
`hashCode`, `toString`, and data accessors to recover the data.

```java
record Constant(int c) implements Expression{}
record Negate(Expression expression) implements Expression{}
record Exponent(Expression expression,int exponent) implements Expression{}
record Addition(Expression left, Expression right) implements Expression{}
record Multiplication(Expression left, Expression right) implements Expression{}
```

Up to this point, we can represent our expression composite just as we did before.

```java
Expression two = new Constant(2);
Expression four = new Constant(4);
Expression negOne = new Negate(new Constant(1));
Expression sumTwoFour = new Addition(two, four);
Expression mult = new Multiplication(sumTwoFour, negOne);
Expression exp = new Exponent(mult, 2);
Expression res = new Addition(exp, new Constant(1));
```

But, how do we *evaluate* it? There is no ```evaluate``` method in the interface?

Well, following the same pattern we would have used in a functional programming language we need to define an evaluate function 
that can act on our record vessels. Here's where JDK 14 compiler is still missing a key piece of functionality: 
a structural type deconstruction that is typically known as [pattern matching][5].

This is how it is supposed to look like eventually:

```java
private static int evaluate(Expression expr) {
    return switch (expr) {
        case Constant(int c) -> c;
        case Negate(Expression e) -> -evaluate(e);
        case Exponent(Expression e, int exp) -> 
               (int) Math.pow(evaluate(e), exp);
        case Addition(Expression l, Expression r) -> 
               evaluate(l) + evaluate(r);
        case Multiplication(Expression l, Expression r) -> 
               evaluate(l) * evaluate(r);
    };
}
```

Notice how the case expressions deconstruct the record by extracting its state and binding it to specific variables declared in the 
record pattern used in the expression. Those binding are later used to evaluate the expression. The function is recursive, and so 
the base case is the Const pattern, since that we can evaluate directly to its constant integer value. The rest of the expressions 
represent an inductive step and are evaluated recursively until they finally reach the constant expression that they encapsulate.

However, this deconstruction functionality is not yet ready and I believe it won't be ready in Java 15 either. Java is getting pretty 
close to the same levels of expressivity of traditional functional programming languages.

Did you know that this sort of feature has existed for a long time? For example, ML has it since 1973. That is 47 years ago! 
In Haskell since 1990, around 30 years ago.

I have more familiarity with SML, so here's an example of how this could be represented in SML code. Notice the initial definition 
of the expression algebraic data type, and then two functions that act on the data type: ```evaluate``` and ```toString```. 
The functions use pattern matching to evaluate the expression to an integer and to represent the contents of the expression as a string 
respectively.

```sml
datatype expression = Const of int 
                    | Neg of expression
                    | Add of (expression * expression)
                    | Sub of (expression * expression)
                    | Mul of (expression * expression)
                    | Div of (expression * expression)

fun evaluate expr =
    case expr of
        Const(x) => x
      | Neg(x) => evaluate(x)
      | Add(x,y) => evaluate(x) + evaluate(y)
      | Sub(x,y) => evaluate(x) - evaluate(y)
      | Mul(x,y) => evaluate(x) * evaluate(y)
      | Div(x,y) => evaluate(x) div evaluate(y)

fun toString expr =
    case expr of
        Const(x) => Int.toString(x)
      | Neg(x) => toString(x)
      | Add(x,y) => " (" ^ toString(x) ^ " + " ^ toString(y) ^ ") "
      | Sub(x,y) => " (" ^ toString(x) ^ " - " ^ toString(y) ^ ") "
      | Mul(x,y) => " (" ^ toString(x) ^ " * " ^ toString(y) ^ ") "
      | Div(x,y) => " (" ^ toString(x) ^ " / " ^ toString(y) ^ ") "


val expr =  Div(Mul(Add(Const(2), Const(4)), Neg(Const(4))), Const(2))
val res = toString(expr) ^ " = " ^ Int.toString(evaluate(expr))
```

The last expression yields the result: ```( ( (2 + 4) * 4) / 2) = 12```.

This transition of object-oriented programming languages like Java and C# to support traditional functional programming constructs 
has been a trend for a while now. Curiously, the opposite transition is much older: most functional programming languages already 
had support for object types long ago.

A few examples:

* The [Racket Programming Language][6], a descendant of Scheme, [supports objects, classes, and inheritance][7]. 
* [Clojure][8], a LISP that runs on top of the Java Virtual Machine, [can make use of objects][9].
* [F#][10], a descendant of ML, runs on top of .net framework, and [can also use objects][11].
* [OCaml][12], another descendant of ML, [makes use of objects][13].
* [Scala][14], is a powerful combination of [object-orientation and functional programming features][15].
* In JavaScript, the transition is more recent, but the language has always supported objects, functions as objects, and using prototypal inheritance it has always being possible to express class hierarchies.

The point being that object-orientation and functional orientation are not mutually exclusive concepts.

## Further Reference

* [Data Classes and Sealed Classes][2] by Brian Goetz
* [Java Feature Spotlight: Sealed Classes][16] by Brian Goetz
* [Java 14 Feature Spotlight: Records][17] by Brian Goetz
* [Does functional programming use object data structures?][18]

[1]:https://stackoverflow.com/a/14924652/697630
[2]:http://cr.openjdk.java.net/~briangoetz/amber/datum_3.html
[3]:https://blog.jetbrains.com/idea/2020/09/java-15-and-intellij-idea/
[4]:https://en.wikipedia.org/wiki/Algebraic_data_type
[5]:https://en.wikipedia.org/wiki/Standard_ML#Algebraic_datatypes_and_pattern_matching
[6]:https://racket-lang.org/
[7]:https://docs.racket-lang.org/reference/mzlib_class.html
[8]:https://clojure.org/
[9]:https://clojure.org/reference/java_interop
[10]:http://fsharp.org/
[11]:https://fsharpforfunandprofit.com/posts/classes/
[12]:https://ocaml.org/
[13]:http://caml.inria.fr/pub/docs/manual-ocaml/objectexamples.html
[14]:https://www.scala-lang.org/
[15]:http://docs.scala-lang.org/tour/classes.html
[16]:https://www.infoq.com/articles/java-sealed-classes
[17]:https://www.infoq.com/articles/java-14-feature-spotlight
[18]:https://stackoverflow.com/a/47822256/697630
