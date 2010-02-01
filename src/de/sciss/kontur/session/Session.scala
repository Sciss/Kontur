/*
 *  Session.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.session

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicDocument, ProcessingThread }
import de.sciss.io.{ IOUtil }
import de.sciss.util.{ Flag }
import de.sciss.kontur.util.{ Model }
import java.awt.{ EventQueue }
import java.io.{ File, IOException }
import scala.xml.{ Node, XML }

object Session {
    def newEmpty: Session = {
       new Session( None, 0 )
    }

    val XML_START_ELEMENT = "konturSession"

    @throws( classOf[ IOException ])
    def newFrom( path: File ) : Session = {
       val xml = XML.loadFile( path )
       if( xml.label != XML_START_ELEMENT ) throw new IOException( "Not a session file" )
       val doc = new Session( Some( path ), (xml \ "idCount").text.toInt )
       doc.fromXML( xml )
       doc
    }
   
    case class DirtyChanged( newDirty: Boolean )
    case class PathChanged( oldPath: Option[ File ], newPath: Option[ File ])
}

class Session( private var pathVar: Option[ File ], private var idCount: Int )
extends BasicDocument with Model {

    import Session._

	 private var pt: Option[ ProcessingThread ] = None
	 private val undo  = new de.sciss.app.UndoManager( this )
    private var dirty = false

//  private var path: Option[ File ] = None

    val timelines   = new Timelines( this )
    val audioFiles  = new AudioFileSeq( this )
    val diffusions  = new Diffusions( this )

    def createID : Long = {
      val res = idCount
      idCount += 1
      res
    }

    // note: the order is crucial
    // in order to resolve dependancies when loading
    def toXML = <konturSession>
  <idCount>{idCount}</idCount>
    {audioFiles.toXML}
    {diffusions.toXML}
    {timelines.toXML}
</konturSession>

    @throws( classOf[ IOException ])
    def fromXML( elem: Node ) {
       audioFiles.fromXML( elem )
       diffusions.fromXML( elem )
       timelines.fromXML( elem )
    }

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

    def setDirty( newDirty: Boolean ) = {
		if( dirty != newDirty ) {
			dirty = newDirty
         dispatch( DirtyChanged( newDirty ))
		}
	}

	def getApplication() : de.sciss.app.Application =
      AbstractApplication.getApplication()

	def getUndoManager() : de.sciss.app.UndoManager = undo

    def dispose() {
       // nada
    }
}