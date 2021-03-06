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

package org.eclipse.birt.report.model.elements.interfaces;

/**
 * The interface master page Line element to store the constants.
 */
public interface IMasterPageModel
{

	/**
	 * Name of the page type property. This gives a name to the page size such
	 * as A4 or US Letter.
	 */

	public static final String TYPE_PROP = "type"; //$NON-NLS-1$

	/**
	 * Name of the property that gives the orientation of a standard-sized page.
	 * Ignored for custom-sized pages.
	 */

	public static final String ORIENTATION_PROP = "orientation"; //$NON-NLS-1$
	/**
	 * The name of the custom height property set when using a custom-sized
	 * page. Ignored for standard-sized pages.
	 */

	public static final String HEIGHT_PROP = "height"; //$NON-NLS-1$

	/**
	 * The name of the custom width property set when using a custom-sized page.
	 * Ignored for standard-sized pages.
	 */

	public static final String WIDTH_PROP = "width"; //$NON-NLS-1$

	/**
	 * Name of the dimension property that gives the amount of space between the
	 * bottom of the page and the page content.
	 */

	public static final String BOTTOM_MARGIN_PROP = "bottomMargin"; //$NON-NLS-1$

	/**
	 * Name of the dimension property that gives the amount of space between the
	 * right of the page and the page content.
	 */

	public static final String RIGHT_MARGIN_PROP = "rightMargin"; //$NON-NLS-1$

	/**
	 * Name of the dimension property that gives the amount of space between the
	 * top of the page and the page content.
	 */

	public static final String TOP_MARGIN_PROP = "topMargin"; //$NON-NLS-1$

	/**
	 * Name of the dimension property that gives the amount of space between the
	 * left of the page and the page content.
	 */

	public static final String LEFT_MARGIN_PROP = "leftMargin"; //$NON-NLS-1$

	/**
	 * Property name for the reference to the shared style.
	 */

	public static final String STYLE_PROP = "style"; //$NON-NLS-1$

	/**
	 * Height of the US Letter page.
	 */

	public static final String US_LETTER_HEIGHT = "11in"; //$NON-NLS-1$

	/**
	 * Width of the US Letter page.
	 */

	public static final String US_LETTER_WIDTH = "8.5in"; //$NON-NLS-1$

	/**
	 * Height of the US Legal page.
	 */

	public static final String US_LEGAL_HEIGHT = "14in"; //$NON-NLS-1$

	/**
	 * Width of the US Legal page.
	 */

	public static final String US_LEGAL_WIDTH = "8.5in"; //$NON-NLS-1$

	/**
	 * Height of the A4 page.
	 */

	public static final String A4_HEIGHT = "297mm"; //$NON-NLS-1$

	/**
	 * Width of the A4 page.
	 */

	public static final String A4_WIDTH = "210mm"; //$NON-NLS-1$

	/**
	 * Height of the ledger page.
	 */

	public static final String US_LEDGER_HEIGHT = "17in";//$NON-NLS-1$

	/**
	 * Width of the ledger page.
	 */

	public static final String US_LEDGER_WIDTH = "11in";//$NON-NLS-1$

	/**
	 * Height of the Super B page.
	 */

	public static final String US_SUPER_B_HEIGHT = "19in";//$NON-NLS-1$

	/**
	 * Width of the Super B page.
	 */

	public static final String US_SUPER_B_WIDTH = "13in";//$NON-NLS-1$

	/**
	 * Height of the A5 page.
	 */

	public static final String A5_HEIGHT = "210mm";//$NON-NLS-1$

	/**
	 * Width of the A5 page.
	 */

	public static final String A5_WIDTH = "148mm";//$NON-NLS-1$

	/**
	 * Height of the A3 page.
	 */

	public static final String A3_HEIGHT = "420mm";//$NON-NLS-1$

	/**
	 * Width of the A3 page.
	 */

	public static final String A3_WIDTH = "297mm";//$NON-NLS-1$

	/**
	 * Name of the method on page start.
	 */
	public static final String ON_PAGE_START_METHOD = "onPageStart"; //$NON-NLS-1$

	/**
	 * Name of the method on page end.
	 */
	public static final String ON_PAGE_END_METHOD = "onPageEnd"; //$NON-NLS-1$
	
	/**
	 * Name of the property that gives the number of columns to appear on the
	 * page.
	 */

	public static final String COLUMNS_PROP = "columns"; //$NON-NLS-1$

	/**
	 * Name of the dimension property that gives the spacing between columns of
	 * a multi-column page.
	 */

	public static final String COLUMN_SPACING_PROP = "columnSpacing"; //$NON-NLS-1$
}
