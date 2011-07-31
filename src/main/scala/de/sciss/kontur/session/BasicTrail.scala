/*
 *  BasicTrail.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.app.AbstractCompoundEdit
import de.sciss.io.Span
import de.sciss.trees.{ Interval, LongManager, ManagedLong, Rect, RTree, Shaped }
import de.sciss.kontur.edit.SimpleEdit
import javax.swing.undo.UndoManager
import scala.collection.mutable.ListBuffer

/**
 *  Basic trail structure using R-Tree
 *  in the background
 */
class BasicTrail[ T <: Stake[ T ]]( doc: Session ) extends Trail[ T ]
with TrailEditor[ T ] {
    implicit private def numberView( num: Long ) = new ManagedLong( num )
    implicit private val numberManager = LongManager
    private type LongRect     = Rect[ Long ]
    private type LongInterval = Interval[ Long ]

//    private val numberView = u => new ManagedLong( u )
    private val tree = new RTree[ Long, StoredStake ]( 1 )

    private def spanToRect( span: Span ) =
      new LongRect( Vector( new LongInterval( span.start, span.stop )))
/*
    private def rectToSpan( r: LongRect ) : Span = {
      val i = r.interval( 0 )
      new Span( i.low, i.high )
    }
*/
    def visitRange( span: Span, byStart: Boolean = true )( f: (T) => Unit ) {
      tree.findOverlapping( spanToRect( span ), (ss: StoredStake) => {
        f( ss.stake )
      })
    }

   def isEmpty : Boolean = tree.isEmpty

    def visitAll( byStart: Boolean = true )( f: (T) => Unit ) {
      // XXX is this the most efficient approach?
      tree.findOverlapping( tree.getRoot.bounds, (ss: StoredStake) => {
        f( ss.stake )
      })
    }

    def getRange( span: Span, byStart: Boolean = true, overlap: Boolean = true ) : List[ T ] = {
       val res = new ListBuffer[ T ]()
       visitRange( span, byStart )( stake => { if( overlap || span.contains( stake.span )) res += stake })
       res.toList
    }

    def getAll( byStart: Boolean = true ) : List[ T ] = {
       val res = new ListBuffer[ T ]()
       visitAll( byStart )( stake => res += stake )
       res.toList
    }

    def get( idx: Int, byStart: Boolean = true ) : T = {
      throw new IllegalStateException( "Not yet implemented" ) // XXX
    }

	def indexOf( stake: T, byStart: Boolean = true ) : Int = {
      -1 // XXX
    }

	def indexOfPos( pos: Long, byStart: Boolean = true ) : Int = {
      -1 // XXX
    }

    def add( stakes: T* ) {
       var modSpan = new Span()
       stakes.foreach( stake => {
         tree.insert( StoredStake( stake ))
         modSpan = if( modSpan.isEmpty ) stake.span else modSpan.union( stake.span )
       })
       if( !modSpan.isEmpty ) dispatch( StakesAdded( modSpan, stakes: _* ))
    }

    def remove( stakes: T* ) {
       var modSpan = new Span()
       stakes.foreach( stake => {
         tree.remove( StoredStake( stake ))
         modSpan = if( modSpan.isEmpty ) stake.span else modSpan.union( stake.span )
       })
       if( !modSpan.isEmpty ) dispatch( StakesRemoved( modSpan, stakes: _* ))
    }

    def dispose() {}

    private case class StoredStake( stake: T ) extends Shaped[ Long ] {
      val shape = spanToRect( stake.span )
    }

    def editor: Option[ TrailEditor[ T ]] = Some( this )
    // ---- TrailEditor ----

	def editInsert( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) {
      throw new IllegalStateException( "Not yet implemented" ) // XXX
    }

	def editRemove( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) {
      throw new IllegalStateException( "Not yet implemented" ) // XXX
    }

	def editClear( ce: AbstractCompoundEdit, span: Span, touchMode: TouchMode = defaultTouchMode ) {
      throw new IllegalStateException( "Not yet implemented" ) // XXX
    }

	def editAdd( ce: AbstractCompoundEdit, stakes: T* ) {
      val edit = new SimpleEdit( "editAddStakes" ) {
        def apply() { add( stakes: _* )}
        def unapply() { remove( stakes: _* )}
      }
      ce.addPerform( edit )
    }

	def editRemove( ce: AbstractCompoundEdit, stakes: T* ) {
      val edit = new SimpleEdit( "editRemoveStakes" ) {
        def apply() { remove( stakes: _* )}
        def unapply() { add( stakes: _* )}
      }
      ce.addPerform( edit )
    }

	def defaultTouchMode: TouchMode = TouchSplit

    def undoManager: UndoManager = doc.getUndoManager
}