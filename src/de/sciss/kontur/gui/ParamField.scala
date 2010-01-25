/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.beans.{ PropertyChangeEvent, PropertyChangeListener }
import de.sciss.util.{ DefaultUnitTranslator, ParamSpace }

class ParamField( ut: ParamSpace.Translator )
extends de.sciss.gui.PrefParamField( ut ) {
    def this() = this( new DefaultUnitTranslator() )

    addPropertyChangeListener( "JComponent.sizeVariant", new PropertyChangeListener {
        def propertyChange( pce: PropertyChangeEvent ) {
            ggNumber.putClientProperty( pce.getPropertyName(), pce.getNewValue() )
        }
    })

    override def getBaseline( width: Int, height: Int ) =
       ggNumber.getBaseline( width, height ) + ggNumber.getY

    def setEditable( b: Boolean ) {
      ggNumber.setEditable( b )
    }

    def isEditable = ggNumber.isEditable
}
