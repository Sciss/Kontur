/*
 *  Timeline.scala
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
package session

import xml.Node
import edit.{Editor, SimpleEdit}
import util.SerializerContext
import de.sciss.span.Span
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

object Timeline {
  final case class SpanChanged(oldSpan: Span, newSpan: Span)
  final case class RateChanged(oldRate: Double, newRate: Double)
}

trait Timeline extends SessionElement {
  def span: Span
  def rate: Double

  def tracks: SessionElementSeq[Track]
  def transport: Option[Transport]
  def editor: Option[TimelineEditor]
}

trait TimelineEditor extends Editor {
  def editSpan(ce: AbstractCompoundEdit, newSpan: Span  ): Unit
  def editRate(ce: AbstractCompoundEdit, newRate: Double): Unit
}

object BasicTimeline {
  val XML_NODE = "timeline"

  def fromXML(c: SerializerContext, doc: Session, node: Node): BasicTimeline = {
    val tl = new BasicTimeline(doc)
    c.id(tl, node)
    tl.fromXML(c, node)
    tl
  }

  def newEmpty(doc: Session) = new BasicTimeline(doc)
}

final class BasicTimeline( doc: Session )
extends Timeline with Renamable with TimelineEditor {
   import Timeline._

   private var spanVar = Span(0L, 0L)
   private var rateVar = 44100.0
   protected var nameVar = "Timeline"
   private val transportVar = new BasicTransport( this )

   def undoManager: UndoManager = doc.undoManager

   def transport: Option[ Transport ] = Some( transportVar )

   val tracks = new Tracks( doc )

   def toXML( c: SerializerContext ) = <timeline id={c.id( this ).toString}>
   <name>{name}</name>
   <span start={spanVar.start.toString} stop={spanVar.stop.toString}/>
   <rate>{rate}</rate>
   {tracks.toXML( c )}
</timeline>

   def fromXML( c: SerializerContext, node: Node ): Unit = {
      nameVar   = (node \ "name").text
      val spanN = node \ "span"
      spanVar   = Span( (spanN \ "@start").text.toLong, (spanN \ "@stop").text.toLong )
      rateVar   = (node \ "rate").text.toDouble
      tracks.fromXML( c, node )
   }

   def span: Span = spanVar
   def span_=( newSpan: Span ): Unit =
      if( newSpan != spanVar ) {
         val change = SpanChanged( spanVar, newSpan )
         spanVar = newSpan
         dispatch( change )
      }

   def rate: Double = rateVar

   def rate_=( newRate: Double ): Unit =
      if( newRate != rateVar ) {
         val change = RateChanged( rateVar, newRate )
         rateVar = newRate
         dispatch( change )
      }

   def editor: Option[ TimelineEditor ] = Some( this )

   // ---- TimelineEditor ----

   protected def editRenameName = "editRenameTimeline"

   def editSpan( ce: AbstractCompoundEdit, newSpan: Span ): Unit = {
      val edit = new SimpleEdit( "editTimelineSpan" ) {
         lazy val oldSpan = span
         def apply(): Unit = { oldSpan; span = newSpan }
         def unapply(): Unit = span = oldSpan
      }
      ce.addPerform( edit )
   }

   def editRate( ce: AbstractCompoundEdit, newRate: Double ): Unit = {
      val edit = new SimpleEdit( "editTimelineRate" ) {
         lazy val oldRate = rate
         def apply(): Unit = { oldRate; rate = newRate }
         def unapply(): Unit = rate = oldRate
      }
      ce.addPerform( edit )
   }
}

class Timelines( doc: Session )
extends BasicSessionElementSeq[ Timeline ]( doc, "Timelines" ) {
  def toXML( c: SerializerContext ) =
    <timelines>
       {innerToXML( c )}
    </timelines>

  def fromXML( c: SerializerContext, parent: Node ): Unit = {
     val innerXML = SessionElement.getSingleXML( parent, "timelines" )
     innerFromXML( c, innerXML )
  }

  protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ Timeline ] =
     (node \ BasicTimeline.XML_NODE).map( n => BasicTimeline.fromXML( c, doc, n ))
}