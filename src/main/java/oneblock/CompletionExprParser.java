package oneblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses a tiny boolean expression language into a {@link CompletionExpr} AST.
 *
 * <p>Grammar:
 *
 * <pre>
 *   expr   := orExpr
 *   orExpr := andExpr (OR andExpr)*
 *   andExpr:= unary (AND unary)*
 *   unary  := NOT unary | primary
 *   primary:= IDENT | ALL | ANY | '(' expr ')'
 * </pre>
 *
 * <p>The keywords {@code all} and {@code any} are expanded at parse-time into an AND / OR of every
 * known group name, so evaluation only needs the completed set.
 */
public final class CompletionExprParser {

  private final String input;
  private int pos;
  private final Set<String> allGroups;

  private CompletionExprParser(String input, Set<String> allGroups) {
    this.input = input;
    this.pos = 0;
    this.allGroups = allGroups != null ? allGroups : Collections.emptySet();
  }

  /**
   * Parse an expression.
   *
   * @param input the expression text, e.g. "gather AND (hunt OR fish)"
   * @param allGroups every group name that exists in the level; used to expand {@code all} / {@code
   *     any}
   * @return a {@link CompletionExpr}; never {@code null}
   * @throws IllegalArgumentException on syntax errors
   */
  public static CompletionExpr parse(String input, Set<String> allGroups) {
    if (input == null || input.trim().isEmpty()) {
      // Default: ANY group completes the level (matches legacy Phase-2 behaviour)
      return new AnyExpr(allGroups);
    }
    CompletionExprParser p = new CompletionExprParser(input.trim(), allGroups);
    CompletionExpr expr = p.parseExpr();
    p.skipWhitespace();
    if (p.pos < p.input.length()) {
      throw new IllegalArgumentException(
          "Unexpected token at position " + p.pos + ": '" + p.input.charAt(p.pos) + "'");
    }
    return expr;
  }

  /* ---- recursive descent ---- */

  private CompletionExpr parseExpr() {
    return parseOrExpr();
  }

  private CompletionExpr parseOrExpr() {
    CompletionExpr left = parseAndExpr();
    while (true) {
      skipWhitespace();
      if (matchKeyword("OR") || matchChar('|')) {
        CompletionExpr right = parseAndExpr();
        left = new OrNode(left, right);
      } else {
        break;
      }
    }
    return left;
  }

  private CompletionExpr parseAndExpr() {
    CompletionExpr left = parseUnary();
    while (true) {
      skipWhitespace();
      if (matchKeyword("AND") || matchChar('&')) {
        CompletionExpr right = parseUnary();
        left = new AndNode(left, right);
      } else {
        break;
      }
    }
    return left;
  }

  private CompletionExpr parseUnary() {
    skipWhitespace();
    if (matchKeyword("NOT") || matchChar('!')) {
      return new NotNode(parseUnary());
    }
    return parsePrimary();
  }

  private CompletionExpr parsePrimary() {
    skipWhitespace();
    if (matchChar('(')) {
      CompletionExpr inner = parseExpr();
      skipWhitespace();
      if (!matchChar(')')) {
        throw new IllegalArgumentException("Missing closing parenthesis at position " + pos);
      }
      return inner;
    }

    String ident = readIdentifier();
    if (ident == null) {
      throw new IllegalArgumentException(
          "Unexpected character at position " + pos + ": '" + peek() + "'");
    }

    String lower = ident.toLowerCase(Locale.ROOT);
    if ("all".equals(lower)) {
      return new AllExpr(allGroups);
    }
    if ("any".equals(lower)) {
      return new AnyExpr(allGroups);
    }
    return new GroupRef(ident);
  }

  /* ---- lexer helpers ---- */

  private void skipWhitespace() {
    while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
      pos++;
    }
  }

  private boolean matchKeyword(String keyword) {
    int saved = pos;
    String ident = readIdentifier();
    if (ident != null && ident.equalsIgnoreCase(keyword)) {
      return true;
    }
    pos = saved;
    return false;
  }

  private boolean matchChar(char c) {
    if (pos < input.length() && input.charAt(pos) == c) {
      pos++;
      return true;
    }
    return false;
  }

  private char peek() {
    return pos < input.length() ? input.charAt(pos) : '\0';
  }

  private String readIdentifier() {
    skipWhitespace();
    int start = pos;
    while (pos < input.length()) {
      char c = input.charAt(pos);
      if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
        pos++;
      } else {
        break;
      }
    }
    return start == pos ? null : input.substring(start, pos);
  }

  /* ---- AST nodes ---- */

  /** Reference to a single task group name. */
  static final class GroupRef implements CompletionExpr {
    final String name;

    GroupRef(String name) {
      this.name = name;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      return completedGroups != null && completedGroups.contains(name);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /** Logical AND of two sub-expressions. */
  static final class AndNode implements CompletionExpr {
    final CompletionExpr left;
    final CompletionExpr right;

    AndNode(CompletionExpr left, CompletionExpr right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      return left.evaluate(completedGroups) && right.evaluate(completedGroups);
    }

    @Override
    public String toString() {
      return "(" + left + " AND " + right + ")";
    }
  }

  /** Logical OR of two sub-expressions. */
  static final class OrNode implements CompletionExpr {
    final CompletionExpr left;
    final CompletionExpr right;

    OrNode(CompletionExpr left, CompletionExpr right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      return left.evaluate(completedGroups) || right.evaluate(completedGroups);
    }

    @Override
    public String toString() {
      return "(" + left + " OR " + right + ")";
    }
  }

  /** Logical NOT of a sub-expression. */
  static final class NotNode implements CompletionExpr {
    final CompletionExpr inner;

    NotNode(CompletionExpr inner) {
      this.inner = inner;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      return !inner.evaluate(completedGroups);
    }

    @Override
    public String toString() {
      return "NOT " + inner;
    }
  }

  /** AND of every known group (keyword {@code all}). */
  static final class AllExpr implements CompletionExpr {
    private final List<GroupRef> groups;

    AllExpr(Set<String> allGroups) {
      List<GroupRef> list = new ArrayList<>();
      for (String g : allGroups) {
        list.add(new GroupRef(g));
      }
      this.groups = list;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      for (GroupRef g : groups) {
        if (!g.evaluate(completedGroups)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "ALL";
    }
  }

  /** OR of every known group (keyword {@code any}). */
  static final class AnyExpr implements CompletionExpr {
    private final List<GroupRef> groups;

    AnyExpr(Set<String> allGroups) {
      List<GroupRef> list = new ArrayList<>();
      for (String g : allGroups) {
        list.add(new GroupRef(g));
      }
      this.groups = list;
    }

    @Override
    public boolean evaluate(Set<String> completedGroups) {
      if (groups.isEmpty()) {
        return true; // no groups => trivially satisfied
      }
      for (GroupRef g : groups) {
        if (g.evaluate(completedGroups)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "ANY";
    }
  }
}
