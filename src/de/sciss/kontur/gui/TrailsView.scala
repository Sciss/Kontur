/*
 *  TrailsView.scala
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

package de.sciss.kontur.gui

import javax.swing.undo.{ UndoManager }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.session.{ Session, SessionElementSeq, Stake, Track, Trail }
import de.sciss.kontur.util.{ Model }

object TrailsView {
    case class SelectionChanged( span: Span, stakes: Stake* )
}

trait TrailsView extends Model {
    def isSelected( s: Stake ) : Boolean
    def editor: Option[ TrailsViewEditor ]
}

trait TrailsViewEditor extends Editor {
   def editSelect( ce: AbstractCompoundEdit, stakes: Stake* ) : Unit
   def editDeselect( ce: AbstractCompoundEdit, stakes: Stake* ) : Unit
}

// XXX currently assumes a 1:1 mapping between tracks and trails
// which might not be the case in the future (several tracks could
// share a trail?)
class BasicTrailsView( doc: Session, val tracks: SessionElementSeq[ Track ])
extends TrailsView with TrailsViewEditor {
  import TrailsView._

  private var selectedStakes = Set[ Stake ]()

  private val tracksListener = (msg: AnyRef) => msg match {
      case tracks.ElementAdded( idx, t ) => addTrack( t )
      case tracks.ElementRemoved( idx, t ) => removeTrack( t )
    }

  private val trailListener = (msg: AnyRef) => msg match {
      case Trail.StakesRemoved( span, stakes @ _* ) => deselect( stakes: _* )
  }

  // ---- constructor ----
  {
      tracks.foreach( t => addTrack( t ))
      tracks.addListener( tracksListener )
  }

  def dispose {
    tracks.removeListener( tracksListener )
    tracks.foreach( t => removeTrack( t ))
    selectedStakes = Set[ Stake ]()
  }

  private def addTrack( t: Track ) {
     t.trail.addListener( trailListener )
  }

  private def removeTrack( t: Track ) {
     t.trail.removeListener( trailListener )
  }

  def select( stakes: Stake* ) : Unit = setSelection( stakes, true )
  def deselect( stakes: Stake* ) : Unit = setSelection( stakes, false )

  private def unionSpan( stakes: Stake* ) : Span = {
     var span = stakes.headOption.map( _.span ) getOrElse new Span()
     stakes.foreach( s => (span = span.union( s.span )))
     span
  }

  private def setSelection( stakes: Seq[ Stake ], state: Boolean ) {
     val toChangeSet = stakes.toSet[ Stake ]
     val (selected, unselected) = selectedStakes.partition( x => toChangeSet.contains( x ))

     val changedStakes = (if( state ) {
        if( unselected.isEmpty ) return
        selectedStakes ++= unselected
        unselected
     } else {
        if( selected.isEmpty ) return
        selectedStakes --= selected
        selected
     }).toList

     if( changedStakes.isEmpty ) return
     dispatch( SelectionChanged( unionSpan( changedStakes: _* ), changedStakes: _* ))
  }

  def isSelected( stake: Stake ) : Boolean = selectedStakes.contains( stake )

  def editor: Option[ TrailsViewEditor ] = Some( this )
   // ---- TrailsViewEditor ----

  def undoManager: UndoManager = doc.getUndoManager

  def editSelect( ce: AbstractCompoundEdit, stakes: Stake* ) : Unit =
    editSetSelection( ce, stakes, true )

  def editDeselect( ce: AbstractCompoundEdit, stakes: Stake* ) : Unit =
    editSetSelection( ce, stakes, false )

  private def editSetSelection( ce: AbstractCompoundEdit, stakes: Seq[ Stake ], state: Boolean ) {
    val sf = stakes.filterNot( stake => isSelected( stake ) == state )
    if( !sf.isEmpty ) {
        val edit = new SimpleEdit( "editStakeSelection", false ) {
            def apply { setSelection( sf, state )}
            def unapply { setSelection( sf, !state )}
        }
        ce.addPerform( edit )
    }
  }

  private class StakeView {
    var selected = false
  }
}