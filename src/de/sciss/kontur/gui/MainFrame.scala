/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.gui.{ LogTextArea }
import de.sciss.kontur.{ Main }
import java.awt.{ BorderLayout, Color, Font }
import java.awt.geom.{ Point2D }
import javax.swing.{ BorderFactory, JInternalFrame, JLabel, JPanel, WindowConstants }

class MainFrame extends AppWindow( AbstractWindow.REGULAR ) {

  // --- constructor ---
  {
//	  val app     = AbstractApplication.getApplication()
      val strMain = app.getResourceString( "frameMain" )

      if( app.getWindowHandler.usesInternalFrames ) {
		setTitle( strMain )
		getWindow.asInstanceOf[ JInternalFrame ].setClosable( false )
	  } else {
		setTitle( app.getName + " : " + strMain )
	  }

      val lta       = new LogTextArea( 16, 40, false, null )
      lta.makeSystemOutput()
      lta.setFont( new Font( "Menlo", Font.PLAIN, 10 ))
      lta.setForeground( Color.white )
      lta.setBackground( Color.black )
//      lta.setBackground( new Color( 0, 0, 0, 0 ))
//      lta.setOpaque( false )
      lta.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ))

      val cp		= getContentPane()
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
      cp.add( ggScroll, BorderLayout.CENTER )

//      cp.setBackground( new Color( 0, 0, 0, 0 ))
//      cp.setOpaque( false )
//      getWindow.setBackground( new Color( 0, 0, 0, 0x7F ))

    setAlpha( 0.85f )

//    cp.add( new JLabel( "Testin" ), BorderLayout.SOUTH )

      getWindow.setBackground( new Color( 0, 0, 0, 0x7F ))

      app.getMenuBarRoot.putMimic( "edit.clear", this, lta.getClearAction )
	  val winListener = new AbstractWindow.Adapter {
			override def windowClosing( e: AbstractWindow.Event ) {
				app.quit
			}
      }
      addListener( winListener )

      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )

      init()
  	  app.addComponent( Main.COMP_MAIN, this )
	  setVisible( true )

//      println( "Testin one two")
  }

   override protected def getPreferredLocation: Point2D = new Point2D.Float( 0f, 0f )

   override def dispose {
		app.removeComponent( Main.COMP_MAIN )
		super.dispose
	}
}
