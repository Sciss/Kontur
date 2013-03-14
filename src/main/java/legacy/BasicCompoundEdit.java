package legacy;

/**
 *  This subclass of <code>SyncCompoundEdit</code> is
 *  the most basic extension of the abstract class
 *  which simply puts empty bodies for the abstract methods.
 *
 *  @author			Hanns Holger Rutz
 *  @version		0.70, 01-May-06
 */
public class BasicCompoundEdit
extends AbstractCompoundEdit
{
	private boolean	significant	= true;

	/**
	 *  Creates a <code>CompountEdit</code> object
	 */
	public BasicCompoundEdit()
	{
		super();
	}

	/**
	 *  Creates a <code>CompountEdit</code> object with a given name
	 *
	 *	@param	presentationName	text describing the compound edit
	 */
	public BasicCompoundEdit( String presentationName )
	{
		super( presentationName );
	}

	public boolean isSignificant()
	{
		if( significant ) return super.isSignificant();
		else return false;
	}

	public void setSignificant( boolean b )
	{
		significant = b;
	}

	/**
	 *  Does nothing
	 */
	protected void undoDone() { /* empty */ }
	/**
	 *  Does nothing
	 */
	protected void redoDone() { /* empty */ }
	/**
	 *  Does nothing
	 */
	protected void cancelDone() { /* empty */ }
}