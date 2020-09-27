package org.example;

import org.example.Expression.*;
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        Expression two = new Constant(2);
        Expression four = new Constant(4);
        Expression negOne = new Negate(new Constant(1));
        Expression sumTwoFour = new Addition(two, four);
        Expression mult = new Multiplication(sumTwoFour, negOne);
        Expression exp = new Exponent(mult, 2);
        Expression res = new Addition(exp, new Constant(1));

        System.out.println(res + " = " + evaluate(res));
    }

    private static int evaluate(Expression expr) {
        return switch (expr) {
            case Constant(int c) -> c;
            case Negate(Expression e) -> -evaluate(e);
            case Exponent(Expression e, int exp) -> (int) Math.pow(evaluate(e), exp);
            case Addition(Expression l, Expression r) -> evaluate(l) + evaluate(r);
            case Multiplication(Expression l, Expression r) -> evaluate(l) * evaluate(r);
        };
    }
}
