/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.gui.{ LogTextArea }
import de.sciss.kontur.{ Main }
import java.awt.{ BorderLayout, Font }
import javax.swing.{ JInternalFrame, WindowConstants }

class MainFrame extends AppWindow( AbstractWindow.REGULAR ) {

  // --- constructor ---
  {
	  val app     = AbstractApplication.getApplication()
      val strMain = app.getResourceString( "frameMain" )

      if( app.getWindowHandler().usesInternalFrames() ) {
		setTitle( strMain )
		getWindow().asInstanceOf[ JInternalFrame ].setClosable( false )
	  } else {
		setTitle( app.getName() + " : " + strMain )
	  }

      val lta       = new LogTextArea( 16, 40, false, null )
	  val ggScroll  = lta.placeMeInAPane()
      lta.makeSystemOutput()
      lta.setFont( new Font( "Menlo", Font.PLAIN, 11 ))

      val cp		= getContentPane()
      cp.add( ggScroll, BorderLayout.CENTER )

      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )

      init()
  	  app.addComponent( Main.COMP_MAIN, this )
	  setVisible( true )

//      println( "Testin one two")
  }
}
