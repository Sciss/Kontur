/*
 *  Timeline.scala
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

package de.sciss.kontur.session

import javax.swing.undo.{ UndoManager }
import scala.xml.{ Node }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ Span }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }

object Timeline {
  case class SpanChanged( oldSpan: Span, newSpan: Span )
  case class RateChanged( oldRate: Double, newRate: Double )
}

trait Timeline extends SessionElement {
//  type Tr = Track[ _ <: Stake[ _ ]]

  def span: Span
  def rate: Double
  def tracks: SessionElementSeq[ Track[ _ <: Stake[ _ ]]]
  def transport: Option[ Transport ]
  def editor: Option[ TimelineEditor ]
}

trait TimelineEditor extends Editor {
  	 def editSpan( ce: AbstractCompoundEdit, newSpan: Span ) : Unit
	 def editRate( ce: AbstractCompoundEdit, newRate: Double ) : Unit
  }

object BasicTimeline {
    val XML_NODE = "timeline"
    
    def fromXML( doc: Session, node: Node ) : BasicTimeline = {
       val id       = (node \ "@id").text.toInt
       val tracksID = (node \ "tracks" \ "@id").text.toInt
       val tl       = new BasicTimeline( id, doc, tracksID )
       tl.fromXML( node )
       tl
    }

    def newEmpty( doc: Session ) =
        new BasicTimeline( doc.createID, doc, doc.createID )
}

class BasicTimeline( val id: Long, doc: Session, tracksID: Long )
extends Timeline with Renameable with TimelineEditor {
  import Timeline._

  private var spanVar = new Span()
  private var rateVar = 44100.0
  protected var nameVar = "Timeline"
//  private val sync = new AnyRef
  private val transportVar = new BasicTransport( this )

  def undoManager: UndoManager = doc.getUndoManager

  def transport: Option[ Transport ] = Some( transportVar )
  
  val tracks = new Tracks( tracksID, doc, this )
//  val audioTrail  = new AudioTrail

  def toXML = <timeline id={id.toString}>
  <name>{name}</name>
  <span start={spanVar.start.toString} stop={spanVar.stop.toString}/>
  <rate>{rate}</rate>
  {tracks.toXML}
</timeline>

  def fromXML( node: Node ) {
      nameVar   = (node \ "name").text
      val spanN = node \ "span"
      spanVar   = new Span( (spanN \ "@start").text.toLong, (spanN \ "@stop").text.toLong )
      rateVar   = (node \ "rate").text.toDouble
      tracks.fromXML( node )
  }

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

class Timelines( doc: Session )
extends BasicSessionElementSeq[ Timeline ]( doc, "Timelines" ) {
  val id = -1L
  def toXML =
    <timelines>
       {innerToXML}
    </timelines>

  def fromXML( parent: Node ) {
     val innerXML = SessionElement.getSingleXML( parent, "timelines" )
     innerFromXML( innerXML )
  }

  protected def elementsFromXML( node: Node ) : Seq[ Timeline ] =
     (node \ BasicTimeline.XML_NODE).map( n => BasicTimeline.fromXML( doc, n ))
}