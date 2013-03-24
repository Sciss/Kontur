/*
 *  MenuFactory.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package gui

import de.sciss.kontur.Kontur
import de.sciss.kontur.util.PrefsUtil
import de.sciss.kontur.session.Session
import java.awt.{FileDialog, Frame}
import java.awt.event.{ActionEvent, InputEvent, KeyEvent}
import java.io.{File, FilenameFilter, FileReader, IOException}
import javax.swing.KeyStroke
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.{Attributes, InputSource, SAXException}
import org.xml.sax.helpers.DefaultHandler
import swing.Action
import de.sciss.desktop.{Menu, Window}

class MenuFactory {
//  private val actionOpen = new ActionOpen(getResourceString("menuOpen"),
//    stroke(VK_O, MENU_SHORTCUT))

//  def openDocument(f: File) {
//    actionOpen.perform(f)
//  }

//  def showPreferences() {
//    val prefsFrame = Kontur.getComponent[PrefsFrame](Kontur.COMP_PREFS).getOrElse(new PrefsFrame())
//    prefsFrame.visible = true
//    prefsFrame.front()
//  }

//  protected def getOpenAction: Action = actionOpen

  import Window.menuShortcut
  import KeyEvent._
  import InputEvent.{ALT_MASK, CTRL_MASK, SHIFT_MASK}

  private def stroke(code: Int, modifiers: Int) = KeyStroke.getKeyStroke(code, modifiers)

  private def proxy(key: String, stroke: KeyStroke): Action = {
    val a         = Action(key)()
    a.accelerator = Some(stroke)
    a.enabled     = false
    a
  }

  val root: Menu.Root = {
    val myCtrl = if (menuShortcut == CTRL_MASK) CTRL_MASK | ALT_MASK else CTRL_MASK

    import Menu._

    val prefs = Kontur.userPrefs
    
    val _root = Root().add(
      // --- file menu ---
      Group("file", "File")
        .add(Group("new", "New")
          .add(Item("empty",            ActionNewEmpty))
          .add(Item("interpreter",      ActionScalaInterpreter))
        )
        .add(Item("open",               GlobalActions.ActionOpen))
        .add(Item("close",              proxy("Close",      stroke(VK_W, menuShortcut))))
        .addLine()
        .add(Item("bounce",             "Bounce to Disk..."))
        .addLine()
        .add(Item("save",               proxy("Save",       stroke(VK_S, menuShortcut))))
        .add(Item("saveAs",             proxy("Save As...", stroke(VK_S, menuShortcut | SHIFT_MASK))))
    )
    .add(
      Group("edit", "Edit")
        .add(Item("undo",               proxy("Undo",       stroke(VK_Z, menuShortcut))))
        .add(Item("redo",               proxy("Redo",       stroke(VK_Z, menuShortcut | SHIFT_MASK))))
        .addLine()
        .add(Item("cut",                proxy("Cut",        stroke(VK_X, menuShortcut))))
        .add(Item("copy",               proxy("Copy",       stroke(VK_C, menuShortcut))))
        .add(Item("paste",              proxy("Paste",      stroke(VK_V, menuShortcut))))
        .add(Item("delete",             proxy("Delete",     stroke(VK_DELETE, 0))))
        .addLine()
        .add(Item("selectAll",          proxy("Select All", stroke(VK_A, menuShortcut))))
    )
    .add(
      // --- timeline menu ---
      Group("timeline", "Timeline")
        .add(Item("trimToSelection",    proxy("Trim to Selection",        stroke(VK_F5, menuShortcut))))
        .add(Item("insertSpan",         proxy("Insert Span...",           stroke(VK_E, menuShortcut | SHIFT_MASK))))
        .add(Item("clearSpan",          proxy("Clear Selected Span",      stroke(VK_BACK_SLASH, menuShortcut))))
        .add(Item("removeSpan",         proxy("Remove Selected Span",     stroke(VK_BACK_SLASH, menuShortcut | SHIFT_MASK))))
        .add(Item("dupSpanToPos",       "Duplicate Span to Position"))
        .addLine()
        .add(Item("nudgeAmount",        "Nudge Amount..."))
        .add(Item("nudgeLeft",          proxy("Nudge Objects Backward",   stroke(VK_MINUS, 0))))
        .add(Item("nudgeRight",         proxy("Nudge Objects Forward",    stroke(VK_PLUS,  0))))
        .addLine()
        .add(Item("selFollowingObj",    proxy("Select Following Objects", stroke(VK_F, myCtrl))))
        .add(Item("alignObjStartToPos", "Align Objects Start To Timeline Position"))
        .add(Item("splitObjects",       proxy("Split Selected Objects",   stroke(VK_Y, myCtrl))))
        .addLine()
        .add(Item("selStopToStart",     "Move Selection Stop To Its Start"))
        .add(Item("selStartToStop",     "Move Selection Start To Its Stop"))
    )
    .add(
      // --- actions menu ---
      Group("actions", "Actions")
        .add(Item("showInEisK", "Show in Eisenkraut"))
    )
    .add(
      // --- operation menu ---
      Group("operation", "Operation")
    )
    .add(
      // --- view menu ---
      Group("view", "View")
    )
    .add(
      // --- window menu ---
      Group("window", "Window")
        .add(Item("observer", ActionObserver))
        .add(Item("ctrlRoom", ActionCtrlRoom))
    )

// XXX TODO
//    val actionLinkObjTimelineSel = new BooleanPrefsMenuAction(getResourceString("menuLinkObjTimelineSel"), null)
//    val miLinkObjTimelineSel = new MenuCheckItem("insertionFollowsPlay", actionLinkObjTimelineSel)
//    actionLinkObjTimelineSel.setCheckItem(miLinkObjTimelineSel)
//    actionLinkObjTimelineSel.setPreferences(prefs, PrefsUtil.KEY_LINKOBJTIMELINESEL)
//    mgOperation.add(miLinkObjTimelineSel)
//
//    val smgViewFadeMode = new MenuGroup("fademode", getResourceString("menuViewFadeMode"))
//    val aViewFadeModeNone = new IntPrefsMenuAction(getResourceString("menuViewFadeModeNone"), null, FadeViewMode.None.id)
//    val rgViewFadeMode = new MenuRadioGroup()
//    smgViewFadeMode.add(new MenuRadioItem(rgViewFadeMode, "none", aViewFadeModeNone)) // crucial reihenfolge : erst item erzeugen, dann gruppe setzen, dann prefs
//    aViewFadeModeNone.setRadioGroup(rgViewFadeMode)
//    aViewFadeModeNone.setPreferences(prefs, PrefsUtil.KEY_FADEVIEWMODE)
//    val aViewFadeModeCurve = new IntPrefsMenuAction(getResourceString("menuViewFadeModeCurve"), null, FadeViewMode.Curve.id)
//    smgViewFadeMode.add(new MenuRadioItem(rgViewFadeMode, "curve", aViewFadeModeCurve))
//    aViewFadeModeCurve.setRadioGroup(rgViewFadeMode)
//    aViewFadeModeCurve.setPreferences(prefs, PrefsUtil.KEY_FADEVIEWMODE)
//    val aViewFadeModeSonogram = new IntPrefsMenuAction(getResourceString("menuViewFadeModeSonogram"), null, FadeViewMode.Sonogram.id)
//    smgViewFadeMode.add(new MenuRadioItem(rgViewFadeMode, "sonogram", aViewFadeModeSonogram))
//    aViewFadeModeSonogram.setRadioGroup(rgViewFadeMode)
//    aViewFadeModeSonogram.setPreferences(prefs, PrefsUtil.KEY_FADEVIEWMODE)
//    mgView.add(smgViewFadeMode)
//
//    val smgViewStakeBorderMode = new MenuGroup("stakebordermode", getResourceString("menuViewStakeBorderMode"));
//    val aViewStakeBorderModeNone = new IntPrefsMenuAction(getResourceString("menuViewStakeBorderModeNone"), null, StakeBorderViewMode.None.id)
//    val rgViewStakeBorderMode = new MenuRadioGroup()
//    smgViewStakeBorderMode.add(new MenuRadioItem(rgViewStakeBorderMode, "none", aViewStakeBorderModeNone))
//    aViewStakeBorderModeNone.setRadioGroup(rgViewStakeBorderMode)
//    aViewStakeBorderModeNone.setPreferences(prefs, PrefsUtil.KEY_STAKEBORDERVIEWMODE)
//    val aViewStakeBorderModeBox = new IntPrefsMenuAction(getResourceString("menuViewStakeBorderModeBox"), null, StakeBorderViewMode.Box.id)
//    smgViewStakeBorderMode.add(new MenuRadioItem(rgViewStakeBorderMode, "box", aViewStakeBorderModeBox))
//    aViewStakeBorderModeBox.setRadioGroup(rgViewStakeBorderMode)
//    aViewStakeBorderModeBox.setPreferences(prefs, PrefsUtil.KEY_STAKEBORDERVIEWMODE)
//    val aViewStakeBorderModeTitledBox = new IntPrefsMenuAction(getResourceString("menuViewStakeBorderModeTitledBox"), null, StakeBorderViewMode.TitledBox.id)
//    smgViewStakeBorderMode.add(new MenuRadioItem(rgViewStakeBorderMode, "titledbox", aViewStakeBorderModeTitledBox))
//    aViewStakeBorderModeTitledBox.setRadioGroup(rgViewStakeBorderMode)
//    aViewStakeBorderModeTitledBox.setPreferences(prefs, PrefsUtil.KEY_STAKEBORDERVIEWMODE)
//    mgView.add(smgViewStakeBorderMode)

    _root
  }

  // ---- internal classes ----
  // action for the New-Empty Document menu item
  private object ActionNewEmpty extends Action("New Empty Document") {
    accelerator = Some(stroke(VK_N, menuShortcut))

    def apply() {
      perform()
    }

    def perform(): Session = {
      val doc = Session.newEmpty
      Kontur.documentHandler.addDocument(doc)
      new SessionTreeFrame(doc)
      doc
    }
  }

  private object ActionScalaInterpreter extends Action("Scala Interpreter") {
    def apply() {
      new ScalaInterpreterFrame()
    }
  }

//  protected class ActionOpen(text: String, shortcut: KeyStroke)
//    extends MenuAction(text, shortcut) {
//    /*
//     *  Open a Session. If the current Session
//     *  contains unsaved changes, the user is prompted
//     *  to confirm. A file chooser will pop up for
//     *  the user to select the session to open.
//     */
//    def actionPerformed(e: ActionEvent) {
//      queryFile().foreach(f => perform(f))
//    }
//
//    private def queryFile(): Option[File] = {
//      val w = app.getComponent(Kontur.COMP_MAIN).asInstanceOf[desktop.Window]
//      val frame = w.getWindow match {
//        case f: Frame => f
//        case _ => null
//      }
//      val prefs = app.getUserPrefs
//
//      val fDlg = new FileDialog(frame, getResourceString("fileDlgOpenSession"), FileDialog.LOAD)
//      fDlg.setDirectory(prefs.get(PrefsUtil.KEY_FILEOPENDIR, System.getProperty("user.home")))
//      val accept = try {
//        Some(new Acceptor)
//      }
//      catch {
//        case _: Throwable => None
//      }
//      accept.foreach(a => fDlg.setFilenameFilter(a))
//      fDlg.setVisible(true)
//      accept.foreach(_.dispose())
//      val strDir = fDlg.getDirectory
//      val strFile = fDlg.getFile
//
//      if (strFile == null) return None; // means the dialog was cancelled
//
//      // save dir prefs
//      prefs.put(PrefsUtil.KEY_FILEOPENDIR, strDir)
//
//      Some(new File(strDir, strFile))
//    }
//
//    private class Acceptor extends DefaultHandler with FilenameFilter {
//      val factory = SAXParserFactory.newInstance()
//      val parser = factory.newSAXParser()
//
//      def accept(dir: File, name: String): Boolean = {
//        val file = new File(dir, name)
//        if (!file.isFile || !file.canRead) return false
//        try {
//          var reader = new FileReader(file)
//          try {
//            // note that the parsing is hell slow for some reason.
//            // therefore we do a quick magic cookie check first
//            val cookie = new Array[Char](5)
//            reader.read(cookie)
//            if (new String(cookie) != "<?xml") return false
//            // sucky FileReader does not support reset
//            //                reader.reset()
//            reader.close()
//            reader = new FileReader(file)
//            val is = new InputSource(reader)
//            parser.reset()
//            parser.parse(is, this)
//            false
//          }
//          catch {
//            case e: SessionFoundException => true
//            case _: Throwable => false
//          }
//          finally {
//            try {
//              reader.close()
//            } catch {
//              case e: Throwable =>
//            }
//          }
//        }
//        catch {
//          case e1: IOException => false
//        }
//      }
//
//      @throws(classOf[SAXException])
//      override def startElement(uri: String, localName: String,
//                                qName: String, attributes: Attributes) {
//
//        // eventually we will have a version check here
//        // (using attributes) and
//        // could then throw more detailed information
//        throw (if (qName == Session.XML_START_ELEMENT) new SessionFoundException
//        else new SessionNotFoundException)
//      }
//
//      def dispose() {
//        // nothing actually
//      }
//
//      private class SessionFoundException extends SAXException
//
//      private class SessionNotFoundException extends SAXException
//
//    }
//
//    /**
//     * Loads a new document file.
//     * a <code>ProcessingThread</code>
//     * started which loads the new session.
//     *
//     * synchronization: this method must be called in event thread
//     *
//     * @param  path	the file of the document to be loaded
//     */
//    def perform(path: File) {
//      try {
//        val doc = Session.newFrom(path)
//        addRecent(path)
//        app.getDocumentHandler.addDocument(this, doc)
//        new SessionTreeFrame(doc)
//      }
//      catch {
//        case e1: IOException =>
//          BasicWindowHandler.showErrorDialog(null, e1, getValue(Action.NAME).toString)
//      }
//    }
//  }

  // action for the Control Room menu item
  private object ActionCtrlRoom extends Action("Control Room") {
    accelerator = Some(stroke(VK_NUMPAD2, menuShortcut))

    def apply() {
      val f = Kontur.getComponent[ControlRoomFrame](Kontur.COMP_CTRLROOM).getOrElse(new ControlRoomFrame())
      f.visible = true
      f.front()
    }
  }

  // action for the Observer menu item
  private object ActionObserver extends Action("Observer") {
    accelerator = Some(stroke(VK_NUMPAD3, menuShortcut))

    def apply() {
      val f = Kontur.getComponent[ObserverFrame](Kontur.COMP_OBSERVER).getOrElse(new ObserverFrame())
      f.visible = true
      f.front()
    }
  }
}