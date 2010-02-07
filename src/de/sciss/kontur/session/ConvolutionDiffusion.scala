/*
 *  ConvolutionDiffusion.scala
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
import scala.xml.{ Node, Null }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.io.{ AudioFile }
import de.sciss.kontur.edit.{ SimpleEdit }

object ConvolutionDiffusion extends DiffusionFactory {
   def fromXML( doc: Session, node: Node ) : ConvolutionDiffusion = {
      val id       = (node \ "@id").text.toInt
      val diff     = new ConvolutionDiffusion( id, doc )
      diff.fromXML( node )
      diff
   }

   def factoryName         = "convolution"
   def humanReadableName   = "Convolution Diffusion"

   case class PathChanged( oldPath: Option[ File ], newPath: Option[ File ])
   case class GainChanged( oldGain: Float, newGain: Float )
}

class ConvolutionDiffusion( id: Long, doc: Session )
extends BasicDiffusion( id, doc ) {
   import Diffusion._ 
   import ConvolutionDiffusion._

   private var pathVar: Option[ File ] = None
   private var numFramesVar         = 0L
   private var numOutputChannelsVar = 1  // use 1 as default to avoid problems allocating dummy buffers
   private var sampleRateVar        = 1.0
   private var gainVar              = 1.0f

   def path = pathVar
   def path_=( newPath: Option[ File ]) {
       var changes: List[ AnyRef ] = Nil
       if( newPath != pathVar ) {
          val defaults = (0L, 1, 1.0)
          val (newNumFrames, newNumOutputChans, newSampleRate) = try {
             newPath.map( p => {
                val af     = AudioFile.openAsRead( p )
                val descr  = af.getDescr
                af.close
                (descr.length, descr.channels, descr.rate)
             }) getOrElse defaults
          }
          catch { case e: IOException => defaults }

          if( newNumOutputChans != numOutputChannelsVar ) {
             changes ::= NumOutputChannelsChanged( numOutputChannelsVar, newNumOutputChans )
          }
          changes ::= PathChanged( pathVar, newPath )
          pathVar                = newPath
          numFramesVar           = newNumFrames
          numOutputChannelsVar   = newNumOutputChans
          sampleRateVar          = newSampleRate 
          changes.foreach( msg => dispatch( msg ))
       }
   }

   def gain = gainVar
   def gain_=( newGain: Float ) {
      if( newGain != gainVar ) {
         val change = GainChanged( gainVar, newGain )
         gainVar = newGain
         dispatch( change )
      }
   }

   def numInputChannels    = 1
   def numOutputChannels   = numOutputChannelsVar
   def numFrames           = numFramesVar
   def sampleRate          = sampleRateVar

   def factoryName         = ConvolutionDiffusion.factoryName

   def toXML =
      <diffusion id={id.toString} class={factoryName}>
         <name>{name}</name>
         {path.map( p => <path>{p.getCanonicalPath}</path>) getOrElse Null}
         <gain>{gain}</gain>
         <numOutputChannels>{numOutputChannels}</numOutputChannels>
         <numFrames>{numFrames}</numFrames>
         <sampleRate>{sampleRate}</sampleRate>
      </diffusion>

   def fromXML( node: Node ) {
       nameVar              = (node \ "name").text
       pathVar              = (node \ "path").headOption.map( n => new File( n.text ))
       gain                 = (node \ "gain").text.toFloat 
       numOutputChannelsVar = (node \ "numOutputChannels").text.toInt
       numFramesVar         = (node \ "numFrames").text.toLong
       sampleRateVar        = (node \ "sampleRate").text.toDouble
   }

   // ---- editor ----
   def editSetPath( ce: AbstractCompoundEdit, newPath: Option[ File ]) {
      val edit = new SimpleEdit( "editSetPath" ) {
         lazy val oldPath = path
         def apply { oldPath; path = newPath }
         def unapply { path = oldPath }
      }
      ce.addPerform( edit )
   }
}