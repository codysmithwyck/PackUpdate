package at.chaosfield.packupdate;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class FxController {

    @FXML
    private Label status;

    @FXML
    private ProgressBar progress;

    private List<String> parameters;

    private Stage primaryStage;

    public void setMain(PackUpdate main) {
        this.primaryStage = main.primaryStage;
        this.parameters = main.getParameters().getRaw();

        Task updater = new Task<List<String>>() {
            @Override
            protected List<String> call() {
                List<String> ret = new ArrayList<>();
                HashMap<String, String[]> updated = new HashMap<>();
                HashMap<String, String[]> updateables = null;

                try {
                    updateables = FileManager.getAvailableUpdates(parameters.get(0), parameters.get(2) + File.separator + parameters.get(1));
                    updateMessage("To Update: " + updateables.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (updateables != null) {
                    int current = 0;
                    updateProgress(current, updateables.size());

                    final String modsPath = parameters.get(2) + File.separator + "mods" + File.separator;
                    final String configPath = parameters.get(2) + File.separator + "config";
                    final String resourcesPath = parameters.get(2);

                    for (Map.Entry<String, String[]> entry : updateables.entrySet()) {
                        updateMessage("Updating " + entry.getKey());

                        switch (entry.getValue()[3]) {
                            case "mod":
                                if (!entry.getValue()[2].equals("")) { //If URL is not empty -> download new Version
                                    if (!entry.getValue()[1].equals("")) //If old version exists delete it
                                        if (!FileManager.deleteLocalFile(modsPath + entry.getKey() + "-" + entry.getValue()[1] + ".jar")) {
                                            ret.add("[" + entry.getKey() + "]" + "Deletion of file " + entry.getKey() + "-" + entry.getValue()[1] + ".jar failed");
                                            continue;
                                        }
                                    try {
                                        FileManager.downloadFile(entry.getValue()[2], modsPath + entry.getKey() + "-" + entry.getValue()[0] + ".jar");
                                    } catch (IOException e) {
                                        ret.add("[" + entry.getKey() + "]" + "Download failed.");
                                    }
                                } else {
                                    if (!FileManager.deleteLocalFile(modsPath + entry.getKey() + "-" + entry.getValue()[1] + ".jar")) {
                                        ret.add("[" + entry.getKey() + "]" + "Deletion of file " + entry.getKey() + "-" + entry.getValue()[1] + ".jar failed");
                                        continue;
                                    }
                                }
                                break;

                            case "config":
                                if (!entry.getValue()[2].equals("")) { //If URL is not empty -> download new Version
                                    if (!FileManager.deleteLocalFolderContents(configPath)) { //delete current config files
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the config folder's content or creating an empty config folder failed.");
                                        continue;
                                    }

                                    try {
                                        FileManager.downloadFile(entry.getValue()[2], configPath + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip");
                                    } catch (IOException e) {
                                        ret.add("[" + entry.getKey() + "]" + "Download failed.");
                                    }

                                    if (!FileManager.unzipLocalFile(configPath + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip", configPath + File.separator)) {
                                        ret.add("[" + entry.getKey() + "]" + "Unpack failed: The zip file seems to be corrupted.");
                                        continue;
                                    }
                                } else {
                                    if (!FileManager.deleteLocalFolderContents(configPath)) {
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the config folder's content or creating an empty config folder failed.");
                                        continue;
                                    }
                                }
                                break;

                            case "resources":
                                if (!entry.getValue()[2].equals("")) { //If URL is not empty -> download new Version
                                    if (!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "resources")) { //delete current config files
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the resources folder's content or creating an empty resources folder failed.");
                                        continue;
                                    }
                                    if (!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "scripts")) {
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the scripts folder's content or creating an empty scripts folder failed.");
                                        continue;
                                    }

                                    try {
                                        FileManager.downloadFile(entry.getValue()[2], resourcesPath + File.separator + "resources" + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip");
                                    } catch (IOException e) {
                                        ret.add("[" + entry.getKey() + "]" + "Download failed.");
                                    }

                                    if (!FileManager.unzipLocalFile(resourcesPath + File.separator + "resources" + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip", resourcesPath + File.separator)) {
                                        ret.add("[" + entry.getKey() + "]" + "Unpack failed: The zip file seems to be corrupted.");
                                        continue;
                                    }
                                } else {
                                    if (!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "resources")) {
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the resources folder's content or creating an empty resources folder failed.");
                                        continue;
                                    }
                                    if (!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "scripts")) {
                                        ret.add("[" + entry.getKey() + "]" + "Either deleting the scripts folder's content or creating an empty scripts folder failed.");
                                        continue;
                                    }
                                }
                                break;

                            default:

                        }

                        updated.put(entry.getKey(), entry.getValue());

                        current++;
                        updateProgress(current, updateables.size());
                        System.out.println("Successfully updated " + entry.getKey());
                    }
                }
                //FileManager.deleteLocalFile(parameters.get(2) + File.separator + parameters.get(1));
                //FileManager.downloadFile(parameters.get(0), parameters.get(2) + File.separator + parameters.get(1));

                //TODO generate cfg file, save it and return errors if any.

                return null;
            }
        };

        progress.progressProperty().bind(updater.progressProperty());
        status.textProperty().bind(updater.messageProperty());
        updater.setOnSucceeded(t -> {
            String[] returnValue = (String[]) updater.getValue();
            if (returnValue != null) {
                System.out.println(returnValue[1] + ": " + returnValue[2]);
                main.errorAlert(returnValue[0], returnValue[1], returnValue[2]);
            }
            primaryStage.close();
        });
        new Thread(updater).start();
    }
}
