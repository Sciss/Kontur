package legacy;

import java.util.EventListener;

/**
 *  Interface for listening
 *  the changes of the contents
 *  of a <code>NumberField</code> gadget
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.25, 17-Sep-05
 *
 *  @see	NumberField#addListener( NumberListener )
 */
public interface NumberListener
extends EventListener
{
	/**
	 *  Notifies the listener that
	 *  a number changed occured.
	 *
	 *  @param  e   the event describing
	 *				the number change
	 */
	public void numberChanged( NumberEvent e );
}