# EventBlock

EventBlock was developed as an independent fork of MrMarL’s OneBlock plugin.

---

## How to Start

### 1. Create the World
Generate a dedicated EventBlock world using [Multiverse-Core](https://modrinth.com/plugin/multiverse-core):
```
/mv create EventBlock normal -g EventBlock
```

### 2. Set the Spawn Point
Initialize the island area:
```
/ob set
```
```
/ob set 500
```

### 3. Join the Game
Players teleport to their island with:
```
/ob join
```
or simply **/ob**

---

## Command Reference

### Core Commands:
- **/ob join** – Join your island
- **/ob invite [player]** – Invite a player
- **/ob kick [player]** – Remove a player
- **/ob accept** – Accept invitation
- **/ob idreset** – Reset your island progress

### Admin & Settings Commands:
- **/ob set** – Set the first block
- **/ob set [distance]** – Island spacing
- **/ob circlemode [true/false]**
- **/ob autojoin [true/false]**
- **/ob protection [true/false]**
- **/ob border [true/false]**
- **/ob droptossup [true/false]**
- **/ob physics [true/false]**
- **/ob lvl_mult [value]**
- **/ob UseEmptyIslands [true/false]**
- **/ob islands [true/false]**
- **/ob islands set_my_by_def**
- **/ob islands default**
- **/ob island_rebirth [true/false]**
- **/ob progress_bar color [color]**
- **/ob progress_bar [true/false]**
- **/ob progress_bar level**
- **/ob progress_bar settext <text>

### Config Commands:
- **/ob reload**
- **/ob listlvl**
- **/ob listlvl [level]**

### Other Commands:
- **/ob idreset [player]**
- **/ob setlevel [player] 14**
- **/ob clear [player]**
- **/ob setleave**
- **/ob leave**

---

## PlaceholderAPI Support

- `%OB_ver%`
- `%OB_lvl%`
- `%OB_next_lvl%`
- `%OB_break_on_this_lvl%`
- `%OB_need_to_lvl_up%`
- `%OB_player_count%`
- `%OB_lvl_name%`
- `%OB_lvl_length%`
- `%OB_next_lvl_name%`
- `%OB_owner_name%`
- `%OB_owner_online%`
- `%OB_percent%`
- `%OB_scale%`
- `%OB_top_1_name%` … `%OB_top_10_name%`
- `%OB_top_1_lvl%` … `%OB_top_10_lvl%`
- `%OB_number_of_invited%`

Append `_by_position` to any placeholder to query the island at your current location.

---

## Custom Blocks Support

- **ItemsAdder** – native id support in `blocks.yml`
- **Oraxen** – native id support in `blocks.yml`
- **Nexo** – native id support in `blocks.yml`
- **CraftEngine** – native id support in `blocks.yml`

Other custom blocks can be spawned via commands in `blocks.yml`.

---

## Island Templates

Create custom default islands (7×12×7 area):
```
/ob islands set_my_by_def
```

---

## Building from Source

Requires **JDK 21** (Temurin recommended) and **Maven 3.9+**. Any system-installed Maven works; the `resources/apache-maven-3.9.15/` directory is gitignored and is only there as a convenience for contributors who have not installed Maven globally.

```powershell
# System-installed Maven (preferred)
mvn -B test
mvn -B -DskipTests clean package

# Or, using the bundled copy if you don't have Maven on PATH
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B test
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B -DskipTests clean package
```

The shaded plugin jar lands in `target/EventBlock-*.jar`. Linux / macOS users invoke the same goals via `mvn`.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow.

---

## License

- Original OneBlock code: MIT License (`LICENSE-upstream`) – Copyright (c) 2022 MrMarL
- All modifications and this distribution: GNU Affero General Public License v3.0 (`LICENSE`)
