/*
 *  AudioFileElement.scala
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

import java.io.{ File, IOException }
import scala.xml.{ Node }
import de.sciss.io.{ AudioFile, AudioFileDescr }

import de.sciss.app.{ AbstractApplication }
import de.sciss.kontur.io.{ SonagramOverview }
import de.sciss.kontur.util.{ PrefsUtil }

object AudioFileElement {
    val XML_NODE = "audioFile"

   def fromXML( doc: Session, node: Node ) : AudioFileElement = {
      val id            = (node \ "@id").text.toInt
      val path          = new File( (node \ "path").text )
      val numFrames     = (node \ "numFrames").text.toLong
      val numChannels   = (node \ "numChannels").text.toInt
      val sampleRate    = (node \ "sampleRate").text.toDouble
      val afe           = new AudioFileElement( id, path, numFrames, numChannels, sampleRate )
//    afe.fromXML( node )
      afe
   }

   @throws( classOf[ IOException ])
   def fromPath( doc: Session, path: File ) : AudioFileElement = {
      val af      = AudioFile.openAsRead( path )
      val descr   = af.getDescr
      af.close
      new AudioFileElement( doc.createID, path, descr.length, descr.channels, descr.rate )
   }
}

class AudioFileElement( val id: Long, val path: File, val numFrames: Long,
                        val numChannels: Int, val sampleRate: Double )
extends SessionElement {
  def name: String = path.getName

  def toXML = <audioFile id={id.toString}>
  <path>{path.getAbsolutePath}</path>
  <numFrames>{numFrames}</numFrames>
  <numChannels>{numChannels}</numChannels>
  <sampleRate>{sampleRate}</sampleRate>
</audioFile>

/*
  lazy val descr: Option[ AudioFileDescr ] = {
      try {
        val af = AudioFile.openAsRead( path )
        af.close
        Some( af.getDescr )
      }
      catch { case e1: IOException => None }
  }
*/

   // XXX it would be good to keep this separated in gui package
   lazy val sona : Option[ SonagramOverview ] = {
      try {
         Some( SonagramOverview.fromPath( path ))
      }
      catch { case e1: IOException => None }
   }
}

class AudioFileSeq( doc: Session )
extends BasicSessionElementSeq[ AudioFileElement ]( doc, "Audio Files" ) {
    val id = -1L
    
    def toXML = <audioFiles>
  {innerToXML}
</audioFiles>

  def fromXML( parent: Node ) {
     val innerXML = SessionElement.getSingleXML( parent, "audioFiles" )
     innerFromXML( innerXML )
  }

  protected def elementsFromXML( node: Node ) : Seq[ AudioFileElement ] =
     (node \ AudioFileElement.XML_NODE).map( n => AudioFileElement.fromXML( doc, n ))
}