package projects.kristina.cmd;

import org.embl.mobie.cmd.FilesCmd;

class KristinaCheckCellposeSegmentationCmd
{
	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = null; // "/Volumes/cba/exchange/kristina-mirkes/data/processed";
		cmd.images = new String[]{ "/g/cba/exchange/kristina-mirkes/data/segmentation-training/.*.tif=raw" } ;
		cmd.labels = new String[]{ "/g/cba/exchange/kristina-mirkes/develop/cellpose-training/output/.*.tif=labels" } ;
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}