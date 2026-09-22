# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Techsenger JediTermFX is a terminal emulator for JavaFX. It is a port of
[JediTerm](https://github.com/JetBrains/jediterm) (originally Swing-based) to JavaFX, so much of the core
emulation logic (VT/xterm emulation, buffer/model code) traces directly back to the original JetBrains
codebase — expect IntelliJ-style code conventions (e.g. `my`-prefixed fields, sparse/no license headers,
`@author` javadoc tags) in older, untouched files, and don't "fix" that style incidentally while touching
unrelated code nearby. See `README.md` for the full feature list, usage examples (dark theme, hyperlinks),
and how to run the demo application.

Requires **JavaFX 19** (see `javafx.version` in root `pom.xml`) and Pty4J for spawning local terminal
processes (Unix/Mac/Windows).

## Language

Everything in the project is written in English — README, documentation, Javadoc, code comments, commit
messages, etc. Always — regardless of what language the conversation with the assistant happens in.

## Scope of work

It is strictly forbidden to operate outside the project directory. This applies absolutely, to every kind
of operation — including read-only ones like `find` or search/grep — not just edits.

The only exception: the developer may explicitly name a directory outside the project directory for a
specific task. In that case, work must stay confined to the project directory plus exactly the
directory/directories the developer named — nothing else. Proactively suggesting or using any other
outside directory on your own is strictly forbidden.

If some source you need isn't in an allowed directory, ask the developer where to find it — never search
the rest of the machine for it on your own.

## Module layout

Three Maven modules, dependency direction top-to-bottom:

- **jeditermfx-core** (`com.techsenger.jeditermfx.core`) — the emulator itself: VT/xterm emulation
  (`emulator`, `emulator.charset`, `emulator.mouse`), the terminal text buffer/model (`model`,
  `model.hyperlinks`), input handling (`input`), and typeahead prediction (`typeahead`). No JavaFX
  dependency — this module is UI-toolkit-agnostic.
- **jeditermfx-ui** (`com.techsenger.jeditermfx.ui`) — the JavaFX rendering/interaction layer on top of
  `jeditermfx-core`: the terminal panel/canvas, settings (`ui.settings`), selection/search (`SubstringFinder`,
  `FindResult`), hyperlink highlighting. Depends on `jeditermfx-core`, Pty4J (spawning local shells), and
  JavaFX (`javafx-base`/`javafx-graphics`/`javafx-controls`).
- **jeditermfx-app** (`com.techsenger.jeditermfx.app`) — the demo application (see
  `jeditermfx-app/src/main/java/.../example/BasicTerminalShellExample.java`); excluded from Maven Central
  publishing (`publishing.plugin.exclusions` in root `pom.xml`).

Every module is a real JPMS module (`module-info.java` under `src/main/java`, and for `core`/`ui` also under
`src/test/java`) — when adding a new package that must be visible outside its module, remember to add
`exports` in `module-info.java`, not just make the class `public`. Unlike some other Techsenger projects,
there is no dynamic module-layer/registry framework here — these are plain, statically resolved JPMS modules.

## Commands

```
mvn clean install                      # build everything (multi-module reactor)
mvn -pl jeditermfx-core install        # build a single module (and its dependents if -am is added)
mvn test -pl jeditermfx-core           # unit tests only, one module
mvn test -Dtest=SomeTestClass#someMethod -pl jeditermfx-core   # single test method
cd jeditermfx-app && mvn javafx:run    # run the demo app (see README "Using Maven")
```

- Unit tests use JUnit 5 (JUnit Jupiter) and run via Surefire during `test`/`install`; no AssertJ or other
  assertion library is included, so assertions go through plain `org.junit.jupiter.api.Assertions`.
- Some legacy test classes ported from JediTerm (e.g. `VtEmulatorTest`, `EmulatorTestAbstract`) drive tests
  through fixture files in a matching resources directory (`<TestName>.txt`/`.after.txt`) rather than inline
  assertions — check for a sibling resources file before assuming a test's expected output is hardcoded.
- **Checkstyle is disabled** for this project (`checkstyle.plugin.skip=true` in the root `pom.xml`), unlike
  other Techsenger projects that inherit the `com.techsenger.checkstyle.config` ruleset from the
  `maven-root` parent POM — no automated style gate runs on `mvn package`/`install` here. The rules in
  "Code style" below are still the house style to apply by hand; see that section for what to follow anyway.
- License headers are **not** consistently applied across source files (most files have none; a few, like
  `jeditermfx-core/.../util/Pair.java`, do) — don't add one to a file just because you're editing it, unless
  asked.
- There is no local CI test workflow; `.github/workflows/snapshot-deploy.yml` only deploys SNAPSHOT versions
  to a Repsy repository on push to `master` — it does not gate on tests.
- Version is a shared reactor version set once in the root `pom.xml` (currently `1.2.0-SNAPSHOT`).

## Nullability

Types use `org.jetbrains.annotations.NotNull`/`@Nullable` (JetBrains annotations, not a custom Techsenger
annotations library) — there is no NullAway or other compile-time null-checking tool enforcing them here, so
treat them as documentation of intent rather than a checked contract, and don't assume the compiler will
catch a missing or wrong one.

## Member ordering

Not caught by any lint tool (Checkstyle is disabled — see Code style below) — must be applied by hand on
every edit, not just when writing new files.

Within a class/interface, members are sorted by three nested keys, each answering a different question and
breaking ties in the one before it:

1. **Scope — static vs. instance.** Does this member belong to the class or to the object? All static
   members form one block, placed entirely before all instance members. This is absolute — e.g. a
   `private static` method is placed above a `public` constructor, because scope outranks visibility.
2. **Role — types → fields → (constructors →) methods.** This is a dependency order, not an arbitrary
   bucket: nested types define the vocabulary that fields are declared with, fields hold the state that
   methods/constructors operate on. So within the static block: nested static types → static fields →
   static methods. Within the instance block: nested instance types → instance fields → constructors →
   instance methods.
3. **Visibility — `public` → `protected` → package-private → `private`.** Within one role (e.g. "instance
   methods"), the public contract comes before implementation detail.

So the full sequence in one class is: static nested types (public→private) → static fields
(public→private) → static methods (public→private) → nested instance types (public→private) → instance
fields (public→private) → constructors (public→private) → instance methods (public→private).

**Field grouping, within one role+visibility bucket.** The three criteria above leave ties: a class
exposing `getX()`/`isX()`, `setX()`, and `xProperty()`-style accessors for the same field will normally have
all three as `public` instance methods, so nothing above orders them relative to each other or to the next
field's trio. **When they share the same access modifier, group them by field** instead of batching all
getters, then all setters, then all property accessors as three separate blocks — `getX()` → `setX()` (if
present) → `xProperty()` (if present), then move on to the next field's trio:

```java
double getWidth();

ReadOnlyDoubleProperty widthProperty();

double getHeight();

ReadOnlyDoubleProperty heightProperty();
```

not

```java
double getWidth();

double getHeight();

ReadOnlyDoubleProperty widthProperty();

ReadOnlyDoubleProperty heightProperty();
```

Field order otherwise follows whichever order is already established (typically the backing fields'
declaration order in the class).

If the field's methods don't share one access modifier — e.g. a setter is `protected` while its getter is
`public` — visibility still wins: keep each method in its own visibility block, don't pull a
lower-visibility method up next to a public one just to keep the trio together.

## Naming convention

A `Map`-typed field/parameter/local/getter/setter is named `<values>By<key>` (e.g. `Map<FileType, Boolean>
selectionsByType`, `getSelectionsByType()`/`setSelectionsByType(...)`), not `<key><values>` (e.g.
`typeSelections`). The `by`-form reads directly as "which value, keyed by which type of key" at the
declaration site, without having to look at the generic type arguments to tell which side is the key.

## Javadoc

Document the contract — what the member does and why a caller would use it — never how it's implemented.
If a sentence just narrates the method body in prose (the steps it performs, the fields it touches along
the way), that's implementation detail the reader can already see by opening the method; cut it. A reader
deciding whether/how to call the member should get what they need without opening its body: what it
returns or does, plus any non-obvious constraint, side effect, or precondition — not a walkthrough.

Target 120-240 characters for the main description (the summary sentence plus any `<p>` continuation)
only — this range does not apply to `@param`/`@return`/`@throws`/`@see`/`{@link}` tags at all. Under 120
characters a javadoc rarely earns its place over just reading the signature; over 240 it has usually
drifted into narrating implementation or into a multi-paragraph essay — shorten it, or split the method
instead of padding its doc further. Avoid `{@link}`/cross-references to other classes or methods where
possible, especially to types from a different module/dependency (patternfx, shellfx, toolkit) — those
references rot silently when the referenced API changes and are harder to notice/fix than one in the same
file.

Accessor methods (get, set, is, xxxProperty) — should not have Javadoc unless they provide information
that is not obvious from the method name and type.

Tags follow a different, simpler rule: keep each tag's description as short as it can be while still
saying what it needs to; the only hard limit on it is the 120-char line length itself (wrap per the
indentation rule below if one line isn't enough). There is no minimum length for a tag.

**Tag coverage.** On `public`/`protected` methods, document every element that appears in the signature —
each `@param`, every checked exception via `@throws`, and so on — as tersely as the 120-char line allows;
a missing tag reads as an oversight on API surface other code depends on. Add `@return` only when the
return value carries semantics beyond what the method name and return type already say — nullability, a
sentinel/special value, which object state it reflects, resource ownership, mutability, a specific format,
or a constraint on the value. Skip `@return` when it would just restate the method name and type (e.g.
`getName()` returning `String` needs no `@return the name`).

On package-private/private methods, add `@param`/`@throws`/etc. only when that specific tag is actually
worth calling out (a non-obvious constraint, a surprising exception) — omitting the routine ones is fine.

Each `@param`, `@return`, `@throws`, and similar tag starts on a new line. The first line holds the tag,
the parameter name (if any), and the start of the description. When the description doesn't fit the
120-char line limit, continue it on the following line(s) using a **fixed 4-space indent relative to the
leading `*`**, not aligned under the start of the description — a fixed indent stays correct regardless of
how long the tag/parameter name is, so it never needs re-indenting when a name changes length:

```java
/**
 * Resolves a file.
 *
 * @param path controls whether the operation should be performed in a
 *     lightweight mode. If {@code false}, additional validation is performed.
 * @param destination specifies the destination used to resolve relative
 *     paths and determine where the resulting files should be stored.
 * @return the resolved file.
 * @throws IOException if the file cannot be resolved.
 */
```

Do not write javadoc on an overriding (`@Override`) method — rule of thumb: the comment belongs only on
the interface method or the parent class method being overridden, not duplicated on every override.

## Code style (Checkstyle)

Checkstyle runs on every `mvn package`/`install` (see Commands above) via the `com.techsenger.checkstyle.config`
artifact (Sun checks-derived, `severity=error`) — a violation fails the build, not just a lint warning. Treat
every rule below as binding when writing or editing Java. `module-info.java` files are exempt from all of it.
Run just this check with `mvn checkstyle:check`, or skip it entirely with `-Dcheckstyle.plugin.skip=true`
or `-P unit-tests`/`-P integration-tests`.

- **Layout**: 120-char line limit, no tabs, no trailing whitespace, file must end with a newline, exactly
  one blank line between the license header and the `package` declaration, no more than 5000 lines/file.
- **Methods**: max 250 non-empty lines, max 8 parameters — both signal you should split the method/introduce
  a parameter object rather than push past them.
- **Imports**: no star imports, no unused imports, no redundant imports.
- **Naming**: standard Java conventions — `PascalCase` types, `camelCase` methods/fields/params/locals,
  `UPPER_SNAKE_CASE` for non-private constants (private constants are exempt, so `private static final` in
  `camelCase`/mixed case is fine).
- **Braces & blocks**: braces required on every `if`/`for`/`while`/etc. (no single-statement bodies without
  `{}`), no empty blocks, no nested blocks, standard left/right-curly placement.
- **Whitespace**: standard spacing around operators/generics/casts/parens (`GenericWhitespace`, `ParenPad`,
  `TypecastParenPad`, `WhitespaceAround`, `WhitespaceAfter`, `NoWhitespaceBefore`/`After`, `OperatorWrap`).
- **Coding**: no empty statements, `equals`/`hashCode` always overridden together, no assignments inside
  expressions (`InnerAssignment`), one variable declaration per statement (no `int a, b;`), simplify boolean
  expressions/returns (`return x == y;` not `if (x == y) return true; else return false;`).
- **Class design**: a class with only private constructors must be `final`; utility classes (only static
  members) must have a private constructor (matches the existing `private Foo() { // empty }` pattern
  already used throughout, e.g. `NavigatorFileIconProvider`); fields should be `private` with accessors
  (`VisibilityModifier`), not exposed directly.
- **Misc**: array brackets on the type, not the variable (`String[] args`, not `String args[]`); long
  literals use uppercase `L` (`100L`, not `100l`).
- **No `TODO` comments** (`TodoComment` module) — since severity is `error`, a matching comment fails the
  build. Don't add new ones; open a tracked issue or just do the work instead.
