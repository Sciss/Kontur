/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.io.{ Span }

case class Marker( pos: Long, name: String ) extends Stake {
  def span = new Span( pos, pos )
}
