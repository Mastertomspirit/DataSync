/*
        DataSync Application
        
        @author Tom Spirit
        
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation; either version 3 of the License, or
        (at your option) any later version.
        
        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.
        
        You should have received a copy of the GNU General Public License
        along with this program; if not, write to the Free Software Foundation,
        Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package de.spiritscorp.DataSync.Job;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.nio.file.Path;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.IO.Preference;

/**
 * Manages the reactive runtime context for an individual synchronization or backup task.
 * Holds task-specific properties, isolated file tables, and active worker thread references.
 * * @author Tom Spirit
 */
public class SyncJobContext {

    private final StringProperty jobName = new SimpleStringProperty();
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("Bereit");
    private final StringProperty logOutput = new SimpleStringProperty("");
    private final StringProperty selectedMode = new SimpleStringProperty("Synchronisieren");
    
    private final Preference taskPreference;
    private Thread activeWorkerThread;

    private final ObservableList<FileRow> duplicateFiles = FXCollections.observableArrayList();

    /**
     * Creates a new isolated synchronization job environment with its own preference clone.
     * * @param name The identification name for the user interface sidebar
     * @param basePreference The template preference instance to derive task-specific settings from
     */
    public SyncJobContext(String name, Preference basePreference) {
        this.jobName.set(name);
        // Creates an isolated copy/instance for this specific task
        this.taskPreference = new Preference();
        // Initialize with default values from the template if needed
        this.taskPreference.setSubDir(basePreference.isSubDir());
        this.taskPreference.setTrashbin(basePreference.isTrashbin());
        this.taskPreference.setAutoDel(basePreference.isAutoDel());
        this.taskPreference.setLogOn(basePreference.isLogOn());
        this.taskPreference.setBgSync(basePreference.isBgSync());
    }

    /**
     * Assigns the thread processing file modifications to allow secure termination handles.
     * * @param thread The execution context running background tasks
     */
    public synchronized void setActiveWorkerThread(Thread thread) {
        this.activeWorkerThread = thread;
    }

    /**
     * Interrupts the active worker loop safely using standard thread interruption signals.
     */
    public synchronized void cancelRunningTask() {
        if (activeWorkerThread != null && activeWorkerThread.isAlive()) {
            activeWorkerThread.interrupt();
            setRunning(false);
            setStatusMessage("Aktion vom Benutzer abgebrochen.");
            appendLog("-> Vorgang abgebrochen.");
        }
    }

    public void appendLog(String line) {
        this.logOutput.set(this.logOutput.get() + line + System.lineSeparator());
    }

    public void clearLog() {
        this.logOutput.set("");
    }

    public String getJobName() { return jobName.get(); }
    public StringProperty jobNameProperty() { return jobName; }

    public boolean isRunning() { return running.get(); }
    public BooleanProperty runningProperty() { return running; }
    public void setRunning(boolean value) { this.running.set(value); }

    public String getStatusMessage() { return statusMessage.get(); }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public void setStatusMessage(String message) { this.statusMessage.set(message); }

    public String getLogOutput() { return logOutput.get(); }
    public StringProperty logOutputProperty() { return logOutput; }

    public String getSelectedMode() { return selectedMode.get(); }
    public StringProperty selectedModeProperty() { return selectedMode; }
    public void setSelectedMode(String mode) { this.selectedMode.set(mode); }

    public Preference getPreference() { return taskPreference; }
    public ObservableList<FileRow> getDuplicateFiles() { return duplicateFiles; }

    /**
     * Wraps file characteristics inside property types suited for dynamic UI grids.
     */
    public static class FileRow {
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final StringProperty fileName = new SimpleStringProperty();
        private final StringProperty size = new SimpleStringProperty();
        private final StringProperty hash = new SimpleStringProperty();
        private final StringProperty path = new SimpleStringProperty();
        private final Path fileSystemPath;

        public FileRow(Path path, FileAttributes attr, String readableSize) {
            this.fileSystemPath = path;
            this.fileName.set(attr.getFileName());
            this.size.set(readableSize);
            this.hash.set(attr.getFileHash());
            this.path.set(path.toString());
        }

        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { this.selected.set(val); }
        
        public StringProperty fileNameProperty() { return fileName; }
        public StringProperty sizeProperty() { return size; }
        public StringProperty hashProperty() { return hash; }
        public StringProperty pathProperty() { return path; }
        public Path getFileSystemPath() { return fileSystemPath; }
    }
}
