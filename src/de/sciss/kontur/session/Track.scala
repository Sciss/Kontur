/*
 *  Track.scala
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

import java.awt.datatransfer.{ DataFlavor }
import java.io.{ IOException }
import scala.xml.{ Node }
import de.sciss.kontur.edit.{ Editor }
import de.sciss.kontur.util.{ SerializerContext }

object Track {
   val flavor = new DataFlavor( classOf[ Track ], "Track" )
}

trait Track extends SessionElement {
   type T <: Stake[ T ]
   def trail: Trail[ T ]
   def editor: Option[ TrackEditor ]
}

trait TrackEditor extends Editor {
  
}

class Tracks( doc: Session, tl: BasicTimeline )
extends BasicSessionElementSeq[ Track ]( doc, "Tracks" ) {

   def toXML( c: SerializerContext ) =
      <tracks id={c.id( this ).toString}>
      {innerToXML( c )}
      </tracks>

   def fromXML( c: SerializerContext, parent: Node ) {
      val innerXML = SessionElement.getSingleXML( parent, "tracks" )
      innerFromXML( c, innerXML )
   }

   protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ Track ] =
      node.child.filter( _.label != "#PCDATA" ).map( ch => {
         (ch \ "@idref").headOption.map( attr => {
            c.byID[ Track ]( attr.text.toLong )
         }).getOrElse( ch.label match {
            case AudioTrack.XML_NODE => AudioTrack.fromXML( c, ch, doc, tl )
            case lb => throw new IOException( "Unknown track type '" + lb + "'" )
         })
      })
}