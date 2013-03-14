package legacy;

import java.io.File;

/**
 *  This kind of event is fired
 *  from a <code>PathField</code> or
 *  <code>PathButton</code> gadget when
 *  the user modified the path.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-06
 *
 *  @see		PathField#addPathListener( PathListener )
 *  @see		PathListener
 *  @see		java.io.File
 */
public class PathEvent
extends BasicEvent
{
	// --- ID values ---
	/**
	 *  returned by getID() : the path changed
	 */
	public static final int CHANGED		= 0;

	private final File	path;

	/**
	 *  Constructs a new <code>PathEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>CHANGED</code>
	 *  @param  when	system time when the event occured
	 *  @param  path	the new path
	 */
	public PathEvent( Object source, int ID, long when, File path )
	{
		super( source, ID, when );

		this.path		= path;
	}

	/**
	 *  Queries the new path
	 *
	 *  @return the new path of the <code>PathField</code>
	 *			or <code>PathButton</code>.
	 */
	public File getPath()
	{
		return path;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof PathEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {

			// XXX beware, when the actionID and actionObj
			// are used, we have to deal with them here

			return true;

		} else return false;
	}
}
