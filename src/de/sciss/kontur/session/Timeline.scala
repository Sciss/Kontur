/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import javax.swing.undo.{ UndoManager }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }

object Timeline {
  case class SpanChanged( oldSpan: Span, newSpan: Span )
  case class RateChanged( oldRate: Double, newRate: Double )
}

trait Timeline extends SessionElement {
  def span: Span
  def rate: Double
  def tracks: SessionElementSeq[ Track ]
  def editor: Option[ TimelineEditor ]
}

trait TimelineEditor extends Editor {
  	 def editSpan( ce: AbstractCompoundEdit, newSpan: Span ) : Unit
	 def editRate( ce: AbstractCompoundEdit, newRate: Double ) : Unit
  }


class BasicTimeline( doc: Session )
extends Timeline with Renameable with TimelineEditor {
  import Timeline._

  private var spanVar = new Span()
  private var rateVar = 44100.0
  protected var nameVar = "Untitled"
//  private val sync = new AnyRef

  def undoManager: UndoManager = doc.getUndoManager

  val tracks      = new BasicSessionElementSeq[ Track ]( doc, "Tracks" )
//  val audioTrail  = new AudioTrail

  def span: Span = spanVar
  def span_=( newSpan: Span ) {
//    sync.synchronized {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
        dispatch( change )
      }
//    }
  }

  def rate: Double = // sync.synchronized {
    rateVar
  // } // sync necessary?

  def rate_=( newRate: Double ) {
//    sync.synchronized {
      if( newRate != rateVar ) {
        val change = RateChanged( rateVar, newRate )
        rateVar = newRate
        dispatch( change )
      }
//    }
  }

  def editor: Option[ TimelineEditor ] = Some( this )

  // ---- TimelineEditor ----
  def editSpan( ce: AbstractCompoundEdit, newSpan: Span ) {
    val edit = new SimpleEdit( "editTimelineSpan" ) {
       lazy val oldSpan = span
       def apply { oldSpan; span = newSpan }
       def unapply { span = oldSpan }
    }
    ce.addPerform( edit )
  }

  def editRate( ce: AbstractCompoundEdit, newRate: Double ) {
    val edit = new SimpleEdit( "editTimelineRate" ) {
       lazy val oldRate = rate
       def apply { oldRate; rate = newRate }
       def unapply { rate = oldRate }
    }
    ce.addPerform( edit )
  }
}