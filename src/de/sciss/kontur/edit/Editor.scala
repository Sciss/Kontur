/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.edit

import java.awt.{ EventQueue }
import javax.swing.undo.{ UndoManager }
import de.sciss.common.{ BasicCompoundEdit }

trait Editor {
//    private var map = Map[ Int, Client ]()
//    private var uniqueID = 0
    def undoManager: UndoManager

	def editBegin( source: Object, name: String ) : Client = {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()

//        val id = nextUniqueID
		Client( source, new BasicCompoundEdit( name ))
	}
    
//    private def nextUniqueID : Int = {
//      val res = uniqueID
//      uniqueID += 1
//      res
//    }

	def editEnd( id: Client ) {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()

//		final Client c = map.remove( id );
//		if( c == null ) throw new IllegalStateException( String.valueOf( id ));
		id.edit.perform()
		id.edit.end()
//		if( undoMgr != null )
          undoManager.addEdit( id.edit )
	}

	def editCancel( id: Client ) {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()

//		final Client c = map.remove( id );
//		if( c == null ) throw new IllegalStateException( String.valueOf( id ));
		id.edit.cancel()
	}

//	protected def getClient( int id ) : Client = map( id )

	case class Client( source: Object, edit: BasicCompoundEdit )
}