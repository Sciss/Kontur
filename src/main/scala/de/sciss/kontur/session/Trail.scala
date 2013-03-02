/*
 *  Trail.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.kontur
package session

import de.sciss.app.AbstractCompoundEdit
import de.sciss.util.Disposable
import edit.Editor
import util.Model
import de.sciss.span.Span

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
extends Disposable with Model {
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

trait TrailEditor[ T <: Stake[ T ]]
extends Editor {
//    type St = T
	def editInsert( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editRemove( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editClear( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editAdd( ce: AbstractCompoundEdit, stakes: T* ) : Unit
	def editRemove( ce: AbstractCompoundEdit, stakes: T* ) : Unit
	def defaultTouchMode: TouchMode
}

// trait TrailVisitor[ T <: Stake ] {
//   def visit( stake: T ) : Unit
// }