package de.sciss.kontur
package desktop

import swing.{Component, Action}
import javax.{swing => j}
import j.KeyStroke

object Implicits {
  implicit final class RichComponent(val component: Component) extends AnyVal {
    def addAction(action: Action, focus: FocusType = FocusType.Default) {
      val a       = action.peer
      val key     = a.getValue(j.Action.NAME).toString
      val stroke  = a.getValue(j.Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke]
      component.peer.registerKeyboardAction(a, key, stroke, focus.id)
    }

    def removeAction(action: Action) {
      val a       = action.peer
      val stroke  = a.getValue(j.Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke]
      component.peer.unregisterKeyboardAction(stroke)
    }
  }
}