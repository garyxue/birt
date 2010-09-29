
package org.eclipse.birt.report.engine.emitter.excel.layout;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.eclipse.birt.report.engine.content.IDataContent;
import org.eclipse.birt.report.engine.content.IStyle;
import org.eclipse.birt.report.engine.emitter.excel.BlankData;
import org.eclipse.birt.report.engine.emitter.excel.BookmarkDef;
import org.eclipse.birt.report.engine.emitter.excel.Data;
import org.eclipse.birt.report.engine.emitter.excel.DataCache;
import org.eclipse.birt.report.engine.emitter.excel.DateTimeUtil;
import org.eclipse.birt.report.engine.emitter.excel.ExcelEmitter;
import org.eclipse.birt.report.engine.emitter.excel.ExcelUtil;
import org.eclipse.birt.report.engine.emitter.excel.HyperlinkDef;
import org.eclipse.birt.report.engine.emitter.excel.RowData;
import org.eclipse.birt.report.engine.emitter.excel.Span;
import org.eclipse.birt.report.engine.emitter.excel.StyleBuilder;
import org.eclipse.birt.report.engine.emitter.excel.StyleConstant;
import org.eclipse.birt.report.engine.emitter.excel.StyleEngine;
import org.eclipse.birt.report.engine.emitter.excel.StyleEntry;

public class ExcelLayoutEngine
{
	public final static String EMPTY = "";
	
	public final static int MAX_ROW = 65535;
	
	public static int MAX_COLUMN = 255;

	private DataCache cache;

	private AxisProcessor axis;

	private StyleEngine engine;	
	
	private ExcelEmitter emitter;

	private Stack<XlsContainer> containers = new Stack<XlsContainer>( );

	private Stack<XlsTable> tables = new Stack<XlsTable>( );

	private Hashtable<String, String> links = new Hashtable<String, String>( );
	
	private static final double DEFAULT_ROW_HEIGHT = 15;
	
	ExcelContext context = null;

	public ExcelLayoutEngine( PageDef page, ExcelContext context,
			ExcelEmitter emitter )
	{
		this.context = context;
		this.emitter = emitter;
		initalize( page );
	}
	
	private void initalize(PageDef page)
	{
		axis = new AxisProcessor( );		
		axis.addCoordinate( page.contentwidth );

		setCacheSize();
		
		ContainerSizeInfo rule = new ContainerSizeInfo( 0, page.contentwidth );
		cache = new DataCache( MAX_COLUMN, MAX_ROW );
		engine = new StyleEngine( this );
		containers.push( createContainer( rule, page.style, null ) );
	}
	
	private void setCacheSize()
	{
		if(context.getOfficeVersion( ).equals( "office2007" ))
		{
			MAX_COLUMN = 10000;
		}
	}

	public XlsContainer getCurrentContainer( )
	{
		return (XlsContainer) containers.peek( );
	}

	public Stack<XlsContainer> getContainers( )
	{
		return containers;
	}

	public void addTable( TableInfo table, IStyle style )
	{
		XlsContainer currentContainer = getCurrentContainer( );
		ContainerSizeInfo parentSizeInfo = currentContainer.getSizeInfo( );
		int startCoordinate = parentSizeInfo.getStartCoordinate( );
		int endCoordinate = parentSizeInfo.getEndCoordinate( );
		//npos is the start position of each column.
		
		int[] columnStartCoordinates = calculateColumnCoordinates( table,
				startCoordinate, endCoordinate );

		splitColumns( startCoordinate, endCoordinate, columnStartCoordinates );

		createTable( table, style, currentContainer, columnStartCoordinates );
	}

	private void createTable( TableInfo tableInfo, IStyle style,
			XlsContainer currentContainer, int[] columnStartCoordinates )
	{
		int leftCordinate = columnStartCoordinates[0];
		int width = columnStartCoordinates[columnStartCoordinates.length - 1]
				- leftCordinate;
		ContainerSizeInfo sizeInfo = new ContainerSizeInfo( leftCordinate,
				width );
		StyleEntry styleEntry = engine.createEntry( sizeInfo, style );
		XlsTable table = new XlsTable( tableInfo, styleEntry,
				sizeInfo, currentContainer );
		tables.push( table );
		addContainer( table );
	}

