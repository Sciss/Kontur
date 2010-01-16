/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicDocument, ProcessingThread }
import de.sciss.io.{ IOUtil }
import de.sciss.util.{ Flag }
import de.sciss.kontur.util.{ Model }
import java.awt.{ EventQueue }
import java.io.{ File, IOException }
import scala.xml.{ XML }

object Session {
   def newEmpty: Session = {
     new Session( None )
   }
   
    case class DirtyChanged( newDirty: Boolean )
    case class PathChanged( oldPath: Option[ File ], newPath: Option[ File ])
}

class Session( private var pathVar: Option[ File ])
extends BasicDocument with Model {

    import Session._

	private var pt: Option[ ProcessingThread ] = None
	private val undo  = new de.sciss.app.UndoManager( this )
    private var dirty = false
    private var idCount = 0
//    private var path: Option[ File ] = None

    val timelines   = new Timelines( this )
    val audioFiles  = new AudioFileSeq( this )
//    val busses      = new SessionElementSeq[ BusElement ]( "Busses" )

    def createID : Long = {
      val res = idCount
      idCount += 1
      res
    }

    def toXML =
      <session>
        <idCount>{idCount}</idCount>
        {audioFiles.toXML}
        {timelines.toXML}
      </session>

    @throws( classOf[ IOException ])
    def save( f: File ) {
      XML.save( f.getAbsolutePath, toXML, "UTF-8", true, null )
    }

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
	  AbstractApplication.getApplication().getDocumentHandler().removeDocument( this, this )
      null
	}

    def path = pathVar
    def path_=( newPath: Option[ File ]) {
       if( newPath != pathVar ) {
          val change = PathChanged( pathVar, newPath )
          pathVar = newPath
          dispatch( change )
       }
    }

    def name: Option[ String ] = pathVar.map( p => {
            val n = p.getName()
            val i = n.lastIndexOf( '.' )
            if( i == -1 ) n else n.substring( 0, i )
        })

	def getName() = name getOrElse null

    def displayName =
       name getOrElse getResourceString( "frameUntitled" )

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
