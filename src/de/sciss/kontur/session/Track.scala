/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

//trait Track[ T <: Stake ] extends SessionElement {
//  def trail: Trail[ T ]
//}

trait Track extends SessionElement {
  def trail: Trail[ _ <: Stake ]
}

class Tracks( val id: Long, doc: Session )
extends BasicSessionElementSeq[ Track ]( doc, "Tracks" ) {

    def toXML =
       <tracks id={id.toString}>
      </tracks>
}