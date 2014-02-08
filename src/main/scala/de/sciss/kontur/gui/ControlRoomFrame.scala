/*
 *  ControlRoomFrame.scala
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
import de.sciss.audiowidgets.j.PeakMeter
import de.sciss.desktop.Window
import de.sciss.desktop.impl.WindowImpl

class ControlRoomFrame extends WindowImpl {
  override protected val style = Window.Auxiliary

  private val ggVolume    = new VolumeFader()
  private val ggLimiter   = new MultiStateButton()
  private val pmg         = new PeakMeter()
  private val b1          = new SpringPanel(2, 4, 2, 4)
//  private var isListening = false

  title = "Control Room" // XXX getResource
  resizable = true

  ggVolume.addChangeListener(new ChangeListener() {
    def stateChanged(e: ChangeEvent): Unit = superCollider.volume = ggVolume.volumeLinear
  })

  ggLimiter.setNumColumns(8)
  ggLimiter.addItem("Limiter")
  // NOTE: BUG WITH CUSTOM COMPOSITE ON WIN-XP!!!
  //		ggLimiter.addItem( "Limiter", null, new Color( 0xFF, 0xFA, 0x9D ), new Color( 0xFA, 0xE7, 0x9D ));
  ggLimiter.addItem("Limiter", null, new Color(0xFF, 0xFA, 0x9D))
  ggLimiter.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      // 				superCollider.setLimiter( ggLimiter.getSelectedIndex() == 1 );
      superCollider.limiter = ggLimiter.getSelectedIndex == 1
    }
  })
  if (superCollider.limiter) ggLimiter.setSelectedIndex(1)

  pmg.borderVisible = true
  pmg.caption       = true
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

  contents = Component.wrap(b1)

//  AbstractWindowHandler.setDeepFont( b1 )

//  // ---- listeners -----
//
//  addListener(new AbstractWindow.Adapter {
//    override def windowOpened(e: AbstractWindow.Event) {
//      startListening()
//    }
//
//    override def windowClosing(e: AbstractWindow.Event) {
//      setVisible(false)
//      dispose()
//    }
//  })

  updateVolume()

  application.addComponent(Kontur.COMP_CTRLROOM, this)

  def handler = Kontur.windowHandler

  private def superCollider = SuperColliderClient.instance

//  override protected def autoUpdatePrefs = true
//
//  override protected def restoreVisibility = true
//
//  override protected def getPreferredLocation: Point2D = new Point2D.Float(0.95f, 0.2f)

  override def dispose(): Unit = {
    application.removeComponent(Kontur.COMP_CTRLROOM)
    // XXX TODO
//    stopListening()

    pmg.dispose()
    super.dispose()
  }

  private def updateVolume(): Unit =
    ggVolume.volumeLinear = superCollider.volume

  private def rebuildMeters(): Unit = {
    pmg.numChannels = 8 // XXX
    b1.makeCompactGrid()
    pack()
  }

//  private def startListening() {
//    isListening = true
//  }
//
//  private def stopListening() {
//    isListening = false
//  }
}