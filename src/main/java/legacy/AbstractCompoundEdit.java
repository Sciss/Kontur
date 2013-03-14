package legacy;

import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.CompoundEdit;

/**
 *  This subclass of <code>CompoundEdit</code> is used
 *  to synchronize an Undo or Redo operation.
 *  If for example, several edits of writing to a
 *  MultirateTrackEditor are recorded, this happens
 *  in a synchronized block. This class guarantees
 *  that an appropriate synchronization is maintained
 *  in an undo / redo operation.
 *
 *  @author			Hanns Holger Rutz
 *  @version		0.58, 28-Sep-07
 *
 *	@todo			perform() should optimize by checking addEdit and replaceEdit calls
 */
public abstract class AbstractCompoundEdit
extends CompoundEdit
implements PerformableEdit
{
	private final String presentationName;
	private List collToPerform = null;

	/**
	 *  Creates a <code>CompountEdit</code> object
	 */
	protected AbstractCompoundEdit()
	{
		this( null );
	}

	/**
	 *  Creates a <code>CompountEdit</code> object with a given name
	 *
	 *	@param	presentationName	text describing the compound edit
	 */
	protected AbstractCompoundEdit( String presentationName )
	{
		super();
		this.presentationName	= presentationName;
	}

	/**
	 *  Performs undo on all compound sub edits
	 */
	public void undo()
	{
		super.undo();
		undoDone();
	}

	/**
	 *  Performs redo on all compound sub edits
	 */
	public void redo()
	{
		super.redo();
		redoDone();
	}

	/**
	 *  Cancels the compound edit and undos all sub edits
	 *  made so far.
	 */
	public void cancel()
	{
		if( collToPerform != null ) {
			for( int i = 0; i < collToPerform.size(); i++ ) {
				((PerformableEdit) collToPerform.get( i )).die();
			}
			collToPerform = null;
		}
		end();
		super.undo();
		super.die();
		cancelDone();
	}

	/**
	 *  This gets called after the undo
	 *  operation but still inside the
	 *  sync block. Subclasses can use this
	 *  to fire any notification events.
	 */
	protected abstract void undoDone();

	/**
	 *  This gets called after the redo
	 *  operation but still inside the
	 *  sync block. Subclasses can use this
	 *  to fire any notification events.
	 */
	protected abstract void redoDone();

	/**
	 *  This gets called after the cancel
	 *  operation but still inside the
	 *  sync block. Subclasses can use this
	 *  to fire any notification events.
	 */
	protected abstract void cancelDone();

	public String getPresentationName()
	{
		if( presentationName != null ) {
			return presentationName;
		} else {
			return super.getPresentationName();
		}
	}

	public String getUndoPresentationName()
	{
		if( presentationName != null ) {
			return( "Undo" /* getResourceString( "menuUndo" ) */ + " " + presentationName );
		} else {
			return super.getUndoPresentationName();
		}
	}

	public String getRedoPresentationName()
	{
		if( presentationName != null ) {
			return( "Redo" /* getResourceString( "menuRedo" ) */ + " " + presentationName );
		} else {
			return super.getRedoPresentationName();
		}
	}

//	protected String getResourceString( String key )
//	{
//		final Application app = AbstractApplication.getApplication();
//		return app != null ? app.getResourceString( key ) : key;
//	}

	public void addPerform( PerformableEdit edit )
	{
		if( collToPerform == null ) {
			collToPerform = new ArrayList();
		}
		collToPerform.add( edit );
	}

	public PerformableEdit perform()
	{
		if( collToPerform == null ) return this;

		PerformableEdit edit;

		for( int i = 0; i < collToPerform.size(); i++ ) {
			edit = (PerformableEdit) collToPerform.get( i );
			edit.perform();
			addEdit( edit );
		}
		collToPerform = null;
//		end();

		return this;
	}

	public void debugDump( int nest )
	{
		final StringBuffer strBuf = new StringBuffer( nest << 1 );
		for( int i = 0; i < nest; i++ ) strBuf.append( "  " );
		final String pre = strBuf.toString();
		PerformableEdit edit;

		nest++;

		System.err.println( pre + "Edits : "+edits.size() );
		for( int i = 0; i < edits.size(); i++ ) {
			edit = (PerformableEdit) edits.get( i );
			System.err.print( pre + " edit #"+i+" = " );
			edit.debugDump( nest );
		}
		System.err.println( pre + "To perform : "+(collToPerform != null ? collToPerform.size() : 0 ));
		if( collToPerform != null ) {
			for( int i = 0; i < collToPerform.size(); i++ ) {
				edit = (PerformableEdit) collToPerform.get( i );
				System.err.print( pre + " perf #"+i+" = " );
				edit.debugDump( nest );
			}
		}
	}
}