/*
 *  ConvolutionDiffusion.scala
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

package de.sciss.kontur.session

import java.io.{ File, IOException }
import scala.xml.{ Node, Null }
import de.sciss.app.AbstractCompoundEdit
import de.sciss.kontur.edit.SimpleEdit
import de.sciss.kontur.util.SerializerContext
import de.sciss.synth.io.AudioFile

/**
 *    @version 0.11, 20-Apr-10
 */
object ConvolutionDiffusion extends DiffusionFactory {
   def fromXML( c: SerializerContext, doc: Session, node: Node ) : ConvolutionDiffusion = {
      val diff = new ConvolutionDiffusion( doc )
      c.id( diff, node )
      diff.fromXML( node )
      diff
   }

   def factoryName         = "convolution"
   def humanReadableName   = "Convolution Diffusion"

   case class PathChanged( oldPath: Option[ File ], newPath: Option[ File ])
   case class GainChanged( oldGain: Float, newGain: Float ) // abs amp
   case class DelayChanged( oldDelay: Float, newDelay: Float ) // seconds
}

class ConvolutionDiffusion( doc: Session )
extends BasicDiffusion( doc ) {
   import Diffusion._ 
   import ConvolutionDiffusion._

   private var pathVar: Option[ File ] = None
   private var numFramesVar         = 0L
   private var numOutputChannelsVar = 1  // use 1 as default to avoid problems allocating dummy buffers
   private var sampleRateVar        = 1.0
   private var gainVar              = 1.0f
   private var delayVar             = 0f

   def path = pathVar
   def path_=( newPath: Option[ File ]) {
       var changes: List[ AnyRef ] = Nil
       if( newPath != pathVar ) {
          val defaults = (0L, 1, 1.0)
          val (newNumFrames, newNumOutputChans, newSampleRate) = try {
             newPath.map( p => {
                val spec = AudioFile.readSpec( p )
                (spec.numFrames, spec.numChannels, spec.sampleRate)
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

   def delay = delayVar

   /**
    *    @param   newDelay   delay in seconds
    */
   def delay_=( newDelay: Float ) {
      if( newDelay != delayVar ) {
         val change = DelayChanged( delayVar, newDelay )
         delayVar = newDelay
         dispatch( change )
      }
   }

   def numInputChannels    = 1
   def numOutputChannels   = numOutputChannelsVar
   def numFrames           = numFramesVar
   def sampleRate          = sampleRateVar

   def factoryName         = ConvolutionDiffusion.factoryName

   def toXML( c: SerializerContext ) =
      <diffusion id={c.id( this ).toString} class={factoryName}>
         <name>{name}</name>
         {path.map( p => <path>{p.getCanonicalPath}</path>) getOrElse Null}
         <gain>{gain}</gain>
         <delay>{delay}</delay>
         <numOutputChannels>{numOutputChannels}</numOutputChannels>
         <numFrames>{numFrames}</numFrames>
         <sampleRate>{sampleRate}</sampleRate>
      </diffusion>

   def fromXML( node: Node ) {
       nameVar              = (node \ "name").text
       pathVar              = (node \ "path").headOption.map( n => new File( n.text ))
       gainVar              = (node \ "gain").text.toFloat
       delayVar             = { val txt = (node \ "delay").text; if( txt.nonEmpty ) txt.toFloat else 0f } 
       numOutputChannelsVar = (node \ "numOutputChannels").text.toInt
       numFramesVar         = (node \ "numFrames").text.toLong
       sampleRateVar        = (node \ "sampleRate").text.toDouble
   }

   // ---- editor ----
   def editSetPath( ce: AbstractCompoundEdit, newPath: Option[ File ]) {
      val edit = new SimpleEdit( "editSetPath" ) {
         lazy val oldPath = path
         def apply() { oldPath; path = newPath }
         def unapply() { path = oldPath }
      }
      ce.addPerform( edit )
   }

   def editSetGain( ce: AbstractCompoundEdit, newGain: Float ) {
      val edit = new SimpleEdit( "editSetGain" ) {
         lazy val oldGain = gain
         def apply() { oldGain; gain = newGain }
         def unapply() { gain = oldGain }
      }
      ce.addPerform( edit )
   }

   def editSetDelay( ce: AbstractCompoundEdit, newDelay: Float ) {
      val edit = new SimpleEdit( "editSetDelay" ) {
         lazy val oldDelay = delay
         def apply() { oldDelay; delay = newDelay }
         def unapply() { delay = oldDelay }
      }
      ce.addPerform( edit )
   }
}