/*
 *  ObserverFrame.scala
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

import java.awt.Dimension
import javax.swing.{JComponent, JTabbedPane}
import javax.swing.event.{ChangeEvent, ChangeListener}
import swing.Component
import session.Session
import de.sciss.desktop.{Window, DocumentHandler}
import de.sciss.desktop.impl.WindowImpl

class ObserverFrame extends WindowImpl {
  override protected def style = Window.Palette

  private val ggTabPane = new JTabbedPane()
  private var mapTabs = Map[String, ObserverPage]()
  private var shown: Option[ObserverPage] = None

  title     = "Observer" // getResourceString("paletteObserver")

//  closeOperation = desktop.Window.CloseHide
//  // init()
  size = new Dimension(300, 300)

  private val listener = Kontur.documentHandler.addListener {
    case DocumentHandler.Activated(newDoc) =>
      mapTabs.foreach { case (_, page) => page.documentChanged(newDoc) }
  }

  // ---- constructor ----

  //        ggTabPane.setPreferredSize( new Dimension( 400, 400 )) // XXX
  ggTabPane.addChangeListener(new ChangeListener {
    def stateChanged(e: ChangeEvent): Unit = {
      val c = ggTabPane.getSelectedComponent
      val newShown = if (c != null) {
        mapTabs.find(_._2.component == c).map(_._2)
      } else None
      if (newShown != shown) {
        shown.foreach(_.pageHidden())
        shown = newShown
        shown.foreach(_.pageShown())
      }
    }
  })
  //        ggTabPane.putClientProperty( "JComponent.sizeVariant", "small" )
  contents = Component.wrap(ggTabPane)

  application.addComponent(Kontur.COMP_OBSERVER, this)

  //  override protected def autoUpdatePrefs = true
//
//  override protected def alwaysPackSize = false

  def handler = Kontur.windowHandler

  override def dispose(): Unit = {
    Kontur.documentHandler.removeListener(listener)
    super.dispose()
  }

  def addPage(page: ObserverPage): Unit = {
    if (containsPage(page.id)) removePage(page.id)
    ggTabPane.addTab(page.title, page.component)
    //       pack()
  }

  def removePage(id: String): Unit =
    mapTabs.get(id).foreach(page => {
      mapTabs -= id
      ggTabPane.remove(page.component)
    })

  def selectPage(id: String): Unit =
    mapTabs.get(id).foreach(page => {
      ggTabPane.setSelectedComponent(page.component)
    })

  def setPageEnabled(id: String, enabled: Boolean): Unit =
    mapTabs.get(id).foreach(page => {
      val idx = ggTabPane.indexOfTabComponent(page.component)
      if (idx >= 0) ggTabPane.setEnabledAt(idx, enabled)
    })

  def containsPage(id: String) = mapTabs.contains(id)

  def getPage(id: String) = mapTabs.get(id)
}

trait ObserverPage /* extends DynamicListening */ {
  def component: JComponent

  def id: String

  def title: String

  def pageShown(): Unit

  def pageHidden(): Unit

  def documentChanged(newDoc: Session)
}

object DiffusionObserverPage {
  val id = "observer.diffusion"
}