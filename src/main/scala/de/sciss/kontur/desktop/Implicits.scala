/*
 *  Implicits.scala
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

package de.sciss.kontur
package desktop

import legacy.Param
import scala.util.control.NonFatal
import de.sciss.desktop.Preferences

object Implicits {
  implicit object ParamPrefs extends Preferences.Type[Param] {
    def toString(value: Param): String = value.toString

    def valueOf(string: String): Option[Param] = try {
      Some(Param.valueOf(string))
    } catch {
      case NonFatal(_) => None
    }
  }
}