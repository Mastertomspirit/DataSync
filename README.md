[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Java Version](https://img.shields.io/badge/Java-25-orange.svg)](#)
[![Java Version](https://img.shields.io/badge/JavaFX-26-ff4500.svg)](#)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-blue.svg)](#)
[![Environment](https://img.shields.io/badge/Environment-GUI%20only-blue.svg)](#)

# DataSync

DataSync is an enterprise-grade, highly parallelized file synchronization, backup, and duplicate analysis application engineered on top of a modern, fully modular Java platform. Operating natively within the system architecture, DataSync runs quietly inside the OS system tray to manage complex, multi-source storage Topologies concurrently. By utilizing transaction-safe file operations, the application guarantees zero data corruption during unexpected halts, ensuring your files remain structurally sound at all times.

> **Important Compatibility Note (Version 1.1.0.0-alpha):** This release introduces a fundamental overhaul of the persistent configuration storage layout. Because of these deep structural mutations, the core storage layer **is not backward compatible with legacy versions (e.g., v0.9.6.0)**. Upon initial initialization, the application's configuration parser automatically detects mismatched or legacy pre-alpha schemas and seamlessly sanitizes the environment back to safe system defaults without interrupting the runtime execution.

---

## 🚀 Core Architectural Features

* **Concurrent Multi-Job Execution Matrix:** Users can deploy an arbitrary number of independent backup, synchronization, or duplication profiles. DataSync processes these workloads simultaneously across distinct worker threads. A massive, long-running network synchronization task will never block rapid, localized background delta scans.
* **Non-Interruptible Transactional File Operations:** DataSync treats file manipulation with maximum fault tolerance. If the host operating system initiates a shutdown or the application process receives a termination signal, the core engine rejects hard, immediate termination. Instead, running thread operations are gracefully joined, allowing active write actions and metadata flushes to complete entirely. This strictly prevents partial file writes or broken destination headers.
* **Modernized JavaFX User Interface & Dynamic Themes:**
  The graphical user interface has been fully reimagined, incorporating vector-aligned Google Material Symbols for intuitive operational state recognition. To seamlessly integrate into any workspace setup, the interface includes **three dynamic presentation themes**, including highly optimized Light Mode and Dark Mode environments.
* **Silent System Tray Lifecycle:**
  Clicking the window's native `X` close button redirects the application stage into the operating system's system tray rather than killing the process. DataSync continues to live autarkically in the background, executing scheduled cron cycles silently without stealing window focus or interrupting the user's workflow.

---

## 📂 Advanced Job Topologies & Operational Behaviors

Every synchronization or backup profile can be fine-tuned via an extensive matrix of optional parameters, allowing precise control over how data flows from sources to destinations:

### Multi-Source Ingestion Framework
DataSync breaks the traditional one-to-one directory mapping constraint. A single configuration profile can monitor **multiple source directories simultaneously**, consolidating distinct physical storage sectors into a unified destination target.

### Directory Content Flattening (`subdir` Configuration exclusive to Backup Mode)
This parameter defines how the contents of multiple source directories are projected onto the destination filesystem root:
* **Enabled (`subdir = true`):** The engine extracts the immediate *contents* (files and nested children) of all configured sources and copies them directly into the root of the destination directory. The original parent folder names are omitted, allowing a flat consolidation of files.
* **Disabled (`subdir = false`):** The engine preserves the identity of the source directories. It creates matching top-level root folders named after the source directories inside the target directory, ensuring absolute structural separation.

### Integrated Isolation Trashbin (Exclusive to Backup Mode)
To prevent accidental data loss during mirroring sequences, DataSync includes an isolated, temporary trashbin allocation layer. When a file is altered or marked for removal, the engine avoids destructive filesystem purges and instead routes the legacy file into a dedicated recycling buffer. 
* *Operational Constraint:* This safety feature is strictly reserved for **Backup Mode** to prevent the recycling volume from overflowing during high-frequency bidirectional synchronizations. (An integration into the Duplicate Scan resolution pipeline is planned for future iterations).

### Automation vs. Manual Interactive Analysis (`Auto-Sync` & `Auto-Delete`)
When running a profile manually to audit your data boundaries, DataSync supports a comprehensive interactive review layer. The advanced automation flags are restricted as follows:
* **Automated Processing (`Auto-Sync` & `Auto-Delete` Enabled):** Reserved exclusively for **Backup Mode**. The engine calculates filesystem deltas and immediately executes necessary copy or destructive mirror-purges without user intervention.
* **Interactive Querying (Flags Disabled):** If these options are unchecked, the engine behaves purely as a non-destructive analysis tool. It compiles a comprehensive structural delta report and explicitly prompts the user via an interactive dialog to approve or reject the calculated file operations before a single byte is altered on disk.
* *Constraint:* Automated background automation is omitted from the pure **Sync** and **Duplicate** modes, where blind destructive runs are inherently unsafe.

### Structured Machine-Readable JSON Logging & Automatic Rotation

Every filesystem interaction, checksum calculation, and profile state transition is pushed simultaneously to a standard console log and a structured, machine-readable JSON log file. This allows system administrators to seamlessly ingest DataSync performance metrics into enterprise log aggregation pipelines (such as Grafana Loki or ELK stacks) for real-time remote monitoring.

To safeguard storage infrastructure during high-throughput synchronization tasks, the logging engine features an **integrated log rotation subsystem (`Logrotater`)**. This prevents disk space exhaustion by automatically managing the lifecycle of active and historical log files based on strict, configurable boundaries:

*   **Size-Based Triggers (`maxFileSize`):** Automatically cuts and rotates the active log file the exact moment it breaches the configured byte threshold, keeping the active log footprint predictable.
*   **Archival Retention Policies (`maxBackupIndex`):** Automatically coordinates index shifting for historical log files and enforces a strict capacity limit, purging the oldest backup segments once the maximum history depth is reached.

### Dynamic Configuration Routing (`--configPath` Bootstrap Flag)
The application lifecycle can be dynamically re-routed during execution initialization by utilizing the new `--configPath <path>` CLI bootstrap argument. Passing this specific operational flag overrides the standard application directory layout ( at `${user.home}/DataSync}`):
* **Decoupled Environment Routing:** DataSync detaches the production environment from the local runtime directory, forcing the engine to read and write the core `conf.json` storage block as well as all machine-readable JSON logs directly within the newly assigned target workspace path.
* **Enterprise Portability:** This parameter transforms the modular system into a fully portable instance, enabling seamless execution from external storage components or allowing system administrators to centralize the active profile configurations on secure network shares.

---

## 🔍 Comprehensive Scan Modes

To balance raw execution speed against absolute cryptographic data integrity across different storage mediums (such as local NVMe arrays versus high-latency network NAS shares), DataSync provides four distinct operational execution modes:

### Synchronize (Standard Sync)
Provides automated, scheduled synchronization between targeted directory structures, operating at configurable intervals with a minimum frequency of one minute. Its operational scope regarding structural directory pruning and depth limitations adapts dynamically to the profile's specialized UI constraints.

### Flat Scan (Metadata Backup)
An ultra-fast, IO-efficient delta computation engine designed for frequent backups. The system checks filesystem metadata snapshots instantly, evaluating changes based on file size boundaries, absolute file paths, creation epochs, and modification timestamps. This delivers rapid sync speeds without taxing storage controllers.

### Deep Scan (Cryptographic Verification Backup)
The ultimate validation tier for long-term archiving and preventing silent data corruption (bit-rot). The engine bypasses superficial OS file headers and hashes the underlying file contents byte-by-byte utilizing a high-performance **SHA-256 cryptographic checksum matrix**. Files are only marked as identical if their hash signatures match perfectly.

### Duplicate Scan (Asynchronous Duplicity Analysis)
A highly specialized diagnostic tool dedicated to recovering wasted storage capacity. Operating asynchronously to keep the UI perfectly fluid, this engine comprehensively dissects the configured source directories. It indexes every single file using content-based cryptographic hashing to isolate and identify identical file twins, even if they have been renamed or scattered across entirely different subfolders.

---

## ⏱️ Background Scheduling & Temporal Metrics

Automated background processing tasks can be bound to profiles independently. Each profile executes its internal delta evaluation loop according to its assigned temporal token:
* High-Frequency Micro-Intervals: `1 Minute`, `5 Minuten`, or `30 Minuten`
* Standard Schedules: `Stündlich` (Hourly)
* Macro-Schedules: `Täglich` (Daily), `Wöchentlich` (Weekly), `Monatlich` (Monthly)

⚠️ **Operating System Constraints:** Native background execution hooks, thread binding, and the asynchronous system tray integration are compiled specifically for **Windows** and **Linux** environments. macOS architectures are currently unsupported.

---

## ⚙️ Command Line Flags

Fine-tune the application startup behavior using the following CLI arguments:

| Short | Long Flag | Description | Example |
| :--- | :--- | :--- | :--- |
| `-b` | `--boot-delay` | Delays the application execution for 5 minutes after system startup (useful for boot-start scripts). | `java -jar DataSync-full.java -b` |
| `-d` | `--debug` | Enables detailed debug output in the system console. | `./bin/java -jar DataSync.jar --debug` |
| `-f` | `--debug-to-file` | Redirects all debug and error logs to a file located at `user.home/DataSync/` (implicitly enables debug mode). | `DataSync.exe -f` |
| `-c` | `--config-dir` | Sets a custom path for the configuration directory instead of the default location. Supports both space and `=` separators. | `-c /myNewDir/` or `--config-dir=/myNewDir/` |


## ⚙️ VM Arguments

When running the jar or debugging the application manually from an IDE or via a custom launcher, the following Java Virtual Machine (VM) properties must be configured:

### Native Access (Required)

Since the user interface framework requires hardware acceleration and native shell hooks, you must grant explicit permission at startup:

```bash
--enable-native-access=javafx.graphics
```

### Logging & Instance Customization (Optional)

You can passing an explicit instance identifier to the JVM. This tag will be dynamically appended to all generated debug log frames, allowing you to distinguish between multiple parallel sync routines:

```bash
-Dapp.instance.name="Your Custom Instance Name"
```

└──> **Log-Line:**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;01-07-2026 00:12:42.13947 \[ Your Custom Instance Name \] \[BgModel\] BgJob running<br>
└──> **Standard:**<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;01-07-2026 00:12:42.13947 \[ DataSync_v1.1.0.0 \] \[BgModel\] BgJob running
   
---

## 🛠️ Build & Requirements

### Requirements
* **Java Development Kit (JDK) 25** or higher.
* **Apache Maven** (for dependency management and building).

### Build Flags & Environment
To successfully compile and test the application, the Java compiler and build tools require specific native access privileges. Ensure your build environment passes these flags:
* `--enable-native-access=ALL-UNNAMED` (Allows build plugins to access native system APIs)
* `-Djdk.io.File.allowDeleteReadOnlyFiles=true` (Ensures safe cleanup of read-only files during the `clean` phase)

---

## 🏗️ Build Commands

### Build & Package (App-Image & Uber-JAR)
This command builds the lightweight, platform-specific runtime using `jlink` and automatically triggers `jpackage` to bundle everything into a production-ready application image.

```bash
mvn -DskipTests clean compile jlink:jlink package
```

### Sign the Runtime or JAR
For signing use the install phase:

```bash
clean compile jlink:jlink install
```

### Comprehensive Quality Verification
Runs the entire test suite, performs static code analysis via PMD/Checkstyle, and verifies the full build integrity:

```bash
mvn clean compile jlink:jlink verify
```
### Documentation & Site Generation
Generates the complete project documentation site, including deep Javadocs and Maven report metrics:

```bash
mvn clean compile jlink:jlink site
```
