/*
 *  TrailView.scala
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
package gui

import edit.{ Editor, SimpleEdit }
import session.{ Session, Stake, Trail }
import util.Model
import de.sciss.span.Span
import de.sciss.span.Span.SpanOrVoid
import legacy.AbstractCompoundEdit
import desktop.UndoManager

object TrailView {
   // FUCKING SCHEISS DOES NOT COMPILE ANY MORE
//    case class SelectionChanged( span: Span, stakes: Stake[ _ ]* )
    case class SelectionChanged( span: Span )
}

trait TrailView[ T <: Stake[ T ]] extends Model {
    def isSelected( s: T ) : Boolean
    def editor: Option[ TrailViewEditor[ T ]]
    def selectedStakes: Set[ T ]
    def trail: Trail[ T ]

//    case class SelectionChanged( span: Span, stakes: T* )
}

trait TrailViewEditor[ T <: Stake[ T ]] extends Editor {
//   type St = T
   def editSelect( ce: AbstractCompoundEdit, stakes: T* ) : Unit
   def editDeselect( ce: AbstractCompoundEdit, stakes: T* ) : Unit
   def view: TrailView[ T ]
}

// XXX currently assumes a 1:1 mapping between tracks and trails
// which might not be the case in the future (several tracks could
// share a trail?)
class BasicTrailView[ T <: Stake[ T ]]( doc: Session, val trail: Trail[ T ])
extends TrailView[ T ] with TrailViewEditor[ T ] {
  import TrailView._

  private var selectedStakesVar = Set[ T ]()

  def selectedStakes = selectedStakesVar

/*
  private val tracksListener = (msg: AnyRef) => msg match {
      case tracks.ElementAdded( idx, t ) => addTrack( t )
      case tracks.ElementRemoved( idx, t ) => removeTrack( t )
    }
*/
  private val trailListener: Model.Listener = {
      case trail.StakesRemoved( span, stakes @ _* ) => deselect( stakes: _* )
  }

  // ---- constructor ----
  {
//      tracks.foreach( t => addTrack( t ))
//      tracks.addListener( tracksListener )
     trail.addListener( trailListener )
  }

  def view: TrailView[ T ] = this

  def dispose() {
//    tracks.removeListener( tracksListener )
//    tracks.foreach( t => removeTrack( t ))
     trail.removeListener( trailListener )
     selectedStakesVar = Set[ T ]()
  }

//  private def addTrack( t: Track ) {
//     t.trail.addListener( trailListener )
//  }

//  private def removeTrack( t: Track ) {
//     t.trail.removeListener( trailListener )
//  }

  def select(   stakes: T* ) { setSelection( stakes, state = true  )}
  def deselect( stakes: T* ) { setSelection( stakes, state = false )}

  private def unionSpan( stakes: T* ): SpanOrVoid = stakes match {
    case head +: tail => tail.foldLeft(head.span)(_ union _.span)
    case _ => Span.Void
  }

  private def setSelection( stakes: Seq[ T ], state: Boolean ) {
     val toChangeSet = stakes.toSet[ T ]
     val (selected, unselected) = toChangeSet.partition( x => selectedStakesVar.contains( x ))
//println( "setSelection : " + stakes + "; " + state + "; --> selected = " + selected.toList +
//        "; unselected = " + unselected.toList )

     val changedStakes = (if( state ) {
        if( unselected.isEmpty ) return
        selectedStakesVar ++= unselected
        unselected
     } else {
        if( selected.isEmpty ) return
        selectedStakesVar --= selected
        selected
     }).toList

      unionSpan( changedStakes: _* ) match {
        case sp @ Span(_, _) if sp.nonEmpty => dispatch( SelectionChanged( sp))
        case _ =>
      }
  }

  def isSelected( stake: T ) : Boolean = selectedStakesVar.contains( stake )

  def editor: Option[ TrailViewEditor[ T ]] = Some( this )
   // ---- TrailViewEditor ----

  def undoManager: UndoManager = doc.undoManager

  def editSelect( ce: AbstractCompoundEdit, stakes: T* ) { editSetSelection( ce, stakes, state = true )}

  def editDeselect( ce: AbstractCompoundEdit, stakes: T* ) { editSetSelection( ce, stakes, state = false )}

  private def editSetSelection( ce: AbstractCompoundEdit, stakes: Seq[ T ], state: Boolean ) {
    val sf = stakes.filterNot( stake => isSelected( stake ) == state )
    if( !sf.isEmpty ) {
        val edit = new SimpleEdit( "editStakeSelection", false ) {
            def apply() { setSelection( sf, state )}
            def unapply() { setSelection( sf, !state )}
        }
        ce.addPerform( edit )
    }
  }
}
