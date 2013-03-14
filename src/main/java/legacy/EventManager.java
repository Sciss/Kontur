package legacy;

import java.awt.EventQueue;
import java.util.ArrayList;

/**
 *  A custom event dispatcher which
 *  carefully deals with synchronization issues.
 *  Assuming, the synchronization requests specified for
 *  some methods are fulfilled, this class is completely
 *  thread safe.
 *  <p>
 *  It is constructed using a second object, the manager's
 *  processor which will be invoked whenever new events are
 *  available in the event FIFO queue. the processor is then
 *  responsible for querying all registered listeners and
 *  calling their appropriate event listening methods.
 *  <p>
 *  Event dispatching is deferred to the Swing thread execution
 *  time since this makes the whole application much more
 *  predictable and easily synchronizable.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.18, 08-Apr-08
 */
public class EventManager
implements Runnable
{
	public static final boolean DEBUG_EVENTS	= false;

	private final ArrayList		collListeners   = new ArrayList();  // sync'ed because always in Swing thread
	private final ArrayList		collQueue		= new ArrayList();  // sync'ed through synchronized( this )
	private boolean				paused			= false;
	private volatile boolean	invoked			= false;

	protected EventManager.Processor eventProcessor;

	private Object[] events = new Object[ 2 ];

	public EventManager( EventManager.Processor eventProcessor )
	{
		this.eventProcessor = eventProcessor;
	}

	protected EventManager() { /* empty */ }

	public void dispose()
	{
		synchronized( this ) {
			collListeners.clear();
			collQueue.clear();
		}
	}

	/**
	 *  Adds a new listener. The listener
	 *  will receive all events queued after this
	 *  method is called. Events already in queue
	 *  at the moment this method is called are not
	 *  passed to the listener.
	 *
	 *  @param  listener	the listener to add
	 */
	public void addListener( Object listener )
	{
		if( listener != null ) {
			synchronized( this ) {
				// since methods executed within the eventProcessor's run method
				// are possible candidates for calling addListener(), we postpone
				// the adding so it is acertained that the getListener() calls
				// in the eventProcessor's run method won't be disturbed!!
				collQueue.add( new PostponedAction( listener, true ));
				EventQueue.invokeLater( this );
			}
		}
	}

	/**
	 *  Removes a listener. Similar to the
	 *  adding process, the listener won't receive
	 *  any events queued after this method is called.
	 *  However, when there are events in the queue
	 *  at the moment when this method is called, they
	 *  will still be past to the old listener.
	 *
	 *  @param  listener	the listener to remove. <code>null</code>
	 *						is allowed (no op).
	 */
	public void removeListener( Object listener )
	{
		if( listener != null ) {
			synchronized( this ) {
				// since methods executed within the eventProcessor's run method
				// are possible candidates for calling removeListener(), we postpone
				// the adding so it is ascertained that the getListener() calls
				// in the eventProcessor's run method won't be disturbed!!
				collQueue.add( new PostponedAction( listener, false ));
				EventQueue.invokeLater( this );
			}
		}
	}

	/**
	 *  Called by add/removeListener and dispatchEvent.
	 *  This method makes the postponed
	 *  collection modifications permanent.
	 *  It calls the eventProcessor as long as there
	 *  are events in the queue.
	 */
	public void run()
	{
		final int numEvents;

		synchronized( this ) {
			invoked = false;
			if( paused ) return;
			// we only process that many events
			// we find NOW in the queue. if the
			// event processor or its listeners
			// add new events they will be processed
			// in the next later invocation
//			eventsInCycle = collQueue.size();
			numEvents = collQueue.size();
			events = collQueue.toArray( events );
			collQueue.clear();
		}

		for( int i = 0; i < numEvents; i++ ) {
			if( events[ i ] instanceof BasicEvent ) {
				try {
					eventProcessor.processEvent( (BasicEvent) events[ i ]);
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			} else {
//				assert events[ i ] instanceof PostponedAction;
				final PostponedAction pa = (PostponedAction) events[ i ];
				if( pa.state ) {
					if( !collListeners.contains( pa.listener )) {
						collListeners.add( pa.listener );
					}
				} else {
					collListeners.remove( pa.listener );
				}
//			} else {
//				assert false : o.getClass().getName();
			}
			events[ i ] = null;
		}
	}

	/**
	 *  Gets a listener from the list
	 *
	 *  @synchronization	MUST BE CALLED FROM THE EVENT DISPATCH THREAD
	 */
	public Object getListener( int index )
	{
		return( collListeners.get( index ));
	}

	/**
	 *  Get the number of listeners
	 *
	 *  @synchronization	MUST BE CALLED FROM THE EVENT DISPATCH THREAD
	 */
	public int countListeners()
	{
		return( collListeners.size() );
	}

	public void debugDump()
	{
		for( int i = 0; i < collListeners.size(); i++ ) {
			System.err.println( "listen "+i+" = "+collListeners.get( i ).toString() );
		}
	}

	/**
	 *  Puts a new event in the queue.
	 *  If the most recent event can
	 *  be incorporated by the new event,
	 *  it will be replaced, otherwise the new
	 *  one is appended to the end. The
	 *  eventProcessor is invoked asynchronously
	 *  in the Swing event thread
	 *
	 *  @param  e   the event to add to the queue.
	 *				before it's added, the event's incorporate
	 *				method will be checked against the most
	 *				recent event in the queue.
	 */
	public void dispatchEvent( BasicEvent e )
	{
		final int		i;
		final boolean	invoke;
		final Object	o;

sync:	synchronized( this ) {
			invoke  = !(paused || invoked);
			i		= collQueue.size() - 1;
			if( i >= 0 ) {
				o = collQueue.get( i );
				if( (o instanceof BasicEvent) && e.incorporate( (BasicEvent) o )) {
					collQueue.set( i, e );
					break sync;
				}
			}
			collQueue.add( e );
		} // synchronized( this )

		if( invoke ) {
			invoked = true;
			EventQueue.invokeLater( this );
		}
	}

	/**
	 *  Pauses event dispatching.
	 *  Events will still be queued, but the
	 *  dispatcher will wait to call any processors
	 *  until resume() is called.
	 */
	public void pause()
	{
		synchronized( this ) {
//System.err.println( "pause" );
			paused = true;
		} // synchronized( this )
	}

	/**
	 *  Resumes event dispatching.
	 *  Any events in the queue will be
	 *  distributed as normal.
	 */
	public void resume()
	{
		boolean invoke;

		synchronized( this ) {
//System.err.println( "resume" );
			paused = false;
			invoke = !collQueue.isEmpty();
		} // synchronized( this )

		if( invoke ) EventQueue.invokeLater( this );
	}

// -------------------- processor interface --------------------

	/**
	 *  Callers of the EventManager constructor
	 *  must provide an object implementing this interface
	 */
	public interface Processor
	{
		/**
		 *  Processes the next event in the queue.
		 *  This gets called in the event thread.
		 *  Usually implementing classes should
		 *  loop through all listeners by calling
		 *  elm.countListeners() and elm.getListener(),
		 *  and invoke specific dispatching methods
		 *  on these listeners.
		 */
		public void processEvent( BasicEvent e );
	}

// -------------------- postpone helper class --------------------

	private class PostponedAction
	{
		protected final Object   listener;
		protected final boolean  state;

		protected PostponedAction( Object listener, boolean state )
		{
			this.listener   = listener;
			this.state		= state;
		}
	}
}
