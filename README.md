# DataSync

### _Currently supported Language: German_

A synchronization app that automatically syncs one or more folders in the background. The source paths can also be scan for duplicates.
On the x-button it minimized to tray.

### Features:

| Scan Type | Description
|---|---|
| Deep Scan | Scan each file with sha256
| Flat Scan | Equals file size, create and modified time, size and path
| Duplicate Scan | Scan the source directory and compares each file with the hash value

</br>

- 📂   __subfolders (optional)__
- 🗑️   __trashbin (optional)__
- 📎   __logging in JSON format (optional)__
- ✂️   __auto delete (optional)__
- 🚴‍♂️   __manual operation__
 
 </br>
 
Background job time:
` 1, 5, 30 minutes`
`hourly` 
`daily`
`weekly`
`monthly`

The automatic background job is not supported on MacOS yet

### Build:

* To build the app with runtime for a specify platform, run Maven on this platform with `mvn clean compile jlink:jlink verify`
 
* To use only the jar, java version 16 is required. Run `mvn package`
