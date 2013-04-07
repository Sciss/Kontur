/*
 *  AudioFileElement.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.kontur
package session

import java.awt.datatransfer.DataFlavor
import java.io.{ File, IOException }
import scala.xml.Node

import util.SerializerContext
import de.sciss.synth.io.AudioFile
import legacy.AbstractCompoundEdit
import de.sciss.sonogram

object AudioFileElement {
    val XML_NODE = "audioFile"

   def fromXML( c: SerializerContext, doc: Session, node: Node ) : AudioFileElement = {
      val path          = new File( (node \ "path").text )
      val numFrames     = (node \ "numFrames").text.toLong
      val numChannels   = (node \ "numChannels").text.toInt
      val sampleRate    = (node \ "sampleRate").text.toDouble
      val afe           = new AudioFileElement( path, numFrames, numChannels, sampleRate )
      c.id( afe, node )
//    afe.fromXML( node )
      afe
   }

   @throws( classOf[ IOException ])
   def fromPath( doc: Session, path: File ) : AudioFileElement = {
      val spec = AudioFile.readSpec( path )
      new AudioFileElement( path, spec.numFrames, spec.numChannels, spec.sampleRate )
   }

   val flavor = new DataFlavor( classOf[ AudioFileElement ], "AudioFileElement" )
}

case class AudioFileElement( path: File, numFrames: Long,
                             numChannels: Int, sampleRate: Double )
extends SessionElement {
  def name: String = path.getName

  def toXML( c: SerializerContext ) = <audioFile id={c.id( this ).toString}>
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
  lazy val sona: Option[sonogram.Overview] = {
    Kontur.getComponent[sonogram.OverviewManager](Kontur.COMP_SONO).map { mgr =>
      mgr.acquire(sonogram.OverviewManager.Job(path))
    }
  }
}

class AudioFileSeq( doc: Session )
extends BasicSessionElementSeq[ AudioFileElement ]( doc, "Audio Files" ) {    
    def toXML( c: SerializerContext ) = <audioFiles>
  {innerToXML( c )}
</audioFiles>

   def fromXML( c: SerializerContext, parent: Node ) {
      val innerXML = SessionElement.getSingleXML( parent, "audioFiles" )
      innerFromXML( c, innerXML )
   }

   protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ AudioFileElement ] =
      (node \ AudioFileElement.XML_NODE).map( n => AudioFileElement.fromXML( c, doc, n ))

   /**
    *  Smart edit checking all usage of that file,
    *  and moving around stakes accordingly.
    */
   def editReplace( ce: AbstractCompoundEdit, oldFile: AudioFileElement, newFile: AudioFileElement ) {
      var trails = Set[ Trail[ _ ]]()
      doc.timelines.foreach( tl => {
         tl.tracks.foreach( t => {
            val trail = t.trail
            if( !trails.contains( trail )) {
               trails += trail
               t match {
                  case atrk: AudioTrack => {
                     val at = atrk.trail 
                     val stakes = at.getAll()
                     var toRemove = Set[ AudioRegion ]()
                     var toAdd    = Set[ AudioRegion ]()
                     stakes.foreach( stake => {
                        if( stake.audioFile == oldFile ) {
                           toRemove += stake
                           toAdd    += stake.replaceAudioFile( newFile )
                        }
                     })
                     if( toRemove.nonEmpty ) {
                        at.editRemove( ce, toRemove.toList: _* )
                        at.editAdd( ce, toAdd.toList: _* )
                     }
                  }
                  case _ =>
               }
            }
         })
      })
      editRemove( ce, oldFile )
      editInsert( ce, indexOf( oldFile ), newFile )
   }

   /**
    *  Smart detection across tracks
    */
   def unused: Seq[ AudioFileElement ] = {
      var trails  = Set[ Trail[ _ ]]()
      var fileSet = toList.toSet
      if( fileSet.isEmpty ) return Nil
      doc.timelines.foreach( tl => {
         tl.tracks.foreach( t => {
            val trail = t.trail
            if( !trails.contains( trail )) {
               trails += trail
               t match {
                  case atrk: AudioTrack => {
                     val at = atrk.trail
                     val stakes = at.getAll()
                     stakes.foreach( stake => {
                        if( fileSet.contains( stake.audioFile )) {
                           fileSet -= stake.audioFile
                           if( fileSet.isEmpty ) return Nil
                        }
                     })
                  }
                  case _ =>
               }
            }
         })
      })
      fileSet.toSeq
   }
}