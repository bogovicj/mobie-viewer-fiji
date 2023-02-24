package org.embl.mobie.lib.hcs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum HCSPattern
{
	MolDevSitesChannels,
	MolDevSites,
	Operetta;

	public static final String WELL = "W";
	public static final String SITE = "S";
	public static final String CHANNEL = "C";

	private final String MD_SITES_CHANNELS = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">.*)_w(?<"+CHANNEL+">[0-9]{1}).*";
	private final String MD_SITES = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">[0-9]{1}).*";

	/*
	Operetta examples:
	r01c01f04p01-ch1sk1fk1fl1.tiff : well = r01c01, site = 04, channel = 1
	 */
	private final String OPERETTA = ".*(?<"+WELL+">r[0-9]{2}c[0-9]{2})f(?<"+SITE+">[0-9]{2})p[0-9]{2}.*-ch(?<"+CHANNEL+">[0-9])sk.*.tiff$";


	public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	// TODO: add the ones below
	private final String PATTERN_MD_A01_WAVELENGTH = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_(?<"+CHANNEL+">.*).tif";
	private final String PATTERN_ALMF_TREAT1_TREAT2_WELLNUM_POSNUM_CHANNEL = ".*--(.*)--(.*)--W(?<"+WELL+">[0-9]{4})--P(?<"+SITE+">[0-9]{3})--T[0-9]{4,5}--Z[0-9]{3}--(?<"+CHANNEL+">.*)";
	private final String PATTERN_SCANR_WELLNUM_SITENUM_CHANNEL = ".*--W(?<"+WELL+">[0-9]{5})--P(?<"+SITE+">[0-9]{5}).*--.*--(?<"+CHANNEL+">.*)\\..*";
	private final String PATTERN_NIKON_TI2_HDF5 = ".*Well([A-Z]{1}[0-9]{2})_Point[A-Z]{1}[0-9]{2}_([0-9]{4})_.*h5$";


	public static HCSPattern fromPath( String fileName )
	{
		for ( HCSPattern hcsPattern : HCSPattern.values() )
		{
			final Matcher matcher = hcsPattern.getMatcher( fileName );
			if ( matcher.matches() )
				return hcsPattern;
		}

		return null;
	}

	public Matcher getMatcher( String path )
	{
		switch( this )
		{
			case Operetta:
				return Pattern.compile( OPERETTA ).matcher( path );
			case MolDevSites:
				return Pattern.compile( MD_SITES ).matcher( path );
			case MolDevSitesChannels:
			default:
				return Pattern.compile( MD_SITES_CHANNELS ).matcher( path );
		}
	}

	public int[] getWellGridPosition( String well )
	{
		switch ( this )
		{
			case Operetta:
				return decodeOperettaWellPosition( well );
			default:
				return decodeA01WellPosition( well );
		}
	}

	private int[] decodeOperettaWellPosition( String well )
	{
		final Matcher matcher = Pattern.compile( "r(?<row>[0-9]{2})c(?<col>[0-9]{2})" ).matcher( well );
		matcher.matches();
		final int row = Integer.parseInt( matcher.group( "row" ) ) - 1;
		final int col = Integer.parseInt( matcher.group( "col" ) ) - 1;
		return new int[]{ row, col };
	}

	public static int[] decodeA01WellPosition( String well )
	{
		int[] wellPosition = new int[ 2 ];
		wellPosition[ 0 ] = Integer.parseInt( well.substring( 1, 3 ) ) - 1;
		wellPosition[ 1 ] = ALPHABET.indexOf( well.substring( 0, 1 ).toUpperCase() );
		return wellPosition;
	}

}