	private void splitColumns( int startCoordinate, int endCoordinate,
			int[] columnStartCoordinates )
	{
		int[] scale = axis.getColumnCoordinatesInRange( startCoordinate,
				endCoordinate );

		for ( int i = 0; i < scale.length - 1; i++ )
		{
			int startPosition = scale[i];
			int endPostion = scale[i + 1];

			int[] range = inRange( startPosition, endPostion, columnStartCoordinates );

			if ( range.length > 0 )
			{				
				int pos = axis.getColumnIndexByCoordinate( startPosition );
				cache.insertColumns( pos, range.length );

				for ( int j = 0; j < range.length; j++ )
				{
					axis.addCoordinate( range[j] );
				}
			}
		}
	}

	private int[] calculateColumnCoordinates( TableInfo table,
			int startCoordinate,
			int endCoordinate )
	{
		XlsContainer currentContainer = getCurrentContainer( );
		int columnCount = table.getColumnCount( );
		int[] columnStartCoordinates = new int[ columnCount + 1 ];
		if ( isRightAligned( currentContainer ) )
		{
			columnStartCoordinates[ columnCount ] = endCoordinate;
			for ( int i = columnCount -  1; i >= 0; i-- )
			{
				columnStartCoordinates[i] = columnStartCoordinates[i + 1]
						- table.getColumnWidth( i );
			}
		}
		else
		{
			columnStartCoordinates[0] = startCoordinate;
			for ( int i = 1; i <= columnCount; i++ )
			{
				columnStartCoordinates[i] = columnStartCoordinates[i - 1]
						+ table.getColumnWidth( i - 1 );
			}
		}
		return columnStartCoordinates;
	}

	private boolean isRightAligned( XlsContainer currentContainer )
	{
		boolean isRightAligned = false;
		String align = currentContainer.getStyle( ).getProperty(
				StyleConstant.H_ALIGN_PROP );
		isRightAligned = "Right".equalsIgnoreCase( align );
		return isRightAligned;
	}

	private int[] inRange( int start, int end, int[] data )
	{
		int[] range = new int[data.length];
		int count = 0;

		for ( int i = 0; i < data.length; i++ )
		{
			if ( ( data[i] > start ) && ( data[i] < end ) )
			{
				count++;
				range[count] = data[i];
			}
		}

		int[] result = new int[count];

		int j = 0;
		for ( int i = 0; i < range.length; i++ )
		{
			if ( range[i] != 0 )
			{
				result[j] = range[i];
				j++;
			}
		}

		return result;
	}

	public void addCell( int col, int colSpan, int rowSpan, IStyle style )
	{
		XlsTable table = tables.peek( );
		ContainerSizeInfo cellSizeInfo = table.getColumnSizeInfo( col, colSpan );
		XlsCell cell = new XlsCell( engine.createEntry( cellSizeInfo, style ),
				cellSizeInfo, getCurrentContainer( ), rowSpan );
		addContainer( cell );
	}

	public void endCell( )
	{
		endNormalContainer( );
	}

	public void addRow( IStyle style )
	{
		XlsContainer parent = getCurrentContainer( );
		ContainerSizeInfo sizeInfo = parent.getSizeInfo( );
		XlsContainer container = createContainer( sizeInfo, style,
				parent );
		container.setEmpty( false );
		addContainer( container );
	}

	public void endRow( )
	{
		synchronize( );
		endContainer( );
	}

	private void synchronize( )
	{
		XlsContainer rowContainer = getCurrentContainer( );
		ContainerSizeInfo rowSizeInfo = rowContainer.getSizeInfo( );
		int startCoordinate = rowSizeInfo.getStartCoordinate( );
		int endCoordinate = rowSizeInfo.getEndCoordinate( );
		int startColumnIndex = axis.getColumnIndexByCoordinate( startCoordinate );
		int endColumnIndex = axis.getColumnIndexByCoordinate( endCoordinate );

		int maxRowIndex = 0;
		int rowIndexes[] = new int[endColumnIndex - startColumnIndex];

		for ( int currentColumnIndex = startColumnIndex; currentColumnIndex < endColumnIndex; currentColumnIndex++ )
		{			
			int rowIndex = cache.getMaxRowIndex( currentColumnIndex );
			rowIndexes[currentColumnIndex - startColumnIndex] = rowIndex;
			maxRowIndex = maxRowIndex > rowIndex ? maxRowIndex : rowIndex;
		}
		int startRowIndex = rowContainer.getRowIndex( );
		if ( maxRowIndex == startRowIndex )
		{
			maxRowIndex++;
		}
		rowContainer.setRowIndex( maxRowIndex );
		
		for ( int currentColumnIndex = startColumnIndex; currentColumnIndex < endColumnIndex; currentColumnIndex++ )
		{
			int rowspan = maxRowIndex - rowIndexes[currentColumnIndex - startColumnIndex];
			if ( rowspan > 0 )
			{
				Data data = null;				
				Data upstair = cache
						.getColumnLastData( currentColumnIndex );
				
				if ( upstair != null
						&& canSpan( upstair, rowContainer, currentColumnIndex,
								endColumnIndex ) )
				{
					Data predata = upstair;
					int rs = predata.getRowSpan( ) + rowspan;
					predata.setRowSpan( rs );
					BlankData blankData = new BlankData( getRealData( predata ) );
					if ( !isInContainer( predata, rowContainer ))
					{
						blankData.decreasRowSpanInDesign( );
					}
					int rowIndex = predata.getRowIndex( );
					for ( int p = 1; p <= rowspan; p++ )
					{
						BlankData blank = new BlankData( predata );
						blank.setRowIndex( rowIndex + p );
						cache.addData( currentColumnIndex, blank );
					}
				}
			}
		}
	}

