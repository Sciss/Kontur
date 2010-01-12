/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import javax.swing.undo.{ UndoManager }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
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
   def editPosition( ce: AbstractCompoundEdit, newPos: Long ) : Unit
   def editScroll( ce: AbstractCompoundEdit, newSpan: Span ) : Unit
   def editSelect( ce: AbstractCompoundEdit, newSpan: Span ) : Unit
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
        dispatch( change )
      }
  }

  private val basicCsr  = new BasicTimelineCursor( timeline )
  def cursor: TimelineCursor = basicCsr
  private val basicSel  = new BasicTimelineSelection( timeline )
  def selection: TimelineSelection = basicSel

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

  def editPosition( ce: AbstractCompoundEdit, newPos: Long ) {
    val edit = new SimpleEdit( "editTimelinePosition" ) {
       lazy val oldPos = basicCsr.position
       def apply { oldPos; basicCsr.position = newPos }
       def unapply { basicCsr.position = oldPos }
    }
    ce.addPerform( edit )
   }

  def editScroll( ce: AbstractCompoundEdit, newSpan: Span ) {
    val edit = new SimpleEdit( "editTimelineScroll" ) {
       lazy val oldSpan = span
       def apply { oldSpan; span = newSpan }
       def unapply { span = oldSpan }
    }
    ce.addPerform( edit )
   }

   def editSelect( ce: AbstractCompoundEdit, newSpan: Span ) {
      val edit = new SimpleEdit( "editTimelineSelection" ) {
        lazy val oldSpan = basicSel.span
        def apply { oldSpan; basicSel.span = newSpan }
        def unapply { basicSel.span = oldSpan }
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
