package oneblock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regression tests for the {@link CompletionExprParser} DSL. */
class CompletionExprTest {

  private static CompletionExpr parse(String expr, String... allGroups) {
    return CompletionExprParser.parse(expr, new HashSet<>(Arrays.asList(allGroups)));
  }

  private static boolean eval(CompletionExpr expr, String... completed) {
    return expr.evaluate(new HashSet<>(Arrays.asList(completed)));
  }

  @Test
  @DisplayName("null/empty expression defaults to ANY (legacy fallback)")
  void defaultIsAny() {
    CompletionExpr e = parse(null, "a", "b", "c");
    assertFalse(eval(e)); // nothing completed
    assertTrue(eval(e, "a")); // one group
    assertTrue(eval(e, "b", "c")); // many groups
  }

  @Test
  @DisplayName("explicit ANY keyword behaves like default")
  void explicitAny() {
    CompletionExpr e = parse("any", "a", "b");
    assertFalse(eval(e));
    assertTrue(eval(e, "a"));
    assertTrue(eval(e, "b"));
    assertTrue(eval(e, "a", "b"));
  }

  @Test
  @DisplayName("ALL keyword requires every group")
  void allGroups() {
    CompletionExpr e = parse("all", "a", "b");
    assertFalse(eval(e));
    assertFalse(eval(e, "a"));
    assertTrue(eval(e, "a", "b"));
  }

  @Test
  @DisplayName("single group reference")
  void singleGroup() {
    CompletionExpr e = parse("gather", "gather", "hunt");
    assertFalse(eval(e));
    assertTrue(eval(e, "gather"));
    assertFalse(eval(e, "hunt")); // wrong group
  }

  @Test
  @DisplayName("AND conjunction")
  void andConjunction() {
    CompletionExpr e = parse("gather AND hunt", "gather", "hunt", "fish");
    assertFalse(eval(e));
    assertFalse(eval(e, "gather"));
    assertFalse(eval(e, "hunt"));
    assertTrue(eval(e, "gather", "hunt"));
    assertTrue(eval(e, "gather", "hunt", "fish"));
  }

  @Test
  @DisplayName("OR disjunction")
  void orDisjunction() {
    CompletionExpr e = parse("gather OR hunt", "gather", "hunt", "fish");
    assertFalse(eval(e));
    assertTrue(eval(e, "gather"));
    assertTrue(eval(e, "hunt"));
    assertFalse(eval(e, "fish"));
  }

  @Test
  @DisplayName("NOT negation")
  void notNegation() {
    CompletionExpr e = parse("NOT gather", "gather", "hunt");
    assertTrue(eval(e));
    assertFalse(eval(e, "gather"));
    assertTrue(eval(e, "hunt"));
  }

  @Test
  @DisplayName("parentheses override precedence")
  void parentheses() {
    // gather AND (hunt OR fish)
    CompletionExpr e = parse("gather AND (hunt OR fish)", "gather", "hunt", "fish");
    assertFalse(eval(e));
    assertFalse(eval(e, "hunt"));
    assertTrue(eval(e, "gather", "hunt"));
    assertTrue(eval(e, "gather", "fish"));
    assertFalse(eval(e, "hunt", "fish")); // missing gather
  }

  @Test
  @DisplayName("mixed AND/OR without parens is left-associative")
  void leftAssociative() {
    // a AND b OR c  =>  (a AND b) OR c
    CompletionExpr e = parse("a AND b OR c", "a", "b", "c");
    assertFalse(eval(e));
    assertFalse(eval(e, "a"));
    assertFalse(eval(e, "b"));
    assertTrue(eval(e, "a", "b")); // (a AND b)
    assertTrue(eval(e, "c")); // c alone satisfies OR
    assertTrue(eval(e, "a", "c")); // c satisfies OR
  }

  @Test
  @DisplayName("empty group set with ANY returns true (no groups = trivially satisfied)")
  void emptyAny() {
    CompletionExpr e = parse("any");
    assertTrue(eval(e));
  }

  @Test
  @DisplayName("empty group set with ALL returns true (vacuous truth)")
  void emptyAll() {
    CompletionExpr e = parse("all");
    assertTrue(eval(e));
  }

  @Test
  @DisplayName("pipe character works as OR")
  void pipeOr() {
    CompletionExpr e = parse("a | b", "a", "b");
    assertTrue(eval(e, "a"));
    assertTrue(eval(e, "b"));
  }

  @Test
  @DisplayName("ampersand character works as AND")
  void ampersandAnd() {
    CompletionExpr e = parse("a & b", "a", "b");
    assertTrue(eval(e, "a", "b"));
    assertFalse(eval(e, "a"));
  }

  @Test
  @DisplayName("exclamation mark works as NOT")
  void bangNot() {
    CompletionExpr e = parse("!a", "a", "b");
    assertFalse(eval(e, "a"));
    assertTrue(eval(e, "b"));
  }

  @Test
  @DisplayName("invalid syntax throws IllegalArgumentException")
  void invalidSyntax() {
    assertThrows(IllegalArgumentException.class, () -> parse("a AND"));
    assertThrows(IllegalArgumentException.class, () -> parse("(a OR b"));
    assertThrows(IllegalArgumentException.class, () -> parse("a @ b"));
  }

  @Test
  @DisplayName("case-insensitive keywords")
  void caseInsensitive() {
    assertTrue(eval(parse("ALL", "x"), "x"));
    assertTrue(eval(parse("Any", "x"), "x"));
    assertTrue(eval(parse("a AnD b", "a", "b"), "a", "b"));
    assertTrue(eval(parse("a Or b", "a", "b"), "a"));
  }
}
