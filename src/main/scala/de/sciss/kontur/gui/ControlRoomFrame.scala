/*
 *  ControlRoomFrame.scala
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

import javax.swing.event.{ChangeEvent, ChangeListener}
import java.awt.event.{ActionEvent, ActionListener}
import de.sciss.kontur.Kontur
import java.awt.{BorderLayout, Color}
import java.awt.geom.Point2D
import de.sciss.kontur.sc.SuperColliderClient
import javax.swing.JPanel
import legacy.{MultiStateButton, SpringPanel}
import swing.Component
import de.sciss.audiowidgets.j.{PeakMeter, PeakMeterBar}

class ControlRoomFrame extends desktop.impl.WindowImpl {
  protected val style = desktop.Window.Auxiliary

  private val ggVolume    = new VolumeFader()
  private val ggLimiter   = new MultiStateButton()
  private val pmg         = new PeakMeter()
  private val b1          = new SpringPanel(2, 4, 2, 4)
  private var isListening = false

  title = "Control Room" // XXX getResource
  resizable = true

  ggVolume.addChangeListener(new ChangeListener() {
    def stateChanged(e: ChangeEvent) {
      superCollider.volume = ggVolume.volumeLinear
    }
  })

  ggLimiter.setNumColumns( 8 )
  ggLimiter.addItem("Limiter")
  // NOTE: BUG WITH CUSTOM COMPOSITE ON WIN-XP!!!
  //		ggLimiter.addItem( "Limiter", null, new Color( 0xFF, 0xFA, 0x9D ), new Color( 0xFA, 0xE7, 0x9D ));
  ggLimiter.addItem("Limiter", null, new Color(0xFF, 0xFA, 0x9D))
  ggLimiter.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent) {
      // 				superCollider.setLimiter( ggLimiter.getSelectedIndex() == 1 );
      superCollider.limiter = ggLimiter.getSelectedIndex == 1
    }
  })
  if (superCollider.limiter) ggLimiter.setSelectedIndex(1)

  pmg.borderVisible = true
  pmg.hasCaption    = true
  // 		oCfg = superCollider.getOutputConfig()
  rebuildMeters()

  val b2 = new JPanel(new BorderLayout())
  b2.add(pmg, BorderLayout.WEST)
  b2.add(ggVolume, BorderLayout.EAST)

  b1.gridAdd(ggLimiter, 0, 0, -1, 1)
  b1.gridAdd(b2, 0, 1, -1, 1)
  // 		b1.gridAdd( ggOutputConfig, 0, 2, -1, 1 )
  // 		b1.gridAdd( ggAudioBox, 0, 3, -1, 1 )
  b1.makeCompactGrid()

  content = Component.wrap(b1)

  AbstractWindowHandler.setDeepFont( b1 )

  // ---- listeners -----

  addListener(new AbstractWindow.Adapter {
    override def windowOpened(e: AbstractWindow.Event) {
      startListening()
    }

    override def windowClosing(e: AbstractWindow.Event) {
      setVisible(false)
      dispose()
    }
  })

  updateVolume()

  closeOperation = desktop.Window.CloseIgnore // window listener see above!
  // init()
  app.addComponent(Kontur.COMP_CTRLROOM, this)

  private def superCollider = SuperColliderClient.instance

  override protected def autoUpdatePrefs = true

  override protected def restoreVisibility = true

  override protected def getPreferredLocation: Point2D = new Point2D.Float(0.95f, 0.2f)

  override def dispose() {
    AbstractApplication.getApplication.removeComponent(Kontur.COMP_CTRLROOM)
    stopListening()

    pmg.dispose()
    super.dispose()
  }

  private def updateVolume() {
    ggVolume.volumeLinear = superCollider.volume
  }

  private def rebuildMeters() {
    pmg.numChannels = 8 // XXX
    b1.makeCompactGrid()
    pack()
  }

  private def startListening() {
    isListening = true
  }

  private def stopListening() {
    isListening = false
  }
}