package legacy;

/**
 *  This kind of event is fired
 *  from a <code>NumberField</code> gadget when
 *  the user modified its contents.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.25, 05-May-06
 *
 *  @see	NumberField#addListener( NumberListener )
 *  @see	NumberListener
 *  @see	java.lang.Number
 */
public class NumberEvent
extends BasicEvent
{
// --- ID values ---
	/**
	 *  returned by getID() : the number changed
	 */
	public static final int CHANGED		= 0;

	private final Number	number;
	private final boolean	adjusting;

	/**
	 *  Constructs a new <code>NumberEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>CHANGED</code>
	 *  @param  when	system time when the event occured
	 *  @param  number  the new number
	 */
	public NumberEvent( Object source, int ID, long when, Number number, boolean adjusting )
	{
		super( source, ID, when );

		this.number		= number;
		this.adjusting	= adjusting;
	}

	public boolean isAdjusting()
	{
		return adjusting;
	}

	/**
	 *  Queries the new number
	 *
	 *  @return the new <code>Number</code> of the
	 *			<code>NumberField</code>. This is either
	 *			an <code>Long</code> or a <code>Double</code>
	 *			depening of the <code>NumberField</code>'s
	 *			<code>NumberSpace</code>.
	 *
	 *  @see	de.sciss.util.NumberSpace#isInteger()
	 */
	public Number getNumber()
	{
		return number;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof NumberEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {

			return true;

		} else return false;
	}
}
