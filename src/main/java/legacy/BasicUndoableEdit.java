package legacy;

import javax.swing.undo.AbstractUndoableEdit;

/**
 *	@version	0.70, 01-May-06
 *	@author		Hanns Holger Rutz
 */
public abstract class BasicUndoableEdit
extends AbstractUndoableEdit
implements PerformableEdit
{
//	protected String getResourceString( String key )
//	{
//		final Application app = AbstractApplication.getApplication();
//		return app != null ? app.getResourceString( key ) : key;
//	}

// UUU
//	public void cancel();

	public void debugDump( int nest )
	{
//		final StringBuffer strBuf = new StringBuffer( nest << 1 );
//		for( int i = 0; i < nest; i++ ) strBuf.append( "  " );
//		System.err.print( strBuf.toString() );
		System.err.println( toString() );
	}

	public String toString()
	{
		return( getClass().getName().toString() + " (\"" + getPresentationName() + "\") ; canUndo = "+canUndo()+"; canRedo = "+canRedo()+"; isSignificant = "+isSignificant() );
	}
}
