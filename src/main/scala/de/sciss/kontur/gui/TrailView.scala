/*
 *  TrailView.scala
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
package gui

import edit.{ Editor, SimpleEdit }
import session.{ Session, Stake, Trail }
import util.Model
import de.sciss.span.Span
import de.sciss.span.Span.SpanOrVoid
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

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

  def dispose(): Unit = {
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

  def select  (stakes: T*): Unit = setSelection(stakes, state = true )
  def deselect(stakes: T*): Unit = setSelection(stakes, state = false)

  private def unionSpan(stakes: T*): SpanOrVoid = stakes match {
    case head +: tail => tail.foldLeft(head.span)(_ union _.span)
    case _ => Span.Void
  }

  private def setSelection(stakes: Seq[T], state: Boolean): Unit = {
    val toChangeSet = stakes.toSet[T]
    val (selected, unselected) = toChangeSet.partition(x => selectedStakesVar.contains(x))
    //println( "setSelection : " + stakes + "; " + state + "; --> selected = " + selected.toList +
    //        "; unselected = " + unselected.toList )

    val changedStakes = (if (state) {
      if (unselected.isEmpty) return
      selectedStakesVar ++= unselected
      unselected
    } else {
      if (selected.isEmpty) return
      selectedStakesVar --= selected
      selected
    }).toList

    unionSpan(changedStakes: _*) match {
      case sp @ Span(_, _) if sp.nonEmpty => dispatch(SelectionChanged(sp))
      case _ =>
    }
  }

  def isSelected(stake: T): Boolean = selectedStakesVar.contains(stake)

  def editor: Option[TrailViewEditor[T]] = Some(this)

  // ---- TrailViewEditor ----

  def undoManager: UndoManager = doc.undoManager

  def editSelect(ce: AbstractCompoundEdit, stakes: T*): Unit =
    editSetSelection(ce, stakes, state = true)

  def editDeselect(ce: AbstractCompoundEdit, stakes: T*): Unit =
    editSetSelection(ce, stakes, state = false)

  private def editSetSelection(ce: AbstractCompoundEdit, stakes: Seq[T], state: Boolean): Unit = {
    val sf = stakes.filterNot(stake => isSelected(stake) == state)
    if (!sf.isEmpty) {
      val edit = new SimpleEdit("editStakeSelection", false) {
        def apply  (): Unit = setSelection(sf,  state)
        def unapply(): Unit = setSelection(sf, !state)
      }
      ce.addPerform(edit)
    }
  }
}