	private boolean canSpan( Data data, XlsContainer rowContainer, int currentColumn, int lastColumn )
	{
		Data realData = getRealData(data);
		if ( realData == null )
			return false;
		if ( !isInContainer( realData, rowContainer ) )
		{
			return false;
		}
		
		//Data can only span if the span doesn't conflict with some other items.
		for ( int i = currentColumn + 1; i < lastColumn; i++ )
		{
			Data lastData = cache.getColumnLastData( i );
			Data lastRealData = getRealData( lastData );
			
			// If there is some item under current data and would be
			// overridden if current data span rows, then current data can't
			// span.
			if ( lastRealData == null
					|| lastRealData.getRowIndex( ) <= realData.getRowIndex( ) )
			{
				continue;
			}
			if ( realData.getSizeInfo( ).getEndCoordinate( ) > lastRealData
					.getSizeInfo( ).getStartCoordinate( ) )
			{
				return false;
			}
		}
		return realData.getRowSpanInDesign( ) > 0;
	}
	
	private Data getRealData(Data data )
	{
		if ( data.isBlank( ) )
		{
			return ((BlankData)data).getData( );
		}
		return data;
	}
	
	private boolean isInContainer(Data data, XlsContainer rowContainer )
	{
		XlsContainer container = data.getContainer( );
		while( container != null )
		{
			if ( container == rowContainer )
			{
				return true;
			}
			container = container.getParent( );
		}
		return false;
	}
	
	public void endTable( )
	{
		if ( !tables.isEmpty( ) )
		{
			tables.pop( );
			endContainer( );
		}
	}

	public void addContainer( IStyle style, HyperlinkDef link )
	{
		XlsContainer parent = getCurrentContainer( );
		ContainerSizeInfo sizeInfo = parent.getSizeInfo( );
		StyleEntry entry = engine.createEntry( sizeInfo, style );
		addContainer( new XlsContainer( entry, sizeInfo, parent ) );
	}

	private void addContainer( XlsContainer child )
	{
		XlsContainer parent = child.getParent( );
		if ( parent != null )
		{
			parent.setEmpty( false );
		}
		containers.push( child);
	}
	
	public void endContainer( )
	{
		setParentContainerIndex( );
		endNormalContainer( );
	}

	private void setParentContainerIndex( )
	{
		XlsContainer container = getCurrentContainer( );
		XlsContainer parent = container.getParent( );
		if ( parent != null )
			parent.setRowIndex( container.getRowIndex( ) );
	}

	private void endNormalContainer( )
	{
		XlsContainer container = getCurrentContainer( );
		if ( container.isEmpty( ) )
		{
			Data data = new Data( EMPTY, container.getStyle( ), Data.STRING,
					container );
			data.setSizeInfo( container.getSizeInfo( ) );
			addData( data );
		}
		engine.applyContainerBottomStyle( );
		containers.pop( );
	}
	
	public void addData( Object txt, IStyle style, HyperlinkDef link, BookmarkDef bookmark )
	{
		ContainerSizeInfo rule = getCurrentContainer( ).getSizeInfo( );
		StyleEntry entry = engine.getStyle( style, rule );
		Data data = createData( txt, entry );
		data.setHyperlinkDef( link );
		data.setBookmark( bookmark );
		data.setSizeInfo( rule );

		addData( data );
	}
	
