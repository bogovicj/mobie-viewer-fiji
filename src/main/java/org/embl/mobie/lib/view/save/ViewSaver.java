/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.view.save;

import ij.IJ;
import ij.gui.GenericDialog;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.create.ProjectCreatorHelper;
import org.embl.mobie.lib.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.ui.UserInterfaceHelper;
import org.embl.mobie.lib.view.AdditionalViews;
import org.embl.mobie.lib.serialize.View;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.github.GitHubUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.lib.view.save.ViewSavingHelper.writeAdditionalViewsJson;
import static org.embl.mobie.lib.view.save.ViewSavingHelper.writeDatasetJson;
import static org.embl.mobie.io.github.GitHubUtils.isGithub;
import static org.embl.mobie.io.util.IOHelper.getFileNames;
import static org.embl.mobie.io.util.S3Utils.isS3;

public class ViewSaver
{
    public static final String NEW_VIEWS_JSON_FILE = "Make new views json file";

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private MoBIE moBIE;
    private MoBIESettings settings;

    enum ProjectSaveLocation {
        datasetJson,
        viewsJson
    }

    enum SaveMethod {
        saveAsNewView,
        overwriteExistingView
    }

    public ViewSaver( MoBIE moBIE) {
        this.moBIE = moBIE;
        this.settings = moBIE.getSettings();
    }

