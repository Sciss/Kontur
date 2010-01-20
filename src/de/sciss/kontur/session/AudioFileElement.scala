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

object AudioFileElement {
    val XML_NODE = "audioFile"

    def fromXML( doc: Session, node: Node ) : AudioFileElement = {
       val id       = (node \ "@id").text.toInt
       val path     = new File( (node \ "path").text )
       val afe      = new AudioFileElement( id, path )
//       afe.fromXML( node )
       afe
    }
}

class AudioFileElement( val id: Long, val path: File ) extends SessionElement {
  def name: String = path.getName

  def toXML = <audioFile id={id.toString}>
  <path>{path.getAbsolutePath}</path>
</audioFile>

  lazy val descr: Option[ AudioFileDescr ] = {
      try {
        val af = AudioFile.openAsRead( path )
        af.close
        Some( af.getDescr )
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