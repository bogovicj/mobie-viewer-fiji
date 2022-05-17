package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.MobieBdvSupplier;
import org.embl.mobie.viewer.bdv.MobieSerializableBdvOptions;
import org.embl.mobie.viewer.bdv.SourcesAtMousePositionSupplier;
import org.embl.mobie.viewer.bdv.ViewerTransformLogger;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.command.BigWarpRegistrationCommand;
import org.embl.mobie.viewer.command.ConfigureLabelRenderingCommand;
import org.embl.mobie.viewer.command.ImageVolumeRenderingConfiguratorCommand;
import org.embl.mobie.viewer.command.LabelVolumeRenderingConfiguratorCommand;
import org.embl.mobie.viewer.command.ManualRegistrationCommand;
import org.embl.mobie.viewer.command.RandomColorSeedChangerCommand;
import org.embl.mobie.viewer.command.ScreenShotMakerCommand;
import org.embl.mobie.viewer.command.ShowRasterImagesCommand;
import org.embl.mobie.viewer.command.SourceAndConverterBlendingModeChangerCommand;
import org.embl.mobie.viewer.display.AbstractSourceDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import org.embl.mobie.viewer.view.ViewManager;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

import static org.embl.mobie.viewer.ui.WindowArrangementHelper.setBdvWindowPositionAndSize;

public class SliceViewer
{
	public static final String UNDO_SEGMENT_SELECTIONS = "Undo Segment Selections [ Ctrl Shift N ]";
	public static final String CHANGE_RANDOM_COLOR_SEED = "Change Random Color Seed";
	public static final String LOAD_ADDITIONAL_VIEWS = "Load Additional Views";
	public static final String SAVE_CURRENT_SETTINGS_AS_VIEW = "Save Current View";
	protected static final String FRAME_TITLE = "MoBIE - BigDataViewer";
	private final SourceAndConverterBdvDisplayService sacDisplayService;
	private BdvHandle bdvHandle;
	private final boolean is2D;
	private final ViewManager viewManager;
	private final int timepoints;
	private final ArrayList< String > projectCommands;

	private SourceAndConverterContextMenuClickBehaviour contextMenu;
	private final SourceAndConverterService sacService;

	public SliceViewer( boolean is2D, ViewManager viewManager, int timepoints, ArrayList< String > projectCommands )
	{
		this.is2D = is2D;
		this.viewManager = viewManager;
		this.timepoints = timepoints;
		this.projectCommands = projectCommands;

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		sacDisplayService = SourceAndConverterServices.getBdvDisplayService();

		bdvHandle = createBdv( timepoints, is2D, FRAME_TITLE );
		setBdvWindowPositionAndSize( bdvHandle );
		sacDisplayService.registerBdvHandle( bdvHandle );

		installContextMenuAndKeyboardShortCuts();
	}

	public BdvHandle getBdvHandle()
	{
		if ( bdvHandle == null )
		{
			bdvHandle = createBdv( timepoints, is2D, FRAME_TITLE );
			sacDisplayService.registerBdvHandle( bdvHandle );
		}
		return bdvHandle;
	}

	private void installContextMenuAndKeyboardShortCuts( )
	{
		final SliceViewRegionSelector sliceViewRegionSelector = new SliceViewRegionSelector( bdvHandle, is2D, () -> viewManager.getAnnotatedRegionDisplays() );

		sacService.registerAction( UNDO_SEGMENT_SELECTIONS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			sliceViewRegionSelector.clearSelection();
		} );

		sacService.registerAction( LOAD_ADDITIONAL_VIEWS, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewManager.getAdditionalViewsLoader().loadAdditionalViewsDialog();
		} );

		sacService.registerAction( SAVE_CURRENT_SETTINGS_AS_VIEW, sourceAndConverters -> {
			// TODO: Maybe only do this for the sacs at the mouse position
			viewManager.getViewsSaver().saveCurrentSettingsAsViewDialog();
		} );

		final Set< String > actionsKeys = sacService.getActionsKeys();

		final ArrayList< String > actions = new ArrayList< String >();
		actions.add( sacService.getCommandName( ScreenShotMakerCommand.class ) );
		actions.add( sacService.getCommandName( ShowRasterImagesCommand.class ) );
		actions.add( sacService.getCommandName( ViewerTransformLogger.class ) );
		actions.add( sacService.getCommandName( BigWarpRegistrationCommand.class ) );
		actions.add( sacService.getCommandName( ManualRegistrationCommand.class ) );
		actions.add( sacService.getCommandName( SourceAndConverterBlendingModeChangerCommand.class ) );
		actions.add( sacService.getCommandName( RandomColorSeedChangerCommand.class ) );
		actions.add( sacService.getCommandName( ConfigureLabelRenderingCommand.class ) );
		actions.add( sacService.getCommandName( LabelVolumeRenderingConfiguratorCommand.class ) );
		actions.add( sacService.getCommandName( ImageVolumeRenderingConfiguratorCommand.class ) );
		actions.add( UNDO_SEGMENT_SELECTIONS );
		actions.add( LOAD_ADDITIONAL_VIEWS );
		actions.add( SAVE_CURRENT_SETTINGS_AS_VIEW );

		if ( projectCommands != null )
		{
			for ( String commandName : projectCommands )
			{
				actions.add( commandName );
			}
		}

		contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions.toArray( new String[0] ) );

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "MoBIE" );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewRegionSelector.run() ).start(),
				"Toggle selection", "ctrl button1" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> sliceViewRegionSelector.clearSelection() ).start(),
				"Clear selection", "ctrl shift N" ) ;

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () ->
						{
							final SourceAndConverter[] sourceAndConverters = sacService.getSourceAndConverters().toArray( new SourceAndConverter[ 0 ] );
							RandomColorSeedChangerCommand.incrementRandomColorSeed( sourceAndConverters );
						}).start(),
				"Change random color seed", "ctrl L" ) ;
	}

	public static BdvHandle createBdv( int numTimepoints, boolean is2D, String frameTitle )
	{
		final MobieSerializableBdvOptions sOptions = new MobieSerializableBdvOptions();
		sOptions.is2D = is2D;
		sOptions.numTimePoints = numTimepoints;
		sOptions.frameTitle = frameTitle;

		IBdvSupplier bdvSupplier = new MobieBdvSupplier( sOptions );
		SourceAndConverterServices.getBdvDisplayService().setDefaultBdvSupplier(bdvSupplier);
		BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}

	public void show( SourceAndConverter< ? > sourceAndConverter, AbstractSourceDisplay display )
	{
		// register
		SourceAndConverterServices.getSourceAndConverterService().register( sourceAndConverter );

		// blending mode
		SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );

		// opacity
		OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

		SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, display.isVisible(), sourceAndConverter );

		display.sourceNameToSourceAndConverter.put( sourceAndConverter.getSpimSource().getName(), sourceAndConverter );
	}
}
