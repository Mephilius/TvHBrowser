# TvHBrowser

A [TV-Browser](https://www.tvbrowser.org/) plugin that connects TV-Browser to a [Tvheadend](https://tvheadend.org/) server. It lets you schedule, inspect and manage Tvheadend recordings directly from the TV-Browser program guide, and shows a tree of upcoming and finished recordings inside TV-Browser.

## Features

- Create a Tvheadend recording (timer) from any program in the TV-Browser guide, and delete, enable or disable it again from the same context menu.
- Automatically matches Tvheadend's EPG events to TV-Browser programs by title, subtitle and start time, so a recording appears as a marked, clickable program instead of a plain text entry.
- When no exact match can be found (e.g. the Tvheadend EPG uses a different title or start time than TV-Browser), a picker dialog lets you choose the right event yourself; that choice is remembered for the timer.
- Shows "Upcoming recordings" and "Finished recordings" trees in the TV-Browser sidebar, grouped by day.
- Polls Tvheadend in the background every 30 seconds, without blocking the TV-Browser UI.

## Requirements

- [TV-Browser](https://www.tvbrowser.org/) 4.2.7 or newer, running on Java 11+.
- A reachable [Tvheadend](https://tvheadend.org/) server with HTSP/HTTP API access enabled for the configured user.

## Installation

1. Download `TvHBrowser.jar` from the [Releases](../../releases) page (or build it yourself, see below).
2. Copy the jar into TV-Browser's `plugins` directory (on Windows typically `%APPDATA%\TV-Browser\<version>\plugins`).
3. Restart TV-Browser and enable the plugin under *Settings → Plugins* if it isn't active already.

## Configuration

Open *Settings → TVHeadend* in TV-Browser and fill in:

| Setting | Description |
| --- | --- |
| TVHeadend FQDN / IP | Hostname or IP address of the Tvheadend server |
| Port | Tvheadend API port (default `9981`, or `443`/`9983` when using HTTPS) |
| Https connection | Enable to use HTTPS instead of plain HTTP |
| Username / Password | Credentials for a Tvheadend user with API and recording permissions |
| Timeout | Connection timeout in milliseconds |
| DVR configuration (UUID) | Optional. Leave empty to use Tvheadend's default DVR profile, or paste the UUID of a specific profile (*Configuration → Recording → Digital Video Recorder Profiles*) |

Closing the settings dialog tests the connection and, on success, does an immediate refresh of the timer list.

On the *Channels* tab, map each Tvheadend channel to the matching TV-Browser channel; this mapping is required for timers to be matched to programs and marked in the guide.

## Building from source

Requires JDK 11+ and Maven. The build needs the TV-Browser core library, which isn't published to Maven Central, so it must be installed into your local repository first:

```bash
mvn install:install-file -Dfile=/path/to/tvbrowser.jar -DgroupId=tvbrowser -DartifactId=core -Dversion=4.2.7 -Dpackaging=jar
mvn -B package
```

The resulting `target/TvHBrowser.jar` is a shaded jar containing the plugin's own dependencies (Gson, org.json), ready to drop into TV-Browser's `plugins` directory.

### Running the tests

```bash
mvn -B test
```

## CI

[.github/workflows/mave.yaml](.github/workflows/mave.yaml) builds the plugin on every push and pull request against `main`/`development`, and publishes a GitHub release with the built jar on pushes to `main`.

## Documentation

- Full English code documentation: [docs/CODE_DOCUMENTATION_EN.md](docs/CODE_DOCUMENTATION_EN.md)

## License

AGPL-3.0. See the license header in [`TvHBrowser.java`](src/main/java/tvhbrowser/TvHBrowser.java).
