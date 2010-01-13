/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractWindow }
import de.sciss.gui.{ MultiStateButton }
import de.sciss.kontur.sc.{ SuperColliderClient }
import de.sciss.tint.sc.{ Server }
import de.sciss.tint.sc.gui.{ ServerStatusPanel }
import java.awt.{ BorderLayout, Color, Dimension }
import java.awt.event.{ ActionEvent }
import javax.swing.{ AbstractAction, BorderFactory, Box, JButton, JPanel,
          JProgressBar, OverlayLayout }
import scala.math._

class SuperColliderFrame extends AppWindow( AbstractWindow.SUPPORT ) {
   private val superCollider = SuperColliderClient.instance
   private val serverPanel = new ServerStatusPanel(
     ServerStatusPanel.COUNTS | ServerStatusPanel.BOOT_BUTTON ) {

     override protected def bootServer: Unit = superCollider.boot
     override protected def stopServer: Unit = superCollider.stop
     override protected def couldBoot: Boolean = true
   }

   // ---- constructor ----
   {
      setTitle( "SuperCollider Server" ) // XXX getResource
      setResizable( false )

      val cp = getContentPane
      cp.add( serverPanel, BorderLayout.CENTER )

      superCollider.addListener( clientListener )
      init()
   }

   private def clientListener( msg: AnyRef ) : Unit = msg match {
      case SuperColliderClient.ServerChanged( server ) =>
          serverPanel.server = server
   }
}