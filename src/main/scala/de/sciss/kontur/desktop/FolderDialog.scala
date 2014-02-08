package de.sciss.kontur.desktop

import java.awt.FileDialog
import java.io.File

final class FolderDialog(parent: java.awt.Frame, title: String) extends FileDialog(parent, title, FileDialog.LOAD) {
	override def getFile: String = {
    val res = super.getFile
    if (res == null) null else ""
  }

	override def getDirectory: String = {
    val d = super.getDirectory
    if (d == null) return null

    val f = super.getFile
    if (f != null) new File(d, f).getPath else d
	}

	override def setMode(mode: Int): Unit = throw new UnsupportedOperationException("setMode on a FolderDialog")

	/** Makes the dialog visible. Since the dialog is modal, this method
	  * will not return until either the user dismisses the dialog or
	  * you make it invisible yourself via <code>setVisible(false)</code>
	  * or <code>dispose()</code>.
	  */
	override def show(): Unit = {
    val key       = "apple.awt.fileDialogForDirectories"
    val props     = sys.props
    val oldValue  = props.put(key, "true")
    try {
      super.show() // deprecated but `setVisible` is not doing the same thing
    } finally {
      oldValue match {
        case Some(v)  => props.put(key, v)
        case _        => props.remove(key)
      }
    }
  }
}