	public void addDateTime(Object txt, IStyle style, HyperlinkDef link, BookmarkDef bookmark)
	{
		ContainerSizeInfo rule = getCurrentContainer( ).getSizeInfo( );
		StyleEntry entry = engine.getStyle( style, rule );
		Data data = null;
		
		IDataContent dataContent = (IDataContent)txt;
		Object value = dataContent.getValue( );
		Date date = ExcelUtil.getDate( value );
		
		//If date time is before 1900, it must be output as string, otherwise, excel can't format the date.
		if ( date != null
				&& ( ( date instanceof Time ) || date.getYear( ) >= 0 ) )
		{
			data = createDateData( value, entry, style.getDateTimeFormat( ) );
			data.setHyperlinkDef( link );
			data.setBookmark( bookmark );
			data.setSizeInfo( rule );
			addData( data );
		}
		else
		{
			addData( dataContent.getText( ), style, link, bookmark );
		}
	}

	public void addCaption( String text )
	{
		ContainerSizeInfo rule = getCurrentContainer( ).getSizeInfo( );
		StyleEntry entry = StyleBuilder.createEmptyStyleEntry( );
		entry.setProperty( StyleEntry.H_ALIGN_PROP, "Center" );
		Data data = createData( text, entry );
		data.setSizeInfo( rule );

		addData( data );
	}

	public Data createData( Object txt, StyleEntry entry )
	{
		String type = Data.STRING;
		Locale locale = emitter.getLocale( );
		if ( Data.NUMBER.equals( ExcelUtil.getType( txt ) ) )
		{
			String format = ExcelUtil.getPattern( txt, entry
					.getProperty( StyleConstant.NUMBER_FORMAT_PROP ) );
			format = ExcelUtil.formatNumberPattern( format, locale );
			entry.setProperty( StyleConstant.NUMBER_FORMAT_PROP, format );
			type = Data.NUMBER;

		}
		else if ( Data.DATE.equals( ExcelUtil.getType( txt )))
		{
			String format = ExcelUtil.getPattern( txt, 
					entry.getProperty( StyleConstant.DATE_FORMAT_PROP ) );
			entry.setProperty( StyleConstant.DATE_FORMAT_PROP, format );			
			type = Data.DATE;
		}
		
		entry.setProperty( StyleConstant.DATA_TYPE_PROP, type );
		
		return new Data( txt, entry, type, getCurrentContainer( ) );
	}
	
	private Data createDateData(Object txt , StyleEntry entry , String timeFormat)
	{
		Locale locale = emitter.getLocale();
		timeFormat = ExcelUtil.parse( timeFormat, locale );
		if ( timeFormat.equals( "" ) )
		{
			if ( txt instanceof java.sql.Date )
			{
				timeFormat = DateTimeUtil
						.formatDateTime( "MMM d, yyyy", locale );
			}
			else if ( txt instanceof java.sql.Time )
			{
				timeFormat = DateTimeUtil.formatDateTime( "H:mm:ss AM/PM",
						locale );
			}
			else
			{
				timeFormat = DateTimeUtil.formatDateTime(
						"MMM d, yyyy H:mm AM/PM", locale );
			}
		}
		else
		{
			timeFormat = DateTimeUtil.formatDateTime( timeFormat, locale );
		}
		entry.setProperty( StyleConstant.DATE_FORMAT_PROP, timeFormat );
		entry.setProperty( StyleConstant.DATA_TYPE_PROP, Data.DATE );
		return new Data( txt, entry, Data.DATE, getCurrentContainer( ) );
	}

	private void addData( Data data )
	{
		XlsContainer container = getCurrentContainer( );
		container.setEmpty( false );
		int col = axis.getColumnIndexByCoordinate( data.getSizeInfo( ).getStartCoordinate( ) );
		int span = axis.getColumnIndexByCoordinate( data.getSizeInfo( ).getEndCoordinate( ) ) - col;
		applyTopBorderStyle( data );
		updataRowIndex( data, container );
		// FIXME: there is a bug when this data is in middle of a row.
		outputDataIfBufferIsFull( );
		addDatatoCache( col, data );
		Data newData = new Data( data );
		for ( int i = col + 1; i < col + span; i++ )
		{
			BlankData blankData = new BlankData( newData );
			addDatatoCache( i, blankData );
		}
		if ( container instanceof XlsCell )
		{
			XlsCell cell = (XlsCell)container;
			data.setRowSpanInDesign( cell.getRowSpan( ) - 1 );
		}
	}
	
	private void updataRowIndex( Data data, XlsContainer container )
	{
		int rowIndex = container.getRowIndex( ) + 1;
		data.setRowIndex( rowIndex );
		container.setRowIndex( rowIndex );
	}

	private void outputDataIfBufferIsFull( )
	{
		if ( getCurrentContainer( ).getRowIndex( ) >= MAX_ROW )
		{
			emitter.outputSheet( );
			cache.clearCachedSheetData( );
			resetContainers( );
		}
	}

