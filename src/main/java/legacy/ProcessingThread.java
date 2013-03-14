package legacy;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *  A subclass of Thread that is capable of
 *  dealing with synchronization issues.
 *  It will pause all event dispatching related
 *  to specified doors which will be locked
 *  during processing. It includes helper
 *  methods for updating a progress bar and
 *  displaying messages and exceptions.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 28-Jun-08
 */
public class ProcessingThread
//extends Thread
implements Runnable, EventManager.Processor, ActionListener /* , Disposable	*/ // , ProgressComponent
{
	private final Client				client;
	private final ProgressComponent		pc;
//	private final LockManager			lm;
//	private final Object				clientArg;
//	private final int					requiredDoors;
	private final Map					clientMap	= Collections.synchronizedMap( new HashMap() );
	protected final String				name;

	private final Runnable				runProgressUpdate, runProcessFinished;
	protected volatile float			progress;
	private volatile float				progOff = 0f, progStop = 1f, progWeight = 1f;
	protected volatile boolean			progressInvoked	= false;
//	private boolean						procAlive;
	protected volatile Exception		exception   = null;
	protected EventManager				elm			= null; // lazily created ; flushed due to synchronized()s?

	protected int						returnCode	= -1;	// flushed due to synchronized()s
	private volatile boolean			shouldCancel= false;

	protected final Object				sync		= new Object();
	private Thread						thread		= null;	// flushed due to synchronized()s

	private static final Map			mapThreads	= Collections.synchronizedMap( new HashMap() );

	/**
	 *  Creates a new ProcessingThread. To start the process
	 *	and starts processing.
	 *
	 *  @param  client			Interface whose method runProcessing() is called
	 *							inside the new thread when it's started.
	 *  @param  pc				Component responsible for displaying progress bar etc.
	 *  @param  procName		Name for the thread and the process monitoring
	 *  @synchronization		must be called in the event thread
	 */
//	public ProcessingThread( String procName, final Client client, final Object clientArg,
//							 final ProgressComponent pc, final Hook hook )
	public ProcessingThread( final Client client, final ProgressComponent pc, String procName )
	{
//		super( procName );

		this.client			= client;
//		this.clientArg		= clientArg;
		this.pc				= pc;
		name				= procName;
//		this.lm				= lm;
//		this.requiredDoors  = requiredDoors;

		// the progress update is called
		// from the rendering thread using
		// EventQueue.invokeLater( Runnable t )
		// because JProgressBar.setValue() is
		// not marked as being 'threadsafe'
		runProgressUpdate = new Runnable() {
			public void run()
			{
				progressInvoked = false;
				pc.setProgression( progress );
			}
		};
		// same for MenuBar enabling
		runProcessFinished = new Runnable() {
			public void run()
			{
//				final boolean success = (returnCode == Client.DONE) || (returnCode == Client.CANCELLED);

				pc.finishProgression( returnCode );
				pc.removeCancelListener( ProcessingThread.this );
//				client.processFinished( ProcessingThread.this, clientArg );
				client.processFinished( ProcessingThread.this );
				synchronized( sync ) {
					if( elm != null ) elm.dispatchEvent( new ProcessingThread.Event( ProcessingThread.this,
						ProcessingThread.Event.STOPPED, System.currentTimeMillis(), ProcessingThread.this ));
				}
				if( (returnCode == ProgressComponent.FAILED) && (exception != null) ) {
					pc.displayError( exception, name );
				}
			}
		};
	}

	public void putClientArg( Object key, Object value )
	{
		if( value != null ) {
			clientMap.put( key, value );
		} else {
			clientMap.remove( key );
		}
	}

	public Object getClientArg( Object key )
	{
		return clientMap.get( key );
	}

	public Map getClientMap()
	{
		return Collections.unmodifiableMap( clientMap );
	}

	public String getName()
	{
		return name;
	}

	/**
	 *	Starts processing. Call this method
	 *	only once!
	 */
	public void start()
	{
		pc.resetProgression();
		pc.setProgressionText( name );
//		hook.runBefore();
		synchronized( sync ) {
			if( thread != null ) throw new IllegalStateException( "Process was already started" );
			thread	= new Thread( this );
			thread.setDaemon( true );
			mapThreads.put( thread, this );
//			procAlive = true;
			thread.start();
			try {
				sync.wait();	// we will be notified when the locks have been attached!
//				if( lm != null ) root.menuFactory.setMenuBarsEnabled( false );
			} catch( InterruptedException e1 ) {
				// XXX
			}
			if( elm != null ) {
				elm.dispatchEvent( new ProcessingThread.Event( this,
					ProcessingThread.Event.STARTED, System.currentTimeMillis(), this ));
			}
			pc.addCancelListener( this );
		}
	}

	/**
	 *  Puts the calling thread in
	 *  wait mode until the processing
	 *  is finished. This should be called in
	 *	the event thread only if the process is
	 *	about to quit very fast since it will
	 *	otherwise block GUI updates. An example for
	 *	its use is the prior request to cancel
	 *	the process. If the amount of time till the
	 *	end of the process is unknown, the preferred
	 *	method to wait for the thread is to register
	 *	a listener!
	 *
	 *	@see		#addListener( ProcessingThread.Listener )
	 */
	public void sync()
	{
		synchronized( sync ) {
//			while( isAlive() && procAlive ) {
			while( (thread != null) && thread.isAlive() ) {
				try {
					sync.wait();
				} catch( InterruptedException e1 ) { /* ignore */ }
			}
		}
	}

	public void sync( int timeout )
	{
		synchronized( sync ) {
//			while( isAlive() && procAlive ) {
//			while( (thread != null) && thread.isAlive() ) {
			if( (thread != null) && thread.isAlive() ) {
				try {
					sync.wait( timeout );
				} catch( InterruptedException e1 ) { /* ignore */ }
			}
		}
	}

	/**
	 *	Forwards the cancel request
	 *	to the client.
	 *
	 *	@param	doSync	if <code>true</code>, wait for the client to
	 *					abort, otherwise return immediately
	 */
	public void cancel( boolean doSync )
	{
		shouldCancel = true;
//		client.processCancel( this, clientArg );
		client.processCancel( this );
		if( doSync ) sync();
	}

//	/**
//	 *	Utility method for the client.
//	 *	This returns <code>true</code> if
//	 *	the client should cancel the process.
//	 */
//	public boolean shouldCancel()
//	{
//		return shouldCancel;
//	}

	public static ProcessingThread currentThread()
	{
		return( (ProcessingThread) mapThreads.get( Thread.currentThread() ));
	}

//	public static void setProgression( float p )
//	{
//		final ProcessingThread pt = currentThread();
//		if( pt != null ) pt.setProgressin( p );
//	}

	public static boolean shouldCancel()
	{
		final ProcessingThread pt = currentThread();
		return( pt == null ? false : pt.shouldCancel );
	}

//	public static void progress( float p )
//	{
//		final ProcessingThread pt = currentThread();
//		if( pt != null ) pt.setProgression( p );
//	}

	public static void update( float progress )
	throws CancelledException
	{
		final ProcessingThread pt = currentThread();
		if( pt != null ) {
			pt.setProgression( progress );
			if( pt.shouldCancel ) throw new CancelledException();
		}
	}

	public boolean isRunning()
	{
//		final ProcessingThread pt = currentThread();
//		if( pt == null ) return false;
		synchronized( sync ) {
			return thread != null;
		}
	}

	/**
	 *	Returns the cient's return code.
	 *
	 *	@return	the return code (<code>Client.DONE</code> etc.)
	 *			or <code>-1</code> if the client had not been started
	 */
	public int getReturnCode()
	{
		return returnCode;
	}

	/**
	 *	Registers a listener to be notified
	 *	when the process starts and terminates.
	 *
	 *	@param	l	the listener to register
	 */
	public void addListener( ProcessingThread.Listener l )
	{
		synchronized( sync ) {
			if( elm == null ) {
				elm = new EventManager( this );
			}
		}
 		elm.addListener( l );
	}

	/**
	 *	Removes a listener from being
	 *	notified when the process starts and terminates.
	 *
	 *	@param	l	the listener to unregister
	 */
	public void removeListener( ProcessingThread.Listener l )
	{
		elm.removeListener( l );
	}

	public void dispose()
	{
		if( elm != null ) {
			elm.dispose();
			elm = null;
		}
		clientMap.clear();
	}

	/**
	 *  The is the main method of a thread; it invokes
	 *  the client's processRun method.
	 */
	public void run()
	{
		try {
//			if( lm != null ) lm.waitExclusive( requiredDoors );
//			hook.runEntered();
			// now it's safe to resume the Swing thread
			synchronized( sync ) {
				sync.notifyAll();
			}

//			returnCode	= client.processRun( this, clientArg );
			returnCode	= client.processRun( this );
		}
		catch( CancelledException e1 ) {
			returnCode	= ProgressComponent.CANCELLED;
		}
		catch( Exception e1 ) {
			exception	= e1;
			returnCode	= ProgressComponent.FAILED;
		}
		finally {
//			if( lm != null ) lm.releaseExclusive( requiredDoors );
//			hook.runExiting();

			synchronized( sync ) {
//				procAlive = false;
				mapThreads.remove( thread );
				thread = null;
				sync.notifyAll();
			}
			EventQueue.invokeLater( runProcessFinished );
		}
	}

	/**
	 *  If the client is capable of catching
	 *  an exception in its execution block,
	 *  it should pass it to the pt calling this
	 *  method.
	 *
	 *  @param  e   exception which was thrown in the client's
	 *				run method. when the thread stops it
	 *				will display this error to the user.
	 */
	public void setException( Exception e )
	{
		exception = e;
	}

	/**
	 *  Queries the last exception thrown in the run method.
	 *
	 *  @return the most recent exception or null
	 *			if no exception was thrown
	 */
	public Exception getException()
	{
		return exception;
	}

	public static void setNextProgStop( float p )
	{
		final ProcessingThread pt = currentThread();
		if( pt != null ) {
//			progress	= progStop;
			pt.progOff		= pt.progress;
			pt.progStop		= p;
			pt.progWeight	= pt.progStop - pt.progress;
		}
	}

	public static void flushProgression()
	{
		final ProcessingThread pt = currentThread();
		if( pt != null ) {
			pt.progOff		= pt.progress;
			pt.progWeight	= pt.progStop - pt.progress;
		}
	}

//	public float getNextProgStop()
//	{
//		return progStop;
//	}

// ------------------ ActionListener interface ------------------

	// events come from cancel gadget
	public void actionPerformed( ActionEvent e )
	{
		synchronized( sync ) {
//			if( procAlive && !shouldCancel ) {
			if( (thread != null) && !shouldCancel ) {
				cancel( false );
			}
		}
	}

// ------------------ EventManager.Processor interface ------------------

	public void processEvent( BasicEvent e )
	{
		ProcessingThread.Listener			listener;
		final ProcessingThread.Event		pte			= (ProcessingThread.Event) e;
		final int							id			= pte.getID();

		for( int i = 0; i < elm.countListeners(); i++ ) {
			listener = (ProcessingThread.Listener) elm.getListener( i );
			switch( id ) {
			case ProcessingThread.Event.STARTED:
				listener.processStarted( pte );
				break;
			case ProcessingThread.Event.STOPPED:
				listener.processStopped( pte );
				break;
			default:
				assert false : id;
				break;
			}
		}
		if( id == ProcessingThread.Event.STOPPED ) {
			dispose();
		}
	}

// ------------------ ProgressComponent interface ------------------

	public Component getComponent()
	{
		return pc.getComponent();
	}

	/**
	 *  Called by the client to update
	 *  the progress bar.
	 *
	 *  @param  p   new progression between zero and one
	 */
	public void setProgression( float p )
	{
		progress = p * progWeight + progOff;
		if( !progressInvoked ) {
			progressInvoked = true;
			EventQueue.invokeLater( runProgressUpdate );
		} // else System.out.println( "clpse" );
	}

	public void resetProgression()
	{
		pc.resetProgression();
	}

//	public void finishProgression( boolean success ) {}
//	public void setProgressionText( String text ) {}
//	public void showMessage( int type, String text ) {}
//	public void displayError( Exception e, String processName ) {}

// ------------------ internal classes/interfaces ------------------

	public interface Client
	{
		public static final int	DONE		= ProgressComponent.DONE;
		public static final int	FAILED		= ProgressComponent.FAILED;
		public static final int	CANCELLED	= ProgressComponent.CANCELLED;

		/**
		 *  Does the processing. This is called inside
		 *  a separate asynchronous thread.
		 *
		 *  @param  context		the corresponding thread. call <code>context.setProgression()</code>
		 *						to update visual progress feedback.
		 *  @return				return code, which is either of <code>DONE</code> on success,
		 *						<code>FAILED</code> on failure, or <code>CANCELLED</code> if cancelled.
		 *						The implementing class may which to call the context's
		 *						<code>setException</code> method if an error occurs.
		 *
		 *  @see	ProcessingThread#setProgression( float )
		 *  @see	ProcessingThread#setException( Exception )
		 *
		 *  @synchronization	like Thread's run method, this is called inside
		 *						a custom thread
		 */
		public int processRun( ProcessingThread context ) throws IOException;

		/**
		 *  This gets invoked when <code>processRun()</code> is finished or
		 *  aborted. It's useful to place GUI related stuff
		 *  in here since this gets called inside the
		 *  Swing thread.
		 *
		 *  @param  context		the corresponding thread. in case of failure
		 *						can be used to query the exception.
		 *
		 *  @synchronization	this is called in the event thread
		 */
		public void processFinished( ProcessingThread context );

		/**
		 *  This gets invoked when the user requests to abort the process.
		 *  The client should set a signal flag for the processing routine
		 *	to cancel as soon as possible. All partly commited edits should
		 *	be undone.
		 *
		 *  @param  context		the corresponding thread. in case of failure
		 *						can be used to query the exception.
		 *
		 *  @synchronization	this is called in the event thread
		 */
		public void processCancel( ProcessingThread context );
	}

	public interface Listener
	{
		public void processStarted( Event e );
		public void processStopped( Event e );
	}

	public static class Event
	extends BasicEvent
	{
		private final ProcessingThread pt;

		// --- ID values ---
		/**
		 *  returned by getID() : the process started running
		 */
		public static final int STARTED		= 0;

		/**
		 *  returned by getID() : the server stopped running.
		 *	the return value of the client can be queried
		 *	by calling <code>getReturnCode</code> on the
		 *	<code>ProcessingThread</code>.
		 *
		 *	@see	ProcessingThread#getReturnCode()
		 */
		public static final int STOPPED		= 1;

		/**
		 */
		public Event( Object source, int ID, long when, ProcessingThread pt )
		{
			super( source, ID, when );

			this.pt		= pt;
		}

		// shortcut method
		public boolean isDone()
		{
			return pt.getReturnCode() == ProgressComponent.DONE;
		}

		// shortcut method
		public boolean isCancelled()
		{
			return pt.getReturnCode() == ProgressComponent.CANCELLED;
		}

		// shortcut method
		public boolean hasFailed()
		{
			return pt.getReturnCode() == ProgressComponent.FAILED;
		}

		/**
		 */
		public ProcessingThread getProcessingThread()
		{
			return pt;
		}

		public boolean incorporate( BasicEvent oldEvent )
		{
			if( (oldEvent instanceof ProcessingThread.Event) &&
				(getSource() == oldEvent.getSource()) &&
				(getID() == oldEvent.getID()) ) {

				// XXX beware, when the actionID and actionObj
				// are used, we have to deal with them here

				return true;

			} else return false;
		}
	}

	public static class CancelledException
	extends IOException
	{
		/* empty */
	}

//	public interface Hook
//	{
//		public void runEntered();
//		public void runExiting();
//		public void runBefore();
//		public void runAfter();
//	}
}