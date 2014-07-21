/*
 *  AudioTrack.scala
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

import javax.swing.undo.UndoManager
import scala.xml.Node
import de.sciss.app.AbstractCompoundEdit
import de.sciss.kontur.edit.SimpleEdit
import de.sciss.kontur.util.SerializerContext

object AudioTrack {
   val XML_NODE = "audioTrack"

   def fromXML( c: SerializerContext, node: Node, doc: Session ) : AudioTrack = {
      val at    = new AudioTrack( doc )
      c.id( at, node )
      at.fromXML( c, node )
      at
   }

   case class DiffusionChanged( oldDiff: Option[ Diffusion ], newDiff: Option[ Diffusion ])
}

class AudioTrack( doc: Session )
extends Track with TrackEditor with Renamable {
   import AudioTrack._

   type T = AudioRegion

   protected var nameVar = "Audio" // XXX

   def undoManager: UndoManager = doc.getUndoManager

   val trail: AudioTrail = new AudioTrail( doc )
   private var diffusionVar: Option[ Diffusion ] = None
   def diffusion = diffusionVar
   def diffusion_=( newDiff: Option[ Diffusion ]): Unit = {
      if( newDiff != diffusionVar ) {
         val change = DiffusionChanged( diffusionVar, newDiff )
         diffusionVar = newDiff
         dispatch( change )
      }
   }

   def toXML( c: SerializerContext ) = if( c.exists( this ))
      <audioTrack idref={c.id( this ).toString}/>
   else
      <audioTrack id={c.id( this ).toString}>
      <name>{name}</name>
      {diffusion.map( diff => <diffusion idref={c.id( diff ).toString}/>) getOrElse scala.xml.Null}
      {trail.toXML( c )}
      </audioTrack>

   def fromXML( c: SerializerContext, node: Node ): Unit = {
      nameVar = (node \ "name").text
      (node \ "diffusion").foreach( diffN => {
         diffusionVar = Some( c.byID[ Diffusion ]( diffN ))
      })
      trail.fromXML( c, node )
   }

   def editor: Option[ TrackEditor ] = Some( this )

   protected def editRenameName = "editRenameTrack"

   // ---- TrackEditor ----

   def editDiffusion( ce: AbstractCompoundEdit, newDiff: Option[ Diffusion ]): Unit = {
      val edit = new SimpleEdit( "editTrackDiffusion" ) {
         lazy val oldDiff = diffusion
         def apply(): Unit = { oldDiff; diffusion = newDiff }
         def unapply(): Unit = { diffusion = oldDiff }
      }
      ce.addPerform( edit )
   }
}