	/**
	 * @param data
	 */
	private void applyTopBorderStyle( Data data )
	{
		XlsContainer container = getCurrentContainer( );
		int rowIndex = container.getRowIndex( );
		XlsContainer parent = container;
		while ( parent != null && parent.getStartRowId( ) == rowIndex )
		{
			StyleBuilder.applyTopBorder( parent.getStyle( ), data
					.getStyle( ) );
			parent = parent.getParent( );
		}
	}

	public XlsContainer createContainer( ContainerSizeInfo sizeInfo,
			IStyle style, XlsContainer parent )
	{
		return new XlsContainer( engine.createEntry( sizeInfo, style ),
				sizeInfo, parent );
	}

	public XlsContainer createCellContainer( IStyle style, XlsContainer parent, int rowSpan )
	{
		ContainerSizeInfo sizeInfo = parent.getSizeInfo( );
		return new XlsCell( engine.createEntry( sizeInfo, style ), sizeInfo,
				parent, rowSpan );
	}

	public Map<StyleEntry,Integer> getStyleMap( )
	{
		return engine.getStyleIDMap( );
	}
	
	public List<BookmarkDef> getNamesRefer( )
	{
		return cache.getBookmarks( );
	}

	public int[] getCoordinates( )
	{
		int[] coord = axis.getColumnWidths( );
		
		if(coord.length <= MAX_COLUMN) 
		{
			return coord;
		}	
		else 
		{
			int[] ncoord = new int[MAX_COLUMN]; 
			System.arraycopy( coord, 0, ncoord, 0, MAX_COLUMN );
			return ncoord;
		}		
	}

	public int getRowCount( )
	{
		int realcount = cache.getMaxRow( );
		return realcount;
	}

	public AxisProcessor getAxis( )
	{
		return axis;
	}

	public Data getColumnLastData( int column )
	{
		return cache.getColumnLastData( column );
	}
	
	private void addDatatoCache( int col, Data value )
	{
		cache.addData( col, value );
	}

	public void complete( )
	{
		Iterator<Data[]> iterator = cache.getRowIterator( );
		while ( iterator.hasNext( ) )
		{
			Data[] rowData = iterator.next( );

			for ( int j = 0; j < rowData.length; j++ )
			{
				Data data = rowData[j];
				if ( data == null || data.isBlank( ) )
				{
					continue;
				}

				int styleid = engine.getStyleID( data.getStyle( ) );
				data.setStyleId( styleid );
				ContainerSizeInfo rule = data.getSizeInfo( );

				// Excel Cell Starts From 1
				int start = axis.getColumnIndexByCoordinate( rule
						.getStartCoordinate( ) ) + 1;
				int end = axis.getColumnIndexByCoordinate( rule
						.getEndCoordinate( ) ) + 1;

				end = Math.min( end, MAX_COLUMN );
				int scount = Math.max( 0, end - start - 1 );
				// Excel Span Starts From 1
				Span span = new Span( start, scount );
				data.setSpan( span );
			}
		}
	}

	/**
	 * 
	 */
	public void resetContainers( )
	{
		for ( XlsContainer container : containers )
		{
			container.setRowIndex( 0 );
			container.setStartRowId( 0 );
		}
		for ( XlsTable table : tables )
		{
			table.setRowIndex( 0 );
		}
	}

	public ExcelLayoutEngineIterator getIterator( )
	{
		return new ExcelLayoutEngineIterator( );
	}

	private class ExcelLayoutEngineIterator implements Iterator<RowData>
	{

		Iterator<Data[]> rowIterator;

		public ExcelLayoutEngineIterator( )
		{
			rowIterator = cache.getRowIterator( );
		}

		public boolean hasNext( )
		{
			return rowIterator.hasNext( );
		}

		public RowData next( )
		{
			Data[] row = rowIterator.next( );
			List<Data> data = new ArrayList<Data>( );
			int width = Math.min( row.length, MAX_COLUMN - 1 );
			double rowHeight = DEFAULT_ROW_HEIGHT;
			for ( int i = 0; i < width; i++ )
			{
				Data d = row[i];
				if ( d == null || d.isBlank( ) )
				{
					continue;
				}

				if ( d.isProcessed( ) )
				{
					continue;
				}

				d.setProcessed( true );
				data.add( row[i] );
			}

			Data[] rowdata = new Data[data.size( )];
			data.toArray( rowdata );
			return new RowData( rowdata, rowHeight );
		}

		public void remove( )
		{
			throw new UnsupportedOperationException( );
		}
	}
	
}