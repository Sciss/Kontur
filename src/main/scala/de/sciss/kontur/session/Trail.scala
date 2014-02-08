/*
 *  Trail.scala
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

import edit.Editor
import util.Model
import de.sciss.span.Span
import legacy.AbstractCompoundEdit

abstract sealed class TouchMode( val id: Int )

case object TouchNone   extends TouchMode( 0 )
case object TouchSplit  extends TouchMode( 1 )
case object TouchResize extends TouchMode( 2 )

trait Stake[ +Repr ] {
  val span: Span
//  def reshape( newStart: Long, newStop: Long, innerMotion: Long ) : Repr
   def move( delta: Long ) : Repr
}

trait ResizableStake[ +Repr ] extends Stake[ Repr ] {
   def moveStart( delta: Long ) : Repr
   def moveStop( delta: Long ) : Repr
   // default semantics is to apply moveStart and moveStop.
   // subclasses can override behavior if needed
   def split( pos: Long ) : (Repr, Repr) =
      (moveStop( pos - span.stop ), moveStart( pos - span.start ))
}

trait SlidableStake[ +Repr ] extends Stake[ Repr ] {
   def moveOuter( delta: Long ) : Repr
   def moveInner( delta: Long ) : Repr
}

trait MuteableStake[ +Repr ] extends Stake[ Repr ] {
   val muted: Boolean
   def mute( newMuted: Boolean ) : Repr
}

//object Trail {
//  case class StakesAdded( span: Span, stakes: Stake* )
//  case class StakesRemoved( span: Span, stakes: Stake* )
//}

trait Trail[ T <: Stake[ T ]]
extends /* Disposable with */ Model {
//    type St = T

   def emptyList: List[ T ] = Nil

// def visitRange( visitor: TrailVisitor[T], span: Span, byStart: Boolean = true )( f: (T) => Unit )
   def visitRange( span: Span, byStart: Boolean = true )( f: (T) => Unit )
// def visitAll( visitor: TrailVisitor[T], byStart: Boolean = true )
   def visitAll( byStart: Boolean = true )( f: (T) => Unit )
   def getRange( span: Span, byStart: Boolean = true, overlap: Boolean = true ) : List[ T ]
   def getAll( byStart: Boolean = true ) : List[ T ]
// def span: Span
	def get( idx: Int, byStart: Boolean = true ) : T
	def indexOf( stake: T, byStart: Boolean = true ) : Int
	def indexOfPos( pos: Long, byStart: Boolean = true ) : Int
//	public boolean contains( Stake stake );
   def editor: Option[ TrailEditor[ T ]]
// def newEmpty: Trail[ T ]
   def isEmpty : Boolean

   case class StakesAdded( span: Span, stakes: T* )
   case class StakesRemoved( span: Span, stakes: T* )
}

trait TrailEditor[T <: Stake[T]] extends Editor {

  def editInsert(ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode): Unit
  def editRemove(ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode): Unit
  def editClear (ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode): Unit

  def editAdd   (ce: AbstractCompoundEdit, stakes: T*): Unit
  def editRemove(ce: AbstractCompoundEdit, stakes: T*): Unit

  def defaultTouchMode: TouchMode
}
