/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import javax.swing.undo.{ UndoManager }
import scala.collection.mutable.{ ArrayBuffer }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }

trait SessionElementSeq[ T <: SessionElement ]
extends SessionElement {
  def get( idx: Int ) : Option[ T ]

  // some collection methods
  def indexOf( elem: T ) : Int
  def contains( elem: T ) : Boolean
  def foreach[ U ]( f: T => U ): Unit
  def toList: List[ T ]
  def filter( p: (T) => Boolean ): List[ T ]
  def size: Int

  def editor: Option[ SessionElementSeqEditor[ T ]]

  case class ElementAdded( index: Int, elem: T )
  case class ElementRemoved( index: Int, elem: T )
}

trait SessionElementSeqEditor[ T ] extends Editor {
  def editInsert( ce: AbstractCompoundEdit, idx: Int, e: T ) : Unit
  def editRemove( ce: AbstractCompoundEdit, e: T ) : Unit
}

class BasicSessionElementSeq[ T <: SessionElement ]( doc: Session, val name: String )
extends SessionElementSeq[ T ]
with SessionElementSeqEditor[ T ] {
// extends ArrayBuffer[T]
// with ObservableBuffer[T]
  private val coll = new ArrayBuffer[T]()

  def undoManager: UndoManager = doc.getUndoManager
/*
  def +=( elem: T ) {
    val idx = coll.size
    coll += elem
    elem.addListener( dispatch( _ ))
    dispatch( ElementAdded( idx, elem ))
  }

  def -=( elem: T ) {
    val idx = coll.indexOf( elem )
    if( idx >= 0 ) {
      coll.remove( idx )
      elem.removeListener( dispatch( _ ))
      dispatch( ElementRemoved( idx, elem ))
    }
  }
*/

  def insert( idx: Int, elem: T ) {
    coll.insert( idx, elem )
    elem.addListener( dispatch( _ ))
    dispatch( ElementAdded( idx, elem ))
  }

  def remove( elem: T ) {
    val idx = coll.indexOf( elem )
    if( idx >= 0 ) {
      coll.remove( idx )
      elem.removeListener( dispatch( _ ))
      dispatch( ElementRemoved( idx, elem ))
    }
  }

  def get( idx: Int ) : Option[ T ] = try {
     Some( coll( idx ))
    } catch { case e: IndexOutOfBoundsException => None }

  // some collection methods
  def indexOf( elem: T ) : Int = coll.indexOf( elem )
  def contains( elem: T ) : Boolean = coll.contains( elem )
  def foreach[ U ]( f: T => U ): Unit = coll.foreach( f )
  def toList: List[ T ] = coll.toList
  def filter( p: (T) => Boolean ): List[ T ] = coll.filter( p ).toList
  def size: Int = coll.size

  def editor: Option[ SessionElementSeqEditor[ T ]] = Some( this )
  // ----  SessionElementSeqEditor ----
  def editInsert( ce: AbstractCompoundEdit, idx: Int, elem: T ) {
      val edit = new SimpleEdit( "editAddSessionElement" ) {
        def apply { insert( idx, elem )}
        def unapply { remove( elem )}
      }
      ce.addPerform( edit )
  }
  
  def editRemove( ce: AbstractCompoundEdit, elem: T ) {
      val edit = new SimpleEdit( "editAddSessionElement" ) {
        lazy val idx = indexOf( elem )
        def apply { remove( elem )}
        def unapply { insert( idx, elem )}
      }
      ce.addPerform( edit )
  }
}