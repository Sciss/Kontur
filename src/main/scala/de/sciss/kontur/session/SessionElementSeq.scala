/*
 *  SessionElementSeq.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package session

import java.io.IOException
import collection.mutable.ArrayBuffer
import xml.Node
import edit.{ Editor, SimpleEdit }
import util.{Model, SerializerContext}
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

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
   def map[ B ]( f: (T) => B ) : List[ B ]
   def size: Int

  // XXX this could be more performative (e.g. using a separate Map)
//  def getByID( id: Long ) : Option[ T ] = find( _.id == id )

  def editor: Option[ SessionElementSeqEditor[ T ]]

  case class ElementAdded( index: Int, elem: T )
  case class ElementRemoved( index: Int, elem: T )
}

trait SessionElementSeqEditor[T] extends Editor {
  def editInsert(ce: AbstractCompoundEdit, idx: Int, e: T): Unit
  def editRemove(ce: AbstractCompoundEdit, e: T): Unit
}

abstract class BasicSessionElementSeq[ T <: SessionElement ]( doc: Session, val name: String )
extends SessionElementSeq[ T ]
with SessionElementSeqEditor[ T ] {
// extends ArrayBuffer[T]
// with ObservableBuffer[T]
  private val coll = new ArrayBuffer[T]()

  def undoManager: UndoManager = doc.undoManager

  protected def innerToXML( context: SerializerContext ) = <coll>
  {coll.map( _.toXML( context ))}
</coll>

  @throws( classOf[ IOException ])
  protected def innerFromXML( c: SerializerContext, node: Node ): Unit = {
     val collXML = SessionElement.getSingleXML( node, "coll" )
     coll ++= elementsFromXML( c, collXML )
  }

  @throws( classOf[ IOException ])
  protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ T ]

  private val forward: Model.Listener = { case msg => dispatch( msg )}

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

  def insert( idx: Int, elem: T ): Unit = {
    coll.insert( idx, elem )
    elem.addListener( forward )
    dispatch( ElementAdded( idx, elem ))
  }

  def remove( elem: T ): Unit = {
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
   def map[ B ]( f: (T) => B ) : List[ B ] = coll.map( f ).toList
   def size: Int = coll.size

  def editor: Option[ SessionElementSeqEditor[ T ]] = Some( this )
  // ----  SessionElementSeqEditor ----
  def editInsert( ce: AbstractCompoundEdit, idx: Int, elem: T ): Unit = {
      val edit = new SimpleEdit( "editAddSessionElement" ) {
        def apply  (): Unit = insert(idx, elem)
        def unapply(): Unit = remove     (elem)
      }
      ce.addPerform( edit )
  }
  
  def editRemove( ce: AbstractCompoundEdit, elem: T ): Unit = {
      val edit = new SimpleEdit( "editAddSessionElement" ) {
        lazy val idx = indexOf( elem )
        def apply(): Unit = { idx; remove( elem )}
        def unapply(): Unit = insert( idx, elem )
      }
      ce.addPerform( edit )
  }
}