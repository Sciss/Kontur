package legacy;

import javax.swing.undo.UndoableEdit;

public interface PerformableEdit
extends UndoableEdit
{
	public PerformableEdit perform();

	public void debugDump( int nest );
}