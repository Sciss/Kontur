/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import javax.swing.undo.{ UndoManager }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.kontur.edit.{ Editor }
import de.sciss.kontur.session.{ Session, Timeline }
import de.sciss.kontur.util.{ Model }

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
   def editPosition( id: AbstractCompoundEdit, newPos: Long ) : Unit
   def editScroll( id: AbstractCompoundEdit, newSpan: Span ) : Unit
   def editSelect( id: AbstractCompoundEdit, newSpan: Span ) : Unit
}

class BasicTimelineView( doc: Session, val timeline: Timeline )
extends TimelineView with TimelineViewEditor {
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

  val cursor: TimelineCursor       = new BasicTimelineCursor( timeline )
  val selection: TimelineSelection = new BasicTimelineSelection( timeline )

  // ---- constructor ----
  {
    cursor.addListener( dispatch( _ ))
    selection.addListener( dispatch( _ ))
    timeline.addListener( dispatch( _ )) // XXX a little dangerous
  }

  def dispose {
    timeline.removeListener( dispatch( _ ))
  }

  def editor: Option[ TimelineViewEditor ] = Some( this )
    // ---- TimelineViewEditor ----

     def undoManager: UndoManager = doc.getUndoManager

  	 def editPosition( id: AbstractCompoundEdit, newPos: Long ) {
       // XXX TODO
     }

	 def editScroll( id: AbstractCompoundEdit, newSpan: Span ) {
       // XXX TODO
     }

	 def editSelect( id: AbstractCompoundEdit, newSpan: Span ) {
       // XXX TODO
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
  case class SpanChanged( oldSpan: Span, newSpan: Span )
}

trait TimelineSelection extends Model {
  def span: Span
}

class BasicTimelineSelection( val timeline: Timeline )
extends TimelineSelection {
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
