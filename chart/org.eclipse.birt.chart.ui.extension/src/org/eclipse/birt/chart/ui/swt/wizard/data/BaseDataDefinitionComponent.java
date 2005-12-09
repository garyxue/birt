/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.chart.ui.swt.wizard.data;

import org.eclipse.birt.chart.model.attribute.FormatSpecifier;
import org.eclipse.birt.chart.model.data.DataPackage;
import org.eclipse.birt.chart.model.data.Query;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.QueryImpl;
import org.eclipse.birt.chart.ui.extension.i18n.Messages;
import org.eclipse.birt.chart.ui.swt.composites.FormatSpecifierDialog;
import org.eclipse.birt.chart.ui.swt.composites.RuleEditorDialog;
import org.eclipse.birt.chart.ui.swt.interfaces.ISelectDataComponent;
import org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider;
import org.eclipse.birt.chart.ui.swt.wizard.internal.ColorPalette;
import org.eclipse.birt.chart.ui.swt.wizard.internal.DataDefinitionTextManager;
import org.eclipse.birt.chart.ui.swt.wizard.internal.DataTextDropListener;
import org.eclipse.birt.chart.ui.swt.wizard.internal.SimpleTextTransfer;
import org.eclipse.birt.chart.ui.util.ChartUIUtil;
import org.eclipse.birt.chart.ui.util.UIHelper;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 */

