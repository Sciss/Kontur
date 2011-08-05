/*
 *  Timeline.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.kontur.session

import javax.swing.undo.UndoManager
import scala.xml.Node
import de.sciss.app.AbstractCompoundEdit
import de.sciss.io.Span
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.util.SerializerContext

object Timeline {
  case class SpanChanged( oldSpan: Span, newSpan: Span )
  case class RateChanged( oldRate: Double, newRate: Double )
}

trait Timeline extends SessionElement {
//  type Tr = Track[ _ <: Stake[ _ ]]

  def span: Span
  def rate: Double
  def tracks: SessionElementSeq[ Track ]
  def transport: Option[ Transport ]
  def editor: Option[ TimelineEditor ]
}

trait TimelineEditor extends Editor {
  	 def editSpan( ce: AbstractCompoundEdit, newSpan: Span ) : Unit
	 def editRate( ce: AbstractCompoundEdit, newRate: Double ) : Unit
  }

object BasicTimeline {
    val XML_NODE = "timeline"
    
    def fromXML( c: SerializerContext, doc: Session, node: Node ) : BasicTimeline = {
       val tl = new BasicTimeline( doc )
       c.id( tl, node )
       tl.fromXML( c, node )
       tl
    }

    def newEmpty( doc: Session ) = new BasicTimeline( doc )
}

class BasicTimeline( doc: Session )
extends Timeline with Renamable with TimelineEditor {
   import Timeline._

   private var spanVar = new Span()
   private var rateVar = 44100.0
   protected var nameVar = "Timeline"
   private val transportVar = new BasicTransport( this )

   def undoManager: UndoManager = doc.getUndoManager

   def transport: Option[ Transport ] = Some( transportVar )

   val tracks = new Tracks( doc )

   def toXML( c: SerializerContext ) = <timeline id={c.id( this ).toString}>
   <name>{name}</name>
   <span start={spanVar.start.toString} stop={spanVar.stop.toString}/>
   <rate>{rate}</rate>
   {tracks.toXML( c )}
</timeline>

   def fromXML( c: SerializerContext, node: Node ) {
      nameVar   = (node \ "name").text
      val spanN = node \ "span"
      spanVar   = new Span( (spanN \ "@start").text.toLong, (spanN \ "@stop").text.toLong )
      rateVar   = (node \ "rate").text.toDouble
      tracks.fromXML( c, node )
   }

   def span: Span = spanVar
   def span_=( newSpan: Span ) {
      if( newSpan != spanVar ) {
         val change = SpanChanged( spanVar, newSpan )
         spanVar = newSpan
         dispatch( change )
      }
   }

   def rate: Double = rateVar

   def rate_=( newRate: Double ) {
      if( newRate != rateVar ) {
         val change = RateChanged( rateVar, newRate )
         rateVar = newRate
         dispatch( change )
      }
   }

   def editor: Option[ TimelineEditor ] = Some( this )

   // ---- TimelineEditor ----

   protected def editRenameName = "editRenameTimeline"

   def editSpan( ce: AbstractCompoundEdit, newSpan: Span ) {
      val edit = new SimpleEdit( "editTimelineSpan" ) {
         lazy val oldSpan = span
         def apply() { oldSpan; span = newSpan }
         def unapply() { span = oldSpan }
      }
      ce.addPerform( edit )
   }

   def editRate( ce: AbstractCompoundEdit, newRate: Double ) {
      val edit = new SimpleEdit( "editTimelineRate" ) {
         lazy val oldRate = rate
         def apply() { oldRate; rate = newRate }
         def unapply() { rate = oldRate }
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

  def fromXML( c: SerializerContext, parent: Node ) {
     val innerXML = SessionElement.getSingleXML( parent, "timelines" )
     innerFromXML( c, innerXML )
  }

  protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ Timeline ] =
     (node \ BasicTimeline.XML_NODE).map( n => BasicTimeline.fromXML( c, doc, n ))
}