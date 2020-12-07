package de.embl.cba.mobie.projects;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagePropertiesEditor;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.command.OpenMoBIEPublishedProjectCommand;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.Prefs;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import net.imglib2.ops.parse.token.Int;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang.WordUtils;
import voltex.Mask;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

// TODO - move some of this to swingutils
// TODO - remove duplicates with other classes e.g imagepropertieseditor

public class ProjectsCreatorPanel extends JFrame {
    public static final int TEXT_FIELD_HEIGHT = 20;
    public static final int COMBOBOX_WIDTH = 270;
    public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );
    private ProjectsCreator projectsCreator;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> imagesComboBox;

    public ProjectsCreatorPanel ( File projectLocation ) {
        this.projectsCreator = new ProjectsCreator( projectLocation );
        addDatasetPanel();
        addImagesPanel();
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }

    public void showProjectsCreatorPanel() {
        this.pack();
        this.setVisible( true );
    }

    public ProjectsCreator getProjectsCreator() {
        return projectsCreator;
    }

    private JButton getButton(String buttonLabel )
    {
        return getButton( buttonLabel, BUTTON_DIMENSION );
    }

    private JButton getButton( String buttonLabel, Dimension dimension )
    {
        final JButton button = new JButton( buttonLabel );
        button.setPreferredSize( dimension ); // TODO
        return button;
    }

    private void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE );
        comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
        comboBox.setMaximumSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
    }
    private JLabel getJLabel( String text )
    {
        return getJLabel( text, 170, 10);
    }

    private JLabel getJLabel( String text, int width, int height )
    {
        final JLabel comp = new JLabel( text );
        comp.setPreferredSize( new Dimension( width, height ) );
        comp.setHorizontalAlignment( SwingConstants.LEFT );
        comp.setHorizontalTextPosition( SwingConstants.LEFT );
        comp.setAlignmentX( Component.LEFT_ALIGNMENT );
        return comp;
    }

    private void addDatasetPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = getButton("Add");
        final JButton editButton = getButton("Edit");

        createDatasetComboBox();
        addButton.addActionListener(e -> addDatasetDialog());
        editButton.addActionListener(e -> editDatasetDialog());

        horizontalLayoutPanel.add(getJLabel("dataset"));
        horizontalLayoutPanel.add(datasetComboBox);
        horizontalLayoutPanel.add(addButton);
        horizontalLayoutPanel.add(editButton);

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void createDatasetComboBox() {
        datasetComboBox = new JComboBox<>( projectsCreator.getCurrentDatasets() );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }

    private class SyncImageAndDatasetComboBox implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateImagesComboBox();
        }
    }

    private void addImagesPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        // final JButton addButton = getButton("Add");
        final JButton editButton = getButton("Edit");

        imagesComboBox = new JComboBox<>( projectsCreator.getCurrentImages( (String) datasetComboBox.getSelectedItem()) );
        setComboBoxDimensions(imagesComboBox);
        // addButton.addActionListener(e -> addImageDialog());
        editButton.addActionListener(e -> editImageDialog() );
        imagesComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);

        horizontalLayoutPanel.add(getJLabel("image"));
        horizontalLayoutPanel.add(imagesComboBox);
        // horizontalLayoutPanel.add(addButton);
        horizontalLayoutPanel.add( editButton );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    public String chooseDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Choose a dataset" );
        String[] currentDatasets = projectsCreator.getCurrentDatasets();
        gd.addChoice("Dataset", currentDatasets, currentDatasets[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoice();

        } else {
            return null;
        }


    }

    public String addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );

        gd.addStringField( "Name of dataset", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            // check not already in datasets
            boolean contains = projectsCreator.isInDatasets( datasetName );
            if ( !contains ) {
                projectsCreator.addDataset( datasetName );
                updateDatasetsComboBox( datasetName );
            }

            return datasetName;

        } else {
            return null;
        }
    }

    private void editDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Edit dataset..." );
        String oldName = (String) datasetComboBox.getSelectedItem();
        boolean isDefault = projectsCreator.isDefaultDataset( oldName );

        gd.addStringField( "Dataset name", oldName);
        if ( isDefault ) {
            gd.addMessage( "This dataset is the default");
        } else {
            gd.addCheckbox("Make default dataset", false);
        }
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String newName = gd.getNextString().trim();
            if ( !newName.equals(oldName) ) {
                // check not already in datasets
                boolean contains = projectsCreator.isInDatasets(newName);
                if (!contains) {
                    projectsCreator.renameDataset(oldName, newName);
                    updateDatasetsComboBox( newName );
                }
            }

            if ( !isDefault ) {
                boolean makeDefault = gd.getNextBoolean();
                if ( makeDefault ) {
                    projectsCreator.makeDefaultDataset( newName );
                }
            }

        }
    }

    // TODO - if rename image, need to update image combobox accordingly
    private void editImageDialog() {
        ImagePropertiesEditor editor = new ImagePropertiesEditor( (String) datasetComboBox.getSelectedItem(),
                (String) imagesComboBox.getSelectedItem(), projectsCreator);
    }

    // private void addImageDialog () {
    //
    //     final JFileChooser jFileChooser = new JFileChooser();
    //     if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
    //         String imagePath = jFileChooser.getSelectedFile().getAbsolutePath();
    //
    //         final GenericDialog gd = new GenericDialog("Add a new image");
    //         gd.addMessage("Adding image to dataset: " + datasetComboBox.getSelectedItem());
    //         gd.addStringField("Name of image", "");
    //         gd.addChoice("Bdv format", new String[]{"n5", "h5"}, "n5");
    //         gd.addStringField("Pixel size unit", "micrometer");
    //         gd.addNumericField("x pixel size", 1);
    //         gd.addNumericField("y pixel size", 1);
    //         gd.addNumericField("z pixel size", 1);
    //         gd.showDialog();
    //
    //         if (!gd.wasCanceled()) {
    //             String imageName = gd.getNextString();
    //             String datasetName = (String) datasetComboBox.getSelectedItem();
    //             String bdvFormat = gd.getNextChoice();
    //             String pixelSizeUnit = gd.getNextString();
    //             double xPixelSize = gd.getNextNumber();
    //             double yPixelSize = gd.getNextNumber();
    //             double zPixelSize = gd.getNextNumber();
    //             projectsCreator.addImage( imagePath, imageName, datasetName, bdvFormat, pixelSizeUnit,
    //                     xPixelSize, yPixelSize, zPixelSize);
    //
    //         }
    //     }
    // }

    private void updateDatasetsComboBox( String selection ) {
        updateDatasetsComboBox();
        datasetComboBox.setSelectedItem( selection );
    }

    private void updateDatasetsComboBox () {
        if ( datasetComboBox != null ) {
            datasetComboBox.removeAllItems();
            for (String datasetName : projectsCreator.getCurrentDatasets()) {
                datasetComboBox.addItem(datasetName);
            }

            if ( imagesComboBox != null ) {
                updateImagesComboBox();
            }
        }
    }

    private void updateImagesComboBox( String selection ) {
        updateImagesComboBox();
        imagesComboBox.setSelectedItem( selection );
    }

    private void updateImagesComboBox () {
        if ( datasetComboBox != null && imagesComboBox != null ) {
            imagesComboBox.removeAllItems();
            String currentDataset = (String) datasetComboBox.getSelectedItem();
            for (String imageName : projectsCreator.getCurrentImages( currentDataset )) {
                imagesComboBox.addItem(imageName);
            }
        }
    }


}
