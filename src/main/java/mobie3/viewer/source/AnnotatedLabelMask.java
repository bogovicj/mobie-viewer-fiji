package mobie3.viewer.source;

import mobie3.viewer.segment.LabelToSegmentMapper;
import mobie3.viewer.segment.SegmentProvider;
import mobie3.viewer.table.SegmentAnnotation;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.IntegerType;

public class AnnotatedLabelMask< T extends IntegerType< T >, SR extends SegmentAnnotation > implements AnnotatedImage< T, SR >
{
	protected Image< T > labelMask;
	protected SegmentsAnnData< SR > annData;
	protected SourcePair< AnnotationType< SR > > sourcePair;

	public AnnotatedLabelMask()
	{
	}

	public AnnotatedLabelMask( Image< T > labelMask, SegmentsAnnData< SR > annData )
	{
		this.labelMask = labelMask;
		this.annData = annData;
	}

	@Override
	public SourcePair< AnnotationType< SR > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			SegmentProvider< SR > segmentProvider = new LabelToSegmentMapper( annData );
			final AnnotatedLabelMaskSource< T, SR > source = new AnnotatedLabelMaskSource<>( getLabelMask().getSourcePair().getSource(), segmentProvider );
			final VolatileAnnotatedLabelMaskSource< T, ? extends Volatile< T >, SR > volatileSource = new VolatileAnnotatedLabelMaskSource<>( getLabelMask().getSourcePair().getVolatileSource(), segmentProvider );
			sourcePair = new DefaultSourcePair<>( source, volatileSource );
		}

		return sourcePair;
	}

	@Override
	public String getName()
	{
		return labelMask.getName();
	}

	@Override
	public Image< T > getLabelMask()
	{
		return labelMask;
	}

	@Override
	public SegmentsAnnData< SR > getAnnData()
	{
		return null;
	}
}
