/*
 *  MainFrame.scala
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

import java.awt.{Color, Font}
import java.awt.geom.Point2D
import javax.swing.{BorderFactory, JInternalFrame}

class MainFrame extends desktop.impl.WindowImpl {
  protected def style = desktop.Window.Regular

  // --- constructor ---
  {
//	  val app     = AbstractApplication.getApplication()
      val strMain = app.getResourceString( "frameMain" )

    if (app.getWindowHandler.usesInternalFrames) {
      title = strMain
      getWindow.asInstanceOf[JInternalFrame].setClosable(false)
    } else {
      title = app.name + " : " + strMain
    }

    val lta       = new LogTextArea( 32, 60, false, null )
      lta.makeSystemOutput()
      lta.setFont( new Font( "Menlo", Font.PLAIN, 10 ))
      lta.setForeground( Color.white )
      lta.setBackground( Color.black )
//      lta.setBackground( new Color( 0, 0, 0, 0 ))
//      lta.setOpaque( false )
      lta.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ))

//      val cp = new JPanel( new BorderLayout() )
//      cp.setOpaque( false )
//      cp.setBackground( new Color( 0, 0, 0, 0 ))
//      setContentPane( cp )
//      cp.add( lta, BorderLayout.CENTER )
	  val ggScroll  = lta.placeMeInAPane()
      ggScroll.setBorder( null )
//      ggScroll.setBackground( new Color( 0, 0, 0, 0 ))
//      ggScroll.setOpaque( false )
//      val vp = ggScroll.getViewport
//      vp.setBackground( new Color( 0, 0, 0, 0 ))
//      vp.setOpaque( false )
     contents = ggScroll

//      cp.setBackground( new Color( 0, 0, 0, 0 ))
//      cp.setOpaque( false )
//      getWindow.setBackground( new Color( 0, 0, 0, 0x7F ))

    alpha = 0.85f

//    cp.add( new JLabel( "Testin" ), BorderLayout.SOUTH )

    component.background = new Color(0, 0, 0, 0x7F)

    app.getMenuBarRoot.putMimic("edit.clear", this, lta.getClearAction)
    val winListener = new AbstractWindow.Adapter {
      override def windowClosing(e: AbstractWindow.Event) {
        app.quit()
      }
    }
    addListener(winListener)

    closeOperation = desktop.Window.CloseIgnore

//    init()
    app.addComponent(Kontur.COMP_MAIN, this)
    visible = true

    // println( "Testin one two")
  }

  override protected def getPreferredLocation: Point2D = new Point2D.Float(0f, 0f)

  override def dispose() {
    app.removeComponent(Kontur.COMP_MAIN)
    super.dispose()
  }
}
