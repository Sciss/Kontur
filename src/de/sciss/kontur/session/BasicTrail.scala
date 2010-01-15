/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.trees.{ Interval, LongManager, ManagedLong, Rect, RTree, Shaped }
import de.sciss.kontur.edit.{ SimpleEdit }
import javax.swing.undo.{ UndoManager }

/**
 *  Basic trail structure using R-Tree
 *  in the background
 */
class BasicTrail[ T <: Stake ]( doc: Session ) extends Trail[ T ]
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

    def visitAll( byStart: Boolean = true )( f: (T) => Unit ) = {
      // XXX is this the most efficient approach?
      tree.findOverlapping( tree.getRoot.bounds, (ss: StoredStake) => {
        f( ss.stake )
      })
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
       if( !modSpan.isEmpty ) dispatch( Trail.Changed( modSpan ))
    }

    def remove( stakes: T* ) {
       var modSpan = new Span()
       stakes.foreach( stake => {
         tree.remove( StoredStake( stake ))
         modSpan = if( modSpan.isEmpty ) stake.span else modSpan.union( stake.span )
       })
       if( !modSpan.isEmpty ) dispatch( Trail.Changed( modSpan ))
    }

    def dispose {}

    private case class StoredStake( val stake: T ) extends Shaped[ Long ] {
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
        def apply { add( stakes: _* )}
        def unapply { remove( stakes: _* )}
      }
      ce.addPerform( edit )
    }

	def editRemove( ce: AbstractCompoundEdit, stakes: T* ) {
      val edit = new SimpleEdit( "editRemoveStakes" ) {
        def apply { remove( stakes: _* )}
        def unapply { add( stakes: _* )}
      }
      ce.addPerform( edit )
    }

	def defaultTouchMode: TouchMode = TouchSplit

    def undoManager: UndoManager = doc.getUndoManager
}