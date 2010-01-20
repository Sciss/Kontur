/*
 *  SessionElementSeq.scala
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

import java.io.{ IOException }
import javax.swing.undo.{ UndoManager }
import scala.collection.mutable.{ ArrayBuffer }
import scala.xml.{ Node }
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
  def find( p: (T) => Boolean): Option[ T ]
  def size: Int

  // XXX this could be more performative (e.g. using a separate Map)
  def getByID( id: Long ) : Option[ T ] = find( _.id == id )

  def editor: Option[ SessionElementSeqEditor[ T ]]

  case class ElementAdded( index: Int, elem: T )
  case class ElementRemoved( index: Int, elem: T )
}

trait SessionElementSeqEditor[ T ] extends Editor {
  def editInsert( ce: AbstractCompoundEdit, idx: Int, e: T ) : Unit
  def editRemove( ce: AbstractCompoundEdit, e: T ) : Unit
}

abstract class BasicSessionElementSeq[ T <: SessionElement ]( doc: Session, val name: String )
extends SessionElementSeq[ T ]
with SessionElementSeqEditor[ T ] {
// extends ArrayBuffer[T]
// with ObservableBuffer[T]
  private val coll = new ArrayBuffer[T]()

  def undoManager: UndoManager = doc.getUndoManager

  protected def innerToXML = <coll>
  {coll.map(_.toXML)}
</coll>

  @throws( classOf[ IOException ])
  protected def innerFromXML( node: Node ) {
     val collXML = SessionElement.getSingleXML( node, "coll" )
     coll ++= elementsFromXML( collXML )
  }

  @throws( classOf[ IOException ])
  protected def elementsFromXML( node: Node ) : Seq[ T ]

  private val forward = (msg: AnyRef) => dispatch( msg )

/*
  def +=( elem: T ) {
    val idx = coll.size
    coll += elem
    elem.addListener( forward )
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
    elem.addListener( forward )
    dispatch( ElementAdded( idx, elem ))
  }

  def remove( elem: T ) {
    val idx = coll.indexOf( elem )
    if( idx >= 0 ) {
      coll.remove( idx )
      elem.removeListener( forward )
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
  def find( p: (T) => Boolean): Option[ T ] = coll.find( p )
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