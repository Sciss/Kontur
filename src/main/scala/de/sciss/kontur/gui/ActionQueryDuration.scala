package de.sciss.kontur.gui

import java.awt.event.ActionEvent
import de.sciss.util.{DefaultUnitTranslator, Param, ParamSpace}
import de.sciss.gui.{GUIUtil, MenuAction, SpringPanel}
import de.sciss.common.BasicWindowHandler
import de.sciss.io.Span
import javax.swing.{KeyStroke, Action, JOptionPane}
import java.awt.Component
import de.sciss.kontur.session.Timeline

abstract class ActionQueryDuration extends MenuAction {
   private var value: Option[ Param ] = None
   private var space: Option[ ParamSpace ] = None

   def actionPerformed( e: ActionEvent ) : Unit = perform

   def perform {
      val msgPane     = new SpringPanel( 4, 2, 4, 2 )
      val timeTrans   = new DefaultUnitTranslator()
      val ggDuration  = new ParamField( timeTrans )
      ggDuration.addSpace( ParamSpace.spcTimeHHMMSS )
      ggDuration.addSpace( ParamSpace.spcTimeSmps )
      ggDuration.addSpace( ParamSpace.spcTimeMillis )
      ggDuration.addSpace( ParamSpace.spcTimePercentF )
      msgPane.gridAdd( ggDuration, 0, 0 )
      msgPane.makeCompactGrid()
      GUIUtil.setInitialDialogFocus( ggDuration )

      val tl = timeline // timelineView.timeline
      timeTrans.setLengthAndRate( tl.span.getLength, tl.rate )

      ggDuration.setValue( value getOrElse initialValue )
      space.foreach( sp => ggDuration.setSpace( sp ))

      val op = new JOptionPane( msgPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
      val result = BasicWindowHandler.showDialog( op, parent, getValue( Action.NAME ).toString )

      if( result == JOptionPane.OK_OPTION ) {
         val v = ggDuration.getValue
         value	= Some( v )
         space	= Some( ggDuration.getSpace )
//         val durationSmps = timeTrans.translate( v, ParamSpace.spcTimeSmps ).`val`
//         initiate( durationSmps.toLong )
         initiate( v, timeTrans )
      } else {
         value = None
         space = None
      }
   }

   protected def editName : String = {
      val name = getValue( Action.NAME ).toString
      if( name.endsWith( "..." )) name.substring( 0, name.length - 3 ) else name
   }

   protected def initialValue : Param
   protected def timeline: Timeline
   protected def initiate( v: Param, trans: ParamSpace.Translator ) : Unit
   protected def parent : Component
}