//package de.sciss.kontur
//package desktop
//package impl
//
//import swing.Action
//
///**
// *  A simple extension of <code>AbstractAction</code>
// *  that puts a <code>KeyStroke</code> into its
// *  <code>ACCELERATOR_KEY</code> field. This field
// *  is read when the action is attached to a
// *  <code>JMenuItem</code>.
// */
//abstract class MenuActionImpl(title: String) extends MenuAction(String) {
//	/**
//	 *  Constructs a new <code>MenuAction</code> with the given
//	 *  text and accelerator shortcut which will be
//	 *  used when the action is attached to a <code>JMenuItem</code>.
//	 *
//	 *  @param  text		text to display in the menu item
//	 *  @param  shortcut	<code>KeyStroke</code> for the
//	 *						menu item's accelerator or <code>null</code>
//	 */
//	public MenuAction( String text, KeyStroke shortcut )
//	{
//		super( text );
//		if( shortcut != null ) putValue( ACCELERATOR_KEY, shortcut );
//	}
//
//	/**
//	 *  Constructs a new <code>MenuAction</code>
//	 *  without accelerator key.
//	 *
//	 *  @param  text		text to display in the menu item
//	 */
//	public MenuAction( String text )
//	{
//		super( text );
//	}
//
//	/**
//	 *  Constructs a new <code>MenuAction</code>
//	 *  without name and accelerator key.
//	 */
//	public MenuAction()
//	{
//		super();
//	}
//
//	/**
//	 *	Copies the mappings of a given
//	 *	<code>Action</code> to this <code>Action</code>.
//	 *	The entries which are copied are name,
//	 *	key short cuts and descriptions. Therefore
//	 *	a menu item carrying this action will look
//	 *	exactly like the one associated with the
//	 *	passed in action. Also the enabled flag is
//	 *	toggled accordingly.
//	 *
//	 *	@param	a	an <code>Action</code> from which to
//	 *				copy the mapping entries
//	 */
//	public void mimic( Action a )
//	{
//		this.putValue( NAME, a.getValue( NAME ));
//		this.putValue( SMALL_ICON, a.getValue( SMALL_ICON ));
//		this.putValue( ACCELERATOR_KEY, a.getValue( ACCELERATOR_KEY ));
//		this.putValue( MNEMONIC_KEY, a.getValue( MNEMONIC_KEY ));
//		this.putValue( SHORT_DESCRIPTION, a.getValue( SHORT_DESCRIPTION ));
//		this.putValue( LONG_DESCRIPTION, a.getValue( LONG_DESCRIPTION ));
//		this.setEnabled( a.isEnabled() );
//	}
//
//	/**
//	 *	Installs this action on the
//	 *	keyboard input and action map of the given
//	 *	component.
//	 *
//	 *	@param	c			the component to install the action on
//	 *	@param	condition	either of <code>JComponent.WHEN_FOCUSED</code>,
//	 *						<code>JComponent.WHEN_IN_FOCUSED_WINDOW</code>, or
//	 *						<code>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code>.
//	 */
//	public void installOn( JComponent c, int condition )
//	{
//		final InputMap	imap = c.getInputMap( condition );
//		final ActionMap amap = c.getActionMap();
//		final String	name = (String) getValue( Action.NAME );
//		imap.put( (KeyStroke) getValue( Action.ACCELERATOR_KEY ), name );
//		amap.put( name, this );
//	}
//
//	/**
//	 *	Deinstalls this action from the
//	 *	keyboard input and action map of the given
//	 *	component.
//	 *
//	 *	@param	c			the component to remove the action from
//	 *	@param	condition	either of <code>JComponent.WHEN_FOCUSED</code>,
//	 *						<code>JComponent.WHEN_IN_FOCUSED_WINDOW</code>, or
//	 *						<code>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code>.
//	 */
//	public void deinstallFrom( JComponent c, int condition )
//	{
//		final InputMap	imap = c.getInputMap( condition );
//		final ActionMap amap = c.getActionMap();
//		imap.remove( (KeyStroke) getValue( Action.ACCELERATOR_KEY ));
//		amap.remove( getValue( Action.NAME ));
//	}
//
//	public abstract void actionPerformed( ActionEvent e );
//}
