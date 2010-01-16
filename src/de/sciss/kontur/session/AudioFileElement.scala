/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import java.io.{ File }

class AudioFileElement( val id: Long, path: File ) extends SessionElement {
  def name: String = path.getName

  def toXML =
    <audioFile id={id.toString}>
      <path>{path.getAbsolutePath}</path>
    </audioFile>
}

class AudioFileSeq( doc: Session )
extends BasicSessionElementSeq[ AudioFileElement ]( doc, "Audio Files" ) {
    val id = -1L
    
    def toXML =
      <audioFiles>
         {toList.map(_.toXML)}
      </audioFiles>
}