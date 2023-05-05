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
package org.embl.mobie.command.view;

import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.results.ResultsTableFetcher;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_X;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_Y;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_Z;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_X;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_Y;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_Z;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "View>View Image and Segmentation and Table..."  )
public class ViewImageAndSegmentationAndTableCommand extends DynamicCommand implements Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Intensity Image" )
	public ImagePlus image;

	@Parameter ( label = "Label Mask Image" )
	public ImagePlus segmentation;

	@Parameter ( label = "Table" )
	public String tableName;

	// see {@code initialize()}
	private HashMap< String, ResultsTable > titleToTable;

	@Override
	public void run()
	{
		final TableDataFormat tableDataFormat = TableDataFormat.ResultsTable;
		final StorageLocation tableStorageLocation = new StorageLocation();
		tableStorageLocation.data = titleToTable.get( tableName );

		final AbstractSpimData< ? > imageData = new SpimDataOpener().open( image );
		final AbstractSpimData< ? > segmentationData = new SpimDataOpener().open( segmentation );

		new MoBIE( "ImageJ", imageData, segmentationData, tableStorageLocation, tableDataFormat );
	}

	@Override
	public void initialize()
	{
		final ResultsTableFetcher tableFetcher = new ResultsTableFetcher();
		titleToTable = tableFetcher.fetchCurrentlyOpenResultsTables();
		MutableModuleItem< String > input = getInfo().getMutableInput("tableName", String.class );
		input.setChoices( new ArrayList<>( titleToTable.keySet() ));
	}
}
