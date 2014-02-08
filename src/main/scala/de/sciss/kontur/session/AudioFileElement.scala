/*
 *  AudioFileElement.scala
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

import java.awt.datatransfer.DataFlavor
import java.io.{ File, IOException }
import scala.xml.Node

import util.SerializerContext
import de.sciss.synth.io.AudioFile
import legacy.AbstractCompoundEdit
import de.sciss.sonogram
import de.sciss.dsp.ConstQ

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
      val cq          = ConstQ.Config()
      cq.maxFreq      = 18000
      cq.maxTimeRes   = 16
      cq.bandsPerOct  = 18
      mgr.acquire(sonogram.OverviewManager.Job(path, cq))
    }
  }
}

class AudioFileSeq( doc: Session )
extends BasicSessionElementSeq[ AudioFileElement ]( doc, "Audio Files" ) {    
    def toXML( c: SerializerContext ) = <audioFiles>
  {innerToXML( c )}
</audioFiles>

   def fromXML( c: SerializerContext, parent: Node ): Unit = {
      val innerXML = SessionElement.getSingleXML( parent, "audioFiles" )
      innerFromXML( c, innerXML )
   }

   protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ AudioFileElement ] =
      (node \ AudioFileElement.XML_NODE).map( n => AudioFileElement.fromXML( c, doc, n ))

   /**
    *  Smart edit checking all usage of that file,
    *  and moving around stakes accordingly.
    */
   def editReplace( ce: AbstractCompoundEdit, oldFile: AudioFileElement, newFile: AudioFileElement ): Unit = {
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