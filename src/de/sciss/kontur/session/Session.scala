/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicDocument, ProcessingThread }
import de.sciss.util.{ Flag }
import java.awt.{ EventQueue }

class Session( name: String ) extends BasicDocument {

	private var pt: Option[ ProcessingThread ] = None
	private val undo  = new de.sciss.app.UndoManager( this )
    private var dirty = false

    val timelines   = new SessionElementSeq[ TimelineElement ]( "Timelines" )
//    val audioFiles  = new SessionElementSeq[ AudioFileElement ]( "Audio Files" )
//    val busses      = new SessionElementSeq[ BusElement ]( "Busses" )

	/**
	 * 	Starts a <code>ProcessingThread</code>. Only one thread
	 * 	can exist at a time. To ensure that no other thread is running,
	 * 	call <code>checkProcess()</code>.
	 *
	 * 	@param	pt	the thread to launch
	 * 	@throws	IllegalMonitorStateException	if called from outside the event thread
	 * 	@throws	IllegalStateException			if another process is still running
	 * 	@see	#checkProcess()
	 * 	@synchronization	must be called in the event thread
	 */
	def start( process: ProcessingThread ) {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()
		if( pt.isDefined ) throw new IllegalStateException( "Process already running" )

		pt = Some( process )
		process.addListener( new ProcessingThread.Listener() {
			def processStarted( e: ProcessingThread.Event ) { /* empty */ }
			def processStopped( e: ProcessingThread.Event ) {
				pt = None
			}
		})
		process.start()
	}

	def closeDocument( force: Boolean, wasClosed: Flag ) : ProcessingThread = {
//		return frame.closeDocument( force, wasClosed );	// XXX should be in here not frame!!!
      wasClosed.set( true )
      null
	}

	def getName() : String = name

    def displayName : String = {
        if( getName == null ) {
			getResourceString( "frameUntitled" )
		} else {
			getName
        }
    }

  	protected def getResourceString( key: String ) : String =
		getApplication().getResourceString( key )
      
	def isDirty() : Boolean = dirty

    def setDirty( dirty: Boolean ) = {
		if( !this.dirty == dirty ) {
			this.dirty = dirty
//			updateTitle()
		}
	}

	def getApplication() : de.sciss.app.Application =
      AbstractApplication.getApplication()

	def getUndoManager() : de.sciss.app.UndoManager = undo

    def dispose() {
       // nada
    }
}

object Session {
   def newEmpty: Session = {
     new Session( null )
   }
}