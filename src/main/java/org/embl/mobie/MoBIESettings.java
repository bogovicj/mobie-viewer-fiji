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
package org.embl.mobie;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.table.TableDataFormat;

import java.util.HashSet;
import java.util.Set;

public class MoBIESettings
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public final Values values = new Values();

	public static MoBIESettings settings()
	{
		return new MoBIESettings();
	}

	public MoBIESettings dataset( String dataset )
	{
		this.values.dataset = dataset;
		return this;
	}

	public MoBIESettings gitProjectBranch( String gitBranch )
	{
		this.values.projectBranch = gitBranch;
		return this;
	}

	public MoBIESettings addImageDataFormat( ImageDataFormat imageDataFormat )
	{
		this.values.imageDataFormats.add( imageDataFormat );
		return this;
	}

	public MoBIESettings addTableDataFormat( TableDataFormat tableDataFormat )
	{
		this.values.tableDataFormats.add( tableDataFormat );
		return this;
	}

	public MoBIESettings imageDataLocation( String imageDataLocation )
	{
		this.values.imageDataLocation = imageDataLocation;
		return this;
	}

	public MoBIESettings tableDataLocation( String tableDataLocation )
	{
		this.values.tableDataLocation = tableDataLocation;
		return this;
	}

	public MoBIESettings gitTablesBranch( String tableDataBranch )
	{
		this.values.tableDataBranch = tableDataBranch;
		return this;
	}

	public MoBIESettings view( String view )
	{
		this.values.view = view;
		return this;
	}

	public MoBIESettings s3AccessAndSecretKey( String[] s3AccessAndSecretKey )
	{
		this.values.s3AccessAndSecretKey = s3AccessAndSecretKey;
		return this;
	}

	public MoBIESettings removeSpatialCalibration( Boolean removeSpatialCalibration )
	{
		this.values.removeSpatialCalibration = removeSpatialCalibration;
		return this;
	}

	public MoBIESettings cli( Boolean cli )
	{
		this.values.cli = cli;
		return this;
	}

	public static class Values
	{
		private String[] s3AccessAndSecretKey;
		private String dataset;
		private String projectBranch = "main";
		private String tableDataBranch;
		private Set< ImageDataFormat > imageDataFormats = new HashSet<>();
		private String imageDataLocation;
		private Set< TableDataFormat > tableDataFormats = new HashSet<>();
		private String tableDataLocation;
		private String view = View.DEFAULT;
		private Boolean removeSpatialCalibration = false;
		private Boolean cli = false; // started from CLI

		public Boolean getRemoveSpatialCalibration()
		{
			return removeSpatialCalibration;
		}

		public String getDataset()
		{
			return dataset;
		}

		public String getProjectBranch()
		{
			return projectBranch;
		}

		public Set< ImageDataFormat > getImageDataFormats() { return imageDataFormats; }

		public Set< TableDataFormat > getTableDataFormats()
		{
			return tableDataFormats;
		}

		public String getImageDataLocation()
		{
			return imageDataLocation;
		}

		public String getTableDataLocation()
		{
			return tableDataLocation;
		}

		public String getTableDataBranch()
		{
			return tableDataBranch != null ? tableDataBranch : projectBranch;
		}

		public String getImageDataBranch()
		{
			return projectBranch;
		}

		public String getView()
		{
			return view;
		}

		public String[] getS3AccessAndSecretKey()
		{
			return s3AccessAndSecretKey;
		}

		public Boolean getCli()
		{
			return cli;
		}
	}
}
