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

package de.sciss.kontur.io

import java.io.File

object Implicits {
  def file(path: String) = new File(path)

  implicit final class RichFile(val file: File) extends AnyVal {
    def / (child: String): File = new File(file, child)

    def updateSuffix(suffix: String): File = {
      val name    = file.getName
      val i       = name.lastIndexOf('.')
      val prefix  = if (i >= 0) name.substring(0, i) else name
      new File(file.getParentFile, prefix + '.' + suffix)
    }
  }
}