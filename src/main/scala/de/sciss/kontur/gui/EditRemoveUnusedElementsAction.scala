/*
 *  EditRemoveUnusedElementsAction.scala
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

package de.sciss.kontur.gui

import java.awt.BorderLayout
import de.sciss.kontur.session.SessionElementSeqEditor
import javax.swing.{JPanel, JLabel, JList, DefaultListSelectionModel, JScrollPane}
import scala.swing.{Component, Action}
import de.sciss.desktop.{OptionPane, Window}

final class EditRemoveUnusedElementsAction[T](elemName: String, ed: SessionElementSeqEditor[T],
                                              collect: => Seq[T], display: T => String = (e: T) => e.toString,
                                              nameInAction: Boolean = false)
  extends Action("Remove Unused " + (if (nameInAction) elemName else "") + "...") {

  private def fullName = "Remove Unused " + elemName

  def apply(): Unit = {
    val unused = collect //
    if (unused.isEmpty) {
      val op = OptionPane.message("There are currently no unused " + elemName + ".", OptionPane.Message.Info)
      op.title = fullName
      Window.showDialog(op)
    } else {
      val pane = new JPanel(new BorderLayout(4, 4))
      pane.add(new JLabel("The following " + elemName + " will be removed:"), BorderLayout.NORTH)
      val list = new JList(unused.map(display(_)).toArray[AnyRef])
      list.setSelectionModel(new DefaultListSelectionModel {
        override def addSelectionInterval(index0: Int, index1: Int) = ()
        override def setSelectionInterval(index0: Int, index1: Int) = ()
      })
      pane.add(new JScrollPane(list), BorderLayout.CENTER)
      val op = OptionPane.confirmation(message = Component.wrap(pane), messageType = OptionPane.Message.Question,
        optionType = OptionPane.Options.OkCancel)
      op.title = fullName
      val result = Window.showDialog(op)
      if (result == OptionPane.Result.Ok) {
        val ce = ed.editBegin(fullName)
        unused.foreach(ed.editRemove(ce, _))
        ed.editEnd(ce)
      }
    }
  }
}