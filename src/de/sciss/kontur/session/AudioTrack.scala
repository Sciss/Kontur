/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

class AudioTrack( val id: Long, doc: Session, tl: BasicTimeline )
extends Track // [ AudioRegion ]
with Renameable {
  protected var nameVar = "Audio" // XXX
  val trail: AudioTrail = new AudioTrail( doc )

  def toXML =
    <audioTrack id={id.toString}>
      <name>{name}</name>
      {trail.toXML}
    </audioTrack>
}