    public void saveCurrentSettingsAsViewDialog()
    {
        final GenericDialog gd = new GenericDialog("Save current view");

        String[] choices = new String[]{ "Save as new view", "Overwrite existing view" };
        gd.addChoice("Save method:", choices, choices[0] );
        gd.addChoice("Save to", new String[]{ MoBIEHelper.FileLocation.Project.toString(), MoBIEHelper.FileLocation.FileSystem.toString()}, MoBIEHelper.FileLocation.Project.toString());
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String saveMethodString = gd.getNextChoice();
            MoBIEHelper.FileLocation fileLocation = MoBIEHelper.FileLocation.valueOf(gd.getNextChoice());

            SaveMethod saveMethod;
            if (saveMethodString.equals("Save as new view")) {
                saveMethod = SaveMethod.saveAsNewView;
            } else {
                saveMethod = SaveMethod.overwriteExistingView;
            }
            viewSettingsDialog( saveMethod, fileLocation );
        }
    }

    public void viewSettingsDialog( SaveMethod saveMethod, MoBIEHelper.FileLocation fileLocation ) {
        final GenericDialog gd = new GenericDialog("View settings");

        if ( saveMethod == SaveMethod.saveAsNewView ) {
            gd.addStringField("View name:", "", 25 );
        }

        String[] currentUiSelectionGroups = moBIE.getUserInterface().getUISelectionGroupNames();
        String[] choices = new String[currentUiSelectionGroups.length + 1];
        choices[0] = "Make New Ui Selection Group";
        for (int i = 0; i < currentUiSelectionGroups.length; i++) {
            choices[i + 1] = currentUiSelectionGroups[i];
        }
        gd.addChoice("Ui Selection Group", choices, choices[0]);

        if ( fileLocation == MoBIEHelper.FileLocation.Project ) {
            String[] jsonChoices = new String[]{"dataset.json", "views.json"};
            gd.addChoice("Save location:", jsonChoices, jsonChoices[0]);
        }

        gd.addCheckbox("exclusive", true);
        gd.addCheckbox("Include viewer transform?", true );

        gd.showDialog();

        if (!gd.wasCanceled()) {

            String viewName = null;
            if( saveMethod == SaveMethod.saveAsNewView ) {
                viewName = UserInterfaceHelper.tidyString( gd.getNextString() );
                if ( viewName == null ) {
                    return;
                }
            }

            String uiSelectionGroup = gd.getNextChoice();
            ProjectSaveLocation projectSaveLocation = null;
            if ( fileLocation == MoBIEHelper.FileLocation.Project ) {
                String projectSaveLocationString = gd.getNextChoice();
                if ( projectSaveLocationString.equals("dataset.json") ) {
                    projectSaveLocation =  ProjectSaveLocation.datasetJson;
                } else if ( projectSaveLocationString.equals("views.json") ) {
                    projectSaveLocation =  ProjectSaveLocation.viewsJson;
                }
            }

            boolean exclusive = gd.getNextBoolean();
            boolean includeViewerTransform = gd.getNextBoolean();

            if (uiSelectionGroup.equals("Make New Ui Selection Group")) {
                uiSelectionGroup = ProjectCreatorHelper.makeNewUiSelectionGroup(currentUiSelectionGroups);
            }

            View currentView = moBIE.getViewManager().createViewFromCurrentState(uiSelectionGroup, exclusive, includeViewerTransform);

            if ( uiSelectionGroup != null && currentView != null ) {
                if ( fileLocation == MoBIEHelper.FileLocation.Project && saveMethod == SaveMethod.saveAsNewView ) {
                    saveNewViewToProject( currentView, viewName, projectSaveLocation );
                } else if ( fileLocation == MoBIEHelper.FileLocation.Project && saveMethod == SaveMethod.overwriteExistingView ) {
                    overwriteExistingViewInProject( currentView, projectSaveLocation );
                } else if ( fileLocation == MoBIEHelper.FileLocation.FileSystem && saveMethod == SaveMethod.saveAsNewView ) {
                    saveNewViewToFileSystem( currentView, viewName );
                } else if ( fileLocation == MoBIEHelper.FileLocation.FileSystem && saveMethod == SaveMethod.overwriteExistingView ) {
                    overwriteExistingViewOnFileSystem( currentView );
                }
            }
        }
    }

    private String chooseFileSystemJson() {
        String jsonPath = UserInterfaceHelper.selectFilePath( "json", "json file", false );

        if ( jsonPath != null && !jsonPath.endsWith(".json") ) {
                jsonPath += ".json";
        }

        return jsonPath;
    }

    private void saveNewViewToFileSystem( View view, String viewName ) {
        new Thread( () -> {
            String jsonPath = chooseFileSystemJson();
            if ( jsonPath != null ) {
                try {
                    saveNewViewToAdditionalViewsJson( view, viewName, jsonPath );
                    addViewToUi( viewName, view );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void overwriteExistingViewOnFileSystem( View view ) {
        new Thread( () -> {
            String jsonPath = chooseFileSystemJson();
            if ( jsonPath != null ) {
                try {
                    overwriteExistingViewInAdditionalViewsJson( view, jsonPath );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void saveNewViewToProject( View view, String viewName, ProjectSaveLocation projectSaveLocation ) {
        try {
            if (projectSaveLocation == ProjectSaveLocation.datasetJson) {
                saveViewToDatasetJson( view, viewName, false );
            } else {
                String viewJsonPath = chooseAdditionalViewsJson( true );
                if (viewJsonPath != null) {
                    saveNewViewToAdditionalViewsJson( view, viewName, viewJsonPath);
                }
            }
            addViewToUi( viewName, view );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void overwriteExistingViewInProject( View view, ProjectSaveLocation projectSaveLocation )
    {
        if ( isS3( moBIE.getProjectLocation() ) )
        {
            throw new UnsupportedOperationException("View saving aborted - saving directly to s3 is not yet supported!");
        }
        else
        {
            try {
                if (projectSaveLocation == ProjectSaveLocation.datasetJson) {
                    overwriteExistingViewInDatasetJson( view );
                } else {
                    String viewJsonPath = chooseAdditionalViewsJson( false );
                    if (viewJsonPath != null) {
                        overwriteExistingViewInAdditionalViewsJson( view, viewJsonPath);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addViewToUi( String viewName, View view ) {
        moBIE.getViews().put( viewName, view );
        Map<String, View > views = new HashMap<>();
        views.put( viewName, view );
        moBIE.getUserInterface().addViews( views );
    }

    public void saveViewToDatasetJson( View view, String viewName, boolean overwrite ) throws IOException
    {
        String datasetJsonPath = moBIE.absolutePath( "dataset.json");
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );

        if ( ! overwrite )
            if ( dataset.views().containsKey( viewName ) )
                throw new IOException( "View saving aborted - this view name already exists!" );

        writeDatasetJson( dataset, view, viewName, datasetJsonPath );
        IJ.log( "View \"" + viewName + "\" written to dataset.json" );
    }

    private void overwriteExistingViewInDatasetJson( View view ) throws IOException {
        String datasetJsonPath = moBIE.absolutePath( "dataset.json");
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );

        if ( dataset.views().keySet().size() > 0 ) {
            String selectedView = new SelectExistingViewDialog( dataset ).getSelectedView();
            if ( selectedView != null ) {
                writeDatasetJson( dataset, view, selectedView, datasetJsonPath );
                IJ.log( selectedView + " overwritten in dataset.json" );
                addViewToUi( selectedView, view );
            }
        } else {
            IJ.log( "View saving aborted - dataset.json contains no views" );
        }
    }

    private boolean jsonExists( String jsonPath ) {
        if ( isGithub( jsonPath )) {
            return new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation(jsonPath) ).jsonExists();
        } else {
            return new File( jsonPath ).exists();
        }
    }

    private void overwriteExistingViewInAdditionalViewsJson( View view, String jsonPath ) throws IOException {

        if ( !jsonExists( jsonPath ) ) {
            IJ.log( "View saving aborted - this views json does not exist" );
            return;
        }

        AdditionalViews additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
        String selectedView = new SelectExistingViewDialog( additionalViews ).getSelectedView();

        if ( selectedView != null ) {
            writeAdditionalViewsJson( additionalViews, view, selectedView, jsonPath );
            IJ.log( selectedView + " overwritten in " + new File(jsonPath).getName() );
            addViewToUi( selectedView, view );
        }
    }

    private void saveNewViewToAdditionalViewsJson( View view, String viewName, String jsonPath ) throws IOException {

        AdditionalViews additionalViews;
        if ( jsonExists( jsonPath ) ) {
            additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
            if ( additionalViews.views.containsKey( viewName ) ) {
                IJ.log( "View saving aborted - this view name already exists!" );
                return;
            }
        } else {
            additionalViews = new AdditionalViews();
            additionalViews.views = new HashMap<>();
        }

        writeAdditionalViewsJson( additionalViews, view, viewName, jsonPath );
        IJ.log( "New view, " + viewName + ", written to " + new File( jsonPath ).getName() );
    }

    private String chooseAdditionalViewsJson( boolean includeOptionToMakeNewViewJson ) {
        String additionalViewsDirectory = moBIE.absolutePath( "misc", "views");
        String[] existingViewFiles = getFileNames(additionalViewsDirectory);

        String jsonFileName = null;
        if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
            jsonFileName = chooseViewsJsonDialog( existingViewFiles, includeOptionToMakeNewViewJson );
        } else if ( includeOptionToMakeNewViewJson ) {
            jsonFileName = makeNewViewFile( existingViewFiles );
        } else {
            IJ.log("View saving aborted - no additional views jsons exist" );
        }

        if ( jsonFileName != null ) {
            return moBIE.absolutePath( "misc", "views", jsonFileName);
        } else {
            return null;
        }
    }

    private String chooseViewsJsonDialog( String[] viewFileNames, boolean includeOptionToMakeNewViewJson ) {
        final GenericDialog gd = new GenericDialog("Choose views json");

        String[] choices;
        if ( includeOptionToMakeNewViewJson ) {
            choices = new String[viewFileNames.length + 1];
            choices[0] = NEW_VIEWS_JSON_FILE;
            for (int i = 0; i < viewFileNames.length; i++) {
                choices[i + 1] = viewFileNames[i];
            }
        } else {
            choices = viewFileNames;
        }

        gd.addChoice("Choose Views Json:", choices, choices[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String choice = gd.getNextChoice();
            if ( includeOptionToMakeNewViewJson && choice.equals( NEW_VIEWS_JSON_FILE ) ) {
                choice = makeNewViewFile( viewFileNames );
            }
            return choice;
        } else {
            return null;
        }

    }

    private String makeNewViewFile( String[] existingViewFiles ) {
        String viewFileName = chooseNewViewsFileNameDialog();

        // get rid of any spaces, warn for unusual characters in basename (without the .json)
        if ( viewFileName != null ) {
            viewFileName = UserInterfaceHelper.tidyString( viewFileName);
        }

        if ( viewFileName != null ) {
            viewFileName += ".json";

            boolean alreadyExists = false;
            if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
                alreadyExists = Arrays.asList(existingViewFiles).contains(viewFileName);
            }
            if ( alreadyExists ) {
                viewFileName = null;
                IJ.log("Saving view aborted - new view file already exists");
            }
        }

        return viewFileName;
    }

    private String chooseNewViewsFileNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose views json filename");
        gd.addStringField("New view json filename:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String viewFileName =  gd.getNextString();

            // we just want the basename, no extension
            if ( viewFileName != null && viewFileName.endsWith(".json") ) {
                viewFileName = FilenameUtils.removeExtension( viewFileName );
            }
            return viewFileName;
        } else {
            return null;
        }

    }

}
