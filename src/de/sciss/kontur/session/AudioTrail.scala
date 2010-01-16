/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.io.{ Span }

class AudioRegion( afe: AudioFileElement, s: Span, n: String )
extends Region( s, n ) {
  def toXML =
    <stake>
      <name>{name}</name>
      <span start={span.start.toString} stop={span.stop.toString}/>
      <audioFile idref={afe.id.toString}/>
    </stake>
}

class AudioTrail( doc: Session ) extends BasicTrail[ AudioRegion ]( doc ) {
  def toXML =
    <trail>
      {getAll().map(_.toXML)}
    </trail>
}
