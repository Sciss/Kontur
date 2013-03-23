package de.sciss.kontur
package gui

import util.{PrefsUtil, Flag}
import session.Session
import legacy.ProcessingThread
import de.sciss.desktop.Window
import collection.breakOut
import java.io.{IOException, FileReader, FilenameFilter, File}
import java.awt.{Frame, FileDialog}
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory
import org.xml.sax
import scala.util.control.NonFatal
import swing.Action
import javax.swing.KeyStroke
import java.awt.event.KeyEvent

object GlobalActions {
  def closeAll(force: Boolean, confirmed: Flag): Option[ProcessingThread] = {
    val dh = Kontur.documentHandler
    dh.documents.foreach { doc =>
      val pt = closeDocument(doc, force, confirmed)
      if (pt.isDefined) return pt
      if (!confirmed()) return None
    }
    confirmed() = true
    None
  }

  def openDocument(file: File) {
    ActionOpen.perform(file)
  }

  def closeDocument(document: Session, force: Boolean, confirmed: Flag): Option[ProcessingThread] = {
    val sfs: List[SessionFrame] = Kontur.windowHandler.windows.collect({
      case sf: SessionFrame if (sf.document == document) => sf
    }).toList

    sfs.headOption match {
      case Some(sf) => sf.closeDocument(force, confirmed)

      case None =>
        println("Wooop -- no document frame found ?!")
        confirmed() = true
    }
    None
  }

  private object ActionOpen extends Action("Open Session") {
    accelerator = Some(KeyStroke.getKeyStroke(KeyEvent.VK_O, Window.menuShortcut))

    /*
     *  Open a Session. If the current Session
     *  contains unsaved changes, the user is prompted
     *  to confirm. A file chooser will pop up for
     *  the user to select the session to open.
     */
    def apply() {
      queryFile().foreach(f => perform(f))
    }

    private def queryFile(): Option[File] = {
      val w = Kontur.getComponent[de.sciss.desktop.Window](Kontur.COMP_MAIN)
      val frame = w.map(_.component) match {
        case Some(f: Frame) => f
        case _ => null
      }
      val prefs = Kontur.userPrefs

      val fDlg = new FileDialog(frame, title, FileDialog.LOAD)
      fDlg.setDirectory(prefs.getOrElse(PrefsUtil.KEY_FILEOPENDIR, new File(sys.props("user.home"))).getPath)
      val accept = try {
        Some(new Acceptor)
      }
      catch {
        case NonFatal(_) => None
      }
      accept.foreach(a => fDlg.setFilenameFilter(a))
      fDlg.setVisible(true)
      accept.foreach(_.dispose())
      val strDir  = fDlg.getDirectory
      val strFile = fDlg.getFile

      if (strFile == null) return None  // means the dialog was cancelled

      // save dir prefs
      prefs.put(PrefsUtil.KEY_FILEOPENDIR, strDir)

      Some(new File(strDir, strFile))
    }

    private class Acceptor extends DefaultHandler with FilenameFilter {
      val factory = SAXParserFactory.newInstance()
      val parser  = factory.newSAXParser()

      def accept(dir: File, name: String): Boolean = {
        val file = new File(dir, name)
        if (!file.isFile || !file.canRead) return false
        try {
          var reader = new FileReader(file)
          try {
            // note that the parsing is hell slow for some reason.
            // therefore we do a quick magic cookie check first
            val cookie = new Array[Char](5)
            reader.read(cookie)
            if (new String(cookie) != "<?xml") return false
            // sucky FileReader does not support reset
            //                reader.reset()
            reader.close()
            reader = new FileReader(file)
            val is = new sax.InputSource(reader)
            parser.reset()
            parser.parse(is, this)
            false
          }
          catch {
            case e: SessionFoundException => true
            case NonFatal(_) => false
          }
          finally {
            reader.close()
          }
        }
        catch {
          case e1: IOException => false
        }
      }

      override def startElement(uri: String, localName: String,
                                qName: String, attributes: sax.Attributes) {
        // eventually we will have a version check here
        // (using attributes) and
        // could then throw more detailed information
        throw (if (qName == Session.XML_START_ELEMENT)
          new SessionFoundException
        else
          new SessionNotFoundException
        )
      }

      def dispose() {
        // nothing actually
      }

      private class SessionFoundException    extends sax.SAXException
      private class SessionNotFoundException extends sax.SAXException
    }


    /**
     * Loads a new document file.
     * a <code>ProcessingThread</code>
     * started which loads the new session.
     *
     * synchronization: this method must be called in event thread
     *
     * @param  file the file of the document to be loaded
     */
    def perform(file: File) {
      try {
        val doc = Session.newFrom(file)
// XXX TODO
//        addRecent(file)
        Kontur.documentHandler.addDocument(this, doc)
        new SessionTreeFrame(doc)
      }
      catch {
        case e1: IOException => Window.showDialog(e1 -> title)
      }
    }
  }
}