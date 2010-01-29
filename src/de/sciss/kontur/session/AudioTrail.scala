/*
 *  AudioTrail.scala
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

import java.io.{ IOException }
import scala.xml.{ Node }
import de.sciss.io.{ Span }
import scala.math._

object AudioRegion {
   val XML_NODE = "stake"

    def fromXML( doc: Session, node: Node ) : AudioRegion = {
        val name   = (node \ "name").text
        val spanN  = node \ "span"
        val span   = new Span( (spanN \ "@start").text.toLong, (spanN \ "@stop").text.toLong )
        val afID   = (node \ "audioFile" \ "@idref").text.toLong
        val af     = doc.audioFiles.getByID( afID ) getOrElse {
            throw new IOException( "Session corrupt. Referencing an invalid audio file #" + afID )}
        val offset = (node \ "offset").text.toLong
        val gain   = (node \ "gain").headOption.map( _.text.toFloat ) getOrElse 1f
        val fadeIn = (node \ "fadeIn"  \ "fade").headOption.map( n => FadeSpec.fromXML( n ))
        val fadeOut= (node \ "fadeOut" \ "fade").headOption.map( n => FadeSpec.fromXML( n ))

        new AudioRegion( span, name, af, offset, gain, fadeIn, fadeOut )
    }
}

case class AudioRegion( span: Span, name: String, audioFile: AudioFileElement,
                        offset: Long, gain: Float, fadeIn: Option[ FadeSpec ],
                        fadeOut: Option[ FadeSpec ])
extends RegionTrait[ AudioRegion ] with SlidableStake[ AudioRegion ] {
  def toXML = <stake>
  <name>{name}</name>
  <span start={span.start.toString} stop={span.stop.toString}/>
  <audioFile idref={audioFile.id.toString}/>
  <offset>{offset}</offset>
  {if( gain != 0f ) Some( <gain>{gain}</gain> ) else None}
  {if( fadeIn.isDefined )  Some( <fadeIn>{fadeIn.map( _.toXML )}</fadeIn> ) else None}
  {if( fadeOut.isDefined ) Some( <fadeOut>{fadeOut.map( _.toXML )}</fadeOut> ) else None}
</stake>

   def move( delta: Long ) : AudioRegion = copy( span = span.shift( delta ))

   def moveOuter( delta: Long ) : AudioRegion = {
      val d = max( -offset, min( audioFile.numFrames - offset - span.getLength, delta ))
      if( d == 0 ) this
      else copy( span = span.shift( d ), offset = offset + d )
   }
   
   def moveInner( delta: Long ) : AudioRegion = {
      val d = max( -offset, min( audioFile.numFrames - offset - span.getLength, delta ))
      if( d == 0 ) this
      else copy( offset = offset + d )
   }
   
   def moveStart( delta: Long ) : AudioRegion = {
      val d = max( -offset, min( span.getLength - 1, delta ))
      if( d == 0 ) this
      else copy( span = span.replaceStart( span.start + d ), offset = offset + d )
   }

   def moveStop( delta: Long ) : AudioRegion = {
      val d = max( -(span.getLength - 1), min( audioFile.numFrames - offset - span.getLength, delta ))
      if( d == 0 ) this
      else copy( span = span.replaceStop( span.stop + d ))
   }

/*
    def replaceStart( newStart: Long ): AudioRegion =
      copy( span = new Span( newStart, span.stop ), offset = offset + newStart - span.start )

    def replaceStop( newStop: Long ): AudioRegion =
      copy( span = new Span( span.start, newStop ))
*/
}

class AudioTrail( doc: Session ) extends BasicTrail[ AudioRegion ]( doc ) {
  def toXML = <trail>
  {getAll().map(_.toXML)}
</trail>

   def fromXML( parent: Node ) {
      val node = SessionElement.getSingleXML( parent, "trail" )
      add( (node \ AudioRegion.XML_NODE).map( n => AudioRegion.fromXML( doc, n )): _* )
   }
}
