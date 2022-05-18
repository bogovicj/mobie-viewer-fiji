package org.embl.mobie.viewer.transform;

import bdv.util.*;
import bdv.viewer.animate.SimilarityTransformAnimator;
import org.embl.mobie.viewer.playground.BdvPlaygroundHelper;
import org.embl.mobie.viewer.bdv.CircleOverlay;
import org.embl.mobie.viewer.MoBIEHelper;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Arrays;

public abstract class SliceViewLocationChanger
{
	public static int animationDurationMillis = 3000;

	private static BdvOverlaySource< BdvOverlay > pointOverlaySource;
	private static CircleOverlay circleOverlay;
	private static boolean pointOverlaySourceIsActive;
	private static boolean isPointOverlayEnabled;

	public static void changeLocation( BdvHandle bdv, ViewerTransform viewerTransform )
	{
		if ( viewerTransform instanceof PositionViewerTransform )
		{
			moveToPosition( bdv, viewerTransform.getParameters(), animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
			if ( isPointOverlayEnabled )
				addPointOverlay( bdv, viewerTransform.getParameters() );
		}
		else if ( viewerTransform instanceof TimepointViewerTransform )
		{
			adaptTimepoint( bdv, viewerTransform );
		}
		else if ( viewerTransform instanceof NormalVectorViewerTransform )
		{
			final AffineTransform3D transform = NormalVectorViewerTransform.createTransform( bdv, viewerTransform.getParameters() );
			changeLocation( bdv, transform, animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
		}
		else if ( viewerTransform instanceof AffineViewerTransform )
		{
			changeLocation( bdv, MoBIEHelper.asAffineTransform3D( viewerTransform.getParameters() ), animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
		}
		else if ( viewerTransform instanceof NormalizedAffineViewerTransform )
		{
			final AffineTransform3D transform = TransformHelper.createUnnormalizedViewerTransform( MoBIEHelper.asAffineTransform3D( viewerTransform.getParameters() ), bdv.getBdvHandle().getViewerPanel() );
			changeLocation( bdv, transform, animationDurationMillis );
			adaptTimepoint( bdv, viewerTransform );
		}
	}

	private static void adaptTimepoint( BdvHandle bdv, ViewerTransform viewerTransform )
	{
		if ( viewerTransform.getTimepoint() != null )
			bdv.getViewerPanel().setTimepoint( viewerTransform.getTimepoint() );
	}

	public static void togglePointOverlay()
	{
		if ( pointOverlaySource == null ) return;

		pointOverlaySourceIsActive = ! pointOverlaySourceIsActive;
		pointOverlaySource.setActive( pointOverlaySourceIsActive );
	}

	private static void addPointOverlay( Bdv bdv, double[] doubles )
	{
		if ( circleOverlay == null )
		{
			circleOverlay = new CircleOverlay( doubles, 5.0 );
			pointOverlaySource = BdvFunctions.showOverlay(
					circleOverlay,
					"point-overlay-" + Arrays.toString( doubles ),
					BdvOptions.options().addTo( bdv ) );
			pointOverlaySourceIsActive = true;
		}
		else
		{
			circleOverlay.addCircle( doubles );
		}
	}

	public static void enablePointOverlay( boolean isPointOverlayEnabled )
	{
		SliceViewLocationChanger.isPointOverlayEnabled = isPointOverlayEnabled;
	}

	public static void moveToPosition( BdvHandle bdv, double[] xyz, long durationMillis )
	{
		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D newViewerTransform = currentViewerTransform.copy();

		// ViewerTransform
		// applyInverse: coordinates in viewer => coordinates in image
		// apply: coordinates in image => coordinates in viewer

		final double[] locationOfTargetCoordinatesInCurrentViewer = new double[ 3 ];
		currentViewerTransform.apply( xyz, locationOfTargetCoordinatesInCurrentViewer );

		for ( int d = 0; d < 3; d++ )
		{
			locationOfTargetCoordinatesInCurrentViewer[ d ] *= -1;
		}

		newViewerTransform.translate( locationOfTargetCoordinatesInCurrentViewer );
		final double[] bdvWindowCenter = BdvPlaygroundHelper.getWindowCentreInPixelUnits( bdv.getViewerPanel() );
		newViewerTransform.translate( bdvWindowCenter );

		if ( durationMillis <= 0 )
		{
			bdv.getBdvHandle().getViewerPanel().state().setViewerTransform(  newViewerTransform );
		}
		else
		{
			final SimilarityTransformAnimator similarityTransformAnimator =
					new SimilarityTransformAnimator(
							currentViewerTransform,
							newViewerTransform,
							0,
							0,
							durationMillis );

			bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		}
	}

	public static void changeLocation( Bdv bdv, AffineTransform3D newViewerTransform, long duration)
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0 ,
						0,
						duration );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
	}

}