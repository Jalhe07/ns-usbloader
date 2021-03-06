/*
    Copyright 2019-2020 Dmitry Isaenko

    This file is part of NS-USBloader.

    NS-USBloader is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NS-USBloader is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NS-USBloader.  If not, see <https://www.gnu.org/licenses/>.
*/
package nsusbloader.Controllers;

import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import nsusbloader.AppPreferences;
import nsusbloader.ServiceWindow;
import nsusbloader.ModelControllers.UpdatesChecker;
import nsusbloader.Utilities.WindowsDrivers.DriversInstall;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SettingsController implements Initializable {
    @FXML
    private CheckBox nspFilesFilterForGLCB,
            validateNSHostNameCb,
            expertModeCb,
            autoDetectIpCb,
            randPortCb;

    @FXML
    private TextField pcIpTextField,
            pcPortTextField,
            pcExtraTextField;

    @FXML
    private CheckBox dontServeCb;

    @FXML
    private VBox expertSettingsVBox;

    @FXML
    private CheckBox autoCheckUpdCb;
    @FXML
    private Hyperlink newVersionLink;

    @FXML
    private CheckBox tfXciSpprtCb;

    @FXML
    private Button langBtn,
            checkForUpdBtn,
            drvInstBtn;
    @FXML
    private ChoiceBox<String> langCB;

    @FXML
    private ChoiceBox<String> glVersionChoiceBox;

    private HostServices hs;

    public static final String[] glSupportedVersions = {"v0.5", "v0.7.x", "v0.8"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nspFilesFilterForGLCB.setSelected(AppPreferences.getInstance().getNspFileFilterGL());

        validateNSHostNameCb.setSelected(AppPreferences.getInstance().getNsIpValidationNeeded());

        expertSettingsVBox.setDisable(!AppPreferences.getInstance().getExpertMode());

        expertModeCb.setSelected(AppPreferences.getInstance().getExpertMode());
        expertModeCb.setOnAction(e-> expertSettingsVBox.setDisable(!expertModeCb.isSelected()));

        autoDetectIpCb.setSelected(AppPreferences.getInstance().getAutoDetectIp());
        pcIpTextField.setDisable(AppPreferences.getInstance().getAutoDetectIp());
        autoDetectIpCb.setOnAction(e->{
            pcIpTextField.setDisable(autoDetectIpCb.isSelected());
            if (!autoDetectIpCb.isSelected())
                pcIpTextField.requestFocus();
        });

        randPortCb.setSelected(AppPreferences.getInstance().getRandPort());
        pcPortTextField.setDisable(AppPreferences.getInstance().getRandPort());
        randPortCb.setOnAction(e->{
            pcPortTextField.setDisable(randPortCb.isSelected());
            if (!randPortCb.isSelected())
                pcPortTextField.requestFocus();
        });

        if (AppPreferences.getInstance().getNotServeRequests()){
            dontServeCb.setSelected(true);

            autoDetectIpCb.setSelected(false);
            autoDetectIpCb.setDisable(true);
            pcIpTextField.setDisable(false);

            randPortCb.setSelected(false);
            randPortCb.setDisable(true);
            pcPortTextField.setDisable(false);
        }
        pcExtraTextField.setDisable(!AppPreferences.getInstance().getNotServeRequests());

        dontServeCb.setOnAction(e->{
            if (dontServeCb.isSelected()){
                autoDetectIpCb.setSelected(false);
                autoDetectIpCb.setDisable(true);
                pcIpTextField.setDisable(false);

                randPortCb.setSelected(false);
                randPortCb.setDisable(true);
                pcPortTextField.setDisable(false);

                pcExtraTextField.setDisable(false);
                pcIpTextField.requestFocus();
            }
            else {
                autoDetectIpCb.setDisable(false);
                autoDetectIpCb.setSelected(true);
                pcIpTextField.setDisable(true);

                randPortCb.setDisable(false);
                randPortCb.setSelected(true);
                pcPortTextField.setDisable(true);

                pcExtraTextField.setDisable(true);
            }
        });

        pcIpTextField.setText(AppPreferences.getInstance().getHostIp());
        pcPortTextField.setText(AppPreferences.getInstance().getHostPort());
        pcExtraTextField.setText(AppPreferences.getInstance().getHostExtra());

        pcIpTextField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().contains(" ") | change.getControlNewText().contains("\t"))
                return null;
            else
                return change;
        }));
        pcPortTextField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("^[0-9]{0,5}$")) {
                if (!change.getControlNewText().isEmpty()
                        && ((Integer.parseInt(change.getControlNewText()) > 65535) || (Integer.parseInt(change.getControlNewText()) == 0))
                ) {
                    ServiceWindow.getErrorNotification(resourceBundle.getString("windowTitleErrorPort"), resourceBundle.getString("windowBodyErrorPort"));
                    return null;
                }
                return change;
            }
            else
                return null;
        }));
        pcExtraTextField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().contains(" ") | change.getControlNewText().contains("\t"))
                return null;
            else
                return change;
        }));

        newVersionLink.setVisible(false);
        newVersionLink.setOnAction(e-> hs.showDocument(newVersionLink.getText()));

        autoCheckUpdCb.setSelected(AppPreferences.getInstance().getAutoCheckUpdates());

        Region btnSwitchImage = new Region();
        btnSwitchImage.getStyleClass().add("regionUpdatesCheck");
        checkForUpdBtn.setGraphic(btnSwitchImage);

        checkForUpdBtn.setOnAction(e->{
            Task<List<String>> updTask = new UpdatesChecker();
            updTask.setOnSucceeded(event->{
                List<String> result = updTask.getValue();
                if (result != null){
                    if (result.get(0).isEmpty()){
                        ServiceWindow.getInfoNotification(resourceBundle.getString("windowTitleNewVersionNOTAval"), resourceBundle.getString("windowBodyNewVersionNOTAval"));
                    }
                    else {
                        setNewVersionLink(result.get(0));
                        ServiceWindow.getInfoNotification(resourceBundle.getString("windowTitleNewVersionAval"), resourceBundle.getString("windowTitleNewVersionAval")+": "+result.get(0) + "\n\n" + result.get(1));
                    }
                }
                else {
                    ServiceWindow.getInfoNotification(resourceBundle.getString("windowTitleNewVersionUnknown"), resourceBundle.getString("windowBodyNewVersionUnknown"));
                }
            });
            Thread updates = new Thread(updTask);
            updates.setDaemon(true);
            updates.start();
        });

        if (isWindows()){
            Region btnDrvImage = new Region();
            btnDrvImage.getStyleClass().add("regionWindows");
            drvInstBtn.setGraphic(btnDrvImage);
            drvInstBtn.setVisible(true);
            drvInstBtn.setOnAction(actionEvent -> new DriversInstall(resourceBundle));
        }

        tfXciSpprtCb.setSelected(AppPreferences.getInstance().getTfXCI());

        // Language settings area
        ObservableList<String> langCBObsList = FXCollections.observableArrayList();
        langCBObsList.add("eng");

        File jarFile;
        try{
            String encodedJarLocation = getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replace("+", "%2B");
            jarFile = new File(URLDecoder.decode(encodedJarLocation, "UTF-8"));
        }
        catch (UnsupportedEncodingException uee){
            uee.printStackTrace();
            jarFile = null;
        }

        if(jarFile != null && jarFile.isFile()) {  // Run with JAR file
            try {
                JarFile jar = new JarFile(jarFile);
                Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith("locale_"))
                        langCBObsList.add(name.substring(7, 10));
                }
                jar.close();
            }
            catch (IOException ioe){
                ioe.printStackTrace();  // TODO: think about better solution?
            }
        }
        else {                                      // Run within IDE
            URL resourceURL = this.getClass().getResource("/");
            String[] filesList = new File(resourceURL.getFile()).list(); // Screw it. This WON'T produce NullPointerException

            for (String jarFileName : filesList)
                if (jarFileName.startsWith("locale_"))
                    langCBObsList.add(jarFileName.substring(7, 10));
        }

        langCB.setItems(langCBObsList);
        if (langCBObsList.contains(AppPreferences.getInstance().getLanguage()))
            langCB.getSelectionModel().select(AppPreferences.getInstance().getLanguage());
        else
            langCB.getSelectionModel().select("eng");

        langBtn.setOnAction(e->{
            AppPreferences.getInstance().setLanguage(langCB.getSelectionModel().getSelectedItem());
            ServiceWindow.getInfoNotification("",
                    ResourceBundle.getBundle("locale", new Locale(langCB.getSelectionModel().getSelectedItem()))
                            .getString("windowBodyRestartToApplyLang"));
        });
        // Set supported old versions
        glVersionChoiceBox.getItems().addAll(glSupportedVersions);
        String oldVer = AppPreferences.getInstance().getGlVersion();  // Overhead; Too much validation of consistency
        glVersionChoiceBox.getSelectionModel().select(oldVer);
    }

    private boolean isWindows(){
        return System.getProperty("os.name").toLowerCase().replace(" ", "").contains("windows");
    }

    public boolean getNSPFileFilterForGL(){return nspFilesFilterForGLCB.isSelected(); }
    public boolean getExpertModeSelected(){ return expertModeCb.isSelected(); }
    public boolean getAutoIpSelected(){ return autoDetectIpCb.isSelected(); }
    public boolean getRandPortSelected(){ return randPortCb.isSelected(); }
    public boolean getNotServeSelected(){ return dontServeCb.isSelected(); }

    public boolean isNsIpValidate(){ return validateNSHostNameCb.isSelected(); }

    public String getHostIp(){ return pcIpTextField.getText(); }
    public String getHostPort(){ return pcPortTextField.getText(); }
    public String getHostExtra(){ return pcExtraTextField.getText(); }
    public boolean getAutoCheckForUpdates(){ return autoCheckUpdCb.isSelected(); }
    public boolean getTfXciNszXczSupport(){ return tfXciSpprtCb.isSelected(); }           // Used also for NSZ/XCZ

    public void registerHostServices(HostServices hostServices){this.hs = hostServices;}

    public void setNewVersionLink(String newVer){
        newVersionLink.setVisible(true);
        newVersionLink.setText("https://github.com/developersu/ns-usbloader/releases/tag/"+newVer);
    }

    public String getGlVer() {
        return glVersionChoiceBox.getValue();
    }
    
    public void updatePreferencesOnExit(){
        AppPreferences preferences = AppPreferences.getInstance();

        preferences.setNsIpValidationNeeded(isNsIpValidate());
        preferences.setExpertMode(getExpertModeSelected());
        preferences.setAutoDetectIp(getAutoIpSelected());
        preferences.setRandPort(getRandPortSelected());
        preferences.setNotServeRequests(getNotServeSelected());
        preferences.setHostIp(getHostIp());
        preferences.setHostPort(getHostPort());
        preferences.setHostExtra(getHostExtra());
        preferences.setAutoCheckUpdates(getAutoCheckForUpdates());
        preferences.setTfXCI(getTfXciNszXczSupport());
        preferences.setNspFileFilterGL(getNSPFileFilterForGL());
        preferences.setGlVersion(getGlVer());
    }
}