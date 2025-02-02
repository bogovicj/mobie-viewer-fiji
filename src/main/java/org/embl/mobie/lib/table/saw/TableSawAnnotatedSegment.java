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
package org.embl.mobie.lib.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.volume.MeshTransformer;

public class TableSawAnnotatedSegment extends AbstractTableSawAnnotation implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };

	private final int timePoint;
	private final int labelId;
	private final double[] position;
	private RealInterval boundingBox;
	private float[] mesh;
	private String source;
	private String uuid;

	// TODO only the model is TableSaw specific
	//   all the other code is generic and could be reused to open other tables
	public TableSawAnnotatedSegment(
			TableSawAnnotationTableModel< TableSawAnnotatedSegment > model,
			int rowIndex,
			String source,
			String uuid,
			Integer labelId,
			int timePoint,
			double[] position,
			FinalRealInterval boundingBox )
	{
		super( model, rowIndex );
		this.source = source;
		this.uuid = uuid;
		this.labelId = labelId;
		this.timePoint = timePoint;
		this.position = position;
		this.boundingBox = boundingBox;
	}

	@Override
	public String imageId()
	{
		return source();
	}

	@Override
	public int label()
	{
		return labelId;
	}

	@Override
	public Integer timePoint()
	{
		return timePoint;
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return position[ d ];
	}

	@Override
	public RealInterval boundingBox()
	{
		return boundingBox;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		this.boundingBox = boundingBox;
	}

	@Override
	public float[] mesh()
	{
		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
	}

	@Override
	public String uuid()
	{
		return uuid;
	}

	@Override
	public String source()
	{
		return source;
	}

	@Override
	public String[] idColumns()
	{
		return idColumns;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		if ( position != null )
			affineTransform3D.apply( position, position );

		if ( boundingBox != null )
			boundingBox = affineTransform3D.estimateBounds( boundingBox );

		if ( mesh != null )
			mesh = MeshTransformer.transform( mesh, affineTransform3D );
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
