package de.embl.cba.platynereis.platybrowser;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.platynereis.platysources.PlatyBrowserImageSourcesModel;
import de.embl.cba.platynereis.bookmark.Bookmark;
import de.embl.cba.platynereis.bookmark.BookmarksParser;
import de.embl.cba.platynereis.bookmark.BookmarksManager;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.platynereis.utils.Utils;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

public class MoBIEViewer extends JFrame
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final PlatyBrowserActionPanel actionPanel;
	private final PlatyBrowserImageSourcesModel imageSourcesModel;
	private String imagesLocation;
	private String tablesLocation;

	private int frameWidth;
	private BookmarksManager bookmarksManager;
	private ArrayList< String > datasets;

	public MoBIEViewer(
			String dataset,
			ArrayList< String > datasets,
			String aImageDataLocation,
			String aTableDataLocation ) throws HeadlessException
	{
		configureDataLocations( dataset, aImageDataLocation, aTableDataLocation );

		imageSourcesModel = new PlatyBrowserImageSourcesModel( imagesLocation, tablesLocation );

		sourcesPanel = new PlatyBrowserSourcesPanel( imageSourcesModel );

		// TODO: this should be the image data location, not the tables!
		fetchBookmarks( tablesLocation );

		// TODO: this should be the image data location, not the tables!
		final double[] levelingVector = fetchLeveling( tablesLocation );

		actionPanel = new PlatyBrowserActionPanel( sourcesPanel, bookmarksManager, levelingVector );

		setJMenuBar( createMenuBar() );
		showFrame( dataset );
		adaptLogWindowPositionAndSize();

		sourcesPanel.setParentComponent( this );

		bookmarksManager.setView( "default" );
		// TODO: show something as default
		//sourcesPanel.addSourceToPanelAndViewer( Constants.DEFAULT_EM_RAW_FILE_ID );

		actionPanel.setBdv( sourcesPanel.getBdv() );
	}

	private double[] fetchLeveling( String dataLocation )
	{
		final String levelingFile = FileAndUrlUtils.combinePath( dataLocation, "misc/leveling.json" );
		try
		{
			InputStream is = FileAndUrlUtils.getInputStream( levelingFile );
			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			final GsonBuilder gsonBuilder = new GsonBuilder();
			LinkedTreeMap linkedTreeMap = gsonBuilder.create().fromJson( reader, Object.class );
			ArrayList< Double >  normalVector = ( ArrayList< Double > ) linkedTreeMap.get( "NormalVector" );
			final double[] doubles = normalVector.stream().mapToDouble( Double::doubleValue ).toArray();
			return doubles;
		}
		catch ( Exception e )
		{
			return null; // new double[]{0.70,0.56,0.43};
		}

	}

	public void configureDataLocations( String dataSet, String aImageDataLocation, String aTableDataLocation )
	{
		this.imagesLocation = aImageDataLocation;
		this.tablesLocation = aTableDataLocation;

		imagesLocation = FileAndUrlUtils.removeTrailingSlash( imagesLocation );
		tablesLocation = FileAndUrlUtils.removeTrailingSlash( tablesLocation );

		imagesLocation = adaptUrl( imagesLocation );
		tablesLocation = adaptUrl( tablesLocation );

		imagesLocation = FileAndUrlUtils.combinePath( imagesLocation, dataSet );
		tablesLocation = FileAndUrlUtils.combinePath( tablesLocation, dataSet );

		Utils.log( "");
		Utils.log( "# Fetching data");
		Utils.log( "Fetching image data from: " + imagesLocation );
		Utils.log( "Fetching table data from: " + tablesLocation );
	}

	public String adaptUrl( String url )
	{
		if ( url.contains( "github.com" ) )
		{
			url = url.replace( "github.com", "raw.githubusercontent.com" );
			url += "/master/data";
		}
		return url;
	}

	public void fetchBookmarks( String tableDataLocation )
	{
		Map< String, Bookmark > nameToBookmark = new BookmarksParser( tableDataLocation, imageSourcesModel ).call();

		bookmarksManager = new BookmarksManager( sourcesPanel, nameToBookmark );
	}

	public void adaptLogWindowPositionAndSize()
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( this.getLocationOnScreen().y + this.getHeight() + 20 );
			log.setSize( this.getWidth(), logWindowHeight  );
			log.setLocation( this.getLocationOnScreen().x, this.getLocationOnScreen().y + this.getHeight() );
		}
	}

	private JMenuBar createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		menuBar.add( createMainMenu() );
		return menuBar;
	}

	private JMenu createMainMenu()
	{
		final JMenu main = new JMenu( "Main" );
		main.add( createPreferencesMenuItem() );
		return main;
	}

	private JMenuItem createPreferencesMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Preferences..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( ()
						-> showPreferencesDialog() ) );
		return menuItem;
	}

	private void showPreferencesDialog()
	{
		new Thread( () -> {
			final NonBlockingGenericDialog gd
					= new NonBlockingGenericDialog( "Preferences" );
			gd.addNumericField( "3D View Voxel Size [micrometer]",
					sourcesPanel.getVoxelSpacing3DView(), 2 );
			gd.addNumericField( "3D View Mesh Smoothing Iterations [#]",
					sourcesPanel.getMeshSmoothingIterations(), 0 );
			gd.addNumericField( "Gene Search Radius [micrometer]",
					actionPanel.getGeneSearchRadiusInMicrometer(), 1 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			sourcesPanel.setVoxelSpacing3DView( gd.getNextNumber() );
			sourcesPanel.setMeshSmoothingIterations( ( int ) gd.getNextNumber() );
			actionPanel.setGeneSearchRadiusInMicrometer( gd.getNextNumber() );

		} ).start();
	}

	public void showFrame( String version )
	{
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int numModalities = actionPanel.getSortedModalities().size();
		final int actionPanelHeight = ( numModalities + 7 ) * 40;
		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesPanel );
		splitPane.setAutoscrolls( true );
		frameWidth = 600;
		setPreferredSize( new Dimension( frameWidth, actionPanelHeight + 200 ) );
		getContentPane().setLayout( new GridLayout() );
		getContentPane().add( splitPane );

		this.setTitle( "PlatyBrowser - Data Version " + version );

		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.pack();
		this.setVisible( true );
	}

	public PlatyBrowserSourcesPanel getSourcesPanel()
	{
		return sourcesPanel;
	}

	public PlatyBrowserActionPanel getActionPanel()
	{
		return actionPanel;
	}
}