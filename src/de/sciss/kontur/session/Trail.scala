/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import java.io.{ IOException }

import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.util.{ Disposable }
import de.sciss.kontur.edit.{ Editor }
import de.sciss.kontur.util.{ Model }

/**
 *	@author		Hanns Holger Rutz
 *	@version	0.13, 10-Jan-10
 */
abstract sealed class TouchMode( val id: Int )

case object TouchNone   extends TouchMode( 0 )
case object TouchSplit  extends TouchMode( 1 )
case object TouchResize extends TouchMode( 2 )

trait Stake {
  def span: Span
}

object Trail {
  case class Changed( span: Span )
}

trait Trail[ T <: Stake ]
extends Disposable with Model {
//    def visitRange( visitor: TrailVisitor[T], span: Span, byStart: Boolean = true )( f: (T) => Unit )
    def visitRange( span: Span, byStart: Boolean = true )( f: (T) => Unit )
//    def visitAll( visitor: TrailVisitor[T], byStart: Boolean = true )
    def visitAll( byStart: Boolean = true )( f: (T) => Unit )
    def span: Span
	def get( idx: Int, byStart: Boolean = true ) : T
	def indexOf( stake: T, byStart: Boolean = true ) : Int
	def indexOfPos( pos: Long, byStart: Boolean = true ) : Int
//	public boolean contains( Stake stake );
}

trait TrailEditor[ T <: Stake ]
extends Editor {
    def track: Trail[ T ]
	def editInsert( id: Editor#Client, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editRemove( id: Editor#Client, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editClear( id: Editor#Client, span: Span, touchMode: TouchMode = defaultTouchMode ) : Unit
	def editAdd( id: Editor#Client, stake: T* ) : Unit
	def editRemove( id: Editor#Client, stake: T* ) : Unit
	def defaultTouchMode: TouchMode
}

// trait TrailVisitor[ T <: Stake ] {
//   def visit( stake: T ) : Unit
// }