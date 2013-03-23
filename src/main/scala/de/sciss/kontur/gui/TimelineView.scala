/*
 *  TimelineView.scala
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

import edit.{Editor, SimpleEdit}
import session.{Session, Timeline}
import util.Model
import de.sciss.span.Span
import Span.SpanOrVoid
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

object TimelineView {
   case class SpanChanged( oldSpan: Span, newSpan: Span )
}

trait TimelineView extends Model {
   def timeline: Timeline
   def span: Span
   def cursor: TimelineCursor
   def selection: TimelineSelection
   def editor: Option[ TimelineViewEditor ]
}

trait TimelineViewEditor extends Editor {
  def editPosition(ce: AbstractCompoundEdit, newPos: Long): Unit
  def editScroll  (ce: AbstractCompoundEdit, newSpan: Span): Unit
  def editSelect  (ce: AbstractCompoundEdit, newSpan: SpanOrVoid): Unit
}

class BasicTimelineView( doc: Session, val timeline: Timeline )
extends TimelineView with TimelineViewEditor {
  import TimelineView._

  private var spanVar = timeline.span

  def span: Span = spanVar
  def span_=( newSpan: Span ) {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
//println( "dispatch " + change )
        dispatch( change )
      }
  }

  private val basicCsr  = new BasicTimelineCursor( timeline )
  def cursor: TimelineCursor = basicCsr
  private val basicSel  = new BasicTimelineSelection( timeline )
  def selection: TimelineSelection = basicSel

  private val forward : Model.Listener = { case msg => dispatch( msg )}

  // ---- constructor ----
  {
    cursor.addListener( forward )
    selection.addListener( forward )
    timeline.addListener( forward ) // XXX a little dangerous
  }

  def dispose() {
    timeline.removeListener( forward )
  }

  def editor: Option[ TimelineViewEditor ] = Some( this )
    // ---- TimelineViewEditor ----

  def undoManager: UndoManager = doc.undoManager

  def editPosition( ce: AbstractCompoundEdit, newPos: Long ) {
    val edit = new SimpleEdit( "editTimelinePosition", false ) {
       lazy val oldPos = basicCsr.position
       def apply() { oldPos; basicCsr.position = newPos }
       def unapply() { basicCsr.position = oldPos }
    }
    ce.addPerform( edit )
   }

  def editScroll( ce: AbstractCompoundEdit, newSpan: Span ) {
//     println( "editScroll " + newSpan )
//     (new Throwable).printStackTrace()

    val edit = new SimpleEdit( "editTimelineScroll", false ) {
       lazy val oldSpan = span
       def apply() {
          oldSpan
          span = newSpan
       }
       def unapply() { span = oldSpan }
    }
    ce.addPerform( edit )
   }

   def editSelect( ce: AbstractCompoundEdit, newSpan: SpanOrVoid ) {
      val edit = new SimpleEdit( "editTimelineSelection", false ) {
        lazy val oldSpan = basicSel.span
        def apply() { oldSpan; basicSel.span = newSpan }
        def unapply() { basicSel.span = oldSpan }
      }
      ce.addPerform( edit )
   }
}

object TimelineCursor {
  case class PositionChanged( oldPosition: Long, newPosition: Long )
}

trait TimelineCursor extends Model {
  def position: Long
}

class BasicTimelineCursor( val timeline: Timeline )
extends TimelineCursor {
  import TimelineCursor._

  private var positionVar = 0L

  def position: Long = positionVar
  def position_=( newPosition: Long ) {
      if( newPosition != positionVar ) {
        val change = PositionChanged( positionVar, newPosition )
        positionVar = newPosition
        dispatch( change )
      }
  }
}

object TimelineSelection {
  case class SpanChanged( oldSpan: SpanOrVoid, newSpan: SpanOrVoid )
}

trait TimelineSelection extends Model {
  def span: SpanOrVoid
}

class BasicTimelineSelection( val timeline: Timeline )
extends TimelineSelection {
  import TimelineSelection._

  private var spanVar = Span.Void: SpanOrVoid

  def span: SpanOrVoid = spanVar
  def span_=( newSpan: SpanOrVoid ) {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
        dispatch( change )
      }
  }
}
