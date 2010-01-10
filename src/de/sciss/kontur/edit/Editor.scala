/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.edit

import java.awt.{ EventQueue }
import javax.swing.undo.{ UndoManager }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.common.{ BasicCompoundEdit }

trait Editor {
//    private var map = Map[ Int, Client ]()
//    private var uniqueID = 0
    def undoManager: UndoManager

	def editBegin( name: String ) : AbstractCompoundEdit = {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()
		new BasicCompoundEdit( name )
	}
    
//    private def nextUniqueID : Int = {
//      val res = uniqueID
//      uniqueID += 1
//      res
//    }

	def editEnd( ce: AbstractCompoundEdit ) {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()
        ce.perform
        ce.end
        undoManager.addEdit( ce )
	}

	def editCancel( ce: AbstractCompoundEdit ) {
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException()
		ce.cancel()
	}

//	protected def getClient( int id ) : Client = map( id )

//	case class Client( source: Object, edit: BasicCompoundEdit )
}