/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

class AudioTrack( doc: Session, tl: BasicTimeline )
extends Track // [ AudioRegion ]
with Renameable {
  protected var nameVar = "Audio" // XXX
  val trail: Trail[ AudioRegion ] = new AudioTrail
}