package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedSegmentAnnotation;
import mobie3.viewer.transform.Transformation;

public class DefaultSegmentsAnnData< SR extends SegmentAnnotation > implements SegmentsAnnData< SR >
{
	private SegmentsTableModel< SR > tableModel;

	public DefaultSegmentsAnnData( SegmentsTableModel< SR > tableModel )
	{
		this.tableModel = tableModel;
	}

	@Override
	public AnnotationTableModel< SR > getTable()
	{
		return tableModel;
	}

	@Override
	public SegmentsAnnData< TransformedSegmentAnnotation > transform( Transformation transformation )
	{
		final TransformedSegmentsTableModel transformedModel = new TransformedSegmentsTableModel( tableModel, transformation );
		final DefaultSegmentsAnnData< TransformedSegmentAnnotation > segmentsAnnData = new DefaultSegmentsAnnData<>( transformedModel );
		return segmentsAnnData;
	}
}
