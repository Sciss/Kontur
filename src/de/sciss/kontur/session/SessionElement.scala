/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import scala.collection.immutable.{ Queue }
import de.sciss.kontur.util.{ Model }

trait SessionElement extends Model {
  def name: String
}
