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