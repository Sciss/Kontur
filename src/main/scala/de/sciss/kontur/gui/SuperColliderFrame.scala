/*
 *  SuperColliderFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import de.sciss.app.AbstractWindow
import de.sciss.kontur.sc.{ SuperColliderClient }
import de.sciss.synth.{ Model, Server }
import de.sciss.synth.swing.{ ServerStatusPanel }
import java.awt.BorderLayout
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent }
import javax.swing.{ AbstractAction, JComponent, KeyStroke }

// note: should be PALETTE, but then we loose the key actions...
class SuperColliderFrame extends AppWindow( AbstractWindow.SUPPORT /* PALETTE */ ) {
   private val superCollider = SuperColliderClient.instance
   private val serverPanel = new ServerStatusPanel(
     ServerStatusPanel.COUNTS | ServerStatusPanel.BOOT_BUTTON ) {

     override protected def bootServer { superCollider.boot }
     override protected def stopServer { superCollider.stop }
     override protected def couldBoot: Boolean = true
   }

   private val clientListener: Model.Listener = {
      case SuperColliderClient.ServerBooting( s ) => serverPanel.booting = Some( s )
      case SuperColliderClient.ServerRunning( s ) => serverPanel.server  = Some( s )
      case SuperColliderClient.ServerTerminated   => serverPanel.server  = None
   }
   
   // ---- constructor ----
   {
      setTitle( "SuperCollider Server" ) // XXX getResource
      setResizable( false )

      // ---- actions ----
      val imap		= getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
      val amap		= getActionMap
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 ), "dumptree" )
      amap.put( "dumptree", new ActionDumpTree( false ))
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.SHIFT_MASK ), "dumptreec" )
      amap.put( "dumptreec", new ActionDumpTree( true ))
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 ), "dumposc" )
      amap.put( "dumposc", new ActionDumpOSC )

      val cp = getContentPane
      cp.add( serverPanel, BorderLayout.CENTER )

      superCollider.addListener( clientListener )
      init()
   }

   override protected def autoUpdatePrefs = true

   private class ActionDumpTree( controls: Boolean  )
   extends AbstractAction {
      def actionPerformed( e: ActionEvent ) {
         superCollider.server.foreach( s => {
             if( s.condition == Server.Running ) {
                s.dumpTree( controls )
             }
         })
      }
   }

   private class ActionDumpOSC
   extends AbstractAction {
      private var dumping = false
      def actionPerformed( e: ActionEvent ) {
         dumping = !dumping
         println( "Dumping is " + (if( dumping ) "on" else "off") ) // XXX resource
         superCollider.dumpOSC( if( dumping ) 1 else 0 )
      }
   }
}