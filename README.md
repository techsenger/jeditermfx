# Techsenger JediTermFX

Techsenger JediTermFX is a Terminal Emulator for JavaFX. The project is a result of porting
[JediTerm](https://github.com/JetBrains/jediterm) (commit 8366f2b) from Swing to JavaFX. JediTermFX exclusively
utilizes JavaFX components. Therefore, the Terminal Emulator based on this library can be seamlessly integrated into
any JavaFX application. A detailed comparison of terminal libraries is provided below.

## Table of Contents
* [Demo](#demo)
    * [Htop](#demo-htop)
    * [Tig](#demo-tig)
    * [MC](#demo-mc)
    * [Maven](#demo-maven)
    * [Bastet](#demo-bastet)
* [Features](#features)
* [Terminal Comparison](#comparison)
* [Usage](#usage)
    * [Dependencies](#usage-dependencies)
    * [Dark Theme](#usage-dark-theme)
    * [Hyperlinks](#usage-hyperlinks)
* [How It Works](#how-it-works)
    * [Terms](#how-it-works-terms)
    * [Pipeline](#how-it-works-pipeline)
* [Code Building](#code-building)
* [Running the Application](#application)
    * [Using Maven](#application-maven)
    * [Using Distro](#application-distro)
* [License](#license)
* [Contributing](#contributing)
* [Support Us](#support-us)

## Demo <a name="demo"></a>

### Htop <a name="demo-htop"></a>

![htop-dark](https://github.com/user-attachments/assets/b390eb08-3897-4ad4-b77f-c55ed395c2a3)

### Tig <a name="demo-tig"></a>

![tig](https://github.com/user-attachments/assets/7e1ace65-bf49-4e2c-bb0e-adb7c89b5857)

### MC <a name="demo-mc"></a>

![mc](https://github.com/user-attachments/assets/f2189273-24ca-4420-908b-fa6d1d4d0df0)

### Maven <a name="demo-maven"></a>

![mvn](https://github.com/user-attachments/assets/51bf7b95-09e4-4d62-877b-b214d1a1d7e7)

### Bastet <a name="demo-bastet"></a>

![bastet](https://github.com/user-attachments/assets/d399109b-87c5-4bf3-a860-51341efe85d6)

## Features <a name="features"></a>

* Local terminal for Unix, Mac and Windows using Pty4J
* Xterm emulation - passes most of tests from vttest
* Xterm 256 colours
* Scrolling
* Copy/Paste
* Mouse support
* Terminal resizing from client or server side
* Terminal tabs

## Terminal Comparison <a name="comparison"></a>

Terminal      | JediTermFX  | [JediTerm](https://github.com/JetBrains/jediterm)  | [TerminalFX](https://github.com/javaterminal/TerminalFX) |
:-------------|:----------- |:--------------|:--------------|
GUI Library   | JavaFX      | Swing         | JavaFX        |
Main Component| Canvas      | JComponent    | WebView       |
Languages     | Java        | Java, Kotlin  | Java, JS      |
JPMS Support  | Yes         | No            | Yes           |

## Usage <a name="usage"></a>

It is recommended to start working with JediTermFX by studying and running the
[BasicTerminalShellExample](jeditermfx-app/src/main/java/com/techsenger/jeditermfx/app/example/BasicTerminalShellExample.java) class.
This class contains the minimal code needed to launch a terminal in a JavaFX application.

### Dependencies <a name="usage-dependencies"></a>

This project is available on Maven Central:

```
<dependency>
    <groupId>com.techsenger.jeditermfx</groupId>
    <artifactId>jeditermfx-core</artifactId>
    <version>${jeditermfx.version}</version>
</dependency>
<dependency>
    <groupId>com.techsenger.jeditermfx</groupId>
    <artifactId>jeditermfx-ui</artifactId>
    <version>${jeditermfx.version}</version>
</dependency>
```

### Dark Theme <a name="usage-dark-theme"></a>

If you need a dark theme, you should override the `getDefaultForeground()` and `getDefaultBackground()` methods in
`UserSettingsProvider`. To run the demo application in dark theme see [Using Maven](#application-maven).

### Hyperlinks <a name="usage-hyperlinks"></a>

JediTermFX provides a wide range of features when working with links. The `HighlightMode` enumeration specifies multiple
modes of working with links and their colors. In the `ALWAYS` modes, links are always underlined and always clickable.
In the `NEVER` modes, links are never underlined and never clickable. In the `HOVER` modes, links become underlined and
clickable only when hovered over. Now let's clarify the difference between the two types of colors. `CUSTOM` colors
are those set by the JediTermFX user in the getHyperlinkColor() method of the settings. `ORIGINAL` colors are those
offered by the program running in the terminal. Thus, links can use either custom colors or the original text colors.

## How It Works <a name="how-it-works"></a>

### Terms <a name="how-it-works-terms"></a>

* **Terminal** — a text input/output device, not a command-interpreter program.

  Historically (1960s-70s), a terminal was standalone physical hardware: a screen and a keyboard wired to a large
  computer. The user typed text, it travelled down the wire to the computer, and the computer sent text back to be
  displayed. The terminal itself didn't "understand" anything, it just passed bytes back and forth.

  Nowadays almost nobody sees a physical terminal, but the concept survives as three distinct layers that a modern
  UX blends into a single window.

* **Terminal emulator** — a windowed program that draws a grid of characters, interprets ANSI codes (colors, cursor
  movement) and captures key presses. Examples are gnome-terminal, xterm, konsole, iTerm2, Windows Terminal, and,
  in this project's case, JediTermFX's own `TerminalPanel`. This is "the terminal" in the everyday sense, the
  window itself.

* **PTY (pseudo-terminal)** — no longer a program but an operating system kernel mechanism. It is a pair of file
  descriptors, **master** and **slave**, that the kernel creates to give a process the illusion that it is
  connected to a real terminal device (`isatty() == true`, it can query the window size, and so on), even though
  there is no physical hardware behind it.

  The **slave** end is handed to the child process as its stdin/stdout/stderr, from the process's point of view it
  behaves exactly like a real terminal device. The **master** end is the other end of the same channel, it is what
  whoever controls the terminal (Pty4J, and through it JediTermFX) reads from and writes to. They always come as a
  pair because a PTY is a two-ended channel: whatever is written to the master shows up as input on the slave
  (this is how key presses reach the shell), and whatever the process on the slave side writes, plain text and ANSI
  escape sequences alike, shows up as output on the master (this is how JediTermFX receives everything the shell
  prints).

  This is a Unix concept: Linux, macOS and BSD have had a real kernel-level PTY for decades, standardized by
  POSIX. Windows had nothing equivalent until Windows 10 version 1809 (2018), when Microsoft added ConPTY
  (the Pseudo Console API), mainly to let Unix-style tools work properly on Windows too. On older Windows
  versions, Pty4J falls back to WinPTY, a third-party library that emulates PTY-like behavior on top of the old
  Windows Console subsystem rather than a true kernel PTY.

* **Shell** (`bash`, `zsh`, PowerShell, `cmd.exe`) — a command-interpreter program, not a terminal. It does not
  draw anything itself, it just reads a line of text from its input, parses it as a command, executes it, and
  writes the result to its output. It has no idea whether the other end is a terminal emulator, a file, or a pipe,
  it simply writes characters (and, if it detects through `isatty()` that its output is a real terminal, it starts
  adding ANSI codes for readability, as covered below).

### Pipeline <a name="how-it-works-pipeline"></a>

**Initialization.** JediTermFX does not create PTYs itself, it relies on [Pty4J](https://github.com/JetBrains/pty4j)
for that. Setting up a terminal session looks like this:

```
JediTermFX -> Pty4J -> OS pseudo-terminal (PTY: master + slave)
    -> child process (e.g. a shell), attached to the PTY slave
```

Pty4J asks the OS kernel to allocate a PTY, spawns the target program with its stdin/stdout/stderr attached to the
PTY's slave side, and exposes the PTY's master side to Java as a `PtyProcess`, which is a regular
`java.lang.Process`. JediTermFX's integration point with this (or any other raw source of terminal I/O) is the
`TtyConnector` interface; `ProcessTtyConnector` is a ready-made implementation wrapping a `java.lang.Process`.
`TtyConnector` only reads and writes raw characters, it has no knowledge of ANSI escape sequences.

**Reading.** Everything the child process writes to its output (plain text interleaved with raw ANSI/VT escape
sequences, which the child process generates itself) then flows through JediTermFX like this:

```
child process stdout/stderr -> PTY slave -> PTY master -> TtyConnector.read()
    -> TtyBasedArrayDataStream -> TerminalStarter (background thread)
    -> JediEmulator (parses ANSI/VT escape sequences)
    -> Terminal / JediTerminal, which updates two different sinks:
         -> TerminalTextBuffer (printed characters and their style)
         -> TerminalDisplay (cursor, window title, selection, mouse mode, and other terminal-device state)
    -> TerminalPanel (JavaFX): paints from TerminalTextBuffer and implements TerminalDisplay
```

1. `ProcessTtyConnector` wraps the process: `read()` reads from `process.getInputStream()` through an
   `InputStreamReader` with the right charset, `write()` writes to `process.getOutputStream()`. It is just an
   adapter from a process to the `TtyConnector` interface, there is no ANSI logic here.
2. `TtyBasedArrayDataStream` reads from that `TtyConnector` in 1024-character chunks, buffering the raw stream.
3. `TerminalStarter.start()` spawns a dedicated background thread that runs `while (myEmulator.hasNext())
   myEmulator.next();`. `myEmulator` is a `JediEmulator`, the actual ANSI/VT100/xterm parser: it reads from the
   `TerminalDataStream` and tells escape sequences (SGR colors, cursor movement, line/screen clearing, scrolling,
   etc.) apart from plain printable characters.
4. Parsed commands are dispatched to the `Terminal` interface, implemented by `JediTerminal`, which routes each one
   to whichever of its two sinks it belongs to. Printed characters go to `TerminalTextBuffer`, a plain model of
   lines with no JavaFX dependency, together with their style, both the foreground/background color and text
   attributes such as bold, italic, underline or blink. Everything about the terminal as an interactive device,
   cursor position/shape/visibility, window title, the current selection, mouse tracking mode, the alternate screen
   buffer toggle and so on, goes to `TerminalDisplay` instead.
5. `TerminalPanel`, the JavaFX UI, implements `TerminalDisplay` itself and separately paints whatever is in
   `TerminalTextBuffer`. It never sees raw ANSI, by the time state reaches it, it is already resolved into styled
   glyphs and plain terminal-device state.

**Writing.** User input flows the same way, but in reverse:

```
TerminalPanel (key press) -> TerminalKeyEncoder -> TtyConnector.write()
    -> PTY master -> PTY slave -> child process's stdin
```

`TerminalKeyEncoder` translates a JavaFX key event into the escape sequence the child process expects, and writes
it through the same `TtyConnector`.

Because ANSI parsing happens once, before the text ever reaches `TerminalTextBuffer`, any code that reads from the
buffer (rendering, selection, search, etc.) works with plain styled text and never has to deal with escape
sequences itself.

## Code Building <a name="code-building"></a>

To build the library use standard Git and Maven commands:

    git clone https://github.com/techsenger/jeditermfx
    cd jeditermfx
    mvn clean install

## Running the Application <a name="application"></a>

The project contains a demo application that shows how to use this library. There are two ways to run the application.

### Using Maven <a name="application-maven"></a>

To run application using maven plugin execute the following commands in the root of the project:

    cd jeditermfx-app
    mvn javafx:run

Please note, that debugger settings are in `jeditermfx-app/pom.xml` file. If you want to try a dark theme,
uncomment the following line in the JavaFX plugin configuration in the pom.xml file:

```
<!--<commandlineArgs>theme=dark</commandlineArgs>-->
```
This will make JediTermFX use the `DarkThemeSettingsProvider`.

### Using Distro <a name="application-distro"></a>

After building the project, you will find a distribution archive named `jeditermfx-app-<version>.tar` in the
`jeditermfx-app/target` directory. Extracting this file will allow you to launch the application
using `.sh` or `.bat` scripts depending on your operating system.

## License <a name="license"></a>

JediTermFX is dual-licensed under both the LGPLv3 (found in the LICENSE-LGPL-3.txt file in the root directory) and
Apache 2.0 License (found in the LICENSE-APACHE-2.0.txt file in the root directory). You may select, at your option,
one of the above-listed licenses.

## Contributing <a name="contributing"></a>

We welcome all contributions. You can help by reporting bugs, suggesting improvements, or submitting pull requests
with fixes and new features. If you have any questions, feel free to reach out — we’ll be happy to assist you.

## Support Us <a name="support-us"></a>

You can support our open-source work through [GitHub Sponsors](https://github.com/sponsors/techsenger).
Your contribution helps us maintain projects, develop new features, and provide ongoing improvements.
Multiple sponsorship tiers are available, each offering different levels of recognition and benefits.



