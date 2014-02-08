/*
 *  AudioTrack.scala
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

package de.sciss.kontur.session

import scala.xml.Node
import de.sciss.kontur.edit.SimpleEdit
import de.sciss.kontur.util.SerializerContext
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

object AudioTrack {
  val XML_NODE = "audioTrack"

  def fromXML(c: SerializerContext, node: Node, doc: Session): AudioTrack = {
    val at = new AudioTrack(doc)
    c.id(at, node)
    at.fromXML(c, node)
    at
  }

  final case class DiffusionChanged(oldDiff: Option[Diffusion], newDiff: Option[Diffusion])
}
final class AudioTrack(doc: Session)
  extends Track with TrackEditor with Renamable {

  import AudioTrack._

  type T = AudioRegion

  protected var nameVar = "Audio" // XXX

  def undoManager: UndoManager = doc.undoManager

  val trail: AudioTrail = new AudioTrail(doc)
  private var diffusionVar: Option[Diffusion] = None

  def diffusion = diffusionVar

  def diffusion_=(newDiff: Option[Diffusion]): Unit =
    if (newDiff != diffusionVar) {
      val change = DiffusionChanged(diffusionVar, newDiff)
      diffusionVar = newDiff
      dispatch(change)
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
         def unapply(): Unit = diffusion = oldDiff
      }
      ce.addPerform( edit )
   }
}