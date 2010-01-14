/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import java.io.{ File }

class AudioFileElement( path: File ) extends SessionElement {
  def name: String = path.getName
}
