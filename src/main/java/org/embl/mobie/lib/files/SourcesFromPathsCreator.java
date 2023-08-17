package org.embl.mobie.lib.files;

import org.embl.mobie.lib.io.FileImageSource;
import org.embl.mobie.lib.transform.GridType;

import java.util.ArrayList;
import java.util.List;

public class SourcesFromPathsCreator
{
	private List< ImageFileSources > imageFileSources;
	private List< LabelFileSources > labelSources;

	public SourcesFromPathsCreator( List < String > imagePaths, List < String > labelPaths, List < String > labelTablePaths, String root, GridType grid )
	{
		// images
		//
		imageFileSources = new ArrayList<>();
		for ( String imagePath : imagePaths )
		{
			final FileImageSource fileImageSource = new FileImageSource( imagePath );
			imageFileSources.add( new ImageFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
		}

		// labels & tables
		//
		labelSources = new ArrayList<>();
		for ( int labelSourceIndex = 0; labelSourceIndex < labelPaths.size(); labelSourceIndex++ )
		{
			final FileImageSource fileImageSource = new FileImageSource( labelPaths.get( labelSourceIndex ) );

			if ( labelTablePaths.size() > labelSourceIndex )
			{
				final String labelTable = labelTablePaths.get( labelSourceIndex );
				labelSources.add( new LabelFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, labelTable, root, grid ) );
			}
			else
			{
				labelSources.add( new LabelFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
			}
		}
	}

	// TODO consider adding back the functionality of groups for sorting the grid
	//			final List< String > groups = MoBIEHelper.getGroupNames( regex );
	//			if ( groups.size() > 0 )
	//			{
	//				final Pattern pattern = Pattern.compile( regex );
	//				final Set< String > set = new LinkedHashSet<>();
	//				for ( String path : paths )
	//				{
	//					final Matcher matcher = pattern.matcher( path );
	//					matcher.matches();
	//					set.add( matcher.group( 1 ) );
	//				}
	//
	//				final ArrayList< String > categories = new ArrayList<>( set );
	//				final int[] numSources = new int[ categories.size() ];
	//				grid.positions = new ArrayList<>();
	//				for ( String source : sources )
	//				{
	//					final Matcher matcher = pattern.matcher( source );
	//					matcher.matches();
	//					final int row = categories.indexOf( matcher.group( rowGroup ) );
	//					final int column = numSources[ row ];
	//					numSources[ row ]++;
	//					grid.positions.add( new int[]{ column, row } );
	//				}
	//			}
	//			}
	//			else
	//			{

	public List< ImageFileSources > getImageSources()
	{
		return imageFileSources;
	}

	public List< LabelFileSources > getLabelSources()
	{
		return labelSources;
	}
}