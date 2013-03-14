package legacy;

import java.util.EventListener;

/**
 *  Interface for listening
 *  the changes of the contents
 *  of a <code>PathField</code> or
 *  <code>PathButton</code> gadget
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 20-May-05
 *
 *  @see	PathField#addPathListener( PathListener )
 */
public interface PathListener
extends EventListener
{
	/**
	 *  Notifies the listener that
	 *  a path changed occured.
	 *
	 *  @param  e   the event describing
	 *				the path change
	 */
	public void pathChanged( PathEvent e );
}