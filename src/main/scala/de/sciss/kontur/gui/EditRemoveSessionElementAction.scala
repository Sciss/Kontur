/*
 *  EditRemoveSessionElementAction.scala
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

import de.sciss.kontur.session.SessionElementSeqEditor
import swing.Action

final class EditRemoveSessionElementAction[T](elemName: String, elem: T, ed: SessionElementSeqEditor[T],
                                              nameInAction: Boolean = false)
  extends Action(if (nameInAction) "Remove " + elemName else "Remove") {

  def apply(): Unit = {
    val ce = ed.editBegin("Remove " + elemName)
    ed.editRemove(ce, elem)
    ed.editEnd(ce)
  }
}