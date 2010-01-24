/*
 *  AudioTrack.scala
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
import javax.swing.undo.{ UndoManager }
import scala.xml.{ Node }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }

object AudioTrack {
   val XML_NODE = "audioTrack"

   def fromXML( node: Node, doc: Session, tl: BasicTimeline ) : AudioTrack = {
       val id    = (node \ "@id").text.toInt
       val at    = new AudioTrack( id, doc, tl )
       at.fromXML( node )
       at
   }

   case class DiffusionChanged( oldDiff: Option[ Diffusion ], newDiff: Option[ Diffusion ])
}

class AudioTrack( val id: Long, doc: Session, tl: BasicTimeline )
extends Track[ AudioRegion ] with TrackEditor[ AudioRegion ] // [ AudioRegion ]
with Renameable {
  import AudioTrack._

  protected var nameVar = "Audio" // XXX

  def undoManager: UndoManager = doc.getUndoManager

  val trail: AudioTrail = new AudioTrail( doc )
  private var diffusionVar: Option[ Diffusion ] = None
  def diffusion = diffusionVar
  def diffusion_=( newDiff: Option[ Diffusion ]) {
     if( newDiff != diffusionVar ) {
        val change = DiffusionChanged( diffusionVar, newDiff )
        diffusionVar = newDiff
        dispatch( change )
     }
  }

  def toXML =
    <audioTrack id={id.toString}>
      <name>{name}</name>
      {diffusion.map( diff => <diffusion idref={diff.id.toString}/>) getOrElse scala.xml.Null}
      {trail.toXML}
    </audioTrack>

  def fromXML( node: Node ) {
      nameVar = (node \ "name").text
      (node \ "diffusion").foreach( diffN => {
//println( "KUUKA" )
          val diffID = (diffN \ "@idref").text.toInt
          diffusionVar = Some( doc.diffusions.getByID( diffID ) getOrElse {
            throw new IOException( "Session corrupt. Referencing an invalid diffusion #" + diffID )})
      })
      trail.fromXML( node )
  }

  def editor: Option[ TrackEditor[ AudioRegion ]] = Some( this )
  // ---- TrackEditor ----
  def editDiffusion( ce: AbstractCompoundEdit, newDiff: Option[ Diffusion ]) {
    val edit = new SimpleEdit( "editTrackDiffusion" ) {
       lazy val oldDiff = diffusion
       def apply { oldDiff; diffusion = newDiff }
       def unapply { diffusion = oldDiff }
    }
    ce.addPerform( edit )
  }
}