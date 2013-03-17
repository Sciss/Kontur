/*
 *  ObserverFrame.scala
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

import java.awt.Dimension
import javax.swing.{JComponent, JTabbedPane}
import javax.swing.event.{ChangeEvent, ChangeListener}
import swing.Component

class ObserverFrame extends desktop.impl.WindowImpl with DocumentListener {
  protected def style = desktop.Window.Palette

    private val ggTabPane = new JTabbedPane()
    private var mapTabs   = Map[ String, ObserverPage ]()
    private var shown: Option[ ObserverPage ] = None

  title     = "Observer" // getResourceString("paletteObserver")

  closeOperation = desktop.Window.CloseHide
  // init()
  size = new Dimension(300, 300)

  // ---- constructor ----
    {

//        ggTabPane.setPreferredSize( new Dimension( 400, 400 )) // XXX
        ggTabPane.addChangeListener( new ChangeListener {
            def stateChanged( e: ChangeEvent ) {
                val c = ggTabPane.getSelectedComponent
                val newShown  = if( c != null ) {
                  mapTabs.find( _._2.component == c ).map( _._2 )
                } else None
                if( newShown != shown ) {
                    shown.foreach( _.pageHidden() )
                    shown = newShown
                    shown.foreach( _.pageShown() )
                }
            }
        })
//        ggTabPane.putClientProperty( "JComponent.sizeVariant", "small" )
        contents = Component.wrap(ggTabPane)

      application.addComponent(Kontur.COMP_OBSERVER, this)
    }

  override protected def autoUpdatePrefs = true

  override protected def alwaysPackSize = false

  def addPage(page: ObserverPage) {
    if (containsPage(page.id)) removePage(page.id)
    ggTabPane.addTab(page.title, page.component)
    //       pack()
  }

  def removePage(id: String) {
    mapTabs.get(id).foreach(page => {
      mapTabs -= id
      ggTabPane.remove(page.component)
    })
  }

  def selectPage(id: String) {
    mapTabs.get(id).foreach(page => {
      ggTabPane.setSelectedComponent(page.component)
    })
  }

  def setPageEnabled(id: String, enabled: Boolean) {
    mapTabs.get(id).foreach(page => {
      val idx = ggTabPane.indexOfTabComponent(page.component)
      if (idx >= 0) ggTabPane.setEnabledAt(idx, enabled)
    })
  }

  def containsPage(id: String) = mapTabs.contains(id)

  def getPage(id: String) = mapTabs.get(id)

  // ---- DocumentListener interface ----

  def documentFocussed(e: DocumentEvent) {
    val newDoc = e.getDocument
    mapTabs.foreach(entry => entry._2.documentChanged(newDoc))
  }

  def documentAdded(e: DocumentEvent) {
    /* ignore */
  }

  def documentRemoved(e: DocumentEvent) {
    /* ignore */
  }
}

trait ObserverPage /* extends DynamicListening */ {
  def component: JComponent

  def id: String

  def title: String

  def pageShown(): Unit

  def pageHidden(): Unit

  def documentChanged(newDoc: Document)
}

object DiffusionObserverPage {
  val id = "observer.diffusion"
}