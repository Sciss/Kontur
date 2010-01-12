/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import scala.collection.mutable.{ ArrayBuffer }

class SessionElementSeq[ T <: SessionElement ]( val name: String )
extends SessionElement {
// extends ArrayBuffer[T]
// with ObservableBuffer[T]
  private val coll = new ArrayBuffer[T]()

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

  def get( idx: Int ) : Option[ T ] = try {
     Some( coll( idx ))
    } catch { case e: IndexOutOfBoundsException => None }

  def indexOf( elem: T ) : Int = coll.indexOf( elem )

  def contains( elem: T ) : Boolean = coll.contains( elem )

  def foreach[ U ]( f: T => U ): Unit = coll.foreach( f )

  case class ElementAdded( index: Int, elem: T )
  case class ElementRemoved( index: Int, elem: T )
}
