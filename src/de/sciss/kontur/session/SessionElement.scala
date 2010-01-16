/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import scala.collection.immutable.{ Queue }
import de.sciss.kontur.util.{ Model }

trait SessionElement extends Model {
  def name: String
  def id: Long
//  def toXML =
//    <elem name={name}>
//    </elem>

  def toXML: scala.xml.Elem

//  protected def innerXML: scala.xml.Elem
}
