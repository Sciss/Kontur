/*
 *  AudioTrail.scala
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

import scala.xml.{ Node, Null }
import de.sciss.io.Span
import de.sciss.kontur.util.SerializerContext
import math._

object AudioRegion {
   val XML_NODE = "stake"

   def fromXML( c: SerializerContext, doc: Session, node: Node ) : AudioRegion = {
      val name    = (node \ "name").text
      val spanN   = node \ "span"
      val span    = new Span( (spanN \ "@start").text.toLong, (spanN \ "@stop").text.toLong )
      val af      = c.byID[ AudioFileElement ]( node \ "audioFile" )
      val offset  = (node \ "offset").text.toLong
      val gain    = (node \ "gain").headOption.map( _.text.toFloat ) getOrElse 1f
      val fadeIn  = (node \ "fadeIn"  \ "fade").headOption.map( n => FadeSpec.fromXML( n ))
      val fadeOut = (node \ "fadeOut" \ "fade").headOption.map( n => FadeSpec.fromXML( n ))
      val muted   = (node \ "muted").nonEmpty

      new AudioRegion( span, name, af, offset, gain, fadeIn, fadeOut, muted )
   }
}

case class AudioRegion( span: Span, name: String, audioFile: AudioFileElement,
                        offset: Long = 0L, gain: Float = 1f, fadeIn: Option[ FadeSpec ] = None,
                        fadeOut: Option[ FadeSpec ] = None, muted: Boolean = false )
extends RegionTrait[ AudioRegion ] with SlidableStake[ AudioRegion ] with MuteableStake[ AudioRegion ] {
   def toXML( c: SerializerContext ) = <stake>
      <name>{name}</name>
      <span start={span.start.toString} stop={span.stop.toString}/>
      <audioFile idref={c.id( audioFile ).toString}/>
      <offset>{offset}</offset>
      {if( gain != 0f ) <gain>{gain}</gain> else Null}
      {fadeIn.map( f => <fadeIn>{ f.toXML }</fadeIn>) getOrElse Null}
      {fadeOut.map( f => <fadeOut>{ f.toXML }</fadeOut>) getOrElse Null}
      {if( muted ) <muted/> else Null}
   </stake>

   def replaceGain( newGain: Float ) : AudioRegion =
      copy( gain = newGain )

   def replaceFadeIn( newFadeIn: Option[ FadeSpec ]) : AudioRegion =
      copy( fadeIn = newFadeIn )

   def replaceFadeOut( newFadeOut: Option[ FadeSpec ]) : AudioRegion =
      copy( fadeOut = newFadeOut )

   def replaceAudioFile( newFile: AudioFileElement ) : AudioRegion = {
      if( newFile.numFrames >= span.getLength + offset ) {
         copy( audioFile = newFile )
      } else {
         val newOffset = min( offset, newFile.numFrames - 1 )
         val newStop   = span.start + min( newFile.numFrames - newOffset, span.getLength )
         copy( audioFile = newFile, span = new Span( span.start, newStop ), offset = newOffset )
      }
   }

   def move( delta: Long ) : AudioRegion = copy( span = span.shift( delta ))

   def rename( newName: String ) : AudioRegion = copy( name = newName )

   def mute( newMuted: Boolean ) : AudioRegion = copy( muted = newMuted )

   override def split( pos: Long ) : (AudioRegion, AudioRegion) = {
      val left = {
         val d = max( -(span.getLength - 1), min( audioFile.numFrames - offset - span.getLength, pos - span.stop ))
         if( d == 0 ) this
         else copy( span = span.replaceStop( span.stop + d ), fadeOut = None )
      }
      val right = {
         val d = max( -offset, min( span.getLength - 1, pos - span.start ))
         if( d == 0 ) this
         else copy( span = span.replaceStart( span.start + d ), offset = offset + d, fadeIn = None )
      }
      (left, right)
   }

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
  def toXML( c: SerializerContext ) = <trail>
  {getAll().map( _.toXML( c ))}
</trail>

   def fromXML( c: SerializerContext, parent: Node ) {
      val node = SessionElement.getSingleXML( parent, "trail" )
      add( (node \ AudioRegion.XML_NODE).map( n => AudioRegion.fromXML( c, doc, n )): _* )
   }
}
