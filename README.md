# DataSync

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](#)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-blue.svg)](#)

An efficient synchronization and backup application that automatically manages your folders in the background. It supports multiple synchronization strategies, duplicate detection, and runs silently in the system tray.

> **Note:** The user interface and logs are currently available in **German**.

---

## 🚀 Features

* **Background Operation:** Minimized to the system tray when closing via the X-button, keeping your workspace clean.
* **Flexible Automation:** Highly configurable background intervals.
* **Advanced Scanning:** Supports multiple scan types tailored to your needs—from fast metadata checks to deep cryptographic verification.
* **Optional Behaviors:**
  * 📂 **Subfolder Support:** Include or exclude subdirectories.
  * 🗑️ **Trashbin Integration:** Move deleted files to a temporary backup instead of permanent deletion (mode-dependent).
  * ✂️ **Auto-Delete:** Clean up destination paths automatically to mirror the source.
  * 📎 **JSON Logging:** Structured logging for easy parsing and monitoring.
  * 🚴‍♂️ **Manual Trigger:** Run any synchronization task instantly on demand.

---

## 🔍 Scan Types

| Scan Type | Description |
| :--- | :--- |
| **Synchronize** | Direct synchronization between two folders. *(Note: Trashbin and subfolder options are disabled in this mode).* |
| **Deep Scan** | Maximum precision. Compares files using cryptographic **SHA-256** checksums. |
| **Flat Scan** | Fast and efficient. Compares files based on metadata: size, creation time, modification time, and file path. |
| **Duplicate Scan** | Analyzes the source directory and identifies identical files by comparing their hash values. |

---

## ⏱️ Background Job Intervals

Automated background tasks can be scheduled to run at the following intervals:
* `1, 5, 30 minutes`
* `hourly`
* `daily`
* `weekly`
* `monthly`

⚠️ **OS Compatibility:** Automatic background execution is currently supported on **Windows** and **Linux**. macOS is not supported yet.

---

## ⚙️ Command Line Flags

Fine-tune the application startup behavior using the following flags:

| Flag | Description |
| :--- | :--- |
| `--firstStart` | Delays the application execution for 5 minutes after system startup (useful for boot-start scripts). |
| `--debug` | Enables detailed debug output in the console. |
| `--debugToFile` | Redirects all debug and error logs to a file located at `user.home/DataSync/`. |

---

## 🛠️ Build & Requirements

### Requirements
* **Java Development Kit (JDK) 21** or higher.
* **Apache Maven** (for building from source).

### Building a Platform-Specific Runtime (jlink)
To build the application bundled with a lightweight runtime tailored for your current platform, run:
```bash
mvn clean compile jlink:jlink verify
```

### Building a Standard JAR
If you prefer a standalone executable JAR (requires Java 21 pre-installed on the target system):
```bash
mvn clean package
```
