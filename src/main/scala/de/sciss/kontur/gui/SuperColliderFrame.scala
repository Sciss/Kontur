/*
 *  SuperColliderFrame.scala
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

import sc.SuperColliderClient
import de.sciss.synth.Server
import java.awt.event.{InputEvent, KeyEvent}
import javax.swing.KeyStroke
import de.sciss.osc
import de.sciss.synth.swing.j.JServerStatusPanel
import util.Model
import swing.{Action, Component}
import de.sciss.desktop.Window
import de.sciss.desktop.impl.WindowImpl

// note: should be PALETTE, but then we loose the key actions...
class SuperColliderFrame extends WindowImpl {
  override protected def style = Window.Auxiliary

  def handler = Kontur.windowHandler

  private val superCollider = SuperColliderClient.instance
  private val serverPanel = new JServerStatusPanel(
    JServerStatusPanel.COUNTS | JServerStatusPanel.BOOT_BUTTON) {

    override protected def bootServer(): Unit = superCollider.boot()
    override protected def stopServer(): Unit = superCollider.stop()

    override protected def couldBoot: Boolean = true
  }

  private val clientListener: Model.Listener = {
    case SuperColliderClient.ServerBooting(s) => serverPanel.booting  = Some(s)
    case SuperColliderClient.ServerRunning(s) => serverPanel.server   = Some(s)
    case SuperColliderClient.ServerTerminated => serverPanel.server   = None
  }

  title     = "SuperCollider Server"  // XXX getResource
  resizable = false

  // ---- actions ----
  addAction("tree",  new ActionDumpTree(controls = false, stroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, 0)))
  addAction("treec", new ActionDumpTree(controls = true , stroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.SHIFT_MASK)))
  addAction("dump", ActionDumpOSC)

  contents = Component.wrap(serverPanel)

  superCollider.addListener(clientListener)

  // init()

//  override protected def autoUpdatePrefs = true

  private class ActionDumpTree(controls: Boolean, stroke: KeyStroke)
    extends Action(s"Dump Tree ($controls)") {

    accelerator = Some(stroke)

    def apply(): Unit =
      superCollider.server.foreach { s =>
        if (s.condition == Server.Running) {
          s.dumpTree(controls)
        }
      }
  }

  private object ActionDumpOSC
    extends Action("Dump OSC") {

    private var dumping = false

    accelerator = Some(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0))

    def apply(): Unit = {
      dumping = !dumping
      println("Dumping is " + (if (dumping) "on" else "off")) // XXX resource
      superCollider.dumpOSC(if (dumping) osc.Dump.Text else osc.Dump.Off)
    }
  }
}