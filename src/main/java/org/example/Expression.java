package org.example;

public sealed interface Expression
        permits Constant, Negate, Exponent, Addition, Multiplication {}

record Constant(int c) implements Expression {}
record Negate(Expression expression) implements Expression {}
record Exponent(Expression expression, int exponent) implements Expression {}
record Addition(Expression left, Expression right) implements Expression {}
record Multiplication(Expression left, Expression right) implements Expression{}
