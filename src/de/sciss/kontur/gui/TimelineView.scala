/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import javax.swing.undo.{ UndoManager }
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ Session, TimelineElement }
import de.sciss.kontur.util.{ Model }

object TimelineView {
  case class SpanChanged( oldSpan: Span, newSpan: Span )
}

class TimelineView( doc: Session, val timeline: TimelineElement )
extends Model {
  import TimelineView._

  private var spanVar = new Span

  def span: Span = spanVar
  def span_=( newSpan: Span ) {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
        dispatch( change )
      }
  }

  val cursor    = new TimelineCursor( timeline )
  val selection = new TimelineSelection( timeline )

  // ---- constructor ----
  {
    cursor.addListener( dispatch (_ ))
    selection.addListener( dispatch (_ ))
  }

  class Editor extends de.sciss.kontur.edit.Editor {
     def undoManager: UndoManager = doc.getUndoManager

  	 def editPosition( id: Client, newPos: Long ) {
       // XXX TODO
     }

	 def editScroll( id: Client, newSpan: Span ) {
       // XXX TODO
     }

	 def editSelect( id: Client, newSpan: Span ) {
       // XXX TODO
     }
  }
}

object TimelineCursor {
  case class PositionChanged( oldPosition: Long, newPosition: Long )
}

class TimelineCursor( val timeline: TimelineElement )
extends Model {
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
  case class SpanChanged( oldSpan: Span, newSpan: Span )
}

class TimelineSelection( val timeline: TimelineElement )
extends Model {
  import TimelineSelection._

  private var spanVar = new Span

  def span: Span = spanVar
  def span_=( newSpan: Span ) {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
        dispatch( change )
      }
  }
}
