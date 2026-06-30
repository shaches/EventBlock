package oneblock.gui.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public final class DialogMenu {
  private final Component title;
  private final List<BodyLine> body;
  private final List<Option> options;
  private final boolean canCloseWithEscape;
  private final Integer columns;

  private DialogMenu(
      Component title,
      List<BodyLine> body,
      List<Option> options,
      boolean canCloseWithEscape,
      Integer columns) {
    this.title = copy(title);
    this.body = List.copyOf(body);
    this.options = List.copyOf(options);
    this.canCloseWithEscape = canCloseWithEscape;
    this.columns = columns;
  }

  public static Builder builder(String title) {
    return new Builder(text(title, null));
  }

  public static Builder builder(Component title) {
    return new Builder(title);
  }

  public void open(Player player) {
    DialogMenuManager.open(player, this);
  }

  Component title() {
    return copy(title);
  }

  List<BodyLine> body() {
    return body;
  }

  List<Option> options() {
    return options;
  }

  boolean canCloseWithEscape() {
    return canCloseWithEscape;
  }

  Integer columns() {
    return columns;
  }

  static Component text(String value, TextColor color) {
    Component component = Component.text(value == null ? "" : value);
    if (color != null) component = component.color(color);
    return component;
  }

  static Component copy(Component component) {
    if (component == null) return Component.empty();
    return component;
  }

  record BodyLine(Component contents, Integer width) {}

  record Option(Component label, Component tooltip, Integer width, DialogMenuAction action) {}

  public static final class Builder {
    private final Component title;
    private final List<BodyLine> body = new ArrayList<>();
    private final List<Option> options = new ArrayList<>();
    private boolean canCloseWithEscape = true;
    private Integer columns;

    private Builder(Component title) {
      this.title = Objects.requireNonNull(title, "title");
    }

    public Builder body(String contents, TextColor color) {
      return body(text(contents, color));
    }

    public Builder body(Component contents) {
      return body(contents, null);
    }

    public Builder body(Component contents, Integer width) {
      body.add(new BodyLine(copy(contents), width));
      return this;
    }

    public Builder option(String label, TextColor color, DialogMenuAction action) {
      return option(text(label, color), null, null, action);
    }

    public Builder option(
        String label,
        TextColor labelColor,
        String tooltip,
        TextColor tooltipColor,
        DialogMenuAction action) {
      return option(text(label, labelColor), text(tooltip, tooltipColor), null, action);
    }

    public Builder option(
        Component label, Component tooltip, Integer width, DialogMenuAction action) {
      options.add(
          new Option(
              copy(label),
              tooltip == null ? null : copy(tooltip),
              width,
              Objects.requireNonNull(action)));
      return this;
    }

    public Builder canCloseWithEscape(boolean canCloseWithEscape) {
      this.canCloseWithEscape = canCloseWithEscape;
      return this;
    }

    public Builder columns(Integer columns) {
      this.columns = columns == null || columns <= 0 ? null : columns;
      return this;
    }

    public DialogMenu build() {
      return new DialogMenu(title, body, options, canCloseWithEscape, columns);
    }

    public void open(Player player) {
      build().open(player);
    }
  }
}
