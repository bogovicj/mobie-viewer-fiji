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
package org.embl.mobie.lib.transform.image;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.AnnotationImage;
import org.embl.mobie.lib.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.transform.AnnotationAffineTransformer;
import org.embl.mobie.lib.transform.TransformHelper;
import org.embl.mobie.lib.transform.TransformedAnnData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class ImageTransformer
{
	// FIXME
	//  Note that the Image Type may change, e.g. if this is an annotated label image
	//  (maybe this is the only case where the Type changes?
	//  Is there a cleaner solution? For normal images the type would not change.
	//  also for annotated images the type would only change
	//  if transformedImage != image.getName(), because otherwise we would transform in place
	//  Maybe two methods, one for really creating a new image and one for in place transformation?
	public static Image< ? > applyAffineTransform( Image< ? > image, AffineTransform3D affineTransform3D, @Nullable String transformedImageName )
	{
		if ( transformedImageName == null )
		{
			// Perform an in place transformation.
			image.transform( affineTransform3D );
			return image;
		}

		// Create a new transformed image.
		if ( image instanceof AnnotatedLabelImage )
		{
			return createTransformedAnnotatedLabelImage( ( AnnotatedLabelImage ) image, affineTransform3D, transformedImageName );
		}
		else if ( image instanceof AnnotationImage )
		{
			throw new UnsupportedOperationException( "Creating a transformed duplicate of an " + image.getClass() + " is currently not supported." );
		}
		else
		{
			return new AffineTransformedImage<>( image, transformedImageName, affineTransform3D );
		}
	}

	public static Image< ? > applyTimepointsTransform( Image< ? > image, HashMap< Integer, Integer > timepointsMap, boolean keep, @Nullable String transformedImageName )
	{
		String name = transformedImageName == null ? image.getName() : transformedImageName;

		final TimepointsTransformedImage< ? > timepointsTransformedImage = new TimepointsTransformedImage<>( image, name, timepointsMap, keep );

		return timepointsTransformedImage;
	}

	private static < A extends Annotation, TA extends A > DefaultAnnotatedLabelImage< TA > createTransformedAnnotatedLabelImage( AnnotatedLabelImage< A > annotatedLabelImage, AffineTransform3D affineTransform3D, String transformedImageName )
	{
		final Image< ? extends IntegerType< ? > > labelImage = annotatedLabelImage.getLabelImage();

		final AnnData< A > annData = annotatedLabelImage.getAnnData();

		final AnnotationAffineTransformer< A, TA > affineTransformer = new AnnotationAffineTransformer<>( affineTransform3D );

		TransformedAnnData< A, TA > transformedAnnData = new TransformedAnnData<>( annData, affineTransformer );

		final Image< ? extends IntegerType< ? > > transformedLabelImage = ( Image< ? extends IntegerType< ? > > ) applyAffineTransform( labelImage, affineTransform3D, transformedImageName );

		// FIXME I guess I have to get the AnnotationAdapter
		//   from the AnnotatedLabelImage?!
		final DefaultAnnotatedLabelImage< TA > transformedAnnotatedImage = new DefaultAnnotatedLabelImage< TA >( transformedLabelImage, transformedAnnData, new DefaultAnnotationAdapter( annData ) );

		return transformedAnnotatedImage;
	}

	public static List< ? extends Image< ? > > gridTransform( List< List< ? extends Image< ? > > > nestedImages, @Nullable List< List< String > > nestedTransformedNames, List< int[] > positions, double[] tileRealDimensions, boolean centerAtOrigin, double[] withinTileOffset )
	{
		final CopyOnWriteArrayList< ? extends Image< ? > > transformedImages = new CopyOnWriteArrayList<>();

		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		final int numGridPositions = nestedImages.size();
		for ( int gridIndex = 0; gridIndex < numGridPositions; gridIndex++ )
		{
			int finalGridIndex = gridIndex;
			futures.add( ThreadHelper.executorService.submit( () -> {
				try
				{
					final List< ? extends Image< ? > > images = nestedImages.get( finalGridIndex );
					final double[] translation = new double[ 2 ];
					for ( int d = 0; d < 2; d++ )
						translation[ d ] = tileRealDimensions[ d ] * positions.get( finalGridIndex )[ d ] + withinTileOffset[ d ];

					List< String > transformedImageNames = nestedTransformedNames == null ? null : nestedTransformedNames.get( finalGridIndex );

					final List< ? extends Image< ? > > translatedImages = translate( images, transformedImageNames, centerAtOrigin, translation[ 0 ], translation[ 1 ] );

					transformedImages.addAll( ( List ) translatedImages );
				}
				catch ( Exception e )
				{
					throw ( e );
				}
			} ) );
		}
		ThreadHelper.waitUntilFinished( futures );

		return transformedImages;
	}

	public static ArrayList< Image< ? > > translate( List< ? extends Image< ? > > images, @Nullable List< String > transformedNames, boolean centerAtOrigin, double translationX, double translationY )
	{
		final ArrayList< Image< ? > > translatedImages = new ArrayList<>();

		for ( Image< ? > image : images )
		{
			AffineTransform3D translationTransform = TransformHelper.createTranslationTransform( translationX, translationY, image, centerAtOrigin );

			String transformedName = transformedNames == null ? null : transformedNames.get( images.indexOf( image ) );

			final Image< ? > transformedImage = applyAffineTransform( image, translationTransform, transformedName );

			translatedImages.add( transformedImage );
		}

		return translatedImages;
	}
}