public class BaseDataDefinitionComponent
		implements
			ISelectDataComponent,
			SelectionListener,
			ModifyListener,
			FocusListener,
			KeyListener
{

	private transient Composite cmpTop;

	private transient Text txtDefinition = null;

	private transient Button btnBuilder = null;

	private transient Button btnRuleEditor = null;

	private transient Button btnFormatEditor = null;

	private transient Query query = null;

	private transient SeriesDefinition seriesdefinition = null;

	private transient IUIServiceProvider serviceprovider = null;

	private transient String sTitle = null;

	private transient Object oContext = null;

	private transient String description = ""; //$NON-NLS-1$

	private transient boolean isQueryModified;

	private transient boolean isFormatSpecifiedEnabled = true;

	public BaseDataDefinitionComponent( SeriesDefinition seriesdefinition,
			Query query, IUIServiceProvider builder, Object oContext,
			String sTitle )
	{
		super( );
		this.query = query;
		this.seriesdefinition = seriesdefinition;
		this.serviceprovider = builder;
		this.oContext = oContext;
		this.sTitle = ( sTitle == null || sTitle.length( ) == 0 )
				? Messages.getString( "BaseDataDefinitionComponent.Text.SpecifyDataDefinition" ) //$NON-NLS-1$
				: sTitle;
	}

	public Composite createArea( Composite parent )
	{
		cmpTop = new Composite( parent, SWT.NONE );
		{
			GridLayout glContent = new GridLayout( );
			glContent.numColumns = 4;
			glContent.marginHeight = 0;
			glContent.marginWidth = 0;
			glContent.horizontalSpacing = 2;
			cmpTop.setLayout( glContent );
			GridData gd = new GridData( GridData.FILL_HORIZONTAL );
			cmpTop.setLayoutData( gd );
		}

		if ( description != null )
		{
			new Label( cmpTop, SWT.NONE ).setText( description );
		}

		txtDefinition = new Text( cmpTop, SWT.BORDER | SWT.SINGLE );
		{
			GridData gdTXTDefinition = new GridData( GridData.FILL_HORIZONTAL );
			gdTXTDefinition.widthHint = 50;
			txtDefinition.setLayoutData( gdTXTDefinition );
			if ( query != null && query.getDefinition( ) != null )
			{
				txtDefinition.setText( query.getDefinition( ) );
				txtDefinition.setToolTipText( getTooltipForDataText( query.getDefinition( ) ) );
			}
			txtDefinition.addModifyListener( this );
			txtDefinition.addFocusListener( this );
			txtDefinition.addKeyListener( this );

			// Listener for handling dropping of custom table header
			DropTarget target = new DropTarget( txtDefinition, DND.DROP_COPY );
			Transfer[] types = new Transfer[]{
				SimpleTextTransfer.getInstance( )
			};
			target.setTransfer( types );
			// Add drop support
			target.addDropListener( new DataTextDropListener( txtDefinition ) );
			// Add color manager
			DataDefinitionTextManager.getInstance( )
					.addDataDefinitionText( txtDefinition );
		}

		btnBuilder = new Button( cmpTop, SWT.PUSH );
		GridData gdBTNBuilder = new GridData( );
		gdBTNBuilder.heightHint = 20;
		gdBTNBuilder.widthHint = 20;
		btnBuilder.setLayoutData( gdBTNBuilder );
		btnBuilder.setImage( UIHelper.getImage( "icons/obj16/expressionbuilder.gif" ) ); //$NON-NLS-1$
		btnBuilder.addSelectionListener( this );
		btnBuilder.setToolTipText( Messages.getString( "DataDefinitionComposite.Tooltip.InvokeExpressionBuilder" ) ); //$NON-NLS-1$
		btnBuilder.getImage( ).setBackground( btnBuilder.getBackground( ) );
		if ( serviceprovider == null )
		{
			btnBuilder.setEnabled( false );
		}

		if ( isFormatSpecifiedEnabled )
		{
			btnFormatEditor = new Button( cmpTop, SWT.PUSH );
			GridData gdBTNFormatEditor = new GridData( );
			gdBTNFormatEditor.heightHint = 20;
			gdBTNFormatEditor.widthHint = 20;
			btnFormatEditor.setLayoutData( gdBTNFormatEditor );
			btnFormatEditor.setImage( UIHelper.getImage( "icons/obj16/formatbuilder.gif" ) ); //$NON-NLS-1$
			btnFormatEditor.addSelectionListener( this );
			btnFormatEditor.setToolTipText( Messages.getString( "BaseDataDefinitionComponent.Text.EditFormat" ) ); //$NON-NLS-1$
			btnFormatEditor.getImage( )
					.setBackground( btnFormatEditor.getBackground( ) );
		}

		// Updatas color setting
		setColor( );

		return cmpTop;
	}

	public void selectArea( boolean selected, Object data )
	{
		if ( data instanceof Object[] )
		{
			Object[] array = (Object[]) data;
			seriesdefinition = (SeriesDefinition) array[0];
			query = (Query) array[1];
			txtDefinition.setText( query.getDefinition( ) );
		}
		setColor( );
	}

	private void setColor( )
	{
		if ( query != null )
		{
			Color cColor = ColorPalette.getInstance( )
					.getColor( ChartUIUtil.getColumnName( query.getDefinition( ) ) );
			ChartUIUtil.setBackgroundColor( txtDefinition, true, cColor );
		}
	}

	public void dispose( )
	{
		DataDefinitionTextManager.getInstance( )
				.removeDataDefinitionText( txtDefinition );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected( SelectionEvent e )
	{
		if ( e.getSource( ).equals( btnBuilder ) )
		{
			String sExpr = serviceprovider.invoke( txtDefinition.getText( ),
					oContext,
					sTitle );
			txtDefinition.setText( sExpr );
			query.setDefinition( sExpr );
		}
		else if ( e.getSource( ).equals( btnRuleEditor ) )
		{
			RuleEditorDialog editor = new RuleEditorDialog( cmpTop.getShell( ),
					(Query) EcoreUtil.copy( query ),
					sTitle );
			if ( !editor.wasCancelled( ) )
			{
				query.getRules( ).addAll( editor.getRules( ) );
			}
		}
		else if ( e.getSource( ).equals( btnFormatEditor ) )
		{
			FormatSpecifier formatspecifier = seriesdefinition.getFormatSpecifier( );
			FormatSpecifierDialog editor = new FormatSpecifierDialog( cmpTop.getShell( ),
					formatspecifier,
					sTitle );
			if ( !editor.wasCancelled( ) )
			{
				if ( editor.getFormatSpecifier( ) == null )
				{
					seriesdefinition.eUnset( DataPackage.eINSTANCE.getSeriesDefinition_FormatSpecifier( ) );
				}
				else
				{
					seriesdefinition.setFormatSpecifier( editor.getFormatSpecifier( ) );
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected( SelectionEvent e )
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	public void modifyText( ModifyEvent e )
	{
		if ( e.getSource( ).equals( txtDefinition ) )
		{
			isQueryModified = true;
			// Reset tooltip
			txtDefinition.setToolTipText( getTooltipForDataText( txtDefinition.getText( ) ) );
		}
	}

	/**
	 * Sets the description in the left of data text box.
	 * 
	 * @param description
	 */
	public void setDescription( String description )
	{
		this.description = description;
	}

	public void focusGained( FocusEvent e )
	{
		// TODO Auto-generated method stub

	}

	public void focusLost( FocusEvent e )
	{
		// Null event is fired by Drop Listener manually
		if ( e == null || e.widget.equals( txtDefinition ) )
		{
			saveQuery( );
		}
	}

	private void saveQuery( )
	{
		if ( isQueryModified )
		{
			if ( query != null )
			{
				query.setDefinition( txtDefinition.getText( ) );
			}
			else
			{
				query = QueryImpl.create( txtDefinition.getText( ) );
				query.eAdapters( ).addAll( seriesdefinition.eAdapters( ) );
			}
			// Refresh color from ColorPalette
			setColor( );
			txtDefinition.getParent( ).layout( );
			isQueryModified = false;
		}
	}

	private String getTooltipForDataText( String queryText )
	{
		if ( queryText.trim( ).length( ) == 0 )
		{
			return Messages.getString( "BaseDataDefinitionComponent.Tooltip.InputValueExpression" ); //$NON-NLS-1$
		}
		return queryText;
	}

	public void keyPressed( KeyEvent e )
	{
		if ( e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR )
		{
			saveQuery( );
		}
	}

	public void keyReleased( KeyEvent e )
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Enables or disables the format specifier. Default value is enabled.
	 * 
	 * @param isEnabled
	 *            The flag to enable or disable the format specifier.
	 */
	public void setFormatSpecifierEnabled( boolean isEnabled )
	{
		this.isFormatSpecifiedEnabled = isEnabled;
	